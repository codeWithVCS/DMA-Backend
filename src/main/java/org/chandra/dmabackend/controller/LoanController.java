package org.chandra.dmabackend.controller;

import jakarta.validation.Valid;
import org.chandra.dmabackend.dto.request.ExistingLoanRequest;
import org.chandra.dmabackend.dto.request.NewLoanRequest;
import org.chandra.dmabackend.dto.response.EmiScheduleResponse;
import org.chandra.dmabackend.dto.response.LoanHealthResponse;
import org.chandra.dmabackend.dto.response.LoanResponse;
import org.chandra.dmabackend.model.Loan;
import org.chandra.dmabackend.model.User;
import org.chandra.dmabackend.repository.LoanRepository;
import org.chandra.dmabackend.repository.UserRepository;
import org.chandra.dmabackend.service.LoanService;
import org.chandra.dmabackend.service.LoanStatusManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class LoanController {

    private final UserRepository userRepository;
    private final LoanService loanService;
    private final LoanRepository loanRepository;
    private final LoanStatusManager loanStatusManager;

    public LoanController(UserRepository userRepository,LoanRepository loanRepository, LoanService loanService, LoanStatusManager loanStatusManager) {
        this.userRepository = userRepository;
        this.loanService = loanService;
        this.loanRepository = loanRepository;
        this.loanStatusManager = loanStatusManager;
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

    @GetMapping("/api/loans/{loanId}/schedule")
    private ResponseEntity<List<EmiScheduleResponse>> getEmiSchedule(@AuthenticationPrincipal UserDetails user,
                                                     @PathVariable Long loanId){
        User dbUser = userRepository.findByEmail(user.getUsername())
                .orElseThrow(()->new IllegalArgumentException("Invalid Credentials"));

        List<EmiScheduleResponse> responses = loanService.getSchedule(loanId, dbUser.getId());

        return ResponseEntity.ok(responses);

    }

    @GetMapping("/api/loans/{loanId}/health")
    public ResponseEntity<LoanHealthResponse> getLoanHealth(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long loanId) {

        User dbUser = userRepository.findByEmail(user.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Invalid Credentials"));

        LoanHealthResponse response =
                loanStatusManager.evaluateLoanHealth(loanId, dbUser.getId());

        return ResponseEntity.ok(response);
    }


}
