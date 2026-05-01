package com.readycare.api.service;

import com.readycare.api.dto.DocumentResponse;
import com.readycare.api.dto.DocumentFileResponse;
import com.readycare.api.dto.ReviewDocumentRequest;
import com.readycare.api.entity.*;
import com.readycare.api.exception.BadRequestException;
import com.readycare.api.exception.NotFoundException;
import com.readycare.api.repository.DocumentTypeRepository;
import com.readycare.api.repository.ProfessionalDocumentRepository;
import com.readycare.api.repository.ProfessionalProfileRepository;
import com.readycare.api.repository.UserRepository;
import com.readycare.api.service.storage.AzureBlobObjectStorageService;
import com.readycare.api.service.storage.StoredObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class VerificationService {

    private final ProfessionalDocumentRepository professionalDocumentRepository;
    private final DocumentTypeRepository documentTypeRepository;
    private final ProfessionalProfileRepository professionalProfileRepository;
    private final UserRepository userRepository;
    private final AzureBlobObjectStorageService objectStorageService;

    public VerificationService(
            ProfessionalDocumentRepository professionalDocumentRepository,
            DocumentTypeRepository documentTypeRepository,
            ProfessionalProfileRepository professionalProfileRepository,
            UserRepository userRepository,
            AzureBlobObjectStorageService objectStorageService
    ) {
        this.professionalDocumentRepository = professionalDocumentRepository;
        this.documentTypeRepository = documentTypeRepository;
        this.professionalProfileRepository = professionalProfileRepository;
        this.userRepository = userRepository;
        this.objectStorageService = objectStorageService;
    }

    @Transactional
    public DocumentResponse uploadDocument(UUID professionalId, UUID documentTypeId, MultipartFile file, String expiryDate) {
        User professional = userRepository.findById(professionalId)
                .orElseThrow(() -> new NotFoundException("Professional not found"));
        if (professional.getType() != UserType.PROFESSIONAL) {
            throw new BadRequestException("User is not a professional");
        }

        DocumentType documentType = documentTypeRepository.findById(documentTypeId)
                .orElseThrow(() -> new NotFoundException("Document type not found"));

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BadRequestException("Unable to read uploaded file");
        }

        String key = "professionals/%s/documents/%s-%s".formatted(
                professionalId,
                UUID.randomUUID(),
                sanitizeFileName(file.getOriginalFilename())
        );
        String fileStorageKey = objectStorageService.putObject(key, bytes, file.getContentType());

        ProfessionalDocument doc = new ProfessionalDocument();
        doc.setProfessional(professional);
        doc.setDocumentType(documentType);
        doc.setFileStorageKey(fileStorageKey);
        doc.setStatus(VerificationStatus.PENDING_REVIEW);
        if (expiryDate != null && !expiryDate.isBlank()) {
            doc.setExpiryDate(java.time.LocalDate.parse(expiryDate));
        }

        doc = professionalDocumentRepository.save(doc);
        recalculateProfessionalStatus(professionalId);
        return toResponse(doc);
    }

    @Transactional
    public DocumentResponse reviewDocument(UUID documentId, ReviewDocumentRequest request) {
        ProfessionalDocument doc = professionalDocumentRepository.findById(documentId)
                .orElseThrow(() -> new NotFoundException("Document not found"));

        User admin = userRepository.findById(request.adminId())
                .orElseThrow(() -> new NotFoundException("Admin user not found"));

        if (admin.getType() != UserType.ADMIN) {
            throw new BadRequestException("Reviewer must be an admin");
        }

        doc.setReviewedBy(admin);
        doc.setReviewedAt(OffsetDateTime.now());

        VerificationStatus status = Boolean.TRUE.equals(request.approve())
                ? VerificationStatus.APPROVED
                : VerificationStatus.REJECTED;
        doc = updateDocumentStatus(doc, status, request.reason());

        if (status == VerificationStatus.APPROVED) {
            objectStorageService.deleteObject(doc.getFileStorageKey());
        }
        return toResponse(doc);
    }

    @Transactional
    public void recalculateProfessionalStatus(UUID professionalId) {
        ProfessionalProfile profile = professionalProfileRepository.findById(professionalId)
                .orElseThrow(() -> new NotFoundException("Professional profile not found"));

        List<ProfessionalDocument> docs = professionalDocumentRepository.findByProfessionalId(professionalId);
        List<DocumentType> required = documentTypeRepository.findAll().stream()
                .filter(DocumentType::isRequired)
                .toList();

        boolean hasRejected = docs.stream().anyMatch(d -> d.getStatus() == VerificationStatus.REJECTED);
        boolean hasPending = docs.stream().anyMatch(d -> d.getStatus() == VerificationStatus.PENDING_REVIEW);

        boolean allRequiredApproved = required.stream().allMatch(req -> docs.stream().anyMatch(d ->
                d.getDocumentType().getId().equals(req.getId()) && d.getStatus() == VerificationStatus.APPROVED));

        if (allRequiredApproved) {
            profile.setOverallVerificationStatus(VerificationStatus.APPROVED);
        } else if (hasRejected) {
            profile.setOverallVerificationStatus(VerificationStatus.REJECTED);
        } else if (hasPending) {
            profile.setOverallVerificationStatus(VerificationStatus.PENDING_REVIEW);
        } else {
            profile.setOverallVerificationStatus(VerificationStatus.NOT_SUBMITTED);
        }

        professionalProfileRepository.save(profile);
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> getDocuments(UUID professionalId) {
        return professionalDocumentRepository.findByProfessionalId(professionalId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private DocumentResponse toResponse(ProfessionalDocument doc) {
        return new DocumentResponse(
                doc.getId(),
                doc.getProfessional().getId(),
                doc.getDocumentType().getId(),
                doc.getDocumentType().getName(),
                doc.getFileStorageKey(),
                doc.getStatus(),
                doc.getExpiryDate(),
                doc.getRejectionReason()
        );
    }

    @Transactional(readOnly = true)
    public DocumentFileResponse getDocumentFileForProfessional(UUID professionalId, UUID documentId) {
        ProfessionalDocument doc = professionalDocumentRepository.findById(documentId)
                .orElseThrow(() -> new NotFoundException("Document not found"));
        if (!doc.getProfessional().getId().equals(professionalId)) {
            throw new BadRequestException("Document does not belong to professional");
        }
        return toFileResponse(doc);
    }

    @Transactional(readOnly = true)
    public DocumentFileResponse getDocumentFileForAdmin(UUID documentId) {
        ProfessionalDocument doc = professionalDocumentRepository.findById(documentId)
                .orElseThrow(() -> new NotFoundException("Document not found"));
        return toFileResponse(doc);
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "upload";
        }
        return fileName.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private ProfessionalDocument updateDocumentStatus(
            ProfessionalDocument doc,
            VerificationStatus status,
            String rejectionReason
    ) {
        doc.setStatus(status);
        doc.setRejectionReason(status == VerificationStatus.REJECTED ? rejectionReason : null);
        doc = professionalDocumentRepository.save(doc);
        recalculateProfessionalStatus(doc.getProfessional().getId());
        return doc;
    }

    private DocumentFileResponse toFileResponse(ProfessionalDocument doc) {
        StoredObject object = objectStorageService.getObject(doc.getFileStorageKey());
        return new DocumentFileResponse(
                object.bytes(),
                object.contentType(),
                extractFileName(doc.getFileStorageKey())
        );
    }

    private String extractFileName(String fileStorageKey) {
        if (fileStorageKey == null || fileStorageKey.isBlank()) {
            return "document";
        }
        int idx = fileStorageKey.lastIndexOf('/');
        return idx >= 0 ? fileStorageKey.substring(idx + 1) : fileStorageKey;
    }
}
