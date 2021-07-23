package uk.gov.hmcts.reform.fpl.model.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;
import uk.gov.hmcts.reform.fpl.enums.LocalAuthorityAction;
import uk.gov.hmcts.reform.fpl.model.Temp;
import uk.gov.hmcts.reform.fpl.model.common.dynamic.DynamicList;

@Data
@Builder(toBuilder = true)
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LocalAuthoritiesEventData {
    @Temp
    private final DynamicList localAuthoritiesToShare;
    @Temp
    private final LocalAuthorityAction localAuthorityAction;
    @Temp
    private final String localAuthorityEmail;
}
