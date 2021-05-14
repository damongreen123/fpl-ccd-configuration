package uk.gov.hmcts.reform.fpl.service.orders.validator;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.fpl.model.CaseData;
import uk.gov.hmcts.reform.fpl.model.event.ManageOrdersEventData;
import uk.gov.hmcts.reform.fpl.service.time.Time;
import uk.gov.hmcts.reform.fpl.utils.FixedTimeConfiguration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static java.util.Objects.deepEquals;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static uk.gov.hmcts.reform.fpl.enums.orders.SupervisionOrderEndDateType.SET_CALENDAR_DAY;
import static uk.gov.hmcts.reform.fpl.enums.orders.SupervisionOrderEndDateType.SET_CALENDAR_DAY_AND_TIME;
import static uk.gov.hmcts.reform.fpl.enums.orders.SupervisionOrderEndDateType.SET_NUMBER_OF_MONTHS;
import static uk.gov.hmcts.reform.fpl.model.order.OrderQuestionBlock.SUPERVISION_ORDER_END_DATE;

class SupervisionOrderEndDateValidatorTest {
    private static final String TEST_INVALID_TIME_MESSAGE = "Enter a valid time";
    private static final String TEST_FUTURE_DATE_MESSAGE = "Enter an end date in the future";
    private static final String TEST_END_DATE_RANGE_MESSAGE = "Supervision orders cannot last longer than 12 months";
    private static final String TEST_UNDER_DATE_RANGE_MESSAGE = "Supervision orders in months should be at least 1";

    private final Time time = new FixedTimeConfiguration().stoppedTime();

    private final SupervisionOrderEndDateValidator underTest = new SupervisionOrderEndDateValidator(time);

    @Test
    void accept() {
        assertThat(underTest.accept()).isEqualTo(SUPERVISION_ORDER_END_DATE);
    }

    @Test
    void shouldReturnErrorForDateOverOneYearFromNow() {
        CaseData caseData = CaseData.builder()
            .manageOrdersEventData(ManageOrdersEventData.builder()
                .manageSupervisionOrderEndDateType(SET_CALENDAR_DAY)
                .manageOrdersSetDateEndDate(dateNow().plusYears(2))
                .build())
            .build();

        assertThat(underTest.validate(caseData)).isEqualTo(List.of(TEST_END_DATE_RANGE_MESSAGE));
    }

    @Test
    void shouldReturnErrorForDateTimeOverOneYearFromNo() {
        final LocalDateTime supervisionOrderEndDateTime = time.now();
        CaseData caseData = CaseData.builder()
            .manageOrdersEventData(ManageOrdersEventData.builder()
                .manageSupervisionOrderEndDateType(SET_CALENDAR_DAY_AND_TIME)
                .manageOrdersSetDateAndTimeEndDate(supervisionOrderEndDateTime.plusMonths(13))
                .build())
            .build();

        assertThat(underTest.validate(caseData)).isEqualTo(
            List.of(TEST_END_DATE_RANGE_MESSAGE));
    }

    @Test
    void shouldReturnErrorForDateInPast() {
        CaseData caseData = CaseData.builder()
            .manageOrdersEventData(ManageOrdersEventData.builder()
                .manageSupervisionOrderEndDateType(SET_CALENDAR_DAY)
                .manageOrdersSetDateEndDate(dateNow().minusDays(7))
                .build())
            .build();

        assertThat(underTest.validate(caseData)).isEqualTo(List.of(TEST_FUTURE_DATE_MESSAGE));
    }

    @Test
    void shouldReturnErrorForDateTimeInPast() {
        final LocalDateTime supervisionOrderEndDateTime = time.now();
        CaseData caseData = CaseData.builder()
            .manageOrdersEventData(ManageOrdersEventData.builder()
                .manageSupervisionOrderEndDateType(SET_CALENDAR_DAY_AND_TIME)
                .manageOrdersSetDateAndTimeEndDate(supervisionOrderEndDateTime.minusDays(10))
                .build())
            .build();

        assertThat(underTest.validate(caseData)).isEqualTo(List.of(TEST_FUTURE_DATE_MESSAGE));
    }

    @Test
    void shouldReturnErrorForNumberOfMonthsOverMaximum() {
        CaseData caseData = CaseData.builder()
            .manageOrdersEventData(ManageOrdersEventData.builder()
                .manageSupervisionOrderEndDateType(SET_NUMBER_OF_MONTHS)
                .manageOrdersSetMonthsEndDate(13)
                .build())
            .build();

        assertThat(underTest.validate(caseData)).isEqualTo(List.of(TEST_END_DATE_RANGE_MESSAGE));
    }

    @Test
    void shouldReturnErrorForNumberOfMonthsLessThanOne() {
        CaseData caseData = CaseData.builder()
            .manageOrdersEventData(ManageOrdersEventData.builder()
                .manageSupervisionOrderEndDateType(SET_NUMBER_OF_MONTHS)
                .manageOrdersSetMonthsEndDate(0)
                .build())
            .build();

        deepEquals(underTest.validate(caseData), TEST_UNDER_DATE_RANGE_MESSAGE);
    }

    @Test
    void shouldReturnErrorWhenInvalidTimeFormatIsEntered() {
        final LocalDateTime invalidTime = dateNow().plusDays(1).atTime(LocalTime.MIDNIGHT);

        CaseData caseData = CaseData.builder()
            .manageOrdersEventData(ManageOrdersEventData.builder()
                .manageSupervisionOrderEndDateType(SET_CALENDAR_DAY_AND_TIME)
                .manageOrdersSetDateAndTimeEndDate(invalidTime)
                .build())
            .build();
        assertThat(underTest.validate(caseData)).isEqualTo(List.of(TEST_INVALID_TIME_MESSAGE));
    }

    private LocalDate dateNow() {
        return time.now().toLocalDate();
    }
}