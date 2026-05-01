package com.readycare.api.repository;

import com.readycare.api.entity.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DocumentTypeRepository extends JpaRepository<DocumentType, UUID> {
    Optional<DocumentType> findByNameIgnoreCase(String name);
}
