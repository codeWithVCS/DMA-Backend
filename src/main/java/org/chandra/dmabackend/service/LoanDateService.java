package org.chandra.dmabackend.service;

import org.chandra.dmabackend.dto.DerivedDates;

import java.time.LocalDate;

public interface LoanDateService {

    DerivedDates deriveDates(LocalDate startDate, LocalDate emiStartDate, Integer emiDayOfMonth);

}
