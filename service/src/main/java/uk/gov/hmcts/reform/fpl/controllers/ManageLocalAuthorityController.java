package uk.gov.hmcts.reform.fpl.controllers;

import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackRequest;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.fpl.enums.LocalAuthorityAction;
import uk.gov.hmcts.reform.fpl.model.CaseData;
import uk.gov.hmcts.reform.fpl.model.event.LocalAuthoritiesEventData;
import uk.gov.hmcts.reform.fpl.service.CaseAssignmentService;
import uk.gov.hmcts.reform.fpl.service.ManageLocalAuthorityService;
import uk.gov.hmcts.reform.fpl.utils.CaseDetailsHelper;
import uk.gov.hmcts.reform.fpl.utils.CaseDetailsMap;

import java.util.List;

import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;
import static uk.gov.hmcts.reform.fpl.enums.LocalAuthorityAction.ADD;
import static uk.gov.hmcts.reform.fpl.enums.LocalAuthorityAction.REMOVE;
import static uk.gov.hmcts.reform.fpl.utils.CaseDetailsMap.caseDetailsMap;

@Api
@RestController
@RequestMapping("/callback/manage-local-authorities")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ManageLocalAuthorityController extends CallbackController {

    private final ManageLocalAuthorityService service;
    private final CaseAssignmentService caseAssignmentService;

    @PostMapping("/about-to-start")
    public AboutToStartOrSubmitCallbackResponse handleAboutToStart(@RequestBody CallbackRequest request) {
        final CaseDetails caseDetails = request.getCaseDetails();
        final CaseData caseData = getCaseData(caseDetails);

        caseDetails.getData().put("localAuthoritiesToShare", service.getLocalAuthoritiesToShare(caseData));

        return respond(caseDetails);
    }

    @PostMapping("local-authority/mid-event")
    public AboutToStartOrSubmitCallbackResponse handleMidEvent(@RequestBody CallbackRequest request) {
        final CaseDetails caseDetails = request.getCaseDetails();
        final CaseData caseData = getCaseData(caseDetails);
        final CaseDetailsMap data = caseDetailsMap(caseDetails);
        final LocalAuthoritiesEventData eventData = caseData.getLocalAuthoritiesEventData();
        final LocalAuthorityAction action = eventData.getLocalAuthorityAction();

        final List<String> errors = service.validateAction(caseData);

        if (isNotEmpty(errors)) {
            return respond(data, errors);
        }

        if (ADD == action) {
            data.put("localAuthorityEmail", service.getEmail(eventData));
        } else if (REMOVE == action) {
            data.put("localAuthorityToRemove", caseData.getSharedLocalAuthorityPolicy().getOrganisation()
                .getOrganisationName());
        }

        return respond(data);
    }

    @PostMapping("local-authority-validate/mid-event")
    public AboutToStartOrSubmitCallbackResponse validate(@RequestBody CallbackRequest request) {
        final CaseDetails caseDetails = request.getCaseDetails();
        final CaseData caseData = getCaseData(caseDetails);

        final List<String> errors = service.validateEmail(caseData.getLocalAuthoritiesEventData());

        return respond(caseDetails, errors);
    }

    @PostMapping("/about-to-submit")
    public AboutToStartOrSubmitCallbackResponse handleAboutToSubmit(@RequestBody CallbackRequest request) {
        final CaseDetails caseDetails = request.getCaseDetails();
        final CaseData caseData = getCaseData(caseDetails);

        final LocalAuthorityAction action = caseData.getLocalAuthoritiesEventData().getLocalAuthorityAction();

        if (ADD.equals(action)) {
            caseDetails.getData().put("sharedLocalAuthorityPolicy", service.buildSharedLocalAuthorityPolicy(caseData));
            caseDetails.getData().put("localAuthorities", service.addSharedLocalAuthority(caseData));

            return respond(removeTemporaryFields(caseDetails));
        }

        if (REMOVE.equals(action)) {
            caseDetails.getData().put("changeOrganisationRequestField", service.getOrgRemovalRequest(caseData));
            caseDetails.getData().put("localAuthorities", service.removeSharedLocalAuthority(caseData));

            return caseAssignmentService.applyDecisionAsSystemUser(removeTemporaryFields(caseDetails));
        }

        return respond(removeTemporaryFields(caseDetails));
    }

    @PostMapping("/submitted")
    public void handleSubmitted(@RequestBody CallbackRequest request) {
        final CaseData caseDataBefore = getCaseDataBefore(request);
        final CaseData caseData = getCaseData(request);

        service.getChangeEvent(caseData, caseDataBefore).ifPresent(this::publishEvent);
    }

    private static CaseDetails removeTemporaryFields(CaseDetails caseDetails) {
        return CaseDetailsHelper.removeTemporaryFields(caseDetails, LocalAuthoritiesEventData.class);
    }

}
