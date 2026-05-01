package com.readycare.api.repository;

import com.readycare.api.entity.ProfessionalProfile;
import com.readycare.api.entity.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProfessionalProfileRepository extends JpaRepository<ProfessionalProfile, UUID> {
    List<ProfessionalProfile> findByOverallVerificationStatus(VerificationStatus status);
}
