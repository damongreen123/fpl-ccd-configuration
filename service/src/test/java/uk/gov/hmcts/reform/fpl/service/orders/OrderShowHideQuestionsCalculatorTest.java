package uk.gov.hmcts.reform.fpl.service.orders;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.reform.fpl.model.order.Order;

import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static uk.gov.hmcts.reform.fpl.model.order.Order.C21_BLANK_ORDER;
import static uk.gov.hmcts.reform.fpl.model.order.Order.C32_CARE_ORDER;

class OrderShowHideQuestionsCalculatorTest {

    private final OrderShowHideQuestionsCalculator underTest = new OrderShowHideQuestionsCalculator();

    @ParameterizedTest(name = "Show hide map for {0}")
    @MethodSource("orderWithExpectedMap")
    void calculate(Order order, Map<String,String> expectedShowHideMap) {
        assertThat(underTest.calculate(order)).isEqualTo(expectedShowHideMap);
    }

    private static Stream<Arguments> orderWithExpectedMap() {
        return Stream.of(
            Arguments.of(C32_CARE_ORDER, Map.of(
                "approvalDate", "YES",
                "approver", "YES",
                "previewOrder", "YES",
                "furtherDirections", "YES",
                "orderDetails", "NO",
                "whichChildren", "YES"
            )),
            Arguments.of(C21_BLANK_ORDER, Map.of(
                "approvalDate", "YES",
                "approver", "YES",
                "previewOrder", "YES",
                "furtherDirections", "NO",
                "orderDetails", "YES",
                "whichChildren", "YES"
            ))
        );
    }
}
