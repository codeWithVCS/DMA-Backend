package org.chandra.dmabackend.service.impl;

import jakarta.transaction.Transactional;
import org.chandra.dmabackend.dto.response.PayEmiResponse;
import org.chandra.dmabackend.model.*;
import org.chandra.dmabackend.repository.EmiScheduleRepository;
import org.chandra.dmabackend.repository.LoanRepository;
import org.chandra.dmabackend.repository.PaymentRepository;
import org.chandra.dmabackend.service.RepaymentService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Service
@Transactional
public class RepaymentServiceImpl implements RepaymentService {

    private final EmiScheduleRepository emiScheduleRepository;
    private final LoanRepository loanRepository;
    private final PaymentRepository paymentRepository;

    public RepaymentServiceImpl(EmiScheduleRepository emiScheduleRepository,
                                LoanRepository loanRepository,
                                PaymentRepository paymentRepository) {
        this.emiScheduleRepository = emiScheduleRepository;
        this.loanRepository = loanRepository;
        this.paymentRepository = paymentRepository;
    }

    @Override
    @Transactional
    public PayEmiResponse payEmi(Long emiId, Long userId, BigDecimal amountPaid) {

        EmiSchedule emi = emiScheduleRepository.findById(emiId)
                .orElseThrow(() -> new IllegalArgumentException("EMI not found"));

        Loan loan = emi.getLoan();

        if(!loan.getUser().getId().equals(userId)){
            throw new IllegalArgumentException("Unauthorized access");
        }

        if("CLOSED".equalsIgnoreCase(loan.getStatus()) || "FORECLOSED".equalsIgnoreCase(loan.getStatus())){
            throw new IllegalArgumentException("Loan is already closed");
        }

        if(emi.getStatus() == EmiScheduleStatus.PAID || emi.getStatus() == EmiScheduleStatus.MISSED || emi.getStatus() == EmiScheduleStatus.FORECLOSED){
            throw new IllegalArgumentException("EMI is not payable");
        }

        BigDecimal emiAmount = emi.getEmiAmount();

        if(amountPaid == null){
            amountPaid = emiAmount;
        }

        if(amountPaid.compareTo(emiAmount) < 0){
            throw new IllegalArgumentException("Amount insufficient for EMI");
        }

        BigDecimal openingBalance = emi.getOpeningBalance();
        BigDecimal monthlyRate = loan.getInterestRate()
                .divide(BigDecimal.valueOf(1200), 34, RoundingMode.HALF_UP);

        BigDecimal interestComponent = openingBalance
                .multiply(monthlyRate)
                .setScale(2, BigDecimal.ROUND_HALF_UP);

        BigDecimal principalComponent = emiAmount
                .subtract(interestComponent)
                .setScale(2, BigDecimal.ROUND_HALF_UP);

        BigDecimal closingBalance = openingBalance.subtract(principalComponent);
        if(closingBalance.compareTo(BigDecimal.ZERO) < 0){
            closingBalance = BigDecimal.ZERO;
        }
        closingBalance = closingBalance.setScale(2, BigDecimal.ROUND_HALF_UP);

        emi.setInterestComponent(interestComponent);
        emi.setPrincipalComponent(principalComponent);
        emi.setClosingBalance(closingBalance);
        emi.setStatus(EmiScheduleStatus.PAID);

        emiScheduleRepository.save(emi);

        loan.setPrincipal(closingBalance);

        if(closingBalance.compareTo(BigDecimal.ZERO) == 0){
            loan.setStatus("CLOSED");
        }

        loanRepository.save(loan);

        Payment payment  = new Payment();
        payment.setLoan(loan);
        payment.setPaymentDate(LocalDate.now());
        payment.setAmountPaid(amountPaid);
        payment.setAllocatedToInterest(interestComponent);
        payment.setAllocatedToPrincipal(principalComponent);
        payment.setOutstandingAfterPayment(closingBalance);
        payment.setPaymentType(PaymentType.EMI);
        payment.setRemarks("EMI payment for month " + emi.getMonthIndex());

        paymentRepository.save(payment);

        PayEmiResponse response = new PayEmiResponse();
        response.setEmiId(emi.getId());
        response.setMonthIndex(emi.getMonthIndex());
        response.setOpeningBalance(openingBalance);
        response.setInterestComponent(interestComponent);
        response.setPrincipalComponent(principalComponent);
        response.setClosingBalance(closingBalance);
        response.setUpdatedLoanOutstanding(closingBalance);
        response.setLoanStatus(loan.getStatus());

        return response;
    }
}
