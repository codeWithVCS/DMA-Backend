package org.chandra.dmabackend.service;

import org.chandra.dmabackend.model.EmiSchedule;
import org.chandra.dmabackend.model.Loan;

import java.util.List;

public interface EmiScheduleGeneratorService {

    List<EmiSchedule> generateSchedule(Loan loan);

}
