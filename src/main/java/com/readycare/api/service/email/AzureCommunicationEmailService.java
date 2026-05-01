package com.readycare.api.service.email;

import com.azure.communication.email.EmailClient;
import com.azure.communication.email.EmailClientBuilder;
import com.azure.communication.email.models.EmailAddress;
import com.azure.communication.email.models.EmailMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
public class AzureCommunicationEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(AzureCommunicationEmailService.class);

    private final String senderAddress;
    private final EmailClient emailClient;

    public AzureCommunicationEmailService(
            @Value("${app.email.connection-string:}") String connectionString,
            @Value("${app.email.sender-address:}") String senderAddress
    ) {
        Assert.hasText(connectionString, "app.email.connection-string must be configured");
        Assert.hasText(senderAddress, "app.email.sender-address must be configured");
        this.senderAddress = senderAddress;
        this.emailClient = new EmailClientBuilder()
                .connectionString(connectionString)
                .buildClient();
    }

    @Override
    public void sendEmail(String to, String subject, String plainTextBody) {
        try {
            EmailMessage message = new EmailMessage()
                    .setSenderAddress(senderAddress)
                    .setToRecipients(new EmailAddress(to))
                    .setSubject(subject)
                    .setBodyPlainText(plainTextBody);
            emailClient.beginSend(message);
        } catch (Exception ex) {
            log.warn("Failed to send email to {}: {}", to, ex.getMessage());
        }
    }
}
