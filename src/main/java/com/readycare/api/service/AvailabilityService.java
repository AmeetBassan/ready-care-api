package com.readycare.api.service;

import com.readycare.api.dto.AvailabilitySlotResponse;
import com.readycare.api.dto.AvailabilityRequest;
import com.readycare.api.dto.UpsertAvailabilityRequest;
import com.readycare.api.entity.AvailabilitySlot;
import com.readycare.api.entity.AvailabilitySlotStatus;
import com.readycare.api.entity.User;
import com.readycare.api.entity.UserType;
import com.readycare.api.exception.BadRequestException;
import com.readycare.api.repository.AvailabilitySlotRepository;
import com.readycare.api.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AvailabilityService {

    private final AvailabilitySlotRepository availabilitySlotRepository;
    private final UserRepository userRepository;

    public AvailabilityService(AvailabilitySlotRepository availabilitySlotRepository, UserRepository userRepository) {
        this.availabilitySlotRepository = availabilitySlotRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public List<AvailabilitySlotResponse> upsertAvailability(UUID professionalId, UpsertAvailabilityRequest request) {
        User professional = userRepository.findById(professionalId)
                .orElseThrow(() -> new BadRequestException("Professional not found"));
        if (professional.getType() != UserType.PROFESSIONAL) {
            throw new BadRequestException("User is not professional");
        }
        if (request.endHour() <= request.startHour()) {
            throw new BadRequestException("endHour must be greater than startHour");
        }

        List<AvailabilitySlotResponse> out = new ArrayList<>();
        for (int hour = request.startHour(); hour < request.endHour(); hour++) {
            OffsetDateTime slotTs = request.day().atTime(hour, 0).atOffset(ZoneOffset.UTC);
            AvailabilitySlot slot = availabilitySlotRepository
                    .findByProfessionalIdAndStartTs(professionalId, slotTs)
                    .orElseGet(AvailabilitySlot::new);

            slot.setProfessional(professional);
            slot.setStartTs(slotTs);
            slot.setStatus(request.status());
            slot = availabilitySlotRepository.save(slot);
            out.add(toResponse(slot));
        }
        return out;
    }

    @Transactional
    public List<AvailabilitySlotResponse> createAvailability(UUID professionalId, AvailabilityRequest request) {
        User professional = userRepository.findById(professionalId)
                .orElseThrow(() -> new BadRequestException("Professional not found"));
        if (professional.getType() != UserType.PROFESSIONAL) {
            throw new BadRequestException("User is not professional");
        }
        validateAvailabilityTimes(request.startTs(), request.endTs());

        long hours = Duration.between(request.startTs(), request.endTs()).toHours();
        List<AvailabilitySlotResponse> out = new ArrayList<>();
        for (int i = 0; i < hours; i++) {
            OffsetDateTime slotTs = request.startTs().plusHours(i);
            AvailabilitySlot slot = availabilitySlotRepository
                    .findByProfessionalIdAndStartTs(professionalId, slotTs)
                    .orElseGet(AvailabilitySlot::new);
            slot.setProfessional(professional);
            slot.setStartTs(slotTs);
            slot.setStatus(request.status());
            out.add(toResponse(availabilitySlotRepository.save(slot)));
        }
        return out;
    }

    public List<AvailabilitySlotResponse> listAvailability(UUID professionalId, OffsetDateTime from, OffsetDateTime to) {
        return availabilitySlotRepository.findByProfessionalIdAndStartTsBetween(professionalId, from, to)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void setSlotStatus(UUID professionalId, UUID slotId, AvailabilitySlotStatus status) {
        AvailabilitySlot slot = availabilitySlotRepository.findById(slotId)
                .orElseThrow(() -> new BadRequestException("Slot not found"));
        if (!slot.getProfessional().getId().equals(professionalId)) {
            throw new BadRequestException("Slot does not belong to professional");
        }
        slot.setStatus(status);
        availabilitySlotRepository.save(slot);
    }

    @Transactional
    public AvailabilitySlotResponse updateAvailabilitySlot(UUID professionalId, UUID slotId, AvailabilityRequest request) {
        AvailabilitySlot slot = availabilitySlotRepository.findById(slotId)
                .orElseThrow(() -> new BadRequestException("Slot not found"));
        if (!slot.getProfessional().getId().equals(professionalId)) {
            throw new BadRequestException("Slot does not belong to professional");
        }
        if (slot.getStatus() == AvailabilitySlotStatus.BOOKED) {
            throw new BadRequestException("Booked slots cannot be updated directly");
        }
        if (!request.endTs().equals(request.startTs().plusHours(1))) {
            throw new BadRequestException("A single availability slot must be exactly one hour");
        }
        validateAvailabilityTimes(request.startTs(), request.endTs());
        slot.setStartTs(request.startTs());
        slot.setStatus(request.status());
        return toResponse(availabilitySlotRepository.save(slot));
    }

    @Transactional
    public void deleteAvailabilitySlot(UUID professionalId, UUID slotId) {
        AvailabilitySlot slot = availabilitySlotRepository.findById(slotId)
                .orElseThrow(() -> new BadRequestException("Slot not found"));
        if (!slot.getProfessional().getId().equals(professionalId)) {
            throw new BadRequestException("Slot does not belong to professional");
        }
        if (slot.getStatus() == AvailabilitySlotStatus.BOOKED) {
            throw new BadRequestException("Booked slots cannot be deleted directly");
        }
        availabilitySlotRepository.delete(slot);
    }

    private AvailabilitySlotResponse toResponse(AvailabilitySlot slot) {
        return new AvailabilitySlotResponse(slot.getId(), slot.getStartTs(), slot.getStatus());
    }

    private void validateAvailabilityTimes(OffsetDateTime startTs, OffsetDateTime endTs) {
        if (!startTs.isBefore(endTs)) {
            throw new BadRequestException("End must be after start");
        }
        if (startTs.getMinute() != 0 || startTs.getSecond() != 0 || endTs.getMinute() != 0 || endTs.getSecond() != 0) {
            throw new BadRequestException("Availability times must be aligned on full hours");
        }
        if (Duration.between(startTs, endTs).getSeconds() % 3600 != 0) {
            throw new BadRequestException("Availability duration must be whole hours");
        }
    }
}
