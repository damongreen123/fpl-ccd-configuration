package uk.gov.hmcts.reform.fpl.service.orders.generator;

import uk.gov.hmcts.reform.fpl.enums.DocmosisTemplates;
import uk.gov.hmcts.reform.fpl.model.CaseData;
import uk.gov.hmcts.reform.fpl.model.order.Order;
import uk.gov.hmcts.reform.fpl.service.orders.docmosis.DocmosisParameters;

public interface NewDocmosisParameterGenerator<T extends DocmosisParameters.DocmosisParametersBuilder> extends DocmosisParameterGenerator {//TODO - change name

    Order accept();

    default DocmosisParameters generate(CaseData caseData) {//TODO - how to not allow child classes to implement this
        T docmosisParametersBuilder = initialiseDocmosisParameterBuilder();

        docmosisParametersBuilder
            .childrenAct(accept().getChildrenAct());

        return runOrderSpecificTransformations(docmosisParametersBuilder, caseData);
    }

    T initialiseDocmosisParameterBuilder();

    DocmosisParameters runOrderSpecificTransformations(T docmosisParametersBuilder, CaseData caseData);

    DocmosisTemplates template();

}
