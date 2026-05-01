package com.readycare.api.controller;

import com.readycare.api.dto.UpdateClientRequest;
import com.readycare.api.dto.UserResponse;
import com.readycare.api.service.AccountService;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/clients")
public class ClientController {

    private final AccountService accountService;

    public ClientController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/{clientId}")
    public UserResponse getClient(@PathVariable UUID clientId) {
        return accountService.getClient(clientId);
    }

    @PutMapping("/{clientId}")
    public UserResponse updateClient(@PathVariable UUID clientId, @RequestBody UpdateClientRequest request) {
        return accountService.updateClient(clientId, request);
    }

    @DeleteMapping("/{clientId}")
    public void deleteClient(@PathVariable UUID clientId) {
        accountService.deleteClient(clientId);
    }


}
