package com.readycare.api.repository;

import com.readycare.api.entity.ClientProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ClientProfileRepository extends JpaRepository<ClientProfile, UUID> {
}
