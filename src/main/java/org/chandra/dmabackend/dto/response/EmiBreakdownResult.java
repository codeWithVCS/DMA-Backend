package org.chandra.dmabackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class EmiBreakdownResult {

    BigDecimal interestComponent;
    BigDecimal principalComponent;
    BigDecimal closingBalance;

}
