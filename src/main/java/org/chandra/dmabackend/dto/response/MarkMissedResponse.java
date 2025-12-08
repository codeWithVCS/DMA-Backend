package org.chandra.dmabackend.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@NoArgsConstructor
@Getter
@Setter
public class MarkMissedResponse {

    private Long emiId;
    private Integer monthIndex;
    private LocalDate dueDate;
    private String status;
    private String loanStatus;

}
