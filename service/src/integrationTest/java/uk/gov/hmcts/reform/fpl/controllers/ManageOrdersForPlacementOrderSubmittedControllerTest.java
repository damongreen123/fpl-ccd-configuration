package uk.gov.hmcts.reform.fpl.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.codec.binary.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.boot.test.autoconfigure.OverrideAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.document.domain.Document;
import uk.gov.hmcts.reform.fpl.model.Address;
import uk.gov.hmcts.reform.fpl.model.CaseData;
import uk.gov.hmcts.reform.fpl.model.Child;
import uk.gov.hmcts.reform.fpl.model.ChildParty;
import uk.gov.hmcts.reform.fpl.model.Respondent;
import uk.gov.hmcts.reform.fpl.model.RespondentParty;
import uk.gov.hmcts.reform.fpl.model.SentDocuments;
import uk.gov.hmcts.reform.fpl.model.common.DocmosisDocument;
import uk.gov.hmcts.reform.fpl.model.common.DocumentReference;
import uk.gov.hmcts.reform.fpl.model.common.Element;
import uk.gov.hmcts.reform.fpl.model.order.Order;
import uk.gov.hmcts.reform.fpl.model.order.generated.GeneratedOrder;
import uk.gov.hmcts.reform.fpl.service.DocumentDownloadService;
import uk.gov.hmcts.reform.fpl.service.UploadDocumentService;
import uk.gov.hmcts.reform.fpl.service.ccd.CoreCaseDataService;
import uk.gov.hmcts.reform.fpl.service.docmosis.DocmosisCoverDocumentsService;
import uk.gov.hmcts.reform.sendletter.api.LetterWithPdfsRequest;
import uk.gov.hmcts.reform.sendletter.api.SendLetterApi;
import uk.gov.hmcts.reform.sendletter.api.SendLetterResponse;
import uk.gov.service.notify.NotificationClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.fpl.Constants.DEFAULT_ADMIN_EMAIL;
import static uk.gov.hmcts.reform.fpl.Constants.DEFAULT_LA_COURT;
import static uk.gov.hmcts.reform.fpl.Constants.LOCAL_AUTHORITY_1_CODE;
import static uk.gov.hmcts.reform.fpl.Constants.LOCAL_AUTHORITY_1_INBOX;
import static uk.gov.hmcts.reform.fpl.Constants.TEST_CASE_ID;
import static uk.gov.hmcts.reform.fpl.Constants.TEST_FAMILY_MAN_NUMBER;
import static uk.gov.hmcts.reform.fpl.NotifyTemplates.PLACEMENT_ORDER_GENERATED_NOTIFICATION_TEMPLATE;
import static uk.gov.hmcts.reform.fpl.model.configuration.Language.ENGLISH;
import static uk.gov.hmcts.reform.fpl.model.order.Order.A70_PLACEMENT_ORDER;
import static uk.gov.hmcts.reform.fpl.testingsupport.IntegrationTestConstants.CAFCASS_EMAIL;
import static uk.gov.hmcts.reform.fpl.testingsupport.IntegrationTestConstants.COVERSHEET_PDF;
import static uk.gov.hmcts.reform.fpl.utils.AssertionHelper.checkUntil;
import static uk.gov.hmcts.reform.fpl.utils.ElementUtils.wrapElements;
import static uk.gov.hmcts.reform.fpl.utils.TestDataHelper.documentSent;
import static uk.gov.hmcts.reform.fpl.utils.TestDataHelper.printRequest;
import static uk.gov.hmcts.reform.fpl.utils.TestDataHelper.testDocument;
import static uk.gov.hmcts.reform.fpl.utils.TestDataHelper.testDocumentBinaries;
import static uk.gov.hmcts.reform.fpl.utils.TestDataHelper.testDocumentReference;

@WebMvcTest(ManageOrdersController.class)
@OverrideAutoConfiguration(enabled = true)
class ManageOrdersForPlacementOrderSubmittedControllerTest extends AbstractCallbackTest {
    //TODO - maybe put these manage orders tests in a separate package

    private static final Order ORDER = A70_PLACEMENT_ORDER;

    //TODO - copied from helper class
    private static final DocumentReference ORDER_DOCUMENT_REFERENCE = testDocumentReference();
    private static final byte[] ORDER_BINARY = testDocumentBinaries();
    private static final String ENCODED_ORDER_DOCUMENT = new String(Base64.encodeBase64(ORDER_BINARY), ISO_8859_1);

    private static final DocumentReference ORDER_NOTIFICATION_DOCUMENT_REFERENCE = testDocumentReference();
    private static final Document ORDER_NOTIFICATION_DOCUMENT = testDocument();
    private static final byte[] ORDER_NOTIFICATION_BINARY = testDocumentBinaries();
    private static final String ENCODED_ORDER_NOTIFICATION_DOCUMENT = new String(Base64.encodeBase64(ORDER_NOTIFICATION_BINARY), ISO_8859_1);


    private static final Respondent FATHER = Respondent.builder().party(RespondentParty.builder().firstName("Father").lastName("Jones").address(Address.builder().addressLine1("10 Father Road").postcode("SG5 7IR").build()).build()).build();//TODO - what if a parent has no address?
    private static final Document FATHER_COVERSHEET_DOCUMENT = testDocument();
    private static final byte[] FATHER_COVERSHEET_BINARY = testDocumentBinaries();

    private static final Respondent MOTHER = Respondent.builder().party(RespondentParty.builder().firstName("Mother").lastName("Jones").address(Address.builder().addressLine1("10 Mother Road").postcode("SG5 7IU").build()).build()).build();
    private static final Document MOTHER_COVERSHEET_DOCUMENT = testDocument();
    private static final byte[] MOTHER_COVERSHEET_BINARY = testDocumentBinaries();//TODO - group and organise these fields

    //TODO - potentially reusable fields
    private static final Map<String, Object> ORDER_EMAIL_PARAMETERS = Map.of(
//        "callout", "^Theodore Bailey, " + TEST_FAMILY_MAN_NUMBER + ", hearing 1 Jan 2015",//TODO - check whether this is needed
        "courtName", DEFAULT_LA_COURT,
        "documentLink", Map.of("file", ENCODED_ORDER_DOCUMENT, "is_csv", false),
        "caseUrl", "http://fake-url/cases/case-details/" + TEST_CASE_ID + "#Orders",
        "childLastName", "Bailey"
    );
    private static final String NOTIFICATION_REFERENCE = "localhost/" + TEST_CASE_ID;

    @MockBean
    private NotificationClient notificationClient;

    @MockBean
    private DocumentDownloadService documentDownloadService;

    @MockBean
    private SendLetterApi sendLetterApi;

    @MockBean
    private UploadDocumentService uploadDocumentService;

    @MockBean
    private DocmosisCoverDocumentsService documentService;

    @Captor
    private ArgumentCaptor<LetterWithPdfsRequest> letterCaptor;

    @MockBean
    private CoreCaseDataService coreCaseDataService;

    @Captor
    private ArgumentCaptor<Map<String, Object>> caseDataDelta;

    private CaseData placementOrderCaseData;

    ManageOrdersForPlacementOrderSubmittedControllerTest() {
        super("manage-orders");
    }

    @BeforeEach
    void setUp() {
        givenFplService();

        placementOrderCaseData = CaseData.builder()
            .id(TEST_CASE_ID)
            .familyManCaseNumber(TEST_FAMILY_MAN_NUMBER)
            .caseLocalAuthority(LOCAL_AUTHORITY_1_CODE)
            .respondents1(wrapElements(FATHER, MOTHER))//TODO - later on, let's add more respondents and choose them from Tomasz's event
            .orderCollection(wrapElements(GeneratedOrder.builder()
                .orderType(ORDER.name())
                .type(ORDER.getTitle())
                .document(ORDER_DOCUMENT_REFERENCE)
                .notificationDocument(ORDER_NOTIFICATION_DOCUMENT_REFERENCE)
                .children(wrapElements(Child.builder().party(ChildParty.builder().firstName("Theodore").lastName("Bailey").build()).build()))
                .build()))
            .build();
    }

    @Test
    void shouldSendPlacementOrderDocumentOrNotificationToExpectedParties() {//TODO - maybe I can have a PostSender Test Helper which sets up all mocks for me
        when(documentDownloadService.downloadDocument(ORDER_DOCUMENT_REFERENCE.getBinaryUrl())).thenReturn(ORDER_BINARY);//TODO - try to specify parameter
        when(documentDownloadService.downloadDocument(ORDER_NOTIFICATION_DOCUMENT_REFERENCE.getBinaryUrl())).thenReturn(ORDER_NOTIFICATION_BINARY);//TODO - try to specify parameter
        when(uploadDocumentService.uploadPDF(ORDER_NOTIFICATION_BINARY, ORDER_NOTIFICATION_DOCUMENT_REFERENCE.getFilename()))
            .thenReturn(ORDER_NOTIFICATION_DOCUMENT);
        when(uploadDocumentService.uploadPDF(FATHER_COVERSHEET_BINARY, COVERSHEET_PDF))
            .thenReturn(FATHER_COVERSHEET_DOCUMENT);
        when(uploadDocumentService.uploadPDF(MOTHER_COVERSHEET_BINARY, COVERSHEET_PDF))
            .thenReturn(MOTHER_COVERSHEET_DOCUMENT);
        when(documentService.createCoverDocuments(TEST_FAMILY_MAN_NUMBER, TEST_CASE_ID, FATHER.getParty(), ENGLISH))
            .thenReturn(DocmosisDocument.builder().bytes(FATHER_COVERSHEET_BINARY).build());
        when(documentService.createCoverDocuments(TEST_FAMILY_MAN_NUMBER, TEST_CASE_ID, MOTHER.getParty(), ENGLISH))
            .thenReturn(DocmosisDocument.builder().bytes(MOTHER_COVERSHEET_BINARY).build());

        UUID firstLetterId = UUID.randomUUID();
        UUID secondLetterId = UUID.randomUUID();
        when(sendLetterApi.sendLetter(any(), any(LetterWithPdfsRequest.class)))
            .thenReturn(new SendLetterResponse(firstLetterId), new SendLetterResponse(secondLetterId));

        postSubmittedEvent(toCallBackRequest(placementOrderCaseData, CaseData.builder().build()));

        //A70
        checkEmailWithOrderWasSent(LOCAL_AUTHORITY_1_INBOX);
        checkEmailWithOrderWasSent(DEFAULT_ADMIN_EMAIL);
        checkEmailWithOrderWasSent(CAFCASS_EMAIL);
        verifyNoMoreInteractions(notificationClient);

        //A206
        //Check document mailed to parents
        checkOrderNotificationLetterWasMailedToParents();
        checkCaseDataHasReferenceToLetters(firstLetterId, secondLetterId);

        //TODO - check that email with A206 is sent to child representative
    }

    private void checkCaseDataHasReferenceToLetters(UUID firstLetterId, UUID secondLetterId) {
        checkUntil(() -> verify(coreCaseDataService).updateCase(eq(TEST_CASE_ID), caseDataDelta.capture()));
        List<Element<SentDocuments>> documentsSent = mapper.convertValue(
            caseDataDelta.getValue().get("documentsSentToParties"), new TypeReference<>() {
            }
        );
        assertThat(documentsSent.get(0).getValue().getDocumentsSentToParty())
            .extracting(Element::getValue)
            .containsExactly(documentSent(
                FATHER.getParty(), FATHER_COVERSHEET_DOCUMENT, ORDER_NOTIFICATION_DOCUMENT, firstLetterId, now()
            ));
        assertThat(documentsSent.get(1).getValue().getDocumentsSentToParty())
            .extracting(Element::getValue)
            .containsExactly(documentSent(
                MOTHER.getParty(), MOTHER_COVERSHEET_DOCUMENT, ORDER_NOTIFICATION_DOCUMENT, secondLetterId, now()
            ));
    }

    private void checkOrderNotificationLetterWasMailedToParents() {
        checkUntil(() -> verify(sendLetterApi, times(2)).sendLetter(
            eq(SERVICE_AUTH_TOKEN),
            letterCaptor.capture()
        ));
        assertThat(letterCaptor.getAllValues()).usingRecursiveComparison().isEqualTo(List.of(
            printRequest(TEST_CASE_ID, ORDER_NOTIFICATION_DOCUMENT_REFERENCE, FATHER_COVERSHEET_BINARY, ORDER_NOTIFICATION_BINARY),
            printRequest(TEST_CASE_ID, ORDER_NOTIFICATION_DOCUMENT_REFERENCE, MOTHER_COVERSHEET_BINARY, ORDER_NOTIFICATION_BINARY)
        ));
    }

    //TODO - test e-mail to respondents solicitors

    private void checkEmailWithOrderWasSent(String recipient) {
        checkUntil(() -> verify(notificationClient).sendEmail(
            eq(PLACEMENT_ORDER_GENERATED_NOTIFICATION_TEMPLATE),
            eq(recipient),
            eq(ORDER_EMAIL_PARAMETERS),
            eq(NOTIFICATION_REFERENCE)
        ));
    }

}
