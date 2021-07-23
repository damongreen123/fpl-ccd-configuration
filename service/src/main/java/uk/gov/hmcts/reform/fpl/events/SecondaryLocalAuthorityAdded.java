package uk.gov.hmcts.reform.fpl.events;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.reform.fpl.model.CaseData;

@Getter
@Builder
@RequiredArgsConstructor
public class SecondaryLocalAuthorityAdded {
    private final CaseData caseData;
}
