package com.readycare.api.service.email;

public interface EmailService {
    void sendEmail(String to, String subject, String plainTextBody);
}
