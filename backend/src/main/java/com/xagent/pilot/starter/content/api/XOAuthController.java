package com.xagent.pilot.starter.content.api;

import com.xagent.pilot.starter.content.infrastructure.x.XOAuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/x/oauth")
public class XOAuthController {

    private final XOAuthService oauthService;

    public XOAuthController(
            XOAuthService oauthService
    ) {
        this.oauthService = oauthService;
    }

    @GetMapping("/connect")
    public ResponseEntity<Void> connect() {

        return ResponseEntity
                .status(HttpStatus.FOUND)
                .location(
                        oauthService
                                .createAuthorizationUri()
                )
                .build();
    }

    @GetMapping("/callback")
    public XOAuthService.ConnectionStatus callback(
            @RequestParam String code,
            @RequestParam String state
    ) {

        return oauthService
                .completeAuthorization(
                        code,
                        state
                );
    }

    @GetMapping("/status")
    public XOAuthService.ConnectionStatus status() {

        return oauthService.status();
    }
}