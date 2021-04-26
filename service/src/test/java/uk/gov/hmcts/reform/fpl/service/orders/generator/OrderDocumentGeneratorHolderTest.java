package uk.gov.hmcts.reform.fpl.service.orders.generator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.gov.hmcts.reform.fpl.model.order.Order;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.fpl.model.order.Order.C21_BLANK_ORDER;
import static uk.gov.hmcts.reform.fpl.model.order.Order.C23_EMERGENCY_PROTECTION_ORDER;
import static uk.gov.hmcts.reform.fpl.model.order.Order.C32_CARE_ORDER;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderDocumentGeneratorHolderTest {

    private List<DocmosisParameterGenerator> generators;
    private List<AdditionalDocumentsCollector> collectors;
    private Map<Order, DocmosisParameterGenerator> typeToGenerator;
    private Map<Order, AdditionalDocumentsCollector> typeToAdditionalDocsCollector;

    // Parameter Generators
    @Mock
    private C32CareOrderDocumentParameterGenerator c32CareOrderDocumentParameterGenerator;
    @Mock
    private C21BlankOrderDocumentParameterGenerator c21BlankOrderDocumentParameterGenerator;
    @Mock
    private C23EPODocumentParameterGenerator c23EPODocumentParameterGenerator;

    // Additional Document Collectors
    @Mock
    private C23EPOAdditionalDocumentsCollector c23EPOAdditionalDocumentsCollector;

    @InjectMocks
    private OrderDocumentGeneratorHolder underTest;

    @BeforeEach
    void setUp() {
        generators = List.of(
            c32CareOrderDocumentParameterGenerator, c21BlankOrderDocumentParameterGenerator,
            c23EPODocumentParameterGenerator
        );
        collectors = List.of(c23EPOAdditionalDocumentsCollector);

        typeToGenerator = Map.of(
            C21_BLANK_ORDER, c21BlankOrderDocumentParameterGenerator,
            C32_CARE_ORDER, c32CareOrderDocumentParameterGenerator,
            C23_EMERGENCY_PROTECTION_ORDER, c23EPODocumentParameterGenerator
        );

        typeToAdditionalDocsCollector = Map.of(
            C23_EMERGENCY_PROTECTION_ORDER, c23EPOAdditionalDocumentsCollector
        );

        generators.forEach(generator -> when(generator.accept()).thenCallRealMethod());
        collectors.forEach(collector -> when(collector.accept()).thenCallRealMethod());
    }

    @Test
    void typeToGenerator() {
        assertThat(underTest.getTypeToGenerator()).isEqualTo(typeToGenerator);
    }

    @Test
    void typeToGeneratorCached() {
        underTest.getTypeToGenerator();
        assertThat(underTest.getTypeToGenerator()).isEqualTo(typeToGenerator);

        generators.forEach(generator -> verify(generator).accept());
    }

    @Test
    void typeToAdditionalDocsCollector() {
        assertThat(underTest.getTypeToAdditionalDocumentsCollector()).isEqualTo(typeToAdditionalDocsCollector);
    }

    @Test
    void typeToAdditionalDocsCollectorCached() {
        underTest.getTypeToGenerator();
        assertThat(underTest.getTypeToAdditionalDocumentsCollector()).isEqualTo(typeToAdditionalDocsCollector);

        collectors.forEach(collector -> verify(collector).accept());
    }
}
