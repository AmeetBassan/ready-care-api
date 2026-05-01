package com.readycare.api.controller;

import com.readycare.api.dto.PatchUserRequest;
import com.readycare.api.dto.ReviewResponse;
import com.readycare.api.dto.UpdateUserStatusRequest;
import com.readycare.api.dto.UserDetailResponse;
import com.readycare.api.dto.UserProfilePictureFileResponse;
import com.readycare.api.dto.UserProfilePictureResponse;
import com.readycare.api.entity.UserType;
import com.readycare.api.service.AccountService;
import com.readycare.api.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")
public class UserController {

    private final AccountService accountService;
    private final ReviewService reviewService;

    public UserController(AccountService accountService, ReviewService reviewService) {
        this.accountService = accountService;
        this.reviewService = reviewService;
    }

    // Reference: https://www.springframework.org/spring-security/reference/6.5/servlet/authentication/architecture.html
    @GetMapping("/me")
    public UserDetailResponse getMe(@AuthenticationPrincipal Jwt jwt) {
        return accountService.getUser(currentUserId(jwt));
    }

    // Reference: https://www.springframework.org/spring-security/reference/6.5/servlet/authentication/architecture.html
    @PatchMapping("/me")
    public UserDetailResponse patchMe(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody PatchUserRequest request
    ) {
        return accountService.patchUser(currentUserId(jwt), request);
    }

    @GetMapping("/me/reviews")
    public List<ReviewResponse> getMyReviews(@AuthenticationPrincipal Jwt jwt) {
        return reviewService.getMyReviews(currentUserId(jwt));
    }

    @PostMapping(value = "/me/profile-picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserProfilePictureResponse uploadProfilePicture(
            @AuthenticationPrincipal Jwt jwt,
            @RequestPart("file") MultipartFile file
    ) {
        return accountService.uploadProfilePicture(currentUserId(jwt), file);
    }

    @GetMapping("/me/profile-picture")
    public ResponseEntity<byte[]> getMyProfilePicture(@AuthenticationPrincipal Jwt jwt) {
        UserProfilePictureFileResponse file = accountService.getProfilePicture(currentUserId(jwt));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.fileName() + "\"")
                .body(file.bytes());
    }

    @GetMapping
    public List<UserDetailResponse> getUsers(@RequestParam(required = false) UserType type) {
        return accountService.getUsers(type);
    }

    @GetMapping("/{userId}")
    public UserDetailResponse getUser(@PathVariable UUID userId) {
        return accountService.getUser(userId);
    }

    @PatchMapping("/{userId}")
    public UserDetailResponse patchUser(
            @PathVariable UUID userId,
            @RequestBody PatchUserRequest request
    ) {
        return accountService.patchUser(userId, request);
    }

    @PatchMapping("/{userId}/status")
    public UserDetailResponse updateUserStatus(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserStatusRequest request
    ) {
        return accountService.updateUserStatus(userId, request);
    }

    @GetMapping("/{userId}/profile-picture")
    public ResponseEntity<byte[]> getUserProfilePicture(@PathVariable UUID userId) {
        UserProfilePictureFileResponse file = accountService.getProfilePicture(userId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.fileName() + "\"")
                .body(file.bytes());
    }

    private UUID currentUserId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
