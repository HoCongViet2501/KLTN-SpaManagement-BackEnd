package com.se.kltn.spamanagement.controller;

import com.se.kltn.spamanagement.dto.request.AccountRequest;
import com.se.kltn.spamanagement.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.Arrays;
import java.util.TimeZone;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "login user")
    public ResponseEntity<Object> login(@RequestParam("username") String username, @RequestParam("password") String password) {
        return ResponseEntity.ok().body(this.authService.login(username, password));
    }

    @PostMapping("/register-customer")
    @Operation(summary = "register new account for customer")
    public ResponseEntity<String> registerCustomer(@Valid @RequestBody AccountRequest accountRequest, @RequestParam("email") String email, HttpServletRequest request) throws MessagingException {
        this.authService.registerCustomer(accountRequest, email, getSiteURL(request));
        return ResponseEntity.ok().body("Register successfully");
    }

    @GetMapping("/verify")
    @Operation(summary = "verify account")
    public ResponseEntity<String> verify(@RequestParam("code") String verificationCode) {
        if (this.authService.verify(verificationCode)) {
            return ResponseEntity.ok().body("Verify successfully");
        } else {
            return ResponseEntity.badRequest().body("Error: Couldn't verify email");
        }
    }

    private String getSiteURL(HttpServletRequest request) {
        String siteURL = request.getRequestURL().toString();
        return siteURL.replace(request.getServletPath(), "");
    }


    @PostMapping("/logout")
    @Operation(summary = "logout account")
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        this.authService.logout(request, response);
    }
}
