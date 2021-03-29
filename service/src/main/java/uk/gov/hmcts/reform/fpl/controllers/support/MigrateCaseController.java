package uk.gov.hmcts.reform.fpl.controllers.support;

import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackRequest;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.fpl.controllers.CallbackController;
import uk.gov.hmcts.reform.fpl.model.CaseData;
import uk.gov.hmcts.reform.fpl.model.HearingBooking;
import uk.gov.hmcts.reform.fpl.model.HearingFurtherEvidenceBundle;
import uk.gov.hmcts.reform.fpl.model.SupportingEvidenceBundle;
import uk.gov.hmcts.reform.fpl.model.common.C2DocumentBundle;
import uk.gov.hmcts.reform.fpl.model.common.Element;
import uk.gov.hmcts.reform.fpl.model.order.HearingOrder;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;

@Api
@RestController
@RequestMapping("/callback/migrate-case")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class MigrateCaseController extends CallbackController {
    private static final String MIGRATION_ID_KEY = "migrationId";
    private static final String FMN_ERROR_MESSAGE = "Unexpected FMN ";

    @PostMapping("/about-to-submit")
    public AboutToStartOrSubmitCallbackResponse handleAboutToSubmit(@RequestBody CallbackRequest callbackRequest) {
        CaseDetails caseDetails = callbackRequest.getCaseDetails();
        Object migrationId = caseDetails.getData().get(MIGRATION_ID_KEY);

        if ("FPLA-2774".equals(migrationId)) {
            run2774(caseDetails);
        }

        if ("FPLA-2905".equals(migrationId)) {
            run2905(caseDetails);
        }

        if ("FPLA-2872".equals(migrationId)) {
            run2872(caseDetails);
        }

        if ("FPLA-2871".equals(migrationId)) {
            run2871(caseDetails);
        }

        if ("FPLA-2885".equals(migrationId)) {
            run2885(caseDetails);
        }

        if ("FPLA-2913".equals(migrationId)) {
            run2913(caseDetails);
        }

        caseDetails.getData().remove(MIGRATION_ID_KEY);
        return respond(caseDetails);
    }

    private void run2913(CaseDetails caseDetails) {
        CaseData caseData = getCaseData(caseDetails);

        if ("SA20C50026".equals(caseData.getFamilyManCaseNumber())) {

            if (caseData.getSealedCMOs().size() < 3) {
                throw new IllegalArgumentException(
                    "Expected at least 3 sealed cmos, but found " + caseData.getSealedCMOs().size());
            }

            Element<HearingOrder> sealedCmo = caseData.getSealedCMOs().get(2);
            List<Element<SupportingEvidenceBundle>> supportingDocuments = sealedCmo.getValue().getSupportingDocs();

            Set<String> documentToBeRemoved = Set.of("Draft Placement Order", "Final Placement Order");

            List<Element<SupportingEvidenceBundle>> supportingDocumentsToBeRemoved = supportingDocuments.stream()
                .filter(doc -> documentToBeRemoved.contains(doc.getValue().getName()))
                .collect(toList());

            if (supportingDocumentsToBeRemoved.size() != 2) {
                throw new IllegalStateException(
                    "Expected 2 documents to be removed, found " + supportingDocumentsToBeRemoved.size());
            }

            supportingDocuments.removeAll(supportingDocumentsToBeRemoved);

            caseDetails.getData().put("sealedCMOs", caseData.getSealedCMOs());

            List<UUID> documentsToBeRemovedIds = supportingDocumentsToBeRemoved.stream()
                .map(Element::getId)
                .collect(toList());

            List<Element<HearingFurtherEvidenceBundle>> evidenceBundles = caseData.getHearingFurtherEvidenceDocuments();

            List<Element<HearingFurtherEvidenceBundle>> hearingBundles = evidenceBundles.stream()
                .filter(bundle -> bundle.getValue().getSupportingEvidenceBundle().stream()
                    .anyMatch(doc -> documentsToBeRemovedIds.contains(doc.getId())))
                .collect(Collectors.toList());

            if (hearingBundles.size() > 1) {
                throw new IllegalStateException(
                    "Expected 1 hearing bundle with documents to be removed, found " + hearingBundles.size());
            }

            if (hearingBundles.size() == 1) {
                hearingBundles.get(0).getValue().getSupportingEvidenceBundle()
                    .removeIf(doc -> documentsToBeRemovedIds.contains(doc.getId()));
                caseDetails.getData().put("hearingFurtherEvidenceDocuments", evidenceBundles);
            } else {
                log.info("No hearing bundle with supporting documents to be removed");
            }

        } else {
            throw new IllegalStateException(FMN_ERROR_MESSAGE + caseData.getFamilyManCaseNumber());
        }
    }

    private void run2774(CaseDetails caseDetails) {
        CaseData caseData = getCaseData(caseDetails);

        if ("NE21C50007".equals(caseData.getFamilyManCaseNumber())) {
            List<Element<HearingBooking>> hearings = caseData.getHearingDetails();

            if (ObjectUtils.isEmpty(hearings)) {
                throw new IllegalArgumentException("No hearings in the case");
            }

            if (hearings.size() < 2) {
                throw new IllegalArgumentException(String.format("Expected 2 hearings in the case but found %s",
                    hearings.size()));
            }

            Element<HearingBooking> hearingToBeRemoved = hearings.get(1);

            hearings.remove(hearingToBeRemoved);

            caseDetails.getData().put("hearingDetails", hearings);
        }
    }


    private void run2905(CaseDetails caseDetails) {
        CaseData caseData = getCaseData(caseDetails);

        if ("CF20C50047".equals(caseData.getFamilyManCaseNumber())) {

            if (isEmpty(caseData.getC2DocumentBundle())) {
                throw new IllegalArgumentException("No C2 document bundles in the case");
            }

            caseData.getC2DocumentBundle().remove(1);
            caseDetails.getData().put("c2DocumentBundle", caseData.getC2DocumentBundle());
        } else {
            throw new IllegalStateException(FMN_ERROR_MESSAGE + caseData.getFamilyManCaseNumber());
        }
    }

    private void run2872(CaseDetails caseDetails) {
        CaseData caseData = getCaseData(caseDetails);

        if ("NE20C50023".equals(caseData.getFamilyManCaseNumber())) {
            if (isEmpty(caseData.getC2DocumentBundle())) {
                throw new IllegalArgumentException("No C2 document bundles in the case");
            }

            if (caseData.getC2DocumentBundle().size() < 5) {
                throw new IllegalArgumentException(String.format("Expected at least 5 C2 document bundles in the case"
                    + " but found %s", caseData.getC2DocumentBundle().size()));
            }

            caseData.getC2DocumentBundle().remove(4);
            caseData.getC2DocumentBundle().remove(3);
            caseData.getC2DocumentBundle().remove(2);

            caseDetails.getData().put("c2DocumentBundle", caseData.getC2DocumentBundle());
        } else {
            throw new IllegalStateException(FMN_ERROR_MESSAGE + caseData.getFamilyManCaseNumber());
        }
    }

    private void run2871(CaseDetails caseDetails) {
        CaseData caseData = getCaseData(caseDetails);

        if ("WR20C50015".equals(caseData.getFamilyManCaseNumber())) {
            log.info("Attempting to remove first C2 from WR20C50015");
            removeFirstC2(caseDetails);
            log.info("Successfully removed C2 from WR20C50015");
        }
    }

    private void removeFirstC2(CaseDetails caseDetails) {
        CaseData caseData = getCaseData(caseDetails);
        List<Element<C2DocumentBundle>> c2DocumentBundle = caseData.getC2DocumentBundle();

        if (isEmpty(c2DocumentBundle)) {
            throw new IllegalArgumentException("No C2s on case");
        }

        c2DocumentBundle.remove(0);

        caseDetails.getData().put("c2DocumentBundle", c2DocumentBundle);

    }

    private void run2885(CaseDetails caseDetails) {
        CaseData caseData = getCaseData(caseDetails);

        if (isEmpty(caseData.getCancelledHearingDetails())) {
            throw new IllegalArgumentException("Case does not contain cancelled hearing bookings");
        }

        caseData.getCancelledHearingDetails().forEach(hearingBookingElement -> {
            switch (hearingBookingElement.getValue().getCancellationReason()) {
                case "OT8":
                    hearingBookingElement.getValue().setCancellationReason("IN1");
                    break;
                case "OT9":
                    hearingBookingElement.getValue().setCancellationReason("OT8");
                    break;
                case "OT10":
                    hearingBookingElement.getValue().setCancellationReason("OT9");
                    break;
            }
        });

        caseDetails.getData().put("cancelledHearingDetails", caseData.getCancelledHearingDetails());
    }
}
