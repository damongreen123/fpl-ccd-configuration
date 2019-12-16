package uk.gov.hmcts.reform.fpl.service;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.document.domain.Document;
import uk.gov.hmcts.reform.fpl.model.CaseData;
import uk.gov.hmcts.reform.fpl.model.CaseManagementOrder;
import uk.gov.hmcts.reform.fpl.model.HearingBooking;
import uk.gov.hmcts.reform.fpl.model.HearingDateDynamicElement;
import uk.gov.hmcts.reform.fpl.model.NextHearing;
import uk.gov.hmcts.reform.fpl.model.OrderAction;
import uk.gov.hmcts.reform.fpl.model.common.Element;
import uk.gov.hmcts.reform.fpl.model.common.dynamic.DynamicList;
import uk.gov.hmcts.reform.fpl.service.time.Time;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.isNull;
import static uk.gov.hmcts.reform.fpl.model.HearingDateDynamicElement.getHearingDynamicElement;
import static uk.gov.hmcts.reform.fpl.model.common.DocumentReference.buildFromDocument;

//TODO: this class will take some of the methods out of draftCMO service.
@Service
public class CaseManagementOrderService {
    private final Time time;
    private final DateFormatterService dateFormatterService;
    private final HearingBookingService hearingBookingService;

    public CaseManagementOrderService(Time time,
                                      DateFormatterService dateFormatterService,
                                      HearingBookingService hearingBookingService) {
        this.time = time;
        this.dateFormatterService = dateFormatterService;
        this.hearingBookingService = hearingBookingService;
    }

    public CaseManagementOrder addDocument(CaseManagementOrder caseManagementOrder, Document document) {
        return caseManagementOrder.toBuilder()
            .orderDoc(buildFromDocument(document))
            .build();
    }

    public CaseManagementOrder addAction(CaseManagementOrder order, OrderAction orderAction) {
        return order.toBuilder()
            .action(orderAction)
            .build();
    }

    public Map<String, Object> extractMapFieldsFromCaseManagementOrder(CaseManagementOrder order) {
        if (isNull(order)) {
            order = CaseManagementOrder.builder().build();
        }

        Map<String, Object> data = new HashMap<>();
        data.put("schedule", order.getSchedule());
        data.put("recitals", order.getRecitals());
        data.put("orderAction", order.getAction());

        return data;
    }

    public OrderAction removeDocumentFromOrderAction(OrderAction orderAction) {
        return orderAction.toBuilder().document(null).build();
    }

    public boolean isHearingDateInFuture(CaseData caseData) {
        LocalDateTime hearingDate = hearingBookingService
            .getHearingBookingByUUID(caseData.getHearingDetails(), caseData.getCmoToAction().getId())
            .getStartDate();

        return time.now().isBefore(hearingDate);
    }

    public CaseManagementOrder addNextHearingToCMO(DynamicList list, CaseManagementOrder order) {
        if (list == null) {
            return order;
        }

        HearingDateDynamicElement hearingDateDynamicElement = getHearingDynamicElement(list);

        return order.toBuilder()
            .nextHearing(NextHearing.builder()
                .id(hearingDateDynamicElement.getId())
                .date(hearingDateDynamicElement.getDate())
                .build())
            .build();
    }

    public String createNextHearingDateLabel(CaseManagementOrder caseManagementOrder,
                                             List<Element<HearingBooking>> hearingBookings) {
        return Optional.ofNullable(caseManagementOrder)
            .map(CaseManagementOrder::getNextHearing)
            .map(NextHearing::getId)
            .map(id -> hearingBookingService.getHearingBookingByUUID(hearingBookings, id))
            .map(HearingBooking::getStartDate)
            .map(this::formatHearingBookingLabel)
            .orElse("");
    }

    private String formatHearingBookingLabel(LocalDateTime startDate) {
        String formattedDate = dateFormatterService.formatLocalDateTimeBaseUsingFormat(startDate, "d MMMM 'at' h:mma");
        return String.format("The next hearing date is on %s", formattedDate);
    }
}
