package com.ds.migration.feign.authentication.service;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.ds.migration.feign.authentication.domain.AuthenticationRequest;
import com.ds.migration.feign.authentication.domain.AuthenticationResponse;

public interface MigrationAuthenticationService {

    @PostMapping(value = "/migration/authentication/token")
    ResponseEntity<AuthenticationResponse> requestJWTUserToken(
            @RequestBody AuthenticationRequest authenticationRequest);
}