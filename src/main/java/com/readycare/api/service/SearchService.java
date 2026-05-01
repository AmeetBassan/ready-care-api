package com.readycare.api.service;

import com.readycare.api.dto.ProfessionalSearchResponse;
import com.readycare.api.entity.AvailabilitySlot;
import com.readycare.api.entity.AvailabilitySlotStatus;
import com.readycare.api.entity.ProfessionalProfile;
import com.readycare.api.entity.VerificationStatus;
import com.readycare.api.repository.AvailabilitySlotRepository;
import com.readycare.api.repository.ProfessionalProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class SearchService {

    private final ProfessionalProfileRepository professionalProfileRepository;
    private final AvailabilitySlotRepository availabilitySlotRepository;

    public SearchService(
            ProfessionalProfileRepository professionalProfileRepository,
            AvailabilitySlotRepository availabilitySlotRepository
    ) {
        this.professionalProfileRepository = professionalProfileRepository;
        this.availabilitySlotRepository = availabilitySlotRepository;
    }

    @Transactional(readOnly = true)
    public List<ProfessionalSearchResponse> search(String city, OffsetDateTime startTs, int durationHours) {
        if (durationHours <= 0) {
            return List.of();
        }
        OffsetDateTime endTs = startTs.plusHours(durationHours);

        return professionalProfileRepository.findByOverallVerificationStatus(VerificationStatus.APPROVED)
                .stream()
                .filter(profile -> profile.getUser().getPrimaryAddress() != null)
                .filter(profile -> profile.getUser().getPrimaryAddress().getCity().equalsIgnoreCase(city))
                .map(profile -> {
                    List<AvailabilitySlot> availableSlots = availabilitySlotRepository
                            .findByProfessionalIdAndStartTsGreaterThanEqualAndStartTsLessThanAndStatus(
                                    profile.getUserId(),
                                    startTs,
                                    endTs,
                                    AvailabilitySlotStatus.FREE
                            );
                    int availableHours = countContiguousAvailableHours(availableSlots, startTs, durationHours);
                    return new ProfessionalSearchResponse(
                            profile.getUserId(),
                            profile.getUser().getFirstName(),
                            profile.getUser().getLastName(),
                            profile.getUser().getPrimaryAddress().getCity(),
                            profile.getHourlyRateOfficeHours(),
                            availableHours
                    );
                })
                .filter(item -> item.availableHours() >= durationHours)
                .toList();
    }

    private int countContiguousAvailableHours(List<AvailabilitySlot> slots, OffsetDateTime startTs, int durationHours) {
        Set<OffsetDateTime> slotStartTimes = new HashSet<>();
        for (AvailabilitySlot slot : slots) {
            slotStartTimes.add(slot.getStartTs());
        }

        int contiguous = 0;
        for (int i = 0; i < durationHours; i++) {
            OffsetDateTime requiredStart = startTs.plusHours(i);
            if (!slotStartTimes.contains(requiredStart)) {
                break;
            }
            contiguous++;
        }
        return contiguous;
    }
}
