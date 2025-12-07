package org.chandra.dmabackend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class DerivedDates {

    private LocalDate startDate;

    private LocalDate emiStartDate;

}
