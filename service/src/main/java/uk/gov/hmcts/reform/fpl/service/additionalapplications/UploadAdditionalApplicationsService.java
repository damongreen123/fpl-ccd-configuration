package uk.gov.hmcts.reform.fpl.service.additionalapplications;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.fpl.enums.AdditionalApplicationType;
import uk.gov.hmcts.reform.fpl.enums.ApplicationType;
import uk.gov.hmcts.reform.fpl.model.CaseData;
import uk.gov.hmcts.reform.fpl.model.Other;
import uk.gov.hmcts.reform.fpl.model.Respondent;
import uk.gov.hmcts.reform.fpl.model.Supplement;
import uk.gov.hmcts.reform.fpl.model.SupportingEvidenceBundle;
import uk.gov.hmcts.reform.fpl.model.common.AdditionalApplicationsBundle;
import uk.gov.hmcts.reform.fpl.model.common.AdditionalApplicationsBundle.AdditionalApplicationsBundleBuilder;
import uk.gov.hmcts.reform.fpl.model.common.C2DocumentBundle;
import uk.gov.hmcts.reform.fpl.model.common.DocumentReference;
import uk.gov.hmcts.reform.fpl.model.common.Element;
import uk.gov.hmcts.reform.fpl.model.common.OtherApplicationsBundle;
import uk.gov.hmcts.reform.fpl.model.common.dynamic.DynamicList;
import uk.gov.hmcts.reform.fpl.model.common.dynamic.DynamicListElement;
import uk.gov.hmcts.reform.fpl.model.document.SealType;
import uk.gov.hmcts.reform.fpl.service.DocumentSealingService;
import uk.gov.hmcts.reform.fpl.service.PeopleInCaseService;
import uk.gov.hmcts.reform.fpl.service.UserService;
import uk.gov.hmcts.reform.fpl.service.docmosis.DocumentConversionService;
import uk.gov.hmcts.reform.fpl.service.time.Time;
import uk.gov.hmcts.reform.fpl.utils.DocumentUploadHelper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;
import static java.util.Comparator.reverseOrder;
import static java.util.Objects.isNull;
import static java.util.function.Predicate.not;
import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static uk.gov.hmcts.reform.fpl.enums.ApplicationType.C2_APPLICATION;
import static uk.gov.hmcts.reform.fpl.utils.DateFormatterHelper.DATE_TIME;
import static uk.gov.hmcts.reform.fpl.utils.DateFormatterHelper.formatLocalDateTimeBaseUsingFormat;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class UploadAdditionalApplicationsService {

    private static final String APPLICANT_SOMEONE_ELSE = "SOMEONE_ELSE";

    private final Time time;
    private final UserService user;
    private final DocumentUploadHelper documentUploadHelper;
    private final DocumentSealingService documentSealingService;
    private final DocumentConversionService documentConversionService;
    private final PeopleInCaseService peopleInCaseService;

    public List<ApplicationType> getApplicationTypes(AdditionalApplicationsBundle bundle) {
        List<ApplicationType> applicationTypes = new ArrayList<>();
        if (!isNull(bundle.getC2DocumentBundle())) {
            applicationTypes.add(C2_APPLICATION);
        }

        if (!isNull(bundle.getOtherApplicationsBundle())) {
            applicationTypes.add(ApplicationType.valueOf(
                bundle.getOtherApplicationsBundle().getApplicationType().name()));
        }
        return applicationTypes;
    }

    public AdditionalApplicationsBundle buildAdditionalApplicationsBundle(CaseData caseData) {
        String applicantName = getSelectedApplicantName(caseData.getApplicantsList(), caseData.getOtherApplicant())
            .filter(not(String::isBlank))
            .orElseThrow(() -> new IllegalArgumentException("Applicant should not be empty"));

        final String uploadedBy = documentUploadHelper.getUploadedDocumentUserDetails();
        final LocalDateTime now = time.now();

        List<Element<Other>> selectedOthers = peopleInCaseService.getSelectedOthers(caseData);
        List<Element<Respondent>> selectedRespondents = peopleInCaseService.getSelectedRespondents(caseData);
        String othersNotified = peopleInCaseService.getPeopleNotified(
            caseData.getRepresentatives(), selectedRespondents, selectedOthers
        );

        AdditionalApplicationsBundleBuilder additionalApplicationsBundleBuilder = AdditionalApplicationsBundle.builder()
            .pbaPayment(caseData.getTemporaryPbaPayment())
            .amountToPay(caseData.getAmountToPay())
            .author(uploadedBy)
            .uploadedDateTime(formatLocalDateTimeBaseUsingFormat(now, DATE_TIME));

        List<AdditionalApplicationType> additionalApplicationTypeList = caseData.getAdditionalApplicationType();
        if (additionalApplicationTypeList.contains(AdditionalApplicationType.C2_ORDER)) {
            additionalApplicationsBundleBuilder.c2DocumentBundle(buildC2DocumentBundle(
                caseData, applicantName, selectedOthers, selectedRespondents, othersNotified, uploadedBy, now
            ));
        }

        if (additionalApplicationTypeList.contains(AdditionalApplicationType.OTHER_ORDER)) {
            additionalApplicationsBundleBuilder.otherApplicationsBundle(buildOtherApplicationsBundle(
                caseData, applicantName, selectedOthers, selectedRespondents, othersNotified, uploadedBy, now
            ));
        }

        return additionalApplicationsBundleBuilder.build();
    }

    public List<Element<C2DocumentBundle>> sortOldC2DocumentCollection(List<Element<C2DocumentBundle>> bundles) {
        bundles.sort(comparing(bundle -> bundle.getValue().getUploadedDateTime(), reverseOrder()));
        return bundles;
    }

    private Optional<String> getSelectedApplicantName(DynamicList applicantsList, String otherApplicant) {
        if (Objects.nonNull(applicantsList)) {
            DynamicListElement selectedElement = applicantsList.getValue();

            if (isNotEmpty(selectedElement)) {
                if (APPLICANT_SOMEONE_ELSE.equals(selectedElement.getCode())) {
                    return isBlank(otherApplicant) ? Optional.empty() : Optional.of(otherApplicant);
                } else {
                    return Optional.of(selectedElement.getLabel());
                }
            }
        }
        return Optional.empty();
    }

    private C2DocumentBundle buildC2DocumentBundle(CaseData caseData,
                                                   String applicantName,
                                                   List<Element<Other>> selectedOthers,
                                                   List<Element<Respondent>> selectedRespondents,
                                                   String othersNotified,
                                                   String uploadedBy,
                                                   LocalDateTime uploadedTime) {
        C2DocumentBundle temporaryC2Document = caseData.getTemporaryC2Document();

        List<Element<SupportingEvidenceBundle>> updatedSupportingEvidenceBundle = getSupportingEvidenceBundle(
            temporaryC2Document.getSupportingEvidenceBundle(), uploadedBy, uploadedTime
        );

        List<Element<Supplement>> updatedSupplementsBundle =
            getSupplementsBundle(temporaryC2Document.getSupplementsBundle(),
                uploadedBy, uploadedTime, SealType.ENGLISH);


        return temporaryC2Document.toBuilder()
            .id(UUID.randomUUID())
            .applicantName(applicantName)
            .author(uploadedBy)
            .document(getDocumentToStore(temporaryC2Document.getDocument(), SealType.ENGLISH))
            .uploadedDateTime(formatLocalDateTimeBaseUsingFormat(uploadedTime, DATE_TIME))
            .supplementsBundle(updatedSupplementsBundle)
            .supportingEvidenceBundle(updatedSupportingEvidenceBundle)
            .type(caseData.getC2Type())
            .respondents(selectedRespondents)
            .others(selectedOthers)
            .othersNotified(othersNotified)
            .build();
    }

    private OtherApplicationsBundle buildOtherApplicationsBundle(CaseData caseData,
                                                                 String applicantName,
                                                                 List<Element<Other>> selectedOthers,
                                                                 List<Element<Respondent>> selectedRespondents,
                                                                 String othersNotified,
                                                                 String uploadedBy,
                                                                 LocalDateTime uploadedTime) {
        OtherApplicationsBundle temporaryOtherApplicationsBundle = caseData.getTemporaryOtherApplicationsBundle();

        List<Element<SupportingEvidenceBundle>> updatedSupportingEvidenceBundle = getSupportingEvidenceBundle(
            temporaryOtherApplicationsBundle.getSupportingEvidenceBundle(), uploadedBy, uploadedTime
        );

        List<Element<Supplement>> updatedSupplementsBundle = getSupplementsBundle(
            temporaryOtherApplicationsBundle.getSupplementsBundle(), uploadedBy, uploadedTime, SealType.ENGLISH);


        return temporaryOtherApplicationsBundle.toBuilder()
            .author(uploadedBy)
            .id(UUID.randomUUID())
            .applicantName(applicantName)
            .uploadedDateTime(formatLocalDateTimeBaseUsingFormat(uploadedTime, DATE_TIME))
            .applicationType(temporaryOtherApplicationsBundle.getApplicationType())
            .document(getDocumentToStore(temporaryOtherApplicationsBundle.getDocument(), SealType.ENGLISH))
            .supportingEvidenceBundle(updatedSupportingEvidenceBundle)
            .supplementsBundle(updatedSupplementsBundle)
            .respondents(selectedRespondents)
            .others(selectedOthers)
            .othersNotified(othersNotified)
            .build();
    }

    private List<Element<SupportingEvidenceBundle>> getSupportingEvidenceBundle(
        List<Element<SupportingEvidenceBundle>> supportingEvidenceBundle,
        String uploadedBy, LocalDateTime uploadedDateTime) {

        supportingEvidenceBundle.forEach(supportingEvidence -> {
            supportingEvidence.getValue().setDateTimeUploaded(uploadedDateTime);
            supportingEvidence.getValue().setUploadedBy(uploadedBy);
        });

        return supportingEvidenceBundle;
    }

    private List<Element<Supplement>> getSupplementsBundle(
        List<Element<Supplement>> supplementsBundle, String uploadedBy, LocalDateTime dateTime, SealType sealType) {

        return supplementsBundle.stream().map(supplementElement -> {
            Supplement incomingSupplement = supplementElement.getValue();

            DocumentReference sealedDocument = documentSealingService.sealDocument(incomingSupplement.getDocument(),
                sealType);
            Supplement modifiedSupplement = incomingSupplement.toBuilder()
                .document(sealedDocument)
                .dateTimeUploaded(dateTime)
                .uploadedBy(uploadedBy)
                .build();

            return supplementElement.toBuilder().value(modifiedSupplement).build();
        }).collect(Collectors.toList());

    }

    private DocumentReference getDocumentToStore(DocumentReference originalDoc, SealType sealType) {
        return user.isHmctsUser() ? documentSealingService.sealDocument(originalDoc, sealType)
                                  : documentConversionService.convertToPdf(originalDoc);
    }
}
