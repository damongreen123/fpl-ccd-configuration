package uk.gov.hmcts.reform.fpl.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.boot.test.autoconfigure.OverrideAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.ccd.client.model.CallbackRequest;
import uk.gov.hmcts.reform.document.domain.Document;
import uk.gov.hmcts.reform.fpl.model.CaseData;
import uk.gov.hmcts.reform.fpl.model.Child;
import uk.gov.hmcts.reform.fpl.model.ChildParty;
import uk.gov.hmcts.reform.fpl.model.Representative;
import uk.gov.hmcts.reform.fpl.model.Respondent;
import uk.gov.hmcts.reform.fpl.model.RespondentParty;
import uk.gov.hmcts.reform.fpl.model.SentDocument;
import uk.gov.hmcts.reform.fpl.model.SentDocuments;
import uk.gov.hmcts.reform.fpl.model.common.DocmosisDocument;
import uk.gov.hmcts.reform.fpl.model.common.DocumentReference;
import uk.gov.hmcts.reform.fpl.model.common.Element;
import uk.gov.hmcts.reform.fpl.model.order.generated.GeneratedOrder;
import uk.gov.hmcts.reform.fpl.service.DocumentDownloadService;
import uk.gov.hmcts.reform.fpl.service.UploadDocumentService;
import uk.gov.hmcts.reform.fpl.service.ccd.CoreCaseDataService;
import uk.gov.hmcts.reform.fpl.service.docmosis.DocmosisCoverDocumentsService;
import uk.gov.hmcts.reform.sendletter.api.LetterWithPdfsRequest;
import uk.gov.hmcts.reform.sendletter.api.SendLetterApi;
import uk.gov.hmcts.reform.sendletter.api.SendLetterResponse;
import uk.gov.service.notify.NotificationClient;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.fpl.Constants.LOCAL_AUTHORITY_1_CODE;
import static uk.gov.hmcts.reform.fpl.Constants.LOCAL_AUTHORITY_1_INBOX;
import static uk.gov.hmcts.reform.fpl.NotifyTemplates.ITEM_TRANSLATED_NOTIFICATION_TEMPLATE;
import static uk.gov.hmcts.reform.fpl.enums.RepresentativeServingPreferences.DIGITAL_SERVICE;
import static uk.gov.hmcts.reform.fpl.enums.RepresentativeServingPreferences.EMAIL;
import static uk.gov.hmcts.reform.fpl.enums.RepresentativeServingPreferences.POST;
import static uk.gov.hmcts.reform.fpl.utils.AssertionHelper.checkUntil;
import static uk.gov.hmcts.reform.fpl.utils.ElementUtils.element;
import static uk.gov.hmcts.reform.fpl.utils.ElementUtils.wrapElements;
import static uk.gov.hmcts.reform.fpl.utils.TestDataHelper.documentSent;
import static uk.gov.hmcts.reform.fpl.utils.TestDataHelper.printRequest;
import static uk.gov.hmcts.reform.fpl.utils.TestDataHelper.testAddress;
import static uk.gov.hmcts.reform.fpl.utils.TestDataHelper.testDocument;
import static uk.gov.hmcts.reform.fpl.utils.TestDataHelper.testDocumentBinaries;
import static uk.gov.hmcts.reform.fpl.utils.TestDataHelper.testDocumentReference;

@WebMvcTest(UploadTranslationsController.class)
@OverrideAutoConfiguration(enabled = true)
class UploadTranslationsSubmittedControllerTest extends AbstractCallbackTest {

    private static final Long CASE_ID = 1614860986487554L;
    private static final String NOTIFICATION_REFERENCE = "localhost/" + CASE_ID;
    private static final String FAMILY_MAN_CASE_NUMBER = "FMN1";
    private static final UUID LETTER_1_ID = randomUUID();
    private static final UUID LETTER_2_ID = randomUUID();
    private static final Document ORDER_DOCUMENT = testDocument();
    private static final Document COVERSHEET_REPRESENTATIVE = testDocument();
    private static final Document COVERSHEET_RESPONDENT = testDocument();
    private static final byte[] ORDER_BINARY = testDocumentBinaries();
    private static final byte[] COVERSHEET_REPRESENTATIVE_BINARY = testDocumentBinaries();
    private static final byte[] COVERSHEET_RESPONDENT_BINARY = testDocumentBinaries();
    private static final DocumentReference ORDER = testDocumentReference();
    private static final String ORDER_TYPE = "Care order (C32A)";
    private static final Map<String, Object> NOTIFICATION_PARAMETERS = getExpectedParametersMap(ORDER_TYPE);
    private static final Element<Representative> REPRESENTATIVE_POST = element(Representative.builder()
        .fullName("First Representative")
        .servingPreferences(POST)
        .address(testAddress())
        .build());
    private static final Element<Representative> REPRESENTATIVE_EMAIL = element(Representative.builder()
        .fullName("Third Representative")
        .servingPreferences(EMAIL)
        .email("third@representatives.com")
        .build());
    private static final Element<Representative> REPRESENTATIVE_DIGITAL = element(Representative.builder()
        .fullName("Second Representative")
        .servingPreferences(DIGITAL_SERVICE)
        .email("second@representatives.com")
        .build());
    private static final Respondent RESPONDENT_NOT_REPRESENTED = Respondent.builder()
        .party(RespondentParty.builder()
            .firstName("Alex")
            .lastName("Jones")
            .address(testAddress())
            .build())
        .build();
    private static final Respondent RESPONDENT_WITHOUT_ADDRESS = Respondent.builder()
        .party(RespondentParty.builder()
            .firstName("Emma")
            .lastName("Jones")
            .build())
        .build();
    private static final Respondent RESPONDENT_REPRESENTED = Respondent.builder()
        .party(RespondentParty.builder()
            .firstName("George")
            .lastName("Jones")
            .address(testAddress())
            .build())
        .representedBy(wrapElements(REPRESENTATIVE_POST.getId(), REPRESENTATIVE_DIGITAL.getId()))
        .build();
    private static final long ASYNC_METHOD_CALL_TIMEOUT = 10000;

    @Captor
    private ArgumentCaptor<Map<String, Object>> caseDataDelta;

    @Captor
    private ArgumentCaptor<LetterWithPdfsRequest> printRequest;

    @MockBean
    private SendLetterApi sendLetterApi;

    @MockBean
    private NotificationClient notificationClient;

    @MockBean
    private CoreCaseDataService coreCaseDataService;

    @MockBean
    private UploadDocumentService uploadDocumentService;

    @MockBean
    private DocumentDownloadService documentDownloadService;

    @MockBean
    private DocmosisCoverDocumentsService documentService;

    UploadTranslationsSubmittedControllerTest() {
        super("upload-translations");
    }

    @BeforeEach
    void mocks() {
        givenFplService();
        when(documentDownloadService.downloadDocument(anyString()))
            .thenReturn(ORDER_BINARY);
        when(uploadDocumentService.uploadPDF(ORDER_BINARY, ORDER.getFilename()))
            .thenReturn(ORDER_DOCUMENT);

        when(documentService.createCoverDocuments(any(), any(), eq(REPRESENTATIVE_POST.getValue())))
            .thenReturn(DocmosisDocument.builder().bytes(COVERSHEET_REPRESENTATIVE_BINARY).build());
        when(uploadDocumentService.uploadPDF(COVERSHEET_REPRESENTATIVE_BINARY, "Coversheet.pdf"))
            .thenReturn(COVERSHEET_REPRESENTATIVE);

        when(documentService.createCoverDocuments(any(), any(), eq(RESPONDENT_NOT_REPRESENTED.getParty())))
            .thenReturn(DocmosisDocument.builder().bytes(COVERSHEET_RESPONDENT_BINARY).build());
        when(uploadDocumentService.uploadPDF(COVERSHEET_RESPONDENT_BINARY, "Coversheet.pdf"))
            .thenReturn(COVERSHEET_RESPONDENT);

        when(sendLetterApi.sendLetter(any(), any(LetterWithPdfsRequest.class)))
            .thenReturn(new SendLetterResponse(LETTER_1_ID), new SendLetterResponse(LETTER_2_ID));
    }

    @Test
    void shouldSendTranslatedNotificationToLocalAuthorityWhenTranslatedOrder() {
        CaseData caseData = caseData();
        CallbackRequest request = CallbackRequest.builder()
            .caseDetails(asCaseDetails(caseData))
            .build();

        postSubmittedEvent(request);

        checkUntil(() -> verify(notificationClient, timeout(ASYNC_METHOD_CALL_TIMEOUT)).sendEmail(
            eq(ITEM_TRANSLATED_NOTIFICATION_TEMPLATE),
            eq(LOCAL_AUTHORITY_1_INBOX), eq(NOTIFICATION_PARAMETERS),
            eq(NOTIFICATION_REFERENCE)
        ));
    }

    @Test
    void shouldSendOrdersByPostWhenOrderTranslated() {

        postSubmittedEvent(caseData());

        checkUntil(() -> {
            verify(sendLetterApi, timeout(ASYNC_METHOD_CALL_TIMEOUT).times(2)).sendLetter(eq(SERVICE_AUTH_TOKEN),
                printRequest.capture());
            verify(coreCaseDataService).updateCase(eq(CASE_ID), caseDataDelta.capture());
        });

        LetterWithPdfsRequest expectedPrintRequest1 = printRequest(
            CASE_ID, ORDER, COVERSHEET_REPRESENTATIVE_BINARY, ORDER_BINARY
        );

        LetterWithPdfsRequest expectedPrintRequest2 = printRequest(
            CASE_ID, ORDER, COVERSHEET_RESPONDENT_BINARY, ORDER_BINARY
        );

        assertThat(printRequest.getAllValues()).usingRecursiveComparison()
            .isEqualTo(List.of(expectedPrintRequest1, expectedPrintRequest2));

        List<Element<SentDocuments>> documentsSent = mapper.convertValue(
            caseDataDelta.getValue().get("documentsSentToParties"), new TypeReference<>() {}
        );

        SentDocument expectedRepresentativeDocument = documentSent(
            REPRESENTATIVE_POST.getValue(), COVERSHEET_REPRESENTATIVE, ORDER_DOCUMENT, LETTER_1_ID, now()
        );

        SentDocument expectedRespondentDocument = documentSent(
            RESPONDENT_NOT_REPRESENTED.getParty(), COVERSHEET_RESPONDENT, ORDER_DOCUMENT, LETTER_2_ID, now()
        );

        assertThat(documentsSent.get(0).getValue().getDocumentsSentToParty())
            .extracting(Element::getValue)
            .containsExactly(expectedRepresentativeDocument);

        assertThat(documentsSent.get(1).getValue().getDocumentsSentToParty())
            .extracting(Element::getValue)
            .containsExactly(expectedRespondentDocument);
    }

    @Test
    void shouldNotifyRepresentativesServedDigitallyWhenOrderTranslated() {

        postSubmittedEvent(caseData());

        checkUntil(() -> verify(notificationClient, timeout(ASYNC_METHOD_CALL_TIMEOUT)).sendEmail(
            eq(ITEM_TRANSLATED_NOTIFICATION_TEMPLATE),
            eq(REPRESENTATIVE_DIGITAL.getValue().getEmail()), eq(NOTIFICATION_PARAMETERS),
            eq(NOTIFICATION_REFERENCE)
        ));
    }

    @Test
    void shouldNotifyRepresentativesServedByEmailWhenOrderTranslated() {

        postSubmittedEvent(caseData());

        checkUntil(() -> verify(notificationClient, timeout(ASYNC_METHOD_CALL_TIMEOUT)).sendEmail(
            eq(ITEM_TRANSLATED_NOTIFICATION_TEMPLATE), eq(REPRESENTATIVE_EMAIL.getValue().getEmail()),
            eq(NOTIFICATION_PARAMETERS), eq(NOTIFICATION_REFERENCE)
        ));
    }

    private CaseData caseData() {
        return CaseData.builder()
            .id(CASE_ID)
            .children1(List.of(element(Child.builder()
                .party(ChildParty.builder()
                    .lastName("ChildLast")
                    .dateOfBirth(LocalDate.of(2012, 1, 2))
                    .build())
                .build())))
            .familyManCaseNumber(FAMILY_MAN_CASE_NUMBER)
            .caseLocalAuthority(LOCAL_AUTHORITY_1_CODE)
            .orderCollection(wrapElements(GeneratedOrder.builder()
                .orderType("C32A_CARE_ORDER")
                .type(ORDER_TYPE)
                .translationUploadDateTime(now().plusSeconds(2))
                .translatedDocument(ORDER)
                .build()))
            .respondents1(wrapElements(RESPONDENT_NOT_REPRESENTED, RESPONDENT_WITHOUT_ADDRESS, RESPONDENT_REPRESENTED))
            .representatives(List.of(REPRESENTATIVE_POST, REPRESENTATIVE_DIGITAL, REPRESENTATIVE_EMAIL))
            .build();
    }

    public static Map<String, Object> getExpectedParametersMap(String orderType) {
        return Map.of(
            "childLastName", "ChildLast",
            "docType", orderType,
            "callout", "^Jones, FMN1",
            "courtName", "Family Court",
            "caseUrl", "http://fake-url/cases/case-details/1614860986487554");
    }
}