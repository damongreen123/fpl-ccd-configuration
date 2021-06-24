package uk.gov.hmcts.reform.fpl.service.orders.generator;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.fpl.enums.DocmosisTemplates;
import uk.gov.hmcts.reform.fpl.model.CaseData;
import uk.gov.hmcts.reform.fpl.model.order.Order;
import uk.gov.hmcts.reform.fpl.service.orders.docmosis.C21BlankOrderDocmosisParameters;
import uk.gov.hmcts.reform.fpl.service.orders.docmosis.DocmosisParameters;

import static org.assertj.core.api.Assertions.assertThat;

class NewDocmosisParameterGeneratorTest {

    private final NewDocmosisParameterGenerator<C21BlankOrderDocmosisParameters.C21BlankOrderDocmosisParametersBuilder> docmosisParameterGenerator = new NewDocmosisParameterGenerator<>() {

        @Override
        public Order accept() {
            return Order.C21_BLANK_ORDER;
        }

        @Override
        public C21BlankOrderDocmosisParameters.C21BlankOrderDocmosisParametersBuilder initialiseDocmosisParameterBuilder() {
            return C21BlankOrderDocmosisParameters.builder();
        }

        @Override
        public DocmosisParameters runOrderSpecificTransformations(C21BlankOrderDocmosisParameters.C21BlankOrderDocmosisParametersBuilder docmosisParametersBuilder, CaseData caseData) {
            return docmosisParametersBuilder
                .courtName("test - court name")
                .build();
        }

        @Override
        public DocmosisTemplates template() {
            return null;
        }

    };

    @Test
    void shouldReturnCommonAndSpecificValuesInDocmosisParameters() {
        DocmosisParameters docmosisParameters = docmosisParameterGenerator.generate(CaseData.builder().amountToPay("123").build());//TODO - make this return the implementation rather than the interface?

        assertThat(docmosisParameters.getChildrenAct()).isEqualTo("Section 31 Children Act 198");
        assertThat(docmosisParameters.getCourtName()).isEqualTo("test - court name");
    }

}
