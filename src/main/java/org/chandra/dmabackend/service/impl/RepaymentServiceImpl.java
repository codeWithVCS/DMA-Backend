package org.chandra.dmabackend.service.impl;

import jakarta.transaction.Transactional;
import org.chandra.dmabackend.dto.response.ForeclosureResponse;
import org.chandra.dmabackend.dto.response.PartPaymentResponse;
import org.chandra.dmabackend.dto.response.PayEmiResponse;
import org.chandra.dmabackend.dto.response.RepaymentHistoryResponse;
import org.chandra.dmabackend.model.*;
import org.chandra.dmabackend.repository.EmiScheduleRepository;
import org.chandra.dmabackend.repository.LoanRepository;
import org.chandra.dmabackend.repository.PaymentRepository;
import org.chandra.dmabackend.service.EmiScheduleGeneratorService;
import org.chandra.dmabackend.service.RepaymentService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class RepaymentServiceImpl implements RepaymentService {

    private final EmiScheduleRepository emiScheduleRepository;
    private final LoanRepository loanRepository;
    private final PaymentRepository paymentRepository;
    private final EmiScheduleGeneratorService emiScheduleGeneratorService;

    public RepaymentServiceImpl(EmiScheduleRepository emiScheduleRepository,
                                LoanRepository loanRepository,
                                PaymentRepository paymentRepository, EmiScheduleGeneratorService emiScheduleGeneratorService) {
        this.emiScheduleRepository = emiScheduleRepository;
        this.loanRepository = loanRepository;
        this.paymentRepository = paymentRepository;
        this.emiScheduleGeneratorService = emiScheduleGeneratorService;
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

    @Override
    @Transactional
    public PartPaymentResponse partPayment(Long loanId, Long userId, BigDecimal amountPaid) {

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Loan not found"));

        if (!loan.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized access");
        }

        if (!loan.getPartPaymentAllowed()) {
            throw new IllegalArgumentException("Part-payment not allowed for this loan");
        }

        if ("CLOSED".equalsIgnoreCase(loan.getStatus()) ||
                "FORECLOSED".equalsIgnoreCase(loan.getStatus())) {
            throw new IllegalArgumentException("Loan is already closed");
        }

        if (amountPaid == null || amountPaid.compareTo(BigDecimal.valueOf(1000)) < 0) {
            throw new IllegalArgumentException("Amount too low for part-payment");
        }

        BigDecimal oldPrincipal = loan.getPrincipal();
        BigDecimal newPrincipal = oldPrincipal.subtract(amountPaid);

        if (newPrincipal.compareTo(BigDecimal.ZERO) < 0) {
            newPrincipal = BigDecimal.ZERO;
        }

        loan.setPrincipal(newPrincipal);

        List<EmiSchedule> schedule =
                emiScheduleRepository.findByLoanOrderByMonthIndexAsc(loan);

        EmiSchedule firstPendingEmi = schedule.stream()
                .filter(emi -> emi.getStatus() == EmiScheduleStatus.PENDING)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No pending EMI entries"));

        LocalDate nextDueDate = firstPendingEmi.getDueDate();

        List<EmiSchedule> pendingEmis =
                emiScheduleRepository.findByLoanAndStatusOrderByMonthIndexAsc(loan, EmiScheduleStatus.PENDING);

        emiScheduleRepository.deleteAll(pendingEmis);

        Loan loanView = new Loan();
        loanView.setUser(loan.getUser());
        loanView.setLoanName(loan.getLoanName());
        loanView.setCategory(loan.getCategory());
        loanView.setLender(loan.getLender());
        loanView.setPrincipal(newPrincipal);
        loanView.setInterestRate(loan.getInterestRate());
        loanView.setEmiAmount(loan.getEmiAmount());
        loanView.setTenureMonths(loan.getTenureMonths());
        loanView.setEmiStartDate(nextDueDate);
        loanView.setStartDate(loan.getStartDate());
        loanView.setForeclosureAllowed(loan.getForeclosureAllowed());
        loanView.setForeclosurePenaltyPercent(loan.getForeclosurePenaltyPercent());
        loanView.setPartPaymentAllowed(loan.getPartPaymentAllowed());
        loanView.setStatus(loan.getStatus());

        List<EmiSchedule> newSchedule = emiScheduleGeneratorService.generateSchedule(loanView);

        for (EmiSchedule emi : newSchedule) {
            emi.setLoan(loan);
        }

        emiScheduleRepository.saveAll(newSchedule);

        if (newPrincipal.compareTo(BigDecimal.ZERO) == 0) {
            loan.setStatus("CLOSED");
        }

        loanRepository.save(loan);

        Payment payment = new Payment();
        payment.setLoan(loan);
        payment.setPaymentDate(LocalDate.now());
        payment.setAmountPaid(amountPaid);
        payment.setAllocatedToInterest(BigDecimal.ZERO);
        payment.setAllocatedToPrincipal(amountPaid);
        payment.setOutstandingAfterPayment(newPrincipal);
        payment.setPaymentType(PaymentType.PART_PAYMENT);
        payment.setRemarks("Part payment made");

        paymentRepository.save(payment);

        PartPaymentResponse response = new PartPaymentResponse();
        response.setOldPrincipal(oldPrincipal);
        response.setNewPrincipal(newPrincipal);
        response.setAmountPaid(amountPaid);
        response.setEmiRowsRecalculated(newSchedule.size());
        response.setLoanStatus(loan.getStatus());

        return response;
    }

    @Override
    @Transactional
    public ForeclosureResponse forecloseLoan(Long loanId, Long userId, BigDecimal amountPaid) {

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Loan not found"));

        if (!loan.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized access");
        }

        if (!loan.getForeclosureAllowed()) {
            throw new IllegalArgumentException("Foreclosure not allowed for this loan");
        }

        if ("CLOSED".equalsIgnoreCase(loan.getStatus()) ||
                "FORECLOSED".equalsIgnoreCase(loan.getStatus())) {
            throw new IllegalArgumentException("Loan already closed or foreclosed");
        }

        BigDecimal principalOutstanding = loan.getPrincipal();
        BigDecimal penaltyPercent = loan.getForeclosurePenaltyPercent() == null
                ? BigDecimal.ZERO
                : loan.getForeclosurePenaltyPercent();

        BigDecimal penaltyAmount =
                principalOutstanding.multiply(penaltyPercent)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        BigDecimal totalAmountRequired =
                principalOutstanding.add(penaltyAmount);

        if (amountPaid.compareTo(totalAmountRequired) < 0) {
            throw new IllegalArgumentException("Insufficient amount for foreclosure");
        }

        List<EmiSchedule> pendingEmis =
                emiScheduleRepository.findByLoanAndStatusOrderByMonthIndexAsc(
                        loan, EmiScheduleStatus.PENDING);

        for (EmiSchedule emi : pendingEmis) {
            emi.setStatus(EmiScheduleStatus.FORECLOSED);
        }
        emiScheduleRepository.saveAll(pendingEmis);

        loan.setPrincipal(BigDecimal.ZERO);
        loan.setStatus("FORECLOSED");
        loanRepository.save(loan);

        Payment payment = new Payment();
        payment.setLoan(loan);
        payment.setPaymentDate(LocalDate.now());
        payment.setAmountPaid(amountPaid);
        payment.setAllocatedToInterest(BigDecimal.ZERO);
        payment.setAllocatedToPrincipal(principalOutstanding);
        payment.setOutstandingAfterPayment(BigDecimal.ZERO);
        payment.setPaymentType(PaymentType.FORECLOSURE);
        payment.setRemarks("Loan foreclosed with penalty of " + penaltyAmount);

        paymentRepository.save(payment);

        ForeclosureResponse response = new ForeclosureResponse();
        response.setPrincipalOutstanding(principalOutstanding);
        response.setPenaltyApplied(penaltyAmount);
        response.setTotalAmountRequired(totalAmountRequired);
        response.setAmountPaid(amountPaid);
        response.setStatus(loan.getStatus());
        response.setPendingEmiCountClosed(pendingEmis.size());

        return response;
    }

    @Override
    public List<RepaymentHistoryResponse> getRepaymentHistory(Long loanId, Long userId) {

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Loan not found"));

        if (!loan.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized access");
        }

        List<Payment> payments = paymentRepository.findByLoanOrderByPaymentDateAsc(loan);

        List<RepaymentHistoryResponse> repaymentHistories = new ArrayList<>();

        for(Payment payment : payments) {

            RepaymentHistoryResponse response = new RepaymentHistoryResponse();
            response.setPaymentId(payment.getId());
            response.setPaymentDate(payment.getPaymentDate());
            response.setAmountPaid(payment.getAmountPaid());
            response.setAllocatedToInterest(payment.getAllocatedToInterest());
            response.setAllocatedToPrincipal(payment.getAllocatedToPrincipal());
            response.setOutstandingAfterPayment(payment.getOutstandingAfterPayment());
            response.setPaymentType(payment.getPaymentType().toString());
            response.setRemarks(payment.getRemarks());

            repaymentHistories.add(response);

        }

        return repaymentHistories;
    }
}
