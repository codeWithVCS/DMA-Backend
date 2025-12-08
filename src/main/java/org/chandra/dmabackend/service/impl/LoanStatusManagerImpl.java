package org.chandra.dmabackend.service.impl;

import jakarta.transaction.Transactional;
import org.chandra.dmabackend.dto.response.LoanHealthResponse;
import org.chandra.dmabackend.dto.response.LoanSummaryResponse;
import org.chandra.dmabackend.model.EmiSchedule;
import org.chandra.dmabackend.model.EmiScheduleStatus;
import org.chandra.dmabackend.model.Loan;
import org.chandra.dmabackend.repository.EmiScheduleRepository;
import org.chandra.dmabackend.repository.LoanRepository;
import org.chandra.dmabackend.service.LoanStatusManager;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class LoanStatusManagerImpl implements LoanStatusManager {

    private final EmiScheduleRepository emiScheduleRepository;
    private final LoanRepository loanRepository;

    public LoanStatusManagerImpl(EmiScheduleRepository emiScheduleRepository,
                                 LoanRepository loanRepository) {
        this.emiScheduleRepository = emiScheduleRepository;
        this.loanRepository = loanRepository;
    }

    @Override
    public void updateLoanStatus(Loan loan) {

        // 1. CLOSED – loan fully paid
        if (loan.getPrincipal().compareTo(BigDecimal.ZERO) == 0) {
            loan.setStatus("CLOSED");
            loanRepository.save(loan);
            return;
        }

        // 2. FORECLOSED – never override foreclosure
        if ("FORECLOSED".equalsIgnoreCase(loan.getStatus())) {
            return;
        }

        // 3. Check if any EMI is MISSED → loan becomes OVERDUE
        List<EmiSchedule> emis = emiScheduleRepository.findByLoanOrderByMonthIndexAsc(loan);

        boolean hasMissed = emis.stream()
                .anyMatch(e -> e.getStatus() == EmiScheduleStatus.MISSED);

        if (hasMissed) {
            loan.setStatus("OVERDUE");
            loanRepository.save(loan);
            return;
        }

        // 4. Otherwise loan remains ACTIVE
        loan.setStatus("ACTIVE");
        loanRepository.save(loan);
    }

    @Override
    public String evaluateLoanStatus(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Loan not found"));
        return loan.getStatus();
    }

    @Override
    public LoanHealthResponse evaluateLoanHealth(Long loanId, Long userId) {

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Loan not found"));

        if (!loan.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized");
        }

        List<EmiSchedule> schedule =
                emiScheduleRepository.findByLoanOrderByMonthIndexAsc(loan);

        int totalEmis = schedule.size();
        int paidEmis = (int) schedule.stream()
                .filter(e -> e.getStatus() == EmiScheduleStatus.PAID)
                .count();
        int pendingEmis = (int) schedule.stream()
                .filter(e -> e.getStatus() == EmiScheduleStatus.PENDING)
                .count();
        int missedEmis = (int) schedule.stream()
                .filter(e -> e.getStatus() == EmiScheduleStatus.MISSED)
                .count();

        EmiSchedule nextPending = schedule.stream()
                .filter(e -> e.getStatus() == EmiScheduleStatus.PENDING)
                .findFirst()
                .orElse(null);

        LoanHealthResponse response = new LoanHealthResponse();

        response.setLoanId(loan.getId());
        response.setLoanStatus(loan.getStatus());

        response.setTotalEmis(totalEmis);
        response.setPaidEmis(paidEmis);
        response.setPendingEmis(pendingEmis);
        response.setMissedEmis(missedEmis);

        if (nextPending != null) {
            response.setNextEmiId(nextPending.getId());
            response.setNextMonthIndex(nextPending.getMonthIndex());
            response.setNextDueDate(nextPending.getDueDate());
            response.setNextEmiAmount(nextPending.getEmiAmount());
        }

        response.setPrincipalOutstanding(loan.getPrincipal());

        response.setHasMissedEmis(missedEmis > 0);
        response.setCanPayNextEmi(nextPending != null &&
                !"CLOSED".equalsIgnoreCase(loan.getStatus()) &&
                !"FORECLOSED".equalsIgnoreCase(loan.getStatus()));

        response.setCanForeclose(
                loan.getForeclosureAllowed() &&
                        !"CLOSED".equalsIgnoreCase(loan.getStatus()) &&
                        !"FORECLOSED".equalsIgnoreCase(loan.getStatus())
        );

        return response;
    }

    @Override
    public List<LoanSummaryResponse> getUserLoanSummaries(Long userId) {

        List<Loan> loans = loanRepository.findByUserId(userId);

        List<LoanSummaryResponse> summaries = new ArrayList<>();

        for (Loan loan : loans) {

            List<EmiSchedule> schedule =
                    emiScheduleRepository.findByLoanOrderByMonthIndexAsc(loan);

            int totalEmis = schedule.size();
            int paidEmis = (int) schedule.stream().filter(e -> e.getStatus() == EmiScheduleStatus.PAID).count();
            int pendingEmis = (int) schedule.stream().filter(e -> e.getStatus() == EmiScheduleStatus.PENDING).count();
            int missedEmis = (int) schedule.stream().filter(e -> e.getStatus() == EmiScheduleStatus.MISSED).count();

            EmiSchedule nextPending = schedule.stream()
                    .filter(e -> e.getStatus() == EmiScheduleStatus.PENDING)
                    .findFirst()
                    .orElse(null);

            LoanSummaryResponse dto = new LoanSummaryResponse();

            dto.setLoanId(loan.getId());
            dto.setLoanName(loan.getLoanName());
            dto.setCategory(loan.getCategory());
            dto.setLender(loan.getLender());
            dto.setPrincipalOutstanding(loan.getPrincipal());
            dto.setLoanStatus(loan.getStatus());

            dto.setTotalEmis(totalEmis);
            dto.setPaidEmis(paidEmis);
            dto.setPendingEmis(pendingEmis);
            dto.setMissedEmis(missedEmis);

            if (nextPending != null) {
                dto.setNextEmiId(nextPending.getId());
                dto.setNextMonthIndex(nextPending.getMonthIndex());
                dto.setNextDueDate(nextPending.getDueDate());
                dto.setNextEmiAmount(nextPending.getEmiAmount());
            }

            dto.setHasMissedEmis(missedEmis > 0);

            dto.setCanPayNextEmi(
                    nextPending != null &&
                            !"CLOSED".equalsIgnoreCase(loan.getStatus()) &&
                            !"FORECLOSED".equalsIgnoreCase(loan.getStatus())
            );

            summaries.add(dto);
        }

        return summaries;
    }

}
