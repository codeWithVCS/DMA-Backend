package org.chandra.dmabackend.service.impl;

import org.chandra.dmabackend.dto.response.EmiBreakdownResult;
import org.chandra.dmabackend.model.EmiSchedule;
import org.chandra.dmabackend.model.EmiScheduleStatus;
import org.chandra.dmabackend.model.Loan;
import org.chandra.dmabackend.service.EmiBreakdownService;
import org.chandra.dmabackend.service.EmiScheduleGeneratorService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class EmiScheduleGeneratorServiceImpl implements EmiScheduleGeneratorService {

    private final EmiBreakdownService emiBreakdownService;

    public EmiScheduleGeneratorServiceImpl(EmiBreakdownService emiBreakdownService) {
        this.emiBreakdownService = emiBreakdownService;
    }

    @Override
    public List<EmiSchedule> generateSchedule(Loan loan) {

        BigDecimal principal = loan.getPrincipal();
        BigDecimal interestRate = loan.getInterestRate();
        Integer tenureMonths = loan.getTenureMonths();
        BigDecimal emiAmount = loan.getEmiAmount();
        LocalDate emiStartDate = loan.getEmiStartDate();

        BigDecimal openingBalance = principal;
        BigDecimal monthlyRate = interestRate.divide(BigDecimal.valueOf(1200), 34, RoundingMode.HALF_UP);
        BigDecimal emi = emiAmount;
        LocalDate dueDate = emiStartDate;
        Integer monthIndex = 1;

        List<EmiSchedule> emiSchedule =  new ArrayList<>();

        for(int i=1; i<=tenureMonths; i++){

            EmiBreakdownResult breakdown = emiBreakdownService.calculate(openingBalance, monthlyRate, emi);

            EmiSchedule schedule = new EmiSchedule();
            schedule.setLoan(loan);
            schedule.setMonthIndex(Integer.valueOf(monthIndex));
            schedule.setDueDate(dueDate);
            schedule.setOpeningBalance(openingBalance);
            schedule.setEmiAmount(emi);
            schedule.setInterestComponent(breakdown.getInterestComponent());
            schedule.setPrincipalComponent(breakdown.getPrincipalComponent());
            schedule.setClosingBalance(breakdown.getClosingBalance());
            schedule.setStatus(EmiScheduleStatus.PENDING);

            emiSchedule.add(schedule);

            openingBalance = breakdown.getClosingBalance();
            dueDate = dueDate.plusMonths(1);
            if(breakdown.getClosingBalance().compareTo(BigDecimal.ZERO) <= 0){
                break;
            }
            monthIndex++;
        }

        return emiSchedule;
    }
}
