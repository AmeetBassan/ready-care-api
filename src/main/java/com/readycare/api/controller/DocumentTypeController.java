package com.readycare.api.controller;

import com.readycare.api.dto.CreateDocumentTypeRequest;
import com.readycare.api.dto.DocumentTypeResponse;
import com.readycare.api.dto.PatchDocumentTypeRequest;
import com.readycare.api.service.DocumentTypeService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/document-types")
public class DocumentTypeController {

    private final DocumentTypeService documentTypeService;

    public DocumentTypeController(DocumentTypeService documentTypeService) {
        this.documentTypeService = documentTypeService;
    }

    @GetMapping
    public List<DocumentTypeResponse> getDocumentTypes() {
        return documentTypeService.getDocumentTypes();
    }

    @PostMapping
    public DocumentTypeResponse createDocumentType(@Valid @RequestBody CreateDocumentTypeRequest request) {
        return documentTypeService.createDocumentType(request);
    }

    @PatchMapping("/{documentTypeId}")
    public DocumentTypeResponse patchDocumentType(
            @PathVariable UUID documentTypeId,
            @RequestBody PatchDocumentTypeRequest request
    ) {
        return documentTypeService.patchDocumentType(documentTypeId, request);
    }
}
