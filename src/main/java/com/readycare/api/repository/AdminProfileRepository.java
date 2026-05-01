package com.readycare.api.repository;

import com.readycare.api.entity.AdminProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AdminProfileRepository extends JpaRepository<AdminProfile, UUID> {
}
