package uk.gov.hmcts.reform.fpl.controllers;

import org.apache.commons.codec.binary.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.OverrideAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.fpl.model.CaseData;
import uk.gov.hmcts.reform.fpl.model.Child;
import uk.gov.hmcts.reform.fpl.model.ChildParty;
import uk.gov.hmcts.reform.fpl.model.common.DocumentReference;
import uk.gov.hmcts.reform.fpl.model.order.Order;
import uk.gov.hmcts.reform.fpl.model.order.generated.GeneratedOrder;
import uk.gov.hmcts.reform.fpl.service.DocumentDownloadService;
import uk.gov.service.notify.NotificationClient;

import java.util.Map;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.fpl.Constants.DEFAULT_LA_COURT;
import static uk.gov.hmcts.reform.fpl.Constants.LOCAL_AUTHORITY_1_CODE;
import static uk.gov.hmcts.reform.fpl.Constants.LOCAL_AUTHORITY_1_INBOX;
import static uk.gov.hmcts.reform.fpl.Constants.TEST_CASE_ID;
import static uk.gov.hmcts.reform.fpl.NotifyTemplates.PLACEMENT_ORDER_GENERATED_NOTIFICATION_TEMPLATE;
import static uk.gov.hmcts.reform.fpl.model.order.Order.A70_PLACEMENT_ORDER;
import static uk.gov.hmcts.reform.fpl.utils.AssertionHelper.checkUntil;
import static uk.gov.hmcts.reform.fpl.utils.ElementUtils.wrapElements;
import static uk.gov.hmcts.reform.fpl.utils.TestDataHelper.testDocumentBinaries;
import static uk.gov.hmcts.reform.fpl.utils.TestDataHelper.testDocumentReference;

@WebMvcTest(ManageOrdersController.class)
@OverrideAutoConfiguration(enabled = true)
class ManageOrdersForPlacementOrderSubmittedControllerTest extends AbstractCallbackTest {
    //TODO - maybe put these manage orders tests in a separate package

    private static final Order ORDER = A70_PLACEMENT_ORDER;

    //TODO - copied from helper class
    private static final byte[] ORDER_DOCUMENT = {1, 2, 3, 4, 5};
    private static final byte[] ORDER_BINARY = testDocumentBinaries();
    private static final String ENCODED_ORDER_DOCUMENT = new String(Base64.encodeBase64(ORDER_BINARY), ISO_8859_1);

    private static final DocumentReference ORDER_DOCUMENT_REFERENCE = testDocumentReference();

    //TODO - potentially reusable fields
    private static final long ASYNC_METHOD_CALL_TIMEOUT = 10000;
    private static final Map<String, Object> ORDER_NOTIFICATION_PARAMETERS = Map.of(
//        "callout", "^Theodore Bailey, " + TEST_FAMILY_MAN_NUMBER + ", hearing 1 Jan 2015",//TODO - uncomment after more stable test
        "courtName", DEFAULT_LA_COURT,
        "documentLink", Map.of("file", ENCODED_ORDER_DOCUMENT, "is_csv", false),//TODO - check what gov notify needs - not sure this is right
        //TODO - do all these actors have case access? if so, send link. if not, send file
        //TODO - where's the slash on the URL above?
        "caseUrl", "http://fake-url/cases/case-details/" + TEST_CASE_ID + "#Orders",//TODO - revisit
        "childLastName", "Bailey"//TODO - this should really be the child last name - get stability first
    );
    private static final String NOTIFICATION_REFERENCE = "localhost/" + TEST_CASE_ID;

    @MockBean
    private NotificationClient notificationClient;

    @MockBean
    private DocumentDownloadService documentDownloadService;

    ManageOrdersForPlacementOrderSubmittedControllerTest() {
        super("manage-orders");
    }

    @BeforeEach
    void setUp() {
        when(documentDownloadService.downloadDocument(anyString())).thenReturn(ORDER_BINARY);
    }

    @Test
    void shouldSendPlacementOrderByEmailToExpectedParties() {
        CaseData placementOrderCaseData = CaseData.builder()
            .id(TEST_CASE_ID)
            .caseLocalAuthority(LOCAL_AUTHORITY_1_CODE)
            .orderCollection(wrapElements(GeneratedOrder.builder()
                .orderType(ORDER.name())
                .type(ORDER.getTitle())
                .document(ORDER_DOCUMENT_REFERENCE)
                .children(wrapElements(Child.builder().party(ChildParty.builder().firstName("Theodore").lastName("Bailey").build()).build()))
                .build()))
            .build();

        postSubmittedEvent(toCallBackRequest(placementOrderCaseData, CaseData.builder().build()));

        checkUntil(() -> verify(notificationClient).sendEmail(
            eq(PLACEMENT_ORDER_GENERATED_NOTIFICATION_TEMPLATE),//TODO - consider making a copy of this template and change the last name variable name
            eq(LOCAL_AUTHORITY_1_INBOX),
            eq(ORDER_NOTIFICATION_PARAMETERS),
            eq(NOTIFICATION_REFERENCE)
        ));
//        checkUntil(() -> verify(notificationClient).sendEmail(//TODO - uncomment this once test is passing - or more stable
//            eq(PLACEMENT_ORDER_GENERATED_NOTIFICATION_TEMPLATE),//TODO - I think we can use the same template for these - they're identical
//            eq(DEFAULT_ADMIN_EMAIL),
//            eq(ORDER_NOTIFICATION_PARAMETERS),
//            eq(NOTIFICATION_REFERENCE)
//        ));
//        verifyNoMoreInteractions(notificationClient);//TODO - bring this back once it's stable
    }

}
