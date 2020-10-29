package uk.gov.hmcts.reform.fpl.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class JudgeNote {
    private final String createdBy;
    private final LocalDate date;
    private final String note;
    private final String judgeName;
}
