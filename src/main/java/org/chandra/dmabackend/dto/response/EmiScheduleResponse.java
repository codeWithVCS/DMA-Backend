package org.chandra.dmabackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class EmiScheduleResponse {

    private Integer monthIndex;
    private LocalDate dueDate;
    private BigDecimal openingBalance;
    private BigDecimal emiAmount;
    private BigDecimal interestComponent;
    private BigDecimal principalComponent;
    private BigDecimal closingBalance;
    private String status;

}
