package uk.gov.hmcts.reform.fpl.docmosis;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockBeans;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ContextConfiguration;
import uk.gov.hmcts.reform.fpl.config.LocalAuthorityNameLookupConfiguration;
import uk.gov.hmcts.reform.fpl.docmosis.generator.DocmosisOrderCaseDataGenerator;
import uk.gov.hmcts.reform.fpl.enums.OrderStatus;
import uk.gov.hmcts.reform.fpl.enums.docmosis.RenderFormat;
import uk.gov.hmcts.reform.fpl.model.CaseData;
import uk.gov.hmcts.reform.fpl.model.common.DocmosisDocument;
import uk.gov.hmcts.reform.fpl.model.order.Order;
import uk.gov.hmcts.reform.fpl.model.order.OrderSourceType;
import uk.gov.hmcts.reform.fpl.selectors.ChildrenSmartSelector;
import uk.gov.hmcts.reform.fpl.service.AppointedGuardianFormatter;
import uk.gov.hmcts.reform.fpl.service.CaseConverter;
import uk.gov.hmcts.reform.fpl.service.CaseDataExtractionService;
import uk.gov.hmcts.reform.fpl.service.CourtService;
import uk.gov.hmcts.reform.fpl.service.DocumentDownloadService;
import uk.gov.hmcts.reform.fpl.service.DocumentSealingService;
import uk.gov.hmcts.reform.fpl.service.ManageOrderDocumentService;
import uk.gov.hmcts.reform.fpl.service.PbaNumberService;
import uk.gov.hmcts.reform.fpl.service.PlacementService;
import uk.gov.hmcts.reform.fpl.service.UploadDocumentService;
import uk.gov.hmcts.reform.fpl.service.docmosis.DocmosisDocumentGeneratorService;
import uk.gov.hmcts.reform.fpl.service.document.DocumentGenerator;
import uk.gov.hmcts.reform.fpl.service.orders.OrderCreationService;
import uk.gov.hmcts.reform.fpl.service.orders.generator.A206PlacementOrderNotificationParameterGenerator;
import uk.gov.hmcts.reform.fpl.service.orders.generator.A70PlacementOrderDocumentParameterGenerator;
import uk.gov.hmcts.reform.fpl.service.orders.generator.C21BlankOrderDocumentParameterGenerator;
import uk.gov.hmcts.reform.fpl.service.orders.generator.C23EPOAdditionalDocumentsCollector;
import uk.gov.hmcts.reform.fpl.service.orders.generator.C23EPODocumentParameterGenerator;
import uk.gov.hmcts.reform.fpl.service.orders.generator.C26SecureAccommodationOrderDocumentParameterGenerator;
import uk.gov.hmcts.reform.fpl.service.orders.generator.C32CareOrderDocumentParameterGenerator;
import uk.gov.hmcts.reform.fpl.service.orders.generator.C32bDischargeOfCareOrderDocumentParameterGenerator;
import uk.gov.hmcts.reform.fpl.service.orders.generator.C33InterimCareOrderDocumentParameterGenerator;
import uk.gov.hmcts.reform.fpl.service.orders.generator.C35aSupervisionOrderDocumentParameterGenerator;
import uk.gov.hmcts.reform.fpl.service.orders.generator.C35bISODocumentParameterGenerator;
import uk.gov.hmcts.reform.fpl.service.orders.generator.C43ChildArrangementOrderDocumentParameterGenerator;
import uk.gov.hmcts.reform.fpl.service.orders.generator.C43ChildArrangementOrderTitleGenerator;
import uk.gov.hmcts.reform.fpl.service.orders.generator.C43aSpecialGuardianshipOrderDocumentParameterGenerator;
import uk.gov.hmcts.reform.fpl.service.orders.generator.C45aParentalResponsibilityOrderDocumentParameterGenerator;
import uk.gov.hmcts.reform.fpl.service.orders.generator.C47AAppointmentOfAChildrensGuardianParameterGenerator;
import uk.gov.hmcts.reform.fpl.service.orders.generator.DocmosisCommonElementDecorator;
import uk.gov.hmcts.reform.fpl.service.orders.generator.OrderDocumentGenerator;
import uk.gov.hmcts.reform.fpl.service.orders.generator.OrderDocumentGeneratorHolder;
import uk.gov.hmcts.reform.fpl.service.orders.generator.UploadedOrderDocumentGenerator;
import uk.gov.hmcts.reform.fpl.service.orders.generator.common.OrderDetailsWithEndTypeGenerator;
import uk.gov.hmcts.reform.fpl.service.orders.generator.common.OrderMessageGenerator;
import uk.gov.hmcts.reform.fpl.service.payment.FeeService;
import uk.gov.hmcts.reform.fpl.utils.ChildSelectionUtils;
import uk.gov.hmcts.reform.fpl.utils.FixedTimeConfiguration;

import java.io.IOException;
import java.util.stream.Stream;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.fpl.utils.ResourceReader.readString;
import static uk.gov.hmcts.reform.fpl.utils.TestDataHelper.testDocument;

@ContextConfiguration(classes = {
    OrderCreationService.class,
    OrderDocumentGenerator.class,
    DocumentGenerator.class,
    DocmosisDocumentGeneratorService.class,
    OrderDocumentGeneratorHolder.class,
    DocmosisCommonElementDecorator.class,
    CaseDataExtractionService.class,
    AppointedGuardianFormatter.class,
    A70PlacementOrderDocumentParameterGenerator.class,
    A206PlacementOrderNotificationParameterGenerator.class,
    C21BlankOrderDocumentParameterGenerator.class,
    C32CareOrderDocumentParameterGenerator.class,
    C23EPODocumentParameterGenerator.class,
    C26SecureAccommodationOrderDocumentParameterGenerator.class,
    C32bDischargeOfCareOrderDocumentParameterGenerator.class,
    C35aSupervisionOrderDocumentParameterGenerator.class,
    C43ChildArrangementOrderDocumentParameterGenerator.class,
    C47AAppointmentOfAChildrensGuardianParameterGenerator.class,
    C33InterimCareOrderDocumentParameterGenerator.class,
    C23EPOAdditionalDocumentsCollector.class,
    C35bISODocumentParameterGenerator.class,
    ManageOrderDocumentService.class,
    OrderMessageGenerator.class,
    C43aSpecialGuardianshipOrderDocumentParameterGenerator.class,
    ManageOrderDocumentService.class,
    OrderMessageGenerator.class,
    C43ChildArrangementOrderTitleGenerator.class,
    C45aParentalResponsibilityOrderDocumentParameterGenerator.class,
    OrderDetailsWithEndTypeGenerator.class,
    UploadedOrderDocumentGenerator.class,
    DocumentSealingService.class,
    ChildrenSmartSelector.class,
    DocmosisHelper.class,
    ChildSelectionUtils.class,
    PlacementService.class,
    FixedTimeConfiguration.class,
    PbaNumberService.class,
    CaseConverter.class
})
@MockBeans({@MockBean(DocumentDownloadService.class), @MockBean(FeeService.class)})
class OrderCreationServiceDocmosisTest extends AbstractDocmosisTest {

    private static final String LA_CODE = "LA_CODE";
    private static final String LA_COURT = "La Court";
    private static final String LA_NAME = "LocalAuthorityName";

    private final DocmosisOrderCaseDataGenerator dataGenerator = new DocmosisOrderCaseDataGenerator();

    @SpyBean
    private DocmosisDocumentGeneratorService generatorService;
    @MockBean
    private UploadDocumentService uploadDocumentService;
    @MockBean
    private LocalAuthorityNameLookupConfiguration localAuthorityNameLookupConfiguration;
    @MockBean
    private CourtService courtService;
    @Autowired
    private DocmosisHelper docmosisHelper;

    @Autowired
    private OrderCreationService underTest;

    @ParameterizedTest
    @MethodSource("allOrders")
    void testAllOrders(Order order) throws IOException {

        String actualContent = getPdfContent(dataGenerator.generateForOrder(order));

        assertThat(actualContent)
            .isEqualToNormalizingWhitespace(getExpectedText(order));
    }

    private String getExpectedText(Order order) {
        String fileName = format("order-generation/%s.txt", order.name());
        try {
            return readString(fileName);
        } catch (Exception e) {
            throw new RuntimeException("Missing assertion text for order " + order.name() + ". Please create a "
                + "filename named " + fileName);
        }
    }

    private static Stream<Arguments> allOrders() {
        return Stream.of(Order.values())
            .filter(order -> order.getSourceType() == OrderSourceType.DIGITAL)
            .map(Arguments::of);
    }

    private String getPdfContent(CaseData caseData, String... ignores) throws IOException {
        when(courtService.getCourtName(caseData)).thenReturn(LA_COURT);
        when(localAuthorityNameLookupConfiguration.getLocalAuthorityName(LA_CODE)).thenReturn(LA_NAME);
        when(uploadDocumentService.uploadDocument(any(byte[].class),
            anyString(),
            anyString())).thenReturn(testDocument());
        doAnswer(resultsCaptor).when(generatorService).generateDocmosisDocument(anyMap(), any(), any(), any());

        underTest.createOrderDocument(caseData,
            OrderStatus.PLAIN,
            RenderFormat.PDF);

        DocmosisDocument docmosisDocument = resultsCaptor.getResult();

        byte[] bytes = docmosisDocument.getBytes();

        storeToOuputFolder(
            caseData.getManageOrdersEventData().getManageOrdersType().fileName(RenderFormat.PDF),
            bytes
        );

        String text = docmosisHelper.extractPdfContent(bytes);
        return docmosisHelper.remove(text, ignores);
    }

}
