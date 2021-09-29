package uk.gov.hmcts.reform.fpl.handlers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.fpl.config.CafcassLookupConfiguration;
import uk.gov.hmcts.reform.fpl.events.order.GeneratedPlacementOrderEvent;
import uk.gov.hmcts.reform.fpl.model.CaseData;
import uk.gov.hmcts.reform.fpl.model.common.DocumentReference;
import uk.gov.hmcts.reform.fpl.model.notify.NotifyData;
import uk.gov.hmcts.reform.fpl.model.notify.RecipientsRequest;
import uk.gov.hmcts.reform.fpl.model.order.generated.GeneratedOrder;
import uk.gov.hmcts.reform.fpl.service.CourtService;
import uk.gov.hmcts.reform.fpl.service.LocalAuthorityRecipientsService;
import uk.gov.hmcts.reform.fpl.service.email.NotificationService;
import uk.gov.hmcts.reform.fpl.service.email.content.OrderIssuedEmailContentProvider;
import uk.gov.hmcts.reform.fpl.service.orders.history.SealedOrderHistoryService;

import java.util.Collection;
import java.util.HashSet;

import static uk.gov.hmcts.reform.fpl.NotifyTemplates.PLACEMENT_ORDER_GENERATED_NOTIFICATION_TEMPLATE;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeneratedPlacementOrderEventHandler {//TODO - maybe this should extend the generic class - consider it last

    private final LocalAuthorityRecipientsService localAuthorityRecipients;
    private final NotificationService notificationService;
    private final OrderIssuedEmailContentProvider orderIssuedEmailContentProvider;
    private final SealedOrderHistoryService sealedOrderHistoryService;
    private final CourtService courtService;
    private final CafcassLookupConfiguration cafcassLookupConfiguration;

    @EventListener
    public void sendPlacementOrderEmail(final GeneratedPlacementOrderEvent orderEvent) {
        final CaseData caseData = orderEvent.getCaseData();
        final DocumentReference orderDocument = orderEvent.getOrderDocument();
        GeneratedOrder lastGeneratedOrder = sealedOrderHistoryService.lastGeneratedOrder(caseData);

        final NotifyData notifyData = orderIssuedEmailContentProvider.getNotifyDataForPlacementOrder(caseData,
            orderDocument, lastGeneratedOrder.getChildren().get(0).getValue());

        sendEmail(caseData, notifyData);
    }

    private void sendEmail(final CaseData caseData,
                           final NotifyData notifyData) {

        Collection<String> recipients = new HashSet<>();

        //Local authority
        recipients.addAll(
            localAuthorityRecipients.getRecipients(RecipientsRequest.builder().caseData(caseData).build())
        );

        //Admin
        recipients.add(courtService.getCourtEmail(caseData));

        //CAFCASS
        recipients.add(cafcassLookupConfiguration.getCafcass(caseData.getCaseLocalAuthority()).getEmail());

        notificationService.sendEmail(PLACEMENT_ORDER_GENERATED_NOTIFICATION_TEMPLATE,
            recipients,
            notifyData,
            caseData.getId());
    }

}
