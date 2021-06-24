package uk.gov.hmcts.reform.fpl.service.orders.generator;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.fpl.config.LocalAuthorityNameLookupConfiguration;
import uk.gov.hmcts.reform.fpl.enums.DocmosisTemplates;
import uk.gov.hmcts.reform.fpl.enums.GeneratedOrderType;
import uk.gov.hmcts.reform.fpl.model.CaseData;
import uk.gov.hmcts.reform.fpl.model.event.ManageOrdersEventData;
import uk.gov.hmcts.reform.fpl.model.order.Order;
import uk.gov.hmcts.reform.fpl.service.orders.docmosis.C21BlankOrderDocmosisParameters;
import uk.gov.hmcts.reform.fpl.service.orders.docmosis.DocmosisParameters;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Component
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class C21BlankOrderDocumentParameterGenerator implements NewDocmosisParameterGenerator<C21BlankOrderDocmosisParameters.C21BlankOrderDocmosisParametersBuilder> {

    private static final GeneratedOrderType TYPE = GeneratedOrderType.BLANK_ORDER;

    private final LocalAuthorityNameLookupConfiguration laNameLookup;

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
        ManageOrdersEventData eventData = caseData.getManageOrdersEventData();

        String localAuthorityCode = caseData.getCaseLocalAuthority();
        String localAuthorityName = laNameLookup.getLocalAuthorityName(localAuthorityCode);

        return docmosisParametersBuilder
            .orderType(TYPE)
            .orderDetails(eventData.getManageOrdersDirections())
            .localAuthorityName(localAuthorityName)
            .orderTitle(getOrderTitle(caseData))
            .build();
    }

    @Override
    public DocmosisTemplates template() {
        return DocmosisTemplates.ORDER;
    }

    private String getOrderTitle(CaseData caseData) {
        ManageOrdersEventData eventData = caseData.getManageOrdersEventData();
        String orderTitle = eventData.getManageOrdersTitle();
        return isBlank(orderTitle) ? "Order" : orderTitle;
    }

}
