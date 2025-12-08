package org.chandra.dmabackend.controller;

import org.chandra.dmabackend.dto.request.ForeclosureRequest;
import org.chandra.dmabackend.dto.request.PartPaymentRequest;
import org.chandra.dmabackend.dto.response.ForeclosureResponse;
import org.chandra.dmabackend.dto.response.PartPaymentResponse;
import org.chandra.dmabackend.dto.request.PayEmiRequest;
import org.chandra.dmabackend.dto.response.PayEmiResponse;
import org.chandra.dmabackend.model.User;
import org.chandra.dmabackend.repository.UserRepository;
import org.chandra.dmabackend.service.RepaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RepaymentController {

    private final UserRepository userRepository;
    private final RepaymentService repaymentService;

    public RepaymentController(UserRepository userRepository, RepaymentService repaymentService) {
        this.userRepository = userRepository;
        this.repaymentService = repaymentService;
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

}
