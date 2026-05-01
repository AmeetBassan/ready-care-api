package com.readycare.api.service.storage;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobDownloadContentResponse;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.models.BlobStorageException;
import com.readycare.api.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;

@Service
public class AzureBlobObjectStorageService {

    private final BlobContainerClient documentsContainerClient;
    private final BlobContainerClient profilePicturesContainerClient;

    public AzureBlobObjectStorageService(
            @Value("${app.storage.connection-string}") String connectionString,
            @Value("${app.storage.documents-container-name}") String documentsContainerName,
            @Value("${app.storage.profile-pictures-container-name}") String profilePicturesContainerName
    ) {
        BlobServiceClient serviceClient = new BlobServiceClientBuilder()
                .connectionString(connectionString)
                .buildClient();
        this.documentsContainerClient = createIfMissing(serviceClient, documentsContainerName);
        this.profilePicturesContainerClient = createIfMissing(serviceClient, profilePicturesContainerName);
    }

    public String putObject(String key, byte[] bytes, String contentType) {
        return putObject(documentsContainerClient, key, bytes, contentType);
    }

    public void deleteObject(String key) {
        deleteObject(documentsContainerClient, key);
    }

    public StoredObject getObject(String key) {
        return getObject(documentsContainerClient, key);
    }

    public String putProfilePictureObject(String key, byte[] bytes, String contentType) {
        return putObject(profilePicturesContainerClient, key, bytes, contentType);
    }

    public void deleteProfilePictureObject(String key) {
        deleteObject(profilePicturesContainerClient, key);
    }

    public StoredObject getProfilePictureObject(String key) {
        return getObject(profilePicturesContainerClient, key);
    }

    private BlobContainerClient createIfMissing(BlobServiceClient serviceClient, String containerName) {
        BlobContainerClient client = serviceClient.getBlobContainerClient(containerName);
        if (!client.exists()) {
            client.create();
        }
        return client;
    }

    private String putObject(BlobContainerClient containerClient, String key, byte[] bytes, String contentType) {
        BlobHttpHeaders headers = new BlobHttpHeaders()
                .setContentType(contentType == null || contentType.isBlank() ? "application/octet-stream" : contentType);

        containerClient.getBlobClient(key)
                .uploadWithResponse(new ByteArrayInputStream(bytes), bytes.length, null, headers, null, null, null, null, null);
        return key;
    }

    private void deleteObject(BlobContainerClient containerClient, String key) {
        containerClient.getBlobClient(key).deleteIfExists();
    }

    private StoredObject getObject(BlobContainerClient containerClient, String key) {
        BlobDownloadContentResponse response;
        try {
            response = containerClient.getBlobClient(key).downloadContentWithResponse(null, null, null, false, null, null);
        } catch (BlobStorageException ex) {
            if (ex.getStatusCode() == 404) {
                throw new NotFoundException("Stored object not found");
            }
            throw ex;
        }
        String contentType = response.getDeserializedHeaders() != null
                ? response.getDeserializedHeaders().getContentType()
                : null;
        return new StoredObject(
                response.getValue().toBytes(),
                contentType == null || contentType.isBlank() ? "application/octet-stream" : contentType
        );
    }
}
