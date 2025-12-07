package org.chandra.dmabackend.service.impl;

import org.chandra.dmabackend.dto.DerivedDates;
import org.chandra.dmabackend.service.LoanDateService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class LoanDateServiceImpl implements LoanDateService {

    @Override
    public DerivedDates deriveDates(LocalDate startDate,
                                    LocalDate emiStartDate,
                                    Integer emiDayOfMonth) {

        // 1. At least one date must be provided
        if (startDate == null && emiStartDate == null) {
            throw new IllegalArgumentException("Either start_date or emi_start_date must be provided");
        }

        // 2. If EMI start date alone is provided → derive start date
        if (startDate == null) {
            // Start date = first day of EMI start month
            LocalDate derivedStart = emiStartDate.withDayOfMonth(1);

            return new DerivedDates(derivedStart, emiStartDate);
        }

        // 3. If start date alone is provided → derive EMI start date
        if (emiStartDate == null) {
            LocalDate derivedEmiStart = computeNextEmiDate(startDate, emiDayOfMonth);
            return new DerivedDates(startDate, derivedEmiStart);
        }

        // 4. Both provided → validate consistency
        LocalDate expectedEmiStart = computeNextEmiDate(startDate, emiDayOfMonth);

        if (!expectedEmiStart.equals(emiStartDate)) {
            throw new IllegalArgumentException(
                    "Provided dates are inconsistent with EMI cycle. Expected EMI start: "
                            + expectedEmiStart + " but got: " + emiStartDate
            );
        }

        return new DerivedDates(startDate, emiStartDate);
    }


    // Helper: Compute first EMI date after loan start date
    private LocalDate computeNextEmiDate(LocalDate startDate, Integer emiDayOfMonth) {

        // Case 1: If EMI day is AFTER the start date in same month → EMI this month
        if (startDate.getDayOfMonth() < emiDayOfMonth) {
            return startDate.withDayOfMonth(emiDayOfMonth);
        }

        // Case 2: Otherwise EMI is next month
        LocalDate nextMonth = startDate.plusMonths(1);
        return nextMonth.withDayOfMonth(
                Math.min(emiDayOfMonth, nextMonth.lengthOfMonth()) // Safety for February
        );
    }
}
