package uk.gov.hmcts.reform.fpl.controllers.documents;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.OverrideAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.ccd.model.CaseAssignedUserRole;
import uk.gov.hmcts.reform.ccd.model.CaseAssignedUserRolesResource;
import uk.gov.hmcts.reform.fpl.enums.CaseRole;
import uk.gov.hmcts.reform.fpl.service.FeatureToggleService;
import uk.gov.hmcts.reform.fpl.service.cafcass.CafcassNotificationService;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.fpl.Constants.LOCAL_AUTHORITY_1_INBOX;
import static uk.gov.hmcts.reform.fpl.Constants.TEST_CASE_ID;
import static uk.gov.hmcts.reform.fpl.NotifyTemplates.DOCUMENT_UPLOADED_NOTIFICATION_TEMPLATE;

@ActiveProfiles("integration-test")
@WebMvcTest(ManageDocumentsController.class)
@OverrideAutoConfiguration(enabled = true)
class ManageDocumentsControllerSubmittedTest extends ManageDocumentsControllerSubmittedBaseTest {

    private static final String SOLICITOR_BUNDLE_NAME = "furtherEvidenceDocumentsSolicitor";

    @MockBean
    private NotificationClient notificationClient;

    @MockBean
    private FeatureToggleService featureToggleService;

    @MockBean
    private CafcassNotificationService cafcassNotificationService;

    ManageDocumentsControllerSubmittedTest() {
        super("manage-documents");
    }

    @BeforeEach
    void init() {
        givenFplService();
    }

    @Test
    void shouldNotPublishEventLAWhenUploadNotificationFeatureIsDisabled() {

        givenCaseRoles(TEST_CASE_ID, USER_ID, CaseRole.SOLICITORA);

        when(featureToggleService.isNewDocumentUploadNotificationEnabled()).thenReturn(false);

        postSubmittedEvent(buildCallbackRequest(SOLICITOR_BUNDLE_NAME, false));

        verifyNoInteractions(notificationClient);
    }

    @Test
    void shouldNotPublishEventWhenConfidentialDocumentsAreUploadedBySolicitor() {
        when(featureToggleService.isNewDocumentUploadNotificationEnabled()).thenReturn(true);
        when(idamClient.getUserDetails(any())).thenReturn(UserDetails.builder().build());
        givenCaseRoles(TEST_CASE_ID, USER_ID, CaseRole.SOLICITORA);

        postSubmittedEvent(buildCallbackRequest(SOLICITOR_BUNDLE_NAME, true));
        verifyNoInteractions(notificationClient);
    }

    @Test
    void shouldPublishEventWithOtherUserWhenUploadNotificationFeatureIsEnabled() throws NotificationClientException {
        when(featureToggleService.isNewDocumentUploadNotificationEnabled()).thenReturn(true);
        when(idamClient.getUserDetails(any())).thenReturn(UserDetails.builder().build());
        givenCaseRoles(TEST_CASE_ID, USER_ID, CaseRole.SOLICITORA);

        postSubmittedEvent(buildCallbackRequest(SOLICITOR_BUNDLE_NAME, false));

        verify(notificationClient).sendEmail(
            eq(DOCUMENT_UPLOADED_NOTIFICATION_TEMPLATE),
            eq(LOCAL_AUTHORITY_1_INBOX),
            anyMap(),
            eq(notificationReference(TEST_CASE_ID)));

        verify(notificationClient).sendEmail(
            eq(DOCUMENT_UPLOADED_NOTIFICATION_TEMPLATE),
            eq(REP_1_EMAIL),
            anyMap(),
            eq(notificationReference(TEST_CASE_ID)));
    }

    private CaseAssignedUserRolesResource buildCaseAssignedUserRole(String role) {
        return CaseAssignedUserRolesResource.builder().caseAssignedUserRoles(List.of(
            CaseAssignedUserRole.builder()
                .caseRole(role)
                .userId("USER_1_ID")
                .caseDataId("123")
                .build()))
            .build();
    }
}
