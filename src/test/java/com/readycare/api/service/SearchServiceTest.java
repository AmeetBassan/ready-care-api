package com.readycare.api.service;

import com.readycare.api.dto.ProfessionalSearchResponse;
import com.readycare.api.entity.Address;
import com.readycare.api.entity.AvailabilitySlot;
import com.readycare.api.entity.AvailabilitySlotStatus;
import com.readycare.api.entity.ProfessionalProfile;
import com.readycare.api.entity.User;
import com.readycare.api.entity.VerificationStatus;
import com.readycare.api.repository.AvailabilitySlotRepository;
import com.readycare.api.repository.ProfessionalProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock
    private ProfessionalProfileRepository professionalProfileRepository;
    @Mock
    private AvailabilitySlotRepository availabilitySlotRepository;

    @InjectMocks
    private SearchService searchService;

    @Test
    void search_includesProfessionalWhenWindowEndsAtAvailabilityBoundary() {
        UUID professionalId = UUID.randomUUID();
        User professional = new User();
        professional.setId(professionalId);
        professional.setFirstName("Alex");
        professional.setLastName("Care");

        Address address = new Address();
        address.setCity("London");
        professional.setPrimaryAddress(address);

        ProfessionalProfile profile = new ProfessionalProfile();
        profile.setUserId(professionalId);
        profile.setUser(professional);
        profile.setOverallVerificationStatus(VerificationStatus.APPROVED);
        profile.setHourlyRateOfficeHours(BigDecimal.valueOf(30));

        OffsetDateTime searchStart = OffsetDateTime.parse("2026-06-10T10:00:00Z");
        AvailabilitySlot s10 = new AvailabilitySlot();
        s10.setStartTs(searchStart);
        s10.setStatus(AvailabilitySlotStatus.FREE);
        AvailabilitySlot s11 = new AvailabilitySlot();
        s11.setStartTs(searchStart.plusHours(1));
        s11.setStatus(AvailabilitySlotStatus.FREE);

        when(professionalProfileRepository.findByOverallVerificationStatus(VerificationStatus.APPROVED))
                .thenReturn(List.of(profile));
        when(availabilitySlotRepository.findByProfessionalIdAndStartTsGreaterThanEqualAndStartTsLessThanAndStatus(
                any(UUID.class), any(OffsetDateTime.class), any(OffsetDateTime.class), any(AvailabilitySlotStatus.class)))
                .thenReturn(List.of(s10, s11));

        List<ProfessionalSearchResponse> result = searchService.search("London", searchStart, 2);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().professionalId()).isEqualTo(professionalId);
        assertThat(result.getFirst().availableHours()).isEqualTo(2);
    }
}
