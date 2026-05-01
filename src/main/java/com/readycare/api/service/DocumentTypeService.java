package com.readycare.api.service;

import com.readycare.api.dto.CreateDocumentTypeRequest;
import com.readycare.api.dto.DocumentTypeResponse;
import com.readycare.api.dto.PatchDocumentTypeRequest;
import com.readycare.api.entity.DocumentType;
import com.readycare.api.exception.BadRequestException;
import com.readycare.api.exception.NotFoundException;
import com.readycare.api.repository.DocumentTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentTypeService {

    private final DocumentTypeRepository documentTypeRepository;

    public DocumentTypeService(DocumentTypeRepository documentTypeRepository) {
        this.documentTypeRepository = documentTypeRepository;
    }

    @Transactional(readOnly = true)
    public List<DocumentTypeResponse> getDocumentTypes() {
        return documentTypeRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(DocumentType::getName, String.CASE_INSENSITIVE_ORDER))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public DocumentTypeResponse createDocumentType(CreateDocumentTypeRequest request) {
        validateUniqueName(request.name(), null);

        DocumentType documentType = new DocumentType();
        documentType.setName(request.name());
        documentType.setDescription(request.description());
        documentType.setRequired(request.required());
        documentType.setHasExpiry(request.hasExpiry());

        return toResponse(documentTypeRepository.save(documentType));
    }

    @Transactional
    public DocumentTypeResponse patchDocumentType(UUID documentTypeId, PatchDocumentTypeRequest request) {
        DocumentType documentType = documentTypeRepository.findById(documentTypeId)
                .orElseThrow(() -> new NotFoundException("Document type not found"));

        if (request.name() != null) {
            validateUniqueName(request.name(), documentTypeId);
            documentType.setName(request.name());
        }
        if (request.description() != null) {
            documentType.setDescription(request.description());
        }
        if (request.required() != null) {
            documentType.setRequired(request.required());
        }
        if (request.hasExpiry() != null) {
            documentType.setHasExpiry(request.hasExpiry());
        }

        return toResponse(documentTypeRepository.save(documentType));
    }

    private void validateUniqueName(String name, UUID currentDocumentTypeId) {
        documentTypeRepository.findByNameIgnoreCase(name)
                .filter(existing -> currentDocumentTypeId == null || !existing.getId().equals(currentDocumentTypeId))
                .ifPresent(existing -> {
                    throw new BadRequestException("Document type name already exists");
                });
    }

    private DocumentTypeResponse toResponse(DocumentType documentType) {
        return new DocumentTypeResponse(
                documentType.getId(),
                documentType.getName(),
                documentType.getDescription(),
                documentType.isRequired(),
                documentType.isHasExpiry()
        );
    }
}
