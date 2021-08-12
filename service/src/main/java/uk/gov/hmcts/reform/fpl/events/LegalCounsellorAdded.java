package uk.gov.hmcts.reform.fpl.events;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.apache.commons.lang3.tuple.Pair;
import uk.gov.hmcts.reform.fpl.model.CaseData;
import uk.gov.hmcts.reform.fpl.model.LegalCounsellor;

@RequiredArgsConstructor
@Value
public class LegalCounsellorAdded {
    CaseData caseData;
    Pair<String, LegalCounsellor> legalCounsellor;
}