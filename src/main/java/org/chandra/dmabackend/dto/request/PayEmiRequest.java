package org.chandra.dmabackend.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@NoArgsConstructor
@Getter
@Setter
public class PayEmiRequest {
    private BigDecimal amountPaid;  // optional
}
