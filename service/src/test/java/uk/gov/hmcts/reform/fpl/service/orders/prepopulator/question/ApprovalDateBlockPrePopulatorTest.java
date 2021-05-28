package uk.gov.hmcts.reform.fpl.service.orders.prepopulator.question;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.fpl.model.CaseData;
import uk.gov.hmcts.reform.fpl.model.HearingBooking;
import uk.gov.hmcts.reform.fpl.model.common.dynamic.DynamicList;
import uk.gov.hmcts.reform.fpl.model.event.ManageOrdersEventData;
import uk.gov.hmcts.reform.fpl.model.order.OrderQuestionBlock;
import uk.gov.hmcts.reform.fpl.service.hearing.HearingService;
import uk.gov.hmcts.reform.fpl.service.time.Time;
import uk.gov.hmcts.reform.fpl.utils.FixedTime;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.fpl.utils.ElementUtils.element;

class ApprovalDateBlockPrePopulatorTest {

    private static final DynamicList MANAGE_ORDERS_APPROVED_AT_HEARING_LIST = mock(DynamicList.class);
    private static final LocalDateTime END_DATE = LocalDateTime.now();
    private static final CaseData CASE_DATA = CaseData.builder()
        .manageOrdersEventData(ManageOrdersEventData.builder()
            .manageOrdersApprovedAtHearingList(MANAGE_ORDERS_APPROVED_AT_HEARING_LIST)
            .build())
        .build();

    private final HearingService hearingService = mock(HearingService.class);
    private final Time time = new FixedTime();
    private static final String APPROVAL_DATE_FIELD = "manageOrdersApprovalDate";

    private final ApprovalDateBlockPrePopulator underTest = new ApprovalDateBlockPrePopulator(hearingService, time);

    @Test
    void accept() {
        assertThat(underTest.accept()).isEqualTo(OrderQuestionBlock.APPROVAL_DATE);
    }


    @Test
    void testApprovalDateIfNotSet() {
        CaseData caseData = CaseData.builder().build();

        Assertions.assertThat(underTest.prePopulate(caseData))
            .containsOnly(Map.entry(APPROVAL_DATE_FIELD, time.now().toLocalDate()));
    }

    @Test
    void shouldRetainCurrentlyPopulatedValue() {
        CaseData caseData = CaseData.builder().manageOrdersEventData(ManageOrdersEventData.builder()
            .manageOrdersApprovalDate(time.now().minusMonths(1).toLocalDate()).build()).build();

        Assertions.assertThat(underTest.prePopulate(caseData)).isEmpty();
    }

    @Test
    void doDefaultTimeWhenNoHearingFound() {
        when(hearingService.findHearing(CASE_DATA,
            MANAGE_ORDERS_APPROVED_AT_HEARING_LIST)).thenReturn(Optional.empty());

        Map<String, Object> actual = underTest.prePopulate(CASE_DATA);

        assertThat(actual).isEqualTo(Map.of(APPROVAL_DATE_FIELD, time.now().toLocalDate()));
    }

    @Test
    void prePopulateWhenHearingFound() {
        when(hearingService.findHearing(CASE_DATA, MANAGE_ORDERS_APPROVED_AT_HEARING_LIST))
            .thenReturn(Optional.of(element(
                HearingBooking.builder()
                    .endDate(END_DATE)
                    .build())));

        Map<String, Object> actual = underTest.prePopulate(CASE_DATA);

        assertThat(actual).isEqualTo(Map.of(
            "manageOrdersApprovalDate", END_DATE.toLocalDate()
        ));
    }

}
