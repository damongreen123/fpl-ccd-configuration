package uk.gov.hmcts.reform.fpl.handlers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.fpl.config.CafcassLookupConfiguration;
import uk.gov.hmcts.reform.fpl.events.order.GeneratedPlacementOrderEvent;
import uk.gov.hmcts.reform.fpl.model.CaseData;
import uk.gov.hmcts.reform.fpl.model.Child;
import uk.gov.hmcts.reform.fpl.model.Respondent;
import uk.gov.hmcts.reform.fpl.model.RespondentSolicitor;
import uk.gov.hmcts.reform.fpl.model.common.DocumentReference;
import uk.gov.hmcts.reform.fpl.model.common.Element;
import uk.gov.hmcts.reform.fpl.model.notify.PlacementOrderIssuedNotifyData;
import uk.gov.hmcts.reform.fpl.model.notify.RecipientsRequest;
import uk.gov.hmcts.reform.fpl.model.order.generated.GeneratedOrder;
import uk.gov.hmcts.reform.fpl.service.CourtService;
import uk.gov.hmcts.reform.fpl.service.LocalAuthorityRecipientsService;
import uk.gov.hmcts.reform.fpl.service.SendDocumentService;
import uk.gov.hmcts.reform.fpl.service.email.NotificationService;
import uk.gov.hmcts.reform.fpl.service.email.content.OrderIssuedEmailContentProvider;
import uk.gov.hmcts.reform.fpl.service.orders.history.SealedOrderHistoryService;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.fpl.Constants.DEFAULT_ADMIN_EMAIL;
import static uk.gov.hmcts.reform.fpl.Constants.LOCAL_AUTHORITY_1_CODE;
import static uk.gov.hmcts.reform.fpl.Constants.PRIVATE_SOLICITOR_USER_EMAIL;
import static uk.gov.hmcts.reform.fpl.Constants.TEST_CASE_ID;
import static uk.gov.hmcts.reform.fpl.Constants.TEST_FAMILY_MAN_NUMBER;
import static uk.gov.hmcts.reform.fpl.NotifyTemplates.PLACEMENT_ORDER_GENERATED_NOTIFICATION_TEMPLATE;
import static uk.gov.hmcts.reform.fpl.handlers.NotificationEventHandlerTestData.CAFCASS_EMAIL_ADDRESS;
import static uk.gov.hmcts.reform.fpl.handlers.NotificationEventHandlerTestData.LOCAL_AUTHORITY_CODE;
import static uk.gov.hmcts.reform.fpl.handlers.NotificationEventHandlerTestData.LOCAL_AUTHORITY_EMAIL_ADDRESS;
import static uk.gov.hmcts.reform.fpl.utils.ElementUtils.element;
import static uk.gov.hmcts.reform.fpl.utils.ElementUtils.wrapElements;
import static uk.gov.hmcts.reform.fpl.utils.TestDataHelper.testDocumentReference;
import static uk.gov.hmcts.reform.fpl.utils.TestDataHelper.testRespondent;

@ExtendWith(MockitoExtension.class)
class GeneratedPlacementOrderEventHandlerTest {

    private static final DocumentReference ORDER_DOCUMENT = testDocumentReference();
    private static final DocumentReference ORDER_NOTIFICATION_DOCUMENT = testDocumentReference();

    @Mock
    private NotificationService notificationService;

    @Mock
    private OrderIssuedEmailContentProvider orderIssuedEmailContentProvider;

    @Mock
    private LocalAuthorityRecipientsService localAuthorityRecipients;

    @Mock
    private SealedOrderHistoryService sealedOrderHistoryService;

    @Mock
    private CourtService courtService;

    @Mock
    private CafcassLookupConfiguration cafcassLookupConfiguration;

    @Mock
    private SendDocumentService sendDocumentService;

    @InjectMocks
    private GeneratedPlacementOrderEventHandler underTest;

    private CaseData basicCaseData;

    @BeforeEach
    void setUp() {
        basicCaseData = CaseData.builder()
            .id(TEST_CASE_ID)
            .familyManCaseNumber(TEST_FAMILY_MAN_NUMBER)
            .caseLocalAuthority(LOCAL_AUTHORITY_1_CODE)
            .build();
    }

    @Test
    void shouldEmailPlacementOrderToRelevantParties() {
        //TODO - consider putting these in @before method
        when(localAuthorityRecipients.getRecipients(RecipientsRequest.builder().caseData(basicCaseData).build()))
            .thenReturn(Set.of(LOCAL_AUTHORITY_EMAIL_ADDRESS));
        Child child = Child.builder().build();
        when(sealedOrderHistoryService.lastGeneratedOrder(any()))
            .thenReturn(GeneratedOrder.builder().children(wrapElements(child)).build());
        PlacementOrderIssuedNotifyData notifyData = mock(PlacementOrderIssuedNotifyData.class);
        when(orderIssuedEmailContentProvider.getNotifyDataForPlacementOrder(basicCaseData, ORDER_DOCUMENT, child))
            .thenReturn(notifyData);
        when(courtService.getCourtEmail(basicCaseData)).thenReturn(DEFAULT_ADMIN_EMAIL);
        when(cafcassLookupConfiguration.getCafcass(LOCAL_AUTHORITY_1_CODE)).thenReturn(new CafcassLookupConfiguration.Cafcass(LOCAL_AUTHORITY_CODE, CAFCASS_EMAIL_ADDRESS));

        underTest.sendPlacementOrderEmail(new GeneratedPlacementOrderEvent(basicCaseData, ORDER_DOCUMENT, ORDER_NOTIFICATION_DOCUMENT, "Order title"));

        verify(notificationService).sendEmail(
            PLACEMENT_ORDER_GENERATED_NOTIFICATION_TEMPLATE,
            Set.of(LOCAL_AUTHORITY_EMAIL_ADDRESS, DEFAULT_ADMIN_EMAIL, CAFCASS_EMAIL_ADDRESS),
            notifyData,
            TEST_CASE_ID
        );
    }

    //TODO - think about scenarios in which one parent doesn't exist or doesn't have an address

    @Test
    void shouldSendOrderNotificationTo_ParentsByPost_And_ChildSolicitorByEmail() {//TODO - Maybe separate these two tests once things are more orderly
        Element<Respondent> father = testRespondent("Father", "Jones");
        Element<Respondent> mother = testRespondent("Mother", "Jones");
        Element<Child> child = element(Child.builder()//TODO - there's quite a bit of duplication between this and the first test
            .solicitor(RespondentSolicitor.builder().email(PRIVATE_SOLICITOR_USER_EMAIL).build())//TODO - first test could have a solicitor - could unify
            .build());
        CaseData caseData = basicCaseData.toBuilder()
            .respondents1(List.of(father, mother))
            .children1(List.of(child))
            .build();
        when(sealedOrderHistoryService.lastGeneratedOrder(any()))
            .thenReturn(GeneratedOrder.builder().children(List.of(child)).build());
        PlacementOrderIssuedNotifyData notifyData = mock(PlacementOrderIssuedNotifyData.class);
        when(orderIssuedEmailContentProvider.getNotifyDataForPlacementOrder(caseData, ORDER_NOTIFICATION_DOCUMENT, child.getValue()))
            .thenReturn(notifyData);

        underTest.sendPlacementOrderNotification(new GeneratedPlacementOrderEvent(caseData, ORDER_DOCUMENT, ORDER_NOTIFICATION_DOCUMENT, "Order title"));

        verify(sendDocumentService).sendDocuments(caseData, List.of(ORDER_NOTIFICATION_DOCUMENT), List.of(father.getValue().getParty(), mother.getValue().getParty()));

        verify(notificationService).sendEmail(
            PLACEMENT_ORDER_GENERATED_NOTIFICATION_TEMPLATE,
            Set.of(PRIVATE_SOLICITOR_USER_EMAIL),
            notifyData,
            TEST_CASE_ID
        );
    }

    //TODO - test when child is not represented
}
