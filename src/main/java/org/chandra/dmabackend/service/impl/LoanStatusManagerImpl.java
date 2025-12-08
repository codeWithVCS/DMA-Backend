package org.chandra.dmabackend.service.impl;

import org.chandra.dmabackend.model.EmiSchedule;
import org.chandra.dmabackend.model.EmiScheduleStatus;
import org.chandra.dmabackend.model.Loan;
import org.chandra.dmabackend.repository.EmiScheduleRepository;
import org.chandra.dmabackend.repository.LoanRepository;
import org.chandra.dmabackend.service.LoanStatusManager;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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
}
