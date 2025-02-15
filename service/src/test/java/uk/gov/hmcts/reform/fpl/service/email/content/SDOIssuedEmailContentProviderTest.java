package uk.gov.hmcts.reform.fpl.service.email.content;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import uk.gov.hmcts.reform.fpl.model.CaseData;
import uk.gov.hmcts.reform.fpl.model.HearingBooking;
import uk.gov.hmcts.reform.fpl.model.Respondent;
import uk.gov.hmcts.reform.fpl.model.RespondentParty;
import uk.gov.hmcts.reform.fpl.model.notify.sdo.SDONotifyData;
import uk.gov.hmcts.reform.fpl.utils.EmailNotificationHelper;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.fpl.enums.TabUrlAnchor.ORDERS;
import static uk.gov.hmcts.reform.fpl.utils.ElementUtils.wrapElements;

@ContextConfiguration(classes = {SDOIssuedContentProvider.class})
class SDOIssuedEmailContentProviderTest extends AbstractEmailContentProviderTest {

    @MockBean
    private EmailNotificationHelper helper;

    @Autowired
    private SDOIssuedContentProvider underTest;

    @BeforeEach
    void setUp() {
        when(helper.getEldestChildLastName(any(CaseData.class))).thenReturn("name");
    }

    @Test
    void buildNotificationParameters() {
        CaseData caseData = CaseData.builder()
            .id(Long.valueOf(CASE_REFERENCE))
            .familyManCaseNumber("FAM NUM")
            .caseLocalAuthority(LOCAL_AUTHORITY_CODE)
            .respondents1(wrapElements(Respondent.builder()
                .party(RespondentParty.builder().lastName("Smith").build())
                .build()))
            .hearingDetails(wrapElements(HearingBooking.builder()
                .startDate(LocalDateTime.of(2020, 1, 1, 0, 0, 0))
                .build()))
            .build();

        when(helper.getEldestChildLastName(caseData.getAllChildren())).thenReturn("Smith");

        SDONotifyData actualData = underTest.buildNotificationParameters(caseData);

        SDONotifyData expectedData = SDONotifyData.builder()
            .callout("Smith, FAM NUM, hearing 1 Jan 2020")
            .lastName("Smith")
            .caseUrl(caseUrl(CASE_REFERENCE, ORDERS))
            .build();

        assertThat(actualData).isEqualTo(expectedData);
    }
}
