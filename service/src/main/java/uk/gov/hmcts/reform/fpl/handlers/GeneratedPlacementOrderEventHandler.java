package uk.gov.hmcts.reform.fpl.handlers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.fpl.config.CafcassLookupConfiguration;
import uk.gov.hmcts.reform.fpl.events.order.GeneratedPlacementOrderEvent;
import uk.gov.hmcts.reform.fpl.model.CaseData;
import uk.gov.hmcts.reform.fpl.model.Child;
import uk.gov.hmcts.reform.fpl.model.Recipient;
import uk.gov.hmcts.reform.fpl.model.Respondent;
import uk.gov.hmcts.reform.fpl.model.common.Element;
import uk.gov.hmcts.reform.fpl.model.notify.NotifyData;
import uk.gov.hmcts.reform.fpl.model.notify.RecipientsRequest;
import uk.gov.hmcts.reform.fpl.model.order.generated.GeneratedOrder;
import uk.gov.hmcts.reform.fpl.service.CourtService;
import uk.gov.hmcts.reform.fpl.service.LocalAuthorityRecipientsService;
import uk.gov.hmcts.reform.fpl.service.SendDocumentService;
import uk.gov.hmcts.reform.fpl.service.email.NotificationService;
import uk.gov.hmcts.reform.fpl.service.email.content.OrderIssuedEmailContentProvider;
import uk.gov.hmcts.reform.fpl.service.orders.history.SealedOrderHistoryService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.fpl.NotifyTemplates.PLACEMENT_ORDER_GENERATED_NOTIFICATION_TEMPLATE;
import static uk.gov.hmcts.reform.fpl.utils.ElementUtils.findElement;

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
    private final SendDocumentService sendDocumentService;

    @EventListener
    public void sendPlacementOrderEmail(final GeneratedPlacementOrderEvent orderEvent) {
        final CaseData caseData = orderEvent.getCaseData();
        GeneratedOrder lastGeneratedOrder = sealedOrderHistoryService.lastGeneratedOrder(caseData);

        final NotifyData notifyData = orderIssuedEmailContentProvider.getNotifyDataForPlacementOrder(caseData,
            orderEvent.getOrderDocument(), lastGeneratedOrder.getChildren().get(0).getValue());

        sendOrderByEmail(caseData, notifyData);
    }

    @EventListener
    public void sendPlacementOrderNotification(final GeneratedPlacementOrderEvent orderEvent) {
         CaseData caseData = orderEvent.getCaseData();

        //Post letters
        //TODO - this is wrong and will be addressed later
        List<Recipient> recipients = caseData.getAllRespondents().stream()
            .map(Element::getValue)
            .map(Respondent::getParty)
            .collect(Collectors.toList());

        sendDocumentService.sendDocuments(caseData, List.of(orderEvent.getOrderNotificationDocument()), recipients);

        //E-mail child solicitor
        GeneratedOrder lastGeneratedOrder = sealedOrderHistoryService.lastGeneratedOrder(caseData);//TODO - some duplication with first method
        UUID childId = lastGeneratedOrder.getChildren().get(0).getId();
        Child child = findElement(childId, caseData.getAllChildren()).map(Element::getValue).orElseThrow();

        final NotifyData notifyData = orderIssuedEmailContentProvider.getNotifyDataForPlacementOrder(caseData,
            orderEvent.getOrderNotificationDocument(),
            child);

        sendEmail(caseData, notifyData, Set.of(child.getSolicitor().getEmail()));
    }

    private void sendOrderByEmail(final CaseData caseData,
                                  final NotifyData notifyData) {

        Set<String> recipients = new HashSet<>();

        //Local authority
        recipients.addAll(
            localAuthorityRecipients.getRecipients(RecipientsRequest.builder().caseData(caseData).build())
        );

        //Admin
        recipients.add(courtService.getCourtEmail(caseData));

        //CAFCASS
        recipients.add(cafcassLookupConfiguration.getCafcass(caseData.getCaseLocalAuthority()).getEmail());

        sendEmail(caseData, notifyData, recipients);
    }

    private void sendEmail(CaseData caseData, NotifyData notifyData, Set<String> recipients) {
        notificationService.sendEmail(PLACEMENT_ORDER_GENERATED_NOTIFICATION_TEMPLATE,
            recipients,
            notifyData,
            caseData.getId());//TODO param
    }

}
