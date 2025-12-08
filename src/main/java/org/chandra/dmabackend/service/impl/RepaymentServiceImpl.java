package org.chandra.dmabackend.service.impl;

import jakarta.transaction.Transactional;
import org.chandra.dmabackend.dto.response.*;
import org.chandra.dmabackend.model.*;
import org.chandra.dmabackend.repository.EmiScheduleRepository;
import org.chandra.dmabackend.repository.LoanRepository;
import org.chandra.dmabackend.repository.PaymentRepository;
import org.chandra.dmabackend.service.EmiScheduleGeneratorService;
import org.chandra.dmabackend.service.LoanStatusManager;
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
    private final LoanStatusManager loanStatusManager;

    public RepaymentServiceImpl(EmiScheduleRepository emiScheduleRepository,
                                LoanRepository loanRepository,
                                PaymentRepository paymentRepository,
                                EmiScheduleGeneratorService emiScheduleGeneratorService,
                                LoanStatusManager loanStatusManager) {
        this.emiScheduleRepository = emiScheduleRepository;
        this.loanRepository = loanRepository;
        this.paymentRepository = paymentRepository;
        this.emiScheduleGeneratorService = emiScheduleGeneratorService;
        this.loanStatusManager = loanStatusManager;
    }

    @Override
    @Transactional
    public PayEmiResponse payEmi(Long emiId, Long userId, BigDecimal amountPaid) {

        EmiSchedule emi = emiScheduleRepository.findById(emiId)
                .orElseThrow(() -> new IllegalArgumentException("EMI not found"));

        Loan loan = emi.getLoan();

        if (!loan.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized access");
        }

        if ("CLOSED".equalsIgnoreCase(loan.getStatus()) ||
                "FORECLOSED".equalsIgnoreCase(loan.getStatus())) {
            throw new IllegalArgumentException("Loan already closed");
        }

        if (emi.getStatus() != EmiScheduleStatus.PENDING) {
            throw new IllegalArgumentException("EMI cannot be paid");
        }

        BigDecimal emiAmount = emi.getEmiAmount();

        if (amountPaid == null) amountPaid = emiAmount;

        if (amountPaid.compareTo(emiAmount) < 0) {
            throw new IllegalArgumentException("Insufficient EMI payment");
        }

        BigDecimal openingBalance = emi.getOpeningBalance();
        BigDecimal monthlyRate = loan.getInterestRate()
                .divide(BigDecimal.valueOf(1200), 34, RoundingMode.HALF_UP);

        BigDecimal interestComponent = openingBalance.multiply(monthlyRate)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal principalComponent = emiAmount.subtract(interestComponent)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal closingBalance = openingBalance.subtract(principalComponent);
        if (closingBalance.compareTo(BigDecimal.ZERO) < 0) closingBalance = BigDecimal.ZERO;
        closingBalance = closingBalance.setScale(2, RoundingMode.HALF_UP);

        emi.setInterestComponent(interestComponent);
        emi.setPrincipalComponent(principalComponent);
        emi.setClosingBalance(closingBalance);
        emi.setStatus(EmiScheduleStatus.PAID);
        emiScheduleRepository.save(emi);

        loan.setPrincipal(closingBalance);
        loanRepository.save(loan);

        Payment p = new Payment();
        p.setLoan(loan);
        p.setPaymentDate(LocalDate.now());
        p.setAmountPaid(amountPaid);
        p.setAllocatedToInterest(interestComponent);
        p.setAllocatedToPrincipal(principalComponent);
        p.setOutstandingAfterPayment(closingBalance);
        p.setPaymentType(PaymentType.EMI);
        p.setRemarks("EMI payment for month " + emi.getMonthIndex());
        paymentRepository.save(p);

        loanStatusManager.updateLoanStatus(loan);

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
            throw new IllegalArgumentException("Part-payment not allowed");
        }

        if ("CLOSED".equalsIgnoreCase(loan.getStatus()) ||
                "FORECLOSED".equalsIgnoreCase(loan.getStatus())) {
            throw new IllegalArgumentException("Loan closed");
        }

        if (amountPaid == null || amountPaid.compareTo(BigDecimal.valueOf(1000)) < 0) {
            throw new IllegalArgumentException("Amount too low for part-payment");
        }

        BigDecimal oldPrincipal = loan.getPrincipal();
        BigDecimal newPrincipal = oldPrincipal.subtract(amountPaid);
        if (newPrincipal.compareTo(BigDecimal.ZERO) < 0) newPrincipal = BigDecimal.ZERO;

        loan.setPrincipal(newPrincipal);

        List<EmiSchedule> schedule =
                emiScheduleRepository.findByLoanOrderByMonthIndexAsc(loan);

        EmiSchedule firstPendingEmi = schedule.stream()
                .filter(e -> e.getStatus() == EmiScheduleStatus.PENDING)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No pending EMIs"));

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

        for (EmiSchedule e : newSchedule) e.setLoan(loan);
        emiScheduleRepository.saveAll(newSchedule);

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

        loanStatusManager.updateLoanStatus(loan);

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
            throw new IllegalArgumentException("Unauthorized");
        }

        if (!loan.getForeclosureAllowed()) {
            throw new IllegalArgumentException("Foreclosure not allowed");
        }

        if ("CLOSED".equalsIgnoreCase(loan.getStatus()) ||
                "FORECLOSED".equalsIgnoreCase(loan.getStatus())) {
            throw new IllegalArgumentException("Loan already resolved");
        }

        BigDecimal principalOutstanding = loan.getPrincipal();
        BigDecimal penaltyPercent = loan.getForeclosurePenaltyPercent() == null
                ? BigDecimal.ZERO
                : loan.getForeclosurePenaltyPercent();

        BigDecimal penaltyAmount = principalOutstanding
                .multiply(penaltyPercent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        BigDecimal totalRequired = principalOutstanding.add(penaltyAmount);

        if (amountPaid.compareTo(totalRequired) < 0) {
            throw new IllegalArgumentException("Insufficient foreclosure amount");
        }

        List<EmiSchedule> pending =
                emiScheduleRepository.findByLoanAndStatusOrderByMonthIndexAsc(loan, EmiScheduleStatus.PENDING);

        for (EmiSchedule e : pending) {
            e.setStatus(EmiScheduleStatus.FORECLOSED);
        }
        emiScheduleRepository.saveAll(pending);

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
        payment.setRemarks("Loan foreclosed with penalty " + penaltyAmount);
        paymentRepository.save(payment);

        loanStatusManager.updateLoanStatus(loan);

        ForeclosureResponse resp = new ForeclosureResponse();
        resp.setPrincipalOutstanding(principalOutstanding);
        resp.setPenaltyApplied(penaltyAmount);
        resp.setTotalAmountRequired(totalRequired);
        resp.setAmountPaid(amountPaid);
        resp.setStatus(loan.getStatus());
        resp.setPendingEmiCountClosed(pending.size());

        return resp;
    }

    @Override
    public List<RepaymentHistoryResponse> getRepaymentHistory(Long loanId, Long userId) {

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Loan not found"));

        if (!loan.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized");
        }

        List<Payment> payments = paymentRepository.findByLoanOrderByPaymentDateAsc(loan);
        List<RepaymentHistoryResponse> list = new ArrayList<>();

        for (Payment p : payments) {
            RepaymentHistoryResponse r = new RepaymentHistoryResponse();
            r.setPaymentId(p.getId());
            r.setPaymentDate(p.getPaymentDate());
            r.setAmountPaid(p.getAmountPaid());
            r.setAllocatedToInterest(p.getAllocatedToInterest());
            r.setAllocatedToPrincipal(p.getAllocatedToPrincipal());
            r.setOutstandingAfterPayment(p.getOutstandingAfterPayment());
            r.setPaymentType(p.getPaymentType().toString());
            r.setRemarks(p.getRemarks());
            list.add(r);
        }

        return list;
    }

    @Override
    @Transactional
    public MarkPaidResponse markEmiPaid(Long emiId, Long userId, LocalDate actualPaymentDate) {

        EmiSchedule emi = emiScheduleRepository.findById(emiId)
                .orElseThrow(() -> new IllegalArgumentException("EMI not found"));

        Loan loan = emi.getLoan();

        if (!loan.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized");
        }

        if (emi.getStatus() != EmiScheduleStatus.PENDING) {
            throw new IllegalArgumentException("EMI cannot be marked PAID");
        }

        if (actualPaymentDate == null) {
            actualPaymentDate = LocalDate.now();
        }

        BigDecimal openingBalance = emi.getOpeningBalance();
        BigDecimal monthlyRate = loan.getInterestRate()
                .divide(BigDecimal.valueOf(1200), 34, RoundingMode.HALF_UP);

        BigDecimal interest = openingBalance.multiply(monthlyRate)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal principal = emi.getEmiAmount().subtract(interest)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal closingBalance = openingBalance.subtract(principal);
        if (closingBalance.compareTo(BigDecimal.ZERO) < 0) closingBalance = BigDecimal.ZERO;

        closingBalance = closingBalance.setScale(2, RoundingMode.HALF_UP);

        emi.setInterestComponent(interest);
        emi.setPrincipalComponent(principal);
        emi.setClosingBalance(closingBalance);
        emi.setStatus(EmiScheduleStatus.PAID);
        emi.setPaymentDate(actualPaymentDate);
        emiScheduleRepository.save(emi);

        loan.setPrincipal(closingBalance);
        loanRepository.save(loan);

        Payment p = new Payment();
        p.setLoan(loan);
        p.setPaymentDate(actualPaymentDate);
        p.setAmountPaid(emi.getEmiAmount());
        p.setAllocatedToInterest(interest);
        p.setAllocatedToPrincipal(principal);
        p.setOutstandingAfterPayment(closingBalance);
        p.setPaymentType(PaymentType.EMI);
        p.setRemarks("Manual EMI paid: Month " + emi.getMonthIndex());
        paymentRepository.save(p);

        loanStatusManager.updateLoanStatus(loan);

        MarkPaidResponse response = new MarkPaidResponse();
        response.setEmiId(emi.getId());
        response.setMonthIndex(emi.getMonthIndex());
        response.setActualPaymentDate(actualPaymentDate);
        response.setOpeningBalance(openingBalance);
        response.setInterestComponent(interest);
        response.setPrincipalComponent(principal);
        response.setClosingBalance(closingBalance);
        response.setUpdatedLoanOutstanding(closingBalance);
        response.setLoanStatus(loan.getStatus());

        return response;
    }

    @Override
    @Transactional
    public MarkMissedResponse markEmiMissed(Long emiId, Long userId) {

        EmiSchedule emi = emiScheduleRepository.findById(emiId)
                .orElseThrow(() -> new IllegalArgumentException("EMI not found"));

        Loan loan = emi.getLoan();

        if (!loan.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized");
        }

        if (emi.getStatus() != EmiScheduleStatus.PENDING) {
            throw new IllegalArgumentException("EMI cannot be marked MISSED");
        }

        emi.setStatus(EmiScheduleStatus.MISSED);
        emiScheduleRepository.save(emi);

        loanStatusManager.updateLoanStatus(loan);

        MarkMissedResponse r = new MarkMissedResponse();
        r.setEmiId(emi.getId());
        r.setMonthIndex(emi.getMonthIndex());
        r.setDueDate(emi.getDueDate());
        r.setStatus("MISSED");
        r.setLoanStatus(loan.getStatus());

        return r;
    }
}
