package uk.gov.hmcts.reform.fpl.controllers.documents;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.OverrideAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.fpl.controllers.AbstractCallbackTest;
import uk.gov.hmcts.reform.fpl.enums.ManageDocumentType;
import uk.gov.hmcts.reform.fpl.model.CaseData;
import uk.gov.hmcts.reform.fpl.model.HearingBooking;
import uk.gov.hmcts.reform.fpl.model.HearingFurtherEvidenceBundle;
import uk.gov.hmcts.reform.fpl.model.ManageDocument;
import uk.gov.hmcts.reform.fpl.model.Respondent;
import uk.gov.hmcts.reform.fpl.model.RespondentStatement;
import uk.gov.hmcts.reform.fpl.model.SupportingEvidenceBundle;
import uk.gov.hmcts.reform.fpl.model.common.AdditionalApplicationsBundle;
import uk.gov.hmcts.reform.fpl.model.common.C2DocumentBundle;
import uk.gov.hmcts.reform.fpl.model.common.Element;
import uk.gov.hmcts.reform.fpl.model.common.OtherApplicationsBundle;
import uk.gov.hmcts.reform.fpl.model.common.dynamic.DynamicList;
import uk.gov.hmcts.reform.fpl.service.UserService;
import uk.gov.hmcts.reform.fpl.testingsupport.DynamicListHelper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static uk.gov.hmcts.reform.fpl.enums.CaseRole.SOLICITORA;
import static uk.gov.hmcts.reform.fpl.enums.CaseRole.representativeSolicitors;
import static uk.gov.hmcts.reform.fpl.enums.FurtherEvidenceType.GUARDIAN_REPORTS;
import static uk.gov.hmcts.reform.fpl.enums.ManageDocumentSubtypeList.OTHER;
import static uk.gov.hmcts.reform.fpl.enums.ManageDocumentSubtypeList.RESPONDENT_STATEMENT;
import static uk.gov.hmcts.reform.fpl.enums.ManageDocumentType.ADDITIONAL_APPLICATIONS_DOCUMENTS;
import static uk.gov.hmcts.reform.fpl.enums.ManageDocumentType.CORRESPONDENCE;
import static uk.gov.hmcts.reform.fpl.enums.OtherApplicationType.C12_WARRANT_TO_ASSIST_PERSON;
import static uk.gov.hmcts.reform.fpl.enums.YesNo.YES;
import static uk.gov.hmcts.reform.fpl.utils.CaseDataGeneratorHelper.createHearingBooking;
import static uk.gov.hmcts.reform.fpl.utils.DateFormatterHelper.DATE_TIME;
import static uk.gov.hmcts.reform.fpl.utils.DateFormatterHelper.formatLocalDateTimeBaseUsingFormat;
import static uk.gov.hmcts.reform.fpl.utils.ElementUtils.element;
import static uk.gov.hmcts.reform.fpl.utils.ElementUtils.wrapElements;
import static uk.gov.hmcts.reform.fpl.utils.TestDataHelper.testRespondent;

@WebMvcTest(ManageDocumentsController.class)
@OverrideAutoConfiguration(enabled = true)
class ManageDocumentsControllerMidEventTest extends AbstractCallbackTest {

    private static final String USER_ROLES = "caseworker-publiclaw-courtadmin";
    private static final long CASE_ID = 12345L;

    @Autowired
    private DynamicListHelper dynamicLists;

    @MockBean
    private UserService userService;

    ManageDocumentsControllerMidEventTest() {
        super("manage-documents");
    }

    @BeforeEach
    void init() {
        given(userService.hasAnyCaseRoleFrom(representativeSolicitors(), CASE_ID)).willReturn(false);
        given(userService.isHmctsUser()).willReturn(true);
    }

    @Test
    void shouldInitialiseCorrespondenceCollectionForHMCTS() {
        List<Element<SupportingEvidenceBundle>> correspondenceDocuments = buildSupportingEvidenceBundle();

        CaseData caseData = CaseData.builder()
            .id(CASE_ID)
            .correspondenceDocuments(correspondenceDocuments)
            .manageDocument(buildManagementDocument(CORRESPONDENCE))
            .build();

        CaseData extractedCaseData = extractCaseData(postMidEvent(caseData, "initialise-manage-document-collections"));

        assertThat(extractedCaseData.getCorrespondenceDocuments()).isEqualTo(correspondenceDocuments);

        assertThat(extractedCaseData.getManageDocument()).isEqualTo(ManageDocument.builder()
            .type(CORRESPONDENCE)
            .hasHearings("No")
            .hasC2s("No")
            .build());
    }

    @Test
    void shouldInitialiseCorrespondenceCollectionForSolicitor() {
        given(userService.hasAnyCaseRoleFrom(representativeSolicitors(), CASE_ID)).willReturn(true);
        given(userService.isHmctsUser()).willReturn(false);
        List<Element<SupportingEvidenceBundle>> correspondenceDocuments = buildSupportingEvidenceBundle();

        CaseData caseData = CaseData.builder()
            .id(CASE_ID)
            .correspondenceDocumentsSolicitor(correspondenceDocuments)
            .manageDocument(buildManagementDocument(CORRESPONDENCE))
            .build();

        CaseData extractedCaseData = extractCaseData(postMidEvent(caseData, "initialise-manage-document-collections"));

        assertThat(extractedCaseData.getCorrespondenceDocumentsSolicitor()).isEqualTo(correspondenceDocuments);

        assertThat(extractedCaseData.getManageDocument()).isEqualTo(ManageDocument.builder()
            .type(CORRESPONDENCE)
            .hasHearings("No")
            .hasC2s("No")
            .build());
    }

    @Test
    void shouldInitialiseC2SupportingDocumentsWhenTheSelectedC2IsInC2DocumentsBundle() {
        UUID selectedC2DocumentId = UUID.randomUUID();
        LocalDateTime today = LocalDateTime.now();

        List<Element<SupportingEvidenceBundle>> c2EvidenceDocuments = buildSupportingEvidenceBundle();

        List<Element<C2DocumentBundle>> c2DocumentBundle = List.of(
            element(buildC2DocumentBundle(today.plusDays(2))),
            element(selectedC2DocumentId, buildC2DocumentBundle(c2EvidenceDocuments)),
            element(buildC2DocumentBundle(today.plusDays(2))));

        CaseData caseData = CaseData.builder()
            .id(CASE_ID)
            .c2DocumentBundle(c2DocumentBundle)
            .manageDocument(buildManagementDocument(ADDITIONAL_APPLICATIONS_DOCUMENTS))
            .manageDocumentsSupportingC2List(selectedC2DocumentId)
            .build();

        CaseData extractedCaseData = extractCaseData(postMidEvent(
            caseData, "initialise-manage-document-collections", USER_ROLES
        ));

        assertThat(extractedCaseData.getSupportingEvidenceDocumentsTemp()).isEqualTo(c2EvidenceDocuments);

        assertThat(extractedCaseData.getManageDocument()).isEqualTo(ManageDocument.builder()
            .type(ADDITIONAL_APPLICATIONS_DOCUMENTS)
            .hasHearings("No")
            .hasC2s("Yes")
            .build());
    }

    @Test
    void shouldInitialiseC2SupportingDocumentsWhenSelectedC2ExistsInAdditionalApplications() {
        UUID selectedBundleId = UUID.randomUUID();
        LocalDateTime today = LocalDateTime.now();

        List<Element<SupportingEvidenceBundle>> c2EvidenceDocuments = buildSupportingEvidenceBundle();

        List<Element<C2DocumentBundle>> c2DocumentBundle = List.of(
            element(buildC2DocumentBundle(today.plusDays(2))),
            element(buildC2DocumentBundle(today.plusDays(2))));

        C2DocumentBundle selectedC2DocumentBundle = C2DocumentBundle.builder()
            .id(selectedBundleId).supportingEvidenceBundle(c2EvidenceDocuments)
            .uploadedDateTime(formatLocalDateTimeBaseUsingFormat(today.plusDays(1), DATE_TIME))
            .build();

        OtherApplicationsBundle otherApplicationsBundle = OtherApplicationsBundle.builder()
            .id(randomUUID()).applicationType(C12_WARRANT_TO_ASSIST_PERSON)
            .uploadedDateTime(formatLocalDateTimeBaseUsingFormat(today.plusDays(1), DATE_TIME))
            .build();

        CaseData caseData = CaseData.builder()
            .id(CASE_ID)
            .c2DocumentBundle(c2DocumentBundle)
            .additionalApplicationsBundle(wrapElements(AdditionalApplicationsBundle.builder()
                .c2DocumentBundle(selectedC2DocumentBundle)
                .otherApplicationsBundle(otherApplicationsBundle).build()))
            .manageDocument(buildManagementDocument(ADDITIONAL_APPLICATIONS_DOCUMENTS))
            .manageDocumentsSupportingC2List(selectedBundleId)
            .build();

        CaseData extractedCaseData = extractCaseData(postMidEvent(
            caseData, "initialise-manage-document-collections", USER_ROLES
        ));

        assertThat(extractedCaseData.getSupportingEvidenceDocumentsTemp()).isEqualTo(c2EvidenceDocuments);
    }

    @Test
    void shouldInitialiseOtherApplicationSupportingDocumentsWhenSelectedApplicationExistsInAdditionalApplications() {
        UUID selectedBundleId = UUID.randomUUID();
        LocalDateTime today = LocalDateTime.now();

        List<Element<SupportingEvidenceBundle>> c2EvidenceDocuments = buildSupportingEvidenceBundle();

        List<Element<C2DocumentBundle>> c2DocumentBundle = List.of(
            element(buildC2DocumentBundle(today.plusDays(2))),
            element(buildC2DocumentBundle(today.plusDays(2))));

        C2DocumentBundle selectedC2DocumentBundle = buildC2DocumentBundle(today.plusDays(1));

        OtherApplicationsBundle otherApplicationsBundle = OtherApplicationsBundle.builder()
            .id(selectedBundleId).applicationType(C12_WARRANT_TO_ASSIST_PERSON)
            .supportingEvidenceBundle(c2EvidenceDocuments)
            .uploadedDateTime(formatLocalDateTimeBaseUsingFormat(today.plusDays(1), DATE_TIME))
            .build();

        CaseData caseData = CaseData.builder()
            .id(CASE_ID)
            .c2DocumentBundle(c2DocumentBundle)
            .additionalApplicationsBundle(wrapElements(AdditionalApplicationsBundle.builder()
                .c2DocumentBundle(selectedC2DocumentBundle)
                .otherApplicationsBundle(otherApplicationsBundle).build()))
            .manageDocument(buildManagementDocument(ADDITIONAL_APPLICATIONS_DOCUMENTS))
            .manageDocumentsSupportingC2List(selectedBundleId)
            .build();

        CaseData extractedCaseData = extractCaseData(postMidEvent(
            caseData, "initialise-manage-document-collections", USER_ROLES
        ));

        assertThat(extractedCaseData.getSupportingEvidenceDocumentsTemp()).isEqualTo(c2EvidenceDocuments);
    }

    @Test
    void shouldThrowExceptionIfUserIsAdminAndHasSolicitorCaseRoles() {
        given(userService.getCaseRoles(CASE_ID)).willReturn(Set.of(SOLICITORA));
        given(userService.isHmctsUser()).willReturn(true);
        given(userService.getUserEmail()).willReturn("test-email@example.com");

        final CaseData caseData = CaseData.builder()
            .id(CASE_ID)
            .manageDocument(buildManagementDocument(ADDITIONAL_APPLICATIONS_DOCUMENTS))
            .build();

        assertThatThrownBy(() -> postMidEvent(caseData, "initialise-manage-document-collections"))
            .hasMessageContaining("User test-email@example.com is HMCTS but has solicitor case roles");
    }

    @Test
    void shouldReturnErrorWhenNoC2sOnCaseAndUserSelectsC2SupportingDocs() {
        CaseData caseData = CaseData.builder()
            .id(CASE_ID)
            .manageDocument(buildManagementDocument(ADDITIONAL_APPLICATIONS_DOCUMENTS))
            .build();
        AboutToStartOrSubmitCallbackResponse callbackResponse = postMidEvent(caseData,
            "initialise-manage-document-collections");

        assertThat(callbackResponse.getErrors()).containsExactly(
            "There are no additional applications to associate supporting documents with");
    }

    @Test
    void shouldReturnValidationErrorsIfSupportingEvidenceDateTimeReceivedOnFurtherEvidenceIsInTheFuture() {
        LocalDateTime futureDate = LocalDateTime.now().plusDays(1);
        CaseData caseData = CaseData.builder()
            .id(CASE_ID)
            .supportingEvidenceDocumentsTemp(List.of(
                element(SupportingEvidenceBundle.builder().dateTimeReceived(futureDate).build())
            ))
            .build();

        AboutToStartOrSubmitCallbackResponse response = postMidEvent(caseData, "validate-supporting-evidence");

        assertThat(response.getErrors()).isNotEmpty();
        assertThat(response.getErrors()).containsExactly("Date received cannot be in the future");
    }

    @Test
    void shouldReturnNoValidationErrorsIfSupportingEvidenceDateTimeReceivedOnFurtherEvidenceIsInThePast() {
        LocalDateTime pastDate = LocalDateTime.now().minusDays(2);
        CaseData caseData = CaseData.builder()
            .id(CASE_ID)
            .supportingEvidenceDocumentsTemp(List.of(
                element(SupportingEvidenceBundle.builder().dateTimeReceived(pastDate).build())
            ))
            .build();

        AboutToStartOrSubmitCallbackResponse response = postMidEvent(caseData, "validate-supporting-evidence");

        assertThat(response.getErrors()).isEmpty();
    }

    @Test
    void shouldInitialiseFurtherEvidencesForHMCTS() {
        final LocalDateTime today = LocalDateTime.now();

        final Element<HearingBooking> otherHearing = element(createHearingBooking(today.minusDays(1), today));
        final Element<HearingBooking> selectedHearing = element(createHearingBooking(today, today.plusDays(1)));

        final Element<HearingFurtherEvidenceBundle> otherHearingEvidences = element(otherHearing.getId(),
            HearingFurtherEvidenceBundle.builder()
                .supportingEvidenceBundle(buildSupportingEvidenceBundle())
                .build());

        final Element<HearingFurtherEvidenceBundle> selectedHearingEvidences = element(selectedHearing.getId(),
            HearingFurtherEvidenceBundle.builder()
                .supportingEvidenceBundle(buildSupportingEvidenceBundle())
                .build());

        final CaseData caseData = CaseData.builder()
            .id(CASE_ID)
            .manageDocumentSubtypeList(OTHER)
            .manageDocumentsRelatedToHearing(YES.getValue())
            .manageDocumentsHearingList(selectedHearing.getId())
            .hearingDetails(List.of(otherHearing, selectedHearing))
            .hearingFurtherEvidenceDocuments(List.of(otherHearingEvidences, selectedHearingEvidences))
            .manageDocument(buildManagementDocument(ManageDocumentType.FURTHER_EVIDENCE_DOCUMENTS))
            .build();

        final DynamicList expectedHearingList = dynamicLists.from(1,
            Pair.of(otherHearing.getValue().toLabel(), otherHearing.getId()),
            Pair.of(selectedHearing.getValue().toLabel(), selectedHearing.getId()));

        CaseData updatedCase = extractCaseData(postMidEvent(caseData, "further-evidence-documents", USER_ROLES));

        assertThat(updatedCase.getManageDocumentsHearingList())
            .extracting(dynamicLists::convert)
            .isEqualTo(expectedHearingList);

        assertThat(updatedCase.getSupportingEvidenceDocumentsTemp())
            .isEqualTo(selectedHearingEvidences.getValue().getSupportingEvidenceBundle());
    }

    @Test
    void shouldInitialiseFurtherEvidencesForSolicitor() {
        given(userService.getCaseRoles(CASE_ID)).willReturn(Set.of(SOLICITORA));
        given(userService.isHmctsUser()).willReturn(false);

        List<Element<SupportingEvidenceBundle>> furtherEvidenceDocuments = buildSupportingEvidenceBundle();
        List<Element<SupportingEvidenceBundle>> furtherEvidenceDocumentsSolicitor =
            buildSupportingEvidenceBundleSolicitor();

        final CaseData caseData = CaseData.builder()
            .id(CASE_ID)
            .manageDocumentSubtypeList(OTHER)
            .furtherEvidenceDocuments(furtherEvidenceDocuments)
            .furtherEvidenceDocumentsSolicitor(furtherEvidenceDocumentsSolicitor)
            .manageDocument(buildManagementDocument(ManageDocumentType.FURTHER_EVIDENCE_DOCUMENTS))
            .build();

        CaseData updatedCase = extractCaseData(postMidEvent(caseData, "further-evidence-documents"));

        assertThat(updatedCase.getSupportingEvidenceDocumentsTemp()).isEqualTo(furtherEvidenceDocumentsSolicitor);
    }

    @Test
    void shouldInitialiseRespondentStatements() {

        final Element<Respondent> selectedRespondent = testRespondent("John", "Smith");
        final Element<Respondent> otherRespondent = testRespondent("George", "Williams");

        final Element<RespondentStatement> selectedRespondentStatements = element(RespondentStatement.builder()
            .respondentId(selectedRespondent.getId())
            .supportingEvidenceBundle(buildSupportingEvidenceBundle())
            .build());

        final Element<RespondentStatement> otherRespondentStatements = element(RespondentStatement.builder()
            .respondentId(otherRespondent.getId())
            .supportingEvidenceBundle(buildSupportingEvidenceBundle())
            .build());

        final CaseData caseData = CaseData.builder()
            .id(CASE_ID)
            .manageDocumentSubtypeList(RESPONDENT_STATEMENT)
            .respondents1(List.of(selectedRespondent, otherRespondent))
            .respondentStatementList(selectedRespondent.getId())
            .respondentStatements(List.of(selectedRespondentStatements, otherRespondentStatements))
            .build();

        final DynamicList expectedRespondentStatements = dynamicLists.from(0,
            Pair.of(selectedRespondent.getValue().getParty().getFullName(), selectedRespondent.getId()),
            Pair.of(otherRespondent.getValue().getParty().getFullName(), otherRespondent.getId()));

        CaseData updatedCased = extractCaseData(postMidEvent(caseData, "further-evidence-documents", USER_ROLES));

        assertThat(updatedCased.getRespondentStatementList())
            .extracting(dynamicLists::convert)
            .isEqualTo(expectedRespondentStatements);

        assertThat(updatedCased.getSupportingEvidenceDocumentsTemp())
            .isEqualTo(selectedRespondentStatements.getValue().getSupportingEvidenceBundle());
    }

    private ManageDocument buildManagementDocument(ManageDocumentType type) {
        return ManageDocument.builder().type(type).build();
    }

    private List<Element<SupportingEvidenceBundle>> buildSupportingEvidenceBundle() {
        return wrapElements(SupportingEvidenceBundle.builder()
            .name(RandomStringUtils.randomAlphabetic(10))
            .uploadedBy("HMCTS")
            .type(GUARDIAN_REPORTS)
            .build());
    }

    private List<Element<SupportingEvidenceBundle>> buildSupportingEvidenceBundleSolicitor() {
        return wrapElements(SupportingEvidenceBundle.builder()
            .name(RandomStringUtils.randomAlphabetic(10))
            .uploadedBy("ExternalSolicitor")
            .type(GUARDIAN_REPORTS)
            .build());
    }

    private C2DocumentBundle buildC2DocumentBundle(LocalDateTime dateTime) {
        return C2DocumentBundle.builder()
            .id(UUID.randomUUID())
            .uploadedDateTime(formatLocalDateTimeBaseUsingFormat(dateTime, DATE_TIME))
            .build();
    }

    private C2DocumentBundle buildC2DocumentBundle(List<Element<SupportingEvidenceBundle>> supportingEvidenceBundle) {
        return buildC2DocumentBundle(now()).toBuilder()
            .supportingEvidenceBundle(supportingEvidenceBundle)
            .build();
    }
}
