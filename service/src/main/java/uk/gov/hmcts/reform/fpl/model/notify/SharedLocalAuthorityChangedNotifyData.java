package uk.gov.hmcts.reform.fpl.model.notify;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public final class SharedLocalAuthorityChangedNotifyData implements NotifyData {

    private String caseName;
    private String ccdNumber;
    private String childLastName;
    private String secondaryLocalAuthority;
    private String designatedLocalAuthority;
}
