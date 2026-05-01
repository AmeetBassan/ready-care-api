package com.readycare.api.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "professional_profiles")
public class ProfessionalProfile {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "overall_verification_status", nullable = false, columnDefinition = "verification_status")
    private VerificationStatus overallVerificationStatus = VerificationStatus.NOT_SUBMITTED;

    private String bio;

    @Column(name = "years_experience")
    private Integer yearsExperience;

    @Column(name = "hourly_rate_office_hours", precision = 10, scale = 2)
    private BigDecimal hourlyRateOfficeHours;

    @Column(name = "hourly_rate_out_of_office_hours", precision = 10, scale = 2)
    private BigDecimal hourlyRateOutOfOfficeHours;

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public VerificationStatus getOverallVerificationStatus() {
        return overallVerificationStatus;
    }

    public void setOverallVerificationStatus(VerificationStatus overallVerificationStatus) {
        this.overallVerificationStatus = overallVerificationStatus;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public Integer getYearsExperience() {
        return yearsExperience;
    }

    public void setYearsExperience(Integer yearsExperience) {
        this.yearsExperience = yearsExperience;
    }

    public BigDecimal getHourlyRateOfficeHours() {
        return hourlyRateOfficeHours;
    }

    public void setHourlyRateOfficeHours(BigDecimal hourlyRateOfficeHours) {
        this.hourlyRateOfficeHours = hourlyRateOfficeHours;
    }

    public BigDecimal getHourlyRateOutOfOfficeHours() {
        return hourlyRateOutOfOfficeHours;
    }

    public void setHourlyRateOutOfOfficeHours(BigDecimal hourlyRateOutOfOfficeHours) {
        this.hourlyRateOutOfOfficeHours = hourlyRateOutOfOfficeHours;
    }
}
