package com.readycare.api.service;

import com.readycare.api.dto.AddressRequest;
import com.readycare.api.dto.CreateAdminRequest;
import com.readycare.api.dto.UserResponse;
import com.readycare.api.entity.Address;
import com.readycare.api.entity.AdminProfile;
import com.readycare.api.entity.GenderType;
import com.readycare.api.entity.User;
import com.readycare.api.entity.UserType;
import com.readycare.api.exception.BadRequestException;
import com.readycare.api.repository.AddressRepository;
import com.readycare.api.repository.AdminProfileRepository;
import com.readycare.api.repository.ClientProfileRepository;
import com.readycare.api.repository.ProfessionalDocumentRepository;
import com.readycare.api.repository.ProfessionalProfileRepository;
import com.readycare.api.repository.UserRepository;
import com.readycare.api.service.storage.AzureBlobObjectStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private AddressRepository addressRepository;
    @Mock
    private ProfessionalProfileRepository professionalProfileRepository;
    @Mock
    private ClientProfileRepository clientProfileRepository;
    @Mock
    private AdminProfileRepository adminProfileRepository;
    @Mock
    private ProfessionalDocumentRepository professionalDocumentRepository;
    @Mock
    private AzureBlobObjectStorageService objectStorageService;
    @Mock
    private HashService hashService;

    @InjectMocks
    private AccountService accountService;

    @Test
    void createAdmin_createsUserAddressAndAdminProfile() {
        CreateAdminRequest request = new CreateAdminRequest(
                "Jane",
                "Admin",
                GenderType.FEMALE,
                LocalDate.of(1990, 1, 10),
                "jane.admin@example.com",
                "447700000001",
                "admin-pass",
                new AddressRequest("Home", "1 Test St", "", "London", "E1 1AA", "GB")
        );

        UUID userId = UUID.randomUUID();
        UUID addressId = UUID.randomUUID();

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());
        when(hashService.sha256("admin-pass")).thenReturn("hashed-pass");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            if (user.getId() == null) {
                user.setId(userId);
            }
            return user;
        });
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> {
            Address address = invocation.getArgument(0);
            address.setId(addressId);
            return address;
        });

        UserResponse response = accountService.createAdmin(request);

        assertThat(response.id()).isEqualTo(userId);
        assertThat(response.type()).isEqualTo(UserType.ADMIN);
        assertThat(response.email()).isEqualTo("jane.admin@example.com");
        assertThat(response.primaryAddressId()).isEqualTo(addressId);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, org.mockito.Mockito.times(2)).save(userCaptor.capture());
        User savedUser = userCaptor.getAllValues().getFirst();
        assertThat(savedUser.getType()).isEqualTo(UserType.ADMIN);
        assertThat(savedUser.getPasswordHash()).isEqualTo("hashed-pass");

        ArgumentCaptor<AdminProfile> profileCaptor = ArgumentCaptor.forClass(AdminProfile.class);
        verify(adminProfileRepository).save(profileCaptor.capture());
        assertThat(profileCaptor.getValue().getUser().getId()).isEqualTo(userId);
    }

    @Test
    void createAdmin_throwsWhenEmailAlreadyExists() {
        CreateAdminRequest request = new CreateAdminRequest(
                "Jane",
                "Admin",
                GenderType.FEMALE,
                LocalDate.of(1990, 1, 10),
                "taken@example.com",
                "447700000001",
                "admin-pass",
                new AddressRequest("Home", "1 Test St", "", "London", "E1 1AA", "GB")
        );

        User existing = new User();
        existing.setEmail("taken@example.com");
        when(userRepository.findByEmail("taken@example.com")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> accountService.createAdmin(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Email already exists");
    }
}
