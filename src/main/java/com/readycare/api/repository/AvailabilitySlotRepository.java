package com.readycare.api.repository;

import com.readycare.api.entity.AvailabilitySlot;
import com.readycare.api.entity.AvailabilitySlotStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AvailabilitySlotRepository extends JpaRepository<AvailabilitySlot, UUID> {
    Optional<AvailabilitySlot> findByProfessionalIdAndStartTs(UUID professionalId, OffsetDateTime startTs);

    List<AvailabilitySlot> findByProfessionalIdAndStartTsBetween(UUID professionalId, OffsetDateTime from, OffsetDateTime to);

    List<AvailabilitySlot> findByProfessionalIdAndStartTsBetweenAndStatus(UUID professionalId, OffsetDateTime from, OffsetDateTime to, AvailabilitySlotStatus status);

    List<AvailabilitySlot> findByProfessionalIdAndStartTsGreaterThanEqualAndStartTsLessThanAndStatus(
            UUID professionalId,
            OffsetDateTime fromInclusive,
            OffsetDateTime toExclusive,
            AvailabilitySlotStatus status
    );
}
