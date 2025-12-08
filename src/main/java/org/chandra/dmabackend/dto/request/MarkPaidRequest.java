package org.chandra.dmabackend.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@NoArgsConstructor
@Getter
@Setter
public class MarkPaidRequest {

    private LocalDate actualPaymentDate;

}
