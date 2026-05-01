package com.readycare.api.service;

import com.readycare.api.dto.*;
import com.readycare.api.entity.*;
import com.readycare.api.exception.BadRequestException;
import com.readycare.api.exception.NotFoundException;
import com.readycare.api.repository.*;
import com.readycare.api.service.storage.AzureBlobObjectStorageService;
import com.readycare.api.service.storage.StoredObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
public class AccountService {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final ProfessionalProfileRepository professionalProfileRepository;
    private final ClientProfileRepository clientProfileRepository;
    private final AdminProfileRepository adminProfileRepository;
    private final ProfessionalDocumentRepository professionalDocumentRepository;
    private final AzureBlobObjectStorageService objectStorageService;
    private final HashService hashService;

    public AccountService(
            UserRepository userRepository,
            AddressRepository addressRepository,
            ProfessionalProfileRepository professionalProfileRepository,
            ClientProfileRepository clientProfileRepository,
            AdminProfileRepository adminProfileRepository,
            ProfessionalDocumentRepository professionalDocumentRepository,
            AzureBlobObjectStorageService objectStorageService,
            HashService hashService
    ) {
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
        this.professionalProfileRepository = professionalProfileRepository;
        this.clientProfileRepository = clientProfileRepository;
        this.adminProfileRepository = adminProfileRepository;
        this.professionalDocumentRepository = professionalDocumentRepository;
        this.objectStorageService = objectStorageService;
        this.hashService = hashService;
    }

    @Transactional
    public ProfessionalResponse createProfessional(CreateProfessionalRequest request) {
        validateEmail(request.email());

        User user = new User();
        user.setType(UserType.PROFESSIONAL);
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setGender(request.gender());
        user.setDob(request.dob());
        user.setEmail(request.email());
        user.setPhoneNumber(request.phoneNumber());
        user.setPasswordHash(hashService.sha256(request.password()));
        user = userRepository.save(user);

        Address address = mapAddress(request.primaryAddress(), user);
        address = addressRepository.save(address);
        user.setPrimaryAddress(address);
        userRepository.save(user);

        ProfessionalProfile profile = new ProfessionalProfile();
        profile.setUser(user);
        profile.setBio(request.bio());
        profile.setYearsExperience(request.yearsExperience());
        profile.setHourlyRateOfficeHours(request.hourlyRateOfficeHours());
        profile.setHourlyRateOutOfOfficeHours(request.hourlyRateOutOfOfficeHours());
        professionalProfileRepository.save(profile);

        return toProfessionalResponse(user, profile);
    }

    @Transactional
    public ProfessionalResponse updateProfessional(UUID professionalId, UpdateProfessionalRequest request) {
        User user = getUserByType(professionalId, UserType.PROFESSIONAL);
        ProfessionalProfile profile = professionalProfileRepository.findById(professionalId)
                .orElseThrow(() -> new NotFoundException("Professional profile not found"));

        if (request.firstName() != null) {
            user.setFirstName(request.firstName());
        }
        if (request.lastName() != null) {
            user.setLastName(request.lastName());
        }
        if (request.phoneNumber() != null) {
            user.setPhoneNumber(request.phoneNumber());
        }
        if (request.bio() != null) {
            profile.setBio(request.bio());
        }
        if (request.yearsExperience() != null) {
            profile.setYearsExperience(request.yearsExperience());
        }
        if (request.hourlyRateOfficeHours() != null) {
            profile.setHourlyRateOfficeHours(request.hourlyRateOfficeHours());
        }
        if (request.hourlyRateOutOfOfficeHours() != null) {
            profile.setHourlyRateOutOfOfficeHours(request.hourlyRateOutOfOfficeHours());
        }
        if (request.city() != null && user.getPrimaryAddress() != null) {
            user.getPrimaryAddress().setCity(request.city());
            addressRepository.save(user.getPrimaryAddress());
        }

        userRepository.save(user);
        professionalProfileRepository.save(profile);
        return toProfessionalResponse(user, profile);
    }

    @Transactional(readOnly = true)
    public List<ProfessionalResponse> getProfessionals() {
        return professionalProfileRepository.findAll()
                .stream()
                .map(profile -> toProfessionalResponse(profile.getUser(), profile))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProfessionalResponse> getPendingProfessionals() {
        return professionalProfileRepository.findByOverallVerificationStatus(VerificationStatus.PENDING_REVIEW)
                .stream()
                .map(profile -> toProfessionalResponse(profile.getUser(), profile))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UserDetailResponse> getUsers(UserType type) {
        List<User> users = type == null ? userRepository.findAll() : userRepository.findByType(type);
        return users.stream()
                .map(this::toUserDetailResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserDetailResponse getUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        return toUserDetailResponse(user);
    }

    @Transactional
    public UserDetailResponse patchUser(UUID userId, PatchUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (request.firstName() != null) {
            user.setFirstName(request.firstName());
        }
        if (request.lastName() != null) {
            user.setLastName(request.lastName());
        }
        if (request.phoneNumber() != null) {
            user.setPhoneNumber(request.phoneNumber());
        }
        if (request.city() != null && user.getPrimaryAddress() != null) {
            user.getPrimaryAddress().setCity(request.city());
            addressRepository.save(user.getPrimaryAddress());
        }

        if (request.professionalProfile() != null) {
            if (user.getType() != UserType.PROFESSIONAL) {
                throw new BadRequestException("Professional profile can only be updated for professional users");
            }
            patchProfessionalProfile(user.getId(), request.professionalProfile());
        }

        userRepository.save(user);
        return toUserDetailResponse(user);
    }

    @Transactional
    public UserDetailResponse updateUserStatus(UUID userId, UpdateUserStatusRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        user.setActive(request.active());
        return toUserDetailResponse(userRepository.save(user));
    }

    @Transactional
    public ProfessionalResponse submitProfessionalApplication(UUID professionalId) {
        User user = getUserByType(professionalId, UserType.PROFESSIONAL);
        ProfessionalProfile profile = professionalProfileRepository.findById(professionalId)
                .orElseThrow(() -> new NotFoundException("Professional profile not found"));

        List<ProfessionalDocument> docs = professionalDocumentRepository.findByProfessionalId(professionalId);
        if (docs.isEmpty()) {
            throw new BadRequestException("At least one document must be uploaded before submitting application");
        }

        profile.setOverallVerificationStatus(VerificationStatus.PENDING_REVIEW);
        professionalProfileRepository.save(profile);
        return toProfessionalResponse(user, profile);
    }

    @Transactional
    public ProfessionalResponse approveProfessional(UUID professionalId) {
        User user = getUserByType(professionalId, UserType.PROFESSIONAL);
        ProfessionalProfile profile = professionalProfileRepository.findById(professionalId)
                .orElseThrow(() -> new NotFoundException("Professional profile not found"));
        profile.setOverallVerificationStatus(VerificationStatus.APPROVED);
        professionalProfileRepository.save(profile);
        return toProfessionalResponse(user, profile);
    }

    @Transactional
    public ProfessionalResponse rejectProfessional(UUID professionalId) {
        User user = getUserByType(professionalId, UserType.PROFESSIONAL);
        ProfessionalProfile profile = professionalProfileRepository.findById(professionalId)
                .orElseThrow(() -> new NotFoundException("Professional profile not found"));
        profile.setOverallVerificationStatus(VerificationStatus.REJECTED);
        professionalProfileRepository.save(profile);
        return toProfessionalResponse(user, profile);
    }

    @Transactional(readOnly = true)
    public ProfessionalResponse getProfessional(UUID professionalId) {
        User user = getUserByType(professionalId, UserType.PROFESSIONAL);
        ProfessionalProfile profile = professionalProfileRepository.findById(professionalId)
                .orElseThrow(() -> new NotFoundException("Professional profile not found"));
        return toProfessionalResponse(user, profile);
    }

    @Transactional(readOnly = true)
    public AddressResponse getProfessionalPrimaryAddress(UUID professionalId) {
        User user = getUserByType(professionalId, UserType.PROFESSIONAL);
        if (user.getPrimaryAddress() == null) {
            throw new NotFoundException("Primary address not found for professional");
        }
        return toAddressResponse(user.getPrimaryAddress());
    }

    @Transactional
    public void deleteProfessional(UUID professionalId) {
        User user = getUserByType(professionalId, UserType.PROFESSIONAL);
        List<ProfessionalDocument> docs = professionalDocumentRepository.findByProfessionalId(professionalId);
        for (ProfessionalDocument doc : docs) {
            objectStorageService.deleteObject(doc.getFileStorageKey());
        }
        userRepository.delete(user);
    }

    @Transactional
    public UserResponse createClient(CreateClientRequest request) {
        validateEmail(request.email());

        User user = new User();
        user.setType(UserType.CLIENT);
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setGender(request.gender());
        user.setDob(request.dob());
        user.setEmail(request.email());
        user.setPhoneNumber(request.phoneNumber());
        user.setPasswordHash(hashService.sha256(request.password()));
        user = userRepository.save(user);

        Address address = mapAddress(request.primaryAddress(), user);
        address = addressRepository.save(address);
        user.setPrimaryAddress(address);
        userRepository.save(user);

        ClientProfile profile = new ClientProfile();
        profile.setUser(user);
        clientProfileRepository.save(profile);

        return toUserResponse(user);
    }

    @Transactional
    public UserResponse updateClient(UUID clientId, UpdateClientRequest request) {
        User user = getUserByType(clientId, UserType.CLIENT);

        if (request.firstName() != null) {
            user.setFirstName(request.firstName());
        }
        if (request.lastName() != null) {
            user.setLastName(request.lastName());
        }
        if (request.phoneNumber() != null) {
            user.setPhoneNumber(request.phoneNumber());
        }
        if (request.city() != null && user.getPrimaryAddress() != null) {
            user.getPrimaryAddress().setCity(request.city());
            addressRepository.save(user.getPrimaryAddress());
        }

        return toUserResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse createAdmin(CreateAdminRequest request) {
        validateEmail(request.email());

        User user = new User();
        user.setType(UserType.ADMIN);
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setGender(request.gender());
        user.setDob(request.dob());
        user.setEmail(request.email());
        user.setPhoneNumber(request.phoneNumber());
        user.setPasswordHash(hashService.sha256(request.password()));
        user = userRepository.save(user);

        Address address = mapAddress(request.primaryAddress(), user);
        address = addressRepository.save(address);
        user.setPrimaryAddress(address);
        userRepository.save(user);

        AdminProfile profile = new AdminProfile();
        profile.setUser(user);
        adminProfileRepository.save(profile);

        return toUserResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getClient(UUID clientId) {
        User user = getUserByType(clientId, UserType.CLIENT);
        return toUserResponse(user);
    }

    @Transactional
    public void deleteClient(UUID clientId) {
        User user = getUserByType(clientId, UserType.CLIENT);
        userRepository.delete(user);
    }

    @Transactional
    public UserProfilePictureResponse uploadProfilePicture(UUID userId, MultipartFile file) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Profile picture file is required");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException ex) {
            throw new BadRequestException("Unable to read uploaded file");
        }

        String key = "users/%s/profile-picture/%s-%s".formatted(
                user.getId(),
                UUID.randomUUID(),
                sanitizeFileName(file.getOriginalFilename())
        );
        String newKey = objectStorageService.putProfilePictureObject(key, bytes, file.getContentType());

        if (user.getProfilePictureStorageKey() != null && !user.getProfilePictureStorageKey().isBlank()) {
            objectStorageService.deleteProfilePictureObject(user.getProfilePictureStorageKey());
        }

        user.setProfilePictureStorageKey(newKey);
        userRepository.save(user);
        return new UserProfilePictureResponse(newKey);
    }

    @Transactional(readOnly = true)
    public UserProfilePictureFileResponse getProfilePicture(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (user.getProfilePictureStorageKey() == null || user.getProfilePictureStorageKey().isBlank()) {
            throw new NotFoundException("Profile picture not found");
        }
        StoredObject object = objectStorageService.getProfilePictureObject(user.getProfilePictureStorageKey());
        return new UserProfilePictureFileResponse(
                object.bytes(),
                object.contentType(),
                extractFileName(user.getProfilePictureStorageKey())
        );
    }

    public User getUserByType(UUID userId, UserType type) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (user.getType() != type) {
            throw new BadRequestException("User is not of expected type " + type);
        }
        return user;
    }

    public ProfessionalProfile getProfessionalProfile(UUID professionalId) {
        return professionalProfileRepository.findById(professionalId)
                .orElseThrow(() -> new NotFoundException("Professional profile not found"));
    }

    public UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getType(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getPrimaryAddress() != null ? user.getPrimaryAddress().getId() : null,
                user.getPrimaryAddress() != null ? user.getPrimaryAddress().getCity() : null
        );
    }

    public ProfessionalResponse toProfessionalResponse(User user, ProfessionalProfile profile) {
        return new ProfessionalResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getPrimaryAddress() != null ? user.getPrimaryAddress().getId() : null,
                user.getPrimaryAddress() != null ? user.getPrimaryAddress().getCity() : null,
                profile.getBio(),
                profile.getYearsExperience(),
                profile.getHourlyRateOfficeHours(),
                profile.getHourlyRateOutOfOfficeHours(),
                profile.getOverallVerificationStatus()
        );
    }

    public UserDetailResponse toUserDetailResponse(User user) {
        return new UserDetailResponse(
                user.getId(),
                user.getType(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.isActive(),
                user.getPrimaryAddress() != null ? user.getPrimaryAddress().getId() : null,
                user.getPrimaryAddress() != null ? user.getPrimaryAddress().getCity() : null,
                user.getType() == UserType.PROFESSIONAL ? toProfessionalProfileResponse(user.getId()) : null,
                user.getType() == UserType.CLIENT ? new ClientProfileResponse() : null,
                user.getType() == UserType.ADMIN ? new AdminProfileResponse() : null
        );
    }

    private ProfessionalProfileResponse toProfessionalProfileResponse(UUID professionalId) {
        ProfessionalProfile profile = professionalProfileRepository.findById(professionalId)
                .orElseThrow(() -> new NotFoundException("Professional profile not found"));
        return new ProfessionalProfileResponse(
                profile.getBio(),
                profile.getYearsExperience(),
                profile.getHourlyRateOfficeHours(),
                profile.getHourlyRateOutOfOfficeHours(),
                profile.getOverallVerificationStatus()
        );
    }

    private void patchProfessionalProfile(UUID professionalId, PatchProfessionalProfileRequest request) {
        ProfessionalProfile profile = professionalProfileRepository.findById(professionalId)
                .orElseThrow(() -> new NotFoundException("Professional profile not found"));
        if (request.bio() != null) {
            profile.setBio(request.bio());
        }
        if (request.yearsExperience() != null) {
            profile.setYearsExperience(request.yearsExperience());
        }
        if (request.hourlyRateOfficeHours() != null) {
            profile.setHourlyRateOfficeHours(request.hourlyRateOfficeHours());
        }
        if (request.hourlyRateOutOfOfficeHours() != null) {
            profile.setHourlyRateOutOfOfficeHours(request.hourlyRateOutOfOfficeHours());
        }
        professionalProfileRepository.save(profile);
    }

    private AddressResponse toAddressResponse(Address address) {
        return new AddressResponse(
                address.getId(),
                address.getLabel(),
                address.getLine1(),
                address.getLine2(),
                address.getCity(),
                address.getPostcode(),
                address.getCountry()
        );
    }

    private void validateEmail(String email) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new BadRequestException("Email already exists");
        }
    }

    private Address mapAddress(AddressRequest request, User user) {
        Address address = new Address();
        address.setUser(user);
        address.setLabel(request.label());
        address.setLine1(request.line1());
        address.setLine2(request.line2());
        address.setCity(request.city());
        address.setPostcode(request.postcode());
        address.setCountry(request.country() == null || request.country().isBlank() ? "GB" : request.country());
        return address;
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "profile-picture";
        }
        return fileName.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private String extractFileName(String storageKey) {
        int index = storageKey.lastIndexOf('/');
        return index >= 0 ? storageKey.substring(index + 1) : storageKey;
    }
}
