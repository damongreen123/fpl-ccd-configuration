package uk.gov.hmcts.reform.fpl.controllers;

import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.ccd.client.model.CallbackRequest;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.fpl.enums.SolicitorRole;
import uk.gov.hmcts.reform.fpl.events.NoticeOfChangeEvent;
import uk.gov.hmcts.reform.fpl.model.CaseData;
import uk.gov.hmcts.reform.fpl.model.common.Element;
import uk.gov.hmcts.reform.fpl.model.interfaces.WithSolicitor;
import uk.gov.hmcts.reform.fpl.service.CaseAssignmentService;
import uk.gov.hmcts.reform.fpl.service.NoticeOfChangeService;
import uk.gov.hmcts.reform.fpl.service.RespondentService;
import uk.gov.hmcts.reform.fpl.service.legalcounsel.RepresentableLegalCounselUpdater;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

@Api
@RestController
@RequestMapping("/callback/noc-decision")
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class NoticeOfChangeController extends CallbackController {

    private final CaseAssignmentService caseAssignmentService;
    private final NoticeOfChangeService noticeOfChangeService;
    private final RespondentService respondentService;
    private final RepresentableLegalCounselUpdater legalCounselUpdater;

    @PostMapping("/about-to-start")
    public CallbackResponse handleAboutToStart(@RequestBody CallbackRequest request) {
        CaseDetails caseDetails = request.getCaseDetails();
        CaseData caseData = getCaseData(caseDetails);
        // deep copy of the original case data to ensure that we preserve the original
        // in about-to-start caseDetailsBefore is null, this makes sense as this is the first callback that can be
        // hit so there wouldn't be any difference in caseDetails and caseDetailsBefore
        CaseData originalCaseData = getCaseData(caseDetails);

        caseDetails.getData().putAll(noticeOfChangeService.updateRepresentation(caseData));

        caseData = getCaseData(caseDetails);

        caseDetails.getData().putAll(legalCounselUpdater.updateLegalCounselFromNoC(caseData, originalCaseData));

        return caseAssignmentService.applyDecision(caseDetails);
    }

    @PostMapping("/submitted")
    public void handleSubmittedEvent(@RequestBody CallbackRequest callbackRequest) {
        CaseData oldCaseData = getCaseDataBefore(callbackRequest);
        CaseData newCaseData = getCaseData(callbackRequest);

        Stream.of(SolicitorRole.Representing.values())
            .flatMap(role -> legalCounselUpdater.buildEventsForAccessRemoval(newCaseData, oldCaseData, role).stream())
            .forEach(this::publishEvent);

        Stream.of(SolicitorRole.Representing.values())
            .flatMap(solicitorRole ->
                respondentService.getRepresentationChanges(
                    solicitorRole.getTarget().apply(newCaseData),
                    solicitorRole.getTarget().apply(oldCaseData),
                    solicitorRole
                ).stream())
            .forEach(
                changeRequest -> {
                    SolicitorRole caseRole = changeRequest.getCaseRole();
                    Function<CaseData, List<Element<WithSolicitor>>> target = caseRole.getRepresenting().getTarget();
                    int solicitorIndex = caseRole.getIndex();
                    publishEvent(new NoticeOfChangeEvent(
                        newCaseData,
                        target.apply(oldCaseData).get(solicitorIndex).getValue(),
                        target.apply(newCaseData).get(solicitorIndex).getValue())
                    );
                }
            );
    }
}
