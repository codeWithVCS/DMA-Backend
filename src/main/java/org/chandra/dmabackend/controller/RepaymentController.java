package org.chandra.dmabackend.controller;

import org.chandra.dmabackend.dto.request.ForeclosureRequest;
import org.chandra.dmabackend.dto.request.MarkPaidRequest;
import org.chandra.dmabackend.dto.request.PartPaymentRequest;
import org.chandra.dmabackend.dto.response.*;
import org.chandra.dmabackend.dto.request.PayEmiRequest;
import org.chandra.dmabackend.model.Loan;
import org.chandra.dmabackend.model.User;
import org.chandra.dmabackend.repository.LoanRepository;
import org.chandra.dmabackend.repository.UserRepository;
import org.chandra.dmabackend.service.RepaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class RepaymentController {

    private final UserRepository userRepository;
    private final RepaymentService repaymentService;
    private final LoanRepository loanRepository;

    public RepaymentController(UserRepository userRepository, RepaymentService repaymentService, LoanRepository loanRepository) {
        this.userRepository = userRepository;
        this.repaymentService = repaymentService;
        this.loanRepository = loanRepository;
    }

    @PostMapping("/api/repayment/emi/{emiId}")
    public ResponseEntity<PayEmiResponse> payEmi(@AuthenticationPrincipal UserDetails user,
                                                 @PathVariable Long emiId,
                                                 @RequestBody PayEmiRequest request){
        User dbUser = userRepository.findByEmail(user.getUsername())
                .orElseThrow(()->new IllegalArgumentException("Invalid Credentials"));

        PayEmiResponse response = repaymentService.payEmi(emiId, dbUser.getId(), request.getAmountPaid());

        return ResponseEntity.status(HttpStatus.OK).body(response);

    }

    @PostMapping("/api/repayment/part-payment/{loanId}")
    public ResponseEntity<PartPaymentResponse> makePartPayment(@AuthenticationPrincipal UserDetails user,
                                                               @PathVariable Long loanId,
                                                               @RequestBody PartPaymentRequest request){
        User dbUser =  userRepository.findByEmail(user.getUsername())
                .orElseThrow(()->new IllegalArgumentException("Invalid Credentials"));
        PartPaymentResponse response = repaymentService.partPayment(loanId, dbUser.getId(), request.getAmountPaid());

        return  ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/api/repayment/foreclose/{loanId}")
    public ResponseEntity<ForeclosureResponse> foreclose(@AuthenticationPrincipal UserDetails user,
                                                         @PathVariable Long loanId,
                                                         @RequestBody ForeclosureRequest request){
        User dbUSer = userRepository.findByEmail(user.getUsername())
                .orElseThrow(()->new IllegalArgumentException("Invalid Credentials"));
        ForeclosureResponse response = repaymentService.forecloseLoan(loanId, dbUSer.getId(), request.getAmountPaid());

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/api/repayment/history/{loanId}")
    public ResponseEntity<List<RepaymentHistoryResponse>> getHistory(@AuthenticationPrincipal UserDetails user,
                                                                     @PathVariable Long loanId){
        User dbUser = userRepository.findByEmail(user.getUsername())
                .orElseThrow(()->new IllegalArgumentException("Invalid Credentials"));
        List<RepaymentHistoryResponse> responses = repaymentService.getRepaymentHistory(loanId,dbUser.getId());

        return ResponseEntity.status(HttpStatus.OK).body(responses);
    }

    @PostMapping("/api/emi/{emiId}/mark-paid")
    public ResponseEntity<MarkPaidResponse> markPaid(@AuthenticationPrincipal UserDetails user,
                                                     @PathVariable Long emiId,
                                                     @RequestBody MarkPaidRequest request) {

        User dbUser = userRepository.findByEmail(user.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Invalid Credentials"));

        MarkPaidResponse response =
                repaymentService.markEmiPaid(emiId, dbUser.getId(), request.getActualPaymentDate());

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/api/emi/{emiId}/mark-missed")
    public ResponseEntity<MarkMissedResponse> markMissed(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long emiId) {

        User dbUser = userRepository.findByEmail(user.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Invalid Credentials"));

        MarkMissedResponse response =
                repaymentService.markEmiMissed(emiId, dbUser.getId());

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }


}
