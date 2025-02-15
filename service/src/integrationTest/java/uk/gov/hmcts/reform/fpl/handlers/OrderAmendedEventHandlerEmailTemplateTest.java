package uk.gov.hmcts.reform.fpl.handlers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockBeans;
import org.springframework.test.context.ContextConfiguration;
import uk.gov.hmcts.reform.fpl.events.order.AmendedOrderEvent;
import uk.gov.hmcts.reform.fpl.model.CaseData;
import uk.gov.hmcts.reform.fpl.model.Child;
import uk.gov.hmcts.reform.fpl.model.ChildParty;
import uk.gov.hmcts.reform.fpl.model.Other;
import uk.gov.hmcts.reform.fpl.model.Respondent;
import uk.gov.hmcts.reform.fpl.model.RespondentParty;
import uk.gov.hmcts.reform.fpl.model.common.DocumentReference;
import uk.gov.hmcts.reform.fpl.model.common.Element;
import uk.gov.hmcts.reform.fpl.model.order.generated.GeneratedOrder;
import uk.gov.hmcts.reform.fpl.service.CaseUrlService;
import uk.gov.hmcts.reform.fpl.service.FeatureToggleService;
import uk.gov.hmcts.reform.fpl.service.SendDocumentService;
import uk.gov.hmcts.reform.fpl.service.email.content.AmendedOrderEmailContentProvider;
import uk.gov.hmcts.reform.fpl.service.email.content.ModifiedItemEmailContentProviderStrategy;
import uk.gov.hmcts.reform.fpl.service.email.content.TranslatedItemEmailContentProvider;
import uk.gov.hmcts.reform.fpl.service.others.OtherRecipientsInbox;
import uk.gov.hmcts.reform.fpl.service.representative.RepresentativeNotificationService;
import uk.gov.hmcts.reform.fpl.testingsupport.email.EmailTemplateTest;
import uk.gov.hmcts.reform.fpl.utils.EmailNotificationHelper;
import uk.gov.service.notify.SendEmailResponse;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.fpl.handlers.NotificationEventHandlerTestData.COURT_NAME;
import static uk.gov.hmcts.reform.fpl.handlers.NotificationEventHandlerTestData.LOCAL_AUTHORITY_CODE;
import static uk.gov.hmcts.reform.fpl.testingsupport.email.EmailContent.emailContent;
import static uk.gov.hmcts.reform.fpl.testingsupport.email.SendEmailResponseAssert.assertThat;
import static uk.gov.hmcts.reform.fpl.utils.ElementUtils.wrapElements;
import static uk.gov.hmcts.reform.fpl.utils.TestDataHelper.testOther;

@ContextConfiguration(classes = {
    AmendedOrderEventHandler.class,
    AmendedOrderEmailContentProvider.class, TranslatedItemEmailContentProvider.class,
    ModifiedDocumentCommonEventHandler.class, ModifiedItemEmailContentProviderStrategy.class,
    EmailNotificationHelper.class, CaseUrlService.class, RepresentativeNotificationService.class
})
@MockBeans({
    // All but the feature toggle service are only mocked because they are dependencies that aren't used
    @MockBean(SendDocumentService.class),
    @MockBean(OtherRecipientsInbox.class),
    @MockBean(FeatureToggleService.class)
})
class OrderAmendedEventHandlerEmailTemplateTest extends EmailTemplateTest {
    private static final GeneratedOrder ORDER = mock(GeneratedOrder.class);
    private static final DocumentReference ORDER_DOCUMENT = mock(DocumentReference.class);
    private static final String BINARY_URL = "/documents/some-random-string/binary";
    private static final long CASE_ID = 12345L;
    private static final String FAMILY_MAN_CASE_NUMBER = "FAM_NUM";
    private static final String CHILD_LAST_NAME = "Smith";
    private static final String RESPONDENT_LAST_NAME = "Jones";
    private static final CaseData CASE_DATA = CaseData.builder()
        .id(CASE_ID)
        .caseLocalAuthority(LOCAL_AUTHORITY_CODE)
        .familyManCaseNumber(FAMILY_MAN_CASE_NUMBER)
        .children1(wrapElements(Child.builder()
            .party(ChildParty.builder()
                .lastName(CHILD_LAST_NAME)
                .dateOfBirth(LocalDate.now())
                .build())
            .build()))
        .respondents1(wrapElements(Respondent.builder()
            .party(RespondentParty.builder()
                .lastName(RESPONDENT_LAST_NAME)
                .build())
            .build()))
        .orderCollection(wrapElements(ORDER))
        .build();

    @Autowired
    private AmendedOrderEventHandler underTest;

    @BeforeEach
    void mocks() {
        when(ORDER.isNewVersion()).thenReturn(true);
        when(ORDER.getType()).thenReturn("Care order");
        when(ORDER_DOCUMENT.getBinaryUrl()).thenReturn(BINARY_URL);
    }

    @Test
    void notifyParties() {
        List<Element<Other>> selectedOthers = wrapElements(testOther("Other 1"));
        underTest.notifyLocalAuthority(new AmendedOrderEvent(CASE_DATA, ORDER_DOCUMENT, "case management order",
            selectedOthers));
        underTest.notifyDigitalRepresentatives(new AmendedOrderEvent(CASE_DATA, ORDER_DOCUMENT, "case management order",
            selectedOthers));
        underTest.notifyEmailRepresentatives(new AmendedOrderEvent(CASE_DATA, ORDER_DOCUMENT, "case management order",
            selectedOthers));

        SendEmailResponse laResponse = response();
        SendEmailResponse digitalRepResponse = response();
        SendEmailResponse emailRepResponse = response();

        assertThat(laResponse)
            .hasSubject("case management order amended, " + CHILD_LAST_NAME)
            .hasBody(emailContent()
                .line("The case management order has been amended and reissued by " + COURT_NAME + " in the case:")
                .line()
                .callout("Jones, FAM_NUM")
                .line()
                .line("You should now check the order and do any required tasks or case updates.")
                .line()
                .line("You can review the order by:")
                .line()
                .list("signing into http://fake-url/cases/case-details/12345#Orders",
                    "using this link http://fake-url/documents/some-random-string/binary")
                .line()
                .line("HM Courts & Tribunal Service")
                .line()
                .end("Do not reply to this email. If you need to contact us, call 0330 808 4424 or email "
                    + "contactfpl@justice.gov.uk")
            );

        assertThat(digitalRepResponse)
            .hasSubject("case management order amended, " + CHILD_LAST_NAME)
            .hasBody(emailContent()
                .line("The case management order has been amended and reissued by " + COURT_NAME + " in the case:")
                .line()
                .callout("Jones, FAM_NUM")
                .line()
                .line("You should now check the order and do any required tasks or case updates.")
                .line()
                .line("You can review the order by:")
                .line()
                .list("signing into http://fake-url/cases/case-details/12345#Orders",
                    "using this link http://fake-url/documents/some-random-string/binary")
                .line()
                .line("HM Courts & Tribunal Service")
                .line()
                .end("Do not reply to this email. If you need to contact us, call 0330 808 4424 or email "
                    + "contactfpl@justice.gov.uk")
            );

        assertThat(emailRepResponse)
            .hasSubject("case management order amended, " + CHILD_LAST_NAME)
            .hasBody(emailContent()
                .line("The case management order has been amended and reissued by " + COURT_NAME + " in the case:")
                .line()
                .callout("Jones, FAM_NUM")
                .line()
                .line("You should now check the order and do any required tasks or case updates.")
                .line()
                .line("You can review the order by:")
                .line()
                .list("signing into http://fake-url/cases/case-details/12345#Orders",
                    "using this link http://fake-url/documents/some-random-string/binary")
                .line()
                .line("HM Courts & Tribunal Service")
                .line()
                .end("Do not reply to this email. If you need to contact us, call 0330 808 4424 or email "
                    + "contactfpl@justice.gov.uk")
            );
    }
}
