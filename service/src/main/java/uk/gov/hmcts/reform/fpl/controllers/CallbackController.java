package uk.gov.hmcts.reform.fpl.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackRequest;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.fpl.model.CaseData;
import uk.gov.hmcts.reform.fpl.service.CaseConverter;

import java.util.List;

public abstract class CallbackController {

    @Autowired
    private CaseConverter caseConverter;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    protected CaseData getCaseData(CaseDetails caseDetails) {
        return caseConverter.convert(caseDetails);
    }

    protected CaseData getCaseData(CallbackRequest callbackRequest) {
        return caseConverter.convert(callbackRequest.getCaseDetails());
    }

    protected CaseData getCaseDataBefore(CallbackRequest callbackRequest) {
        return caseConverter.convert(callbackRequest.getCaseDetailsBefore());
    }

    protected AboutToStartOrSubmitCallbackResponse respond(CaseDetails caseDetails) {
        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(caseDetails.getData())
            .build();
    }

    protected AboutToStartOrSubmitCallbackResponse respond(CaseDetails caseDetails, List<String> errors) {
        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(caseDetails.getData())
            .errors(errors)
            .build();
    }

    protected void publishEvent(Object event) {
        eventPublisher.publishEvent(event);
    }
}
