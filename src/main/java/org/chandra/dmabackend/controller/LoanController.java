package org.chandra.dmabackend.controller;

import jakarta.validation.Valid;
import org.chandra.dmabackend.dto.request.ExistingLoanRequest;
import org.chandra.dmabackend.dto.request.NewLoanRequest;
import org.chandra.dmabackend.dto.response.LoanResponse;
import org.chandra.dmabackend.model.User;
import org.chandra.dmabackend.repository.UserRepository;
import org.chandra.dmabackend.service.LoanService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LoanController {

    private final UserRepository userRepository;
    private final LoanService loanService;

    public LoanController(UserRepository userRepository, LoanService loanService) {
        this.userRepository = userRepository;
        this.loanService = loanService;
    }

    @PostMapping("/api/loans/new")
    public ResponseEntity<LoanResponse> createNewLoan(@AuthenticationPrincipal UserDetails user,
                                                      @Valid @RequestBody NewLoanRequest request){
        User dbUser = userRepository.findByEmail(user.getUsername())
                .orElseThrow(()->new IllegalArgumentException("Invalid Credentials"));

        LoanResponse response = loanService.createNewLoan(request, dbUser.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);

    }

    @PostMapping("/api/loans/existing")
    public ResponseEntity<LoanResponse> createExistingLoan(@AuthenticationPrincipal UserDetails user,
                                                           @Valid @RequestBody ExistingLoanRequest request){
        User dbUser = userRepository.findByEmail(user.getUsername())
                .orElseThrow(()->new IllegalArgumentException("Invalid Credentials"));
        LoanResponse response = loanService.createExistingLoan(request, dbUser.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

}
