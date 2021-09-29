package uk.gov.hmcts.reform.fpl.handlers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.fpl.events.order.GeneratedPlacementOrderEvent;
import uk.gov.hmcts.reform.fpl.model.CaseData;
import uk.gov.hmcts.reform.fpl.model.Child;
import uk.gov.hmcts.reform.fpl.model.common.DocumentReference;
import uk.gov.hmcts.reform.fpl.model.common.Element;
import uk.gov.hmcts.reform.fpl.model.notify.PlacementOrderIssuedNotifyData;
import uk.gov.hmcts.reform.fpl.model.notify.RecipientsRequest;
import uk.gov.hmcts.reform.fpl.model.order.generated.GeneratedOrder;
import uk.gov.hmcts.reform.fpl.service.LocalAuthorityRecipientsService;
import uk.gov.hmcts.reform.fpl.service.email.NotificationService;
import uk.gov.hmcts.reform.fpl.service.email.content.OrderIssuedEmailContentProvider;
import uk.gov.hmcts.reform.fpl.service.orders.history.SealedOrderHistoryService;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.fpl.Constants.TEST_CASE_ID;
import static uk.gov.hmcts.reform.fpl.NotifyTemplates.PLACEMENT_ORDER_GENERATED_NOTIFICATION_TEMPLATE;
import static uk.gov.hmcts.reform.fpl.handlers.NotificationEventHandlerTestData.LOCAL_AUTHORITY_EMAIL_ADDRESS;
import static uk.gov.hmcts.reform.fpl.utils.TestDataHelper.testChild;
import static uk.gov.hmcts.reform.fpl.utils.TestDataHelper.testDocumentReference;

@ExtendWith(MockitoExtension.class)
class GeneratedPlacementOrderEventHandlerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private OrderIssuedEmailContentProvider orderIssuedEmailContentProvider;

    @Mock
    private LocalAuthorityRecipientsService localAuthorityRecipients;

    @Mock
    private SealedOrderHistoryService sealedOrderHistoryService;

    @InjectMocks
    private GeneratedPlacementOrderEventHandler underTest;

    @Test
    void shouldEmailPlacementOrderToRelevantParties() {
        CaseData caseData = CaseData.builder().id(TEST_CASE_ID).build();
        when(localAuthorityRecipients.getRecipients(RecipientsRequest.builder().caseData(caseData).build()))
            .thenReturn(Set.of(LOCAL_AUTHORITY_EMAIL_ADDRESS));
        DocumentReference orderDocument = testDocumentReference();
        Element<Child> child = testChild();
        given(sealedOrderHistoryService.lastGeneratedOrder(any()))
            .willReturn(GeneratedOrder.builder().children(List.of(child)).build());
        PlacementOrderIssuedNotifyData notifyData = mock(PlacementOrderIssuedNotifyData.class);
        when(orderIssuedEmailContentProvider.getNotifyDataForPlacementOrder(caseData, orderDocument, child.getValue()))
            .thenReturn(notifyData);

        underTest.notifyParties(new GeneratedPlacementOrderEvent(caseData, orderDocument, "Order title"));

        verify(notificationService).sendEmail(
            PLACEMENT_ORDER_GENERATED_NOTIFICATION_TEMPLATE,
            Set.of(LOCAL_AUTHORITY_EMAIL_ADDRESS),
            notifyData,
            TEST_CASE_ID
        );
    }

}
