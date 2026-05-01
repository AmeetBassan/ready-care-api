package com.readycare.api.repository;

import com.readycare.api.entity.ProfessionalDocument;
import com.readycare.api.entity.VerificationStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProfessionalDocumentRepository extends JpaRepository<ProfessionalDocument, UUID> {
    @EntityGraph(attributePaths = {"professional", "documentType"})
    List<ProfessionalDocument> findByProfessionalId(UUID professionalId);

    @Override
    @EntityGraph(attributePaths = {"professional", "documentType"})
    Optional<ProfessionalDocument> findById(UUID id);

    long countByProfessionalIdAndStatus(UUID professionalId, VerificationStatus status);
}
