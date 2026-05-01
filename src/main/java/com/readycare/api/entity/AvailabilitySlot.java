package com.readycare.api.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "availability_slots")
public class AvailabilitySlot extends AuditableEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "professional_id", nullable = false)
    private User professional;

    @Column(name = "start_ts", nullable = false)
    private OffsetDateTime startTs;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "availability_slot_status")
    private AvailabilitySlotStatus status;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public User getProfessional() {
        return professional;
    }

    public void setProfessional(User professional) {
        this.professional = professional;
    }

    public OffsetDateTime getStartTs() {
        return startTs;
    }

    public void setStartTs(OffsetDateTime startTs) {
        this.startTs = startTs;
    }

    public AvailabilitySlotStatus getStatus() {
        return status;
    }

    public void setStatus(AvailabilitySlotStatus status) {
        this.status = status;
    }
}
