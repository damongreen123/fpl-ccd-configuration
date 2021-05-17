package uk.gov.hmcts.reform.fpl.controllers;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.autoconfigure.OverrideAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackRequest;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.fpl.service.FeatureToggleService;
import uk.gov.hmcts.reform.fpl.service.IdentityService;
import uk.gov.hmcts.reform.fpl.utils.DocumentUploadHelper;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;

@WebMvcTest(UploadDocumentsController.class)
@OverrideAutoConfiguration(enabled = true)
class RenderDocumentsControllerAboutToSubmitTest extends AbstractCallbackTest {

    @MockBean
    private IdentityService identityService;

    @MockBean
    private DocumentUploadHelper documentUploadHelper;

    @MockBean
    private FeatureToggleService featureToggleService;

    RenderDocumentsControllerAboutToSubmitTest() {
        super("render-documents");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shoudlRenderIfSomeDocument(boolean isFurtherEvidenceTabEnabled) {

        given(featureToggleService.isFurtherEvidenceDocumentTabEnabled()).willReturn(isFurtherEvidenceTabEnabled);

        when(identityService.generateId()).thenReturn(UUID.randomUUID()).thenReturn(UUID.randomUUID());
        given(documentUploadHelper.getUploadedDocumentUserDetails()).willReturn("siva@swansea.gov.uk");

        CaseDetails caseDetails = CaseDetails.builder().data(someCaseDataWithDocuments()).build();

        CallbackRequest callbackRequest = CallbackRequest.builder()
            .caseDetails(caseDetails)
            .build();

        AboutToStartOrSubmitCallbackResponse callbackResponse = postAboutToSubmitEvent(callbackRequest);

        if (isFurtherEvidenceTabEnabled) {
            assertThat((String) callbackResponse.getData().get("documentViewLA")).isNotEmpty();
            assertThat((String) callbackResponse.getData().get("documentViewHMCTS")).isNotEmpty();
            assertThat((String) callbackResponse.getData().get("documentViewNC")).isNotEmpty();
            assertThat(callbackResponse.getData().get("showFurtherEvidenceTab")).isEqualTo("YES");
        } else {
            assertThat(callbackResponse.getData().get("documentViewLA")).isNull();
            assertThat(callbackResponse.getData().get("documentViewHMCTS")).isNull();
            assertThat(callbackResponse.getData().get("documentViewNC")).isNull();
            assertThat((String) callbackResponse.getData().get("showFurtherEvidenceTab")).isNull();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldRenderIfNoDocuments(boolean isFurtherEvidenceTabEnabled) {

        given(featureToggleService.isFurtherEvidenceDocumentTabEnabled()).willReturn(isFurtherEvidenceTabEnabled);

        when(identityService.generateId()).thenReturn(UUID.randomUUID()).thenReturn(UUID.randomUUID());
        given(documentUploadHelper.getUploadedDocumentUserDetails()).willReturn("siva@swansea.gov.uk");

        CaseDetails caseDetails = CaseDetails.builder().data(Map.of()).build();

        CallbackRequest callbackRequest = CallbackRequest.builder()
            .caseDetails(caseDetails)
            .build();

        AboutToStartOrSubmitCallbackResponse callbackResponse = postAboutToSubmitEvent(callbackRequest);

        if (isFurtherEvidenceTabEnabled) {
            assertThat((String) callbackResponse.getData().get("documentViewLA")).isNull();
            assertThat((String) callbackResponse.getData().get("documentViewHMCTS")).isNull();
            assertThat((String) callbackResponse.getData().get("documentViewNC")).isNull();
            assertThat(callbackResponse.getData().get("showFurtherEvidenceTab")).isEqualTo("NO");
        } else {
            assertThat(callbackResponse.getData().get("documentViewLA")).isNull();
            assertThat(callbackResponse.getData().get("documentViewHMCTS")).isNull();
            assertThat(callbackResponse.getData().get("documentViewNC")).isNull();
            assertThat((String) callbackResponse.getData().get("showFurtherEvidenceTab")).isNull();
        }
    }

    private Map<String, Object> someCaseDataWithDocuments() {
        return Map.of(
            "applicationDocuments", List.of(
                Map.of(
                    "id", UUID.randomUUID(),
                    "value", Map.of(
                        "document", Map.of(
                            "document_url", "https://AnotherdocuURL",
                            "document_filename", "mockChecklist.pdf",
                            "document_binary_url", "http://dm-store:8080/documents/fakeUrl/binary"
                        ),
                        "documentType", "SOCIAL_WORK_STATEMENT"
                    )
                )
            )
        );
    }
}
