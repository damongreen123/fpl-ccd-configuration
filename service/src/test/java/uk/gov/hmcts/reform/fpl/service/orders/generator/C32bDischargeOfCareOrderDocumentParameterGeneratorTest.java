package uk.gov.hmcts.reform.fpl.service.orders.generator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.fpl.config.LocalAuthorityNameLookupConfiguration;
import uk.gov.hmcts.reform.fpl.enums.DocmosisTemplates;
import uk.gov.hmcts.reform.fpl.model.CaseData;
import uk.gov.hmcts.reform.fpl.model.HearingVenue;
import uk.gov.hmcts.reform.fpl.model.configuration.Language;
import uk.gov.hmcts.reform.fpl.model.event.ManageOrdersEventData;
import uk.gov.hmcts.reform.fpl.model.order.Order;
import uk.gov.hmcts.reform.fpl.service.HearingVenueLookUpService;
import uk.gov.hmcts.reform.fpl.service.orders.docmosis.C32bDischargeOfCareOrderDocmosisParameters;
import uk.gov.hmcts.reform.fpl.service.orders.docmosis.DocmosisParameters;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class})
public class C32bDischargeOfCareOrderDocumentParameterGeneratorTest {

    private static final String LA_CODE = "LA_CODE";
    private static final String LA_NAME = "Local Authority Name";
    private static final String VENUE_ID = "1";
    private static final String VENUE_NAME = "Venue";
    private static final String DIRECTIONS = "The Court discharges the care order made by Venue made on 1st June 2021.";
    private static final String DIRECTIONS_WELSH = "Mae’r Llys yn diddymu’r gorchymyn gofal a wnaethpwyd gan Venue ar 1af Mehefin 2021.";
    private static final String FURTHER_DIRECTIONS = "Further test directions";
    public static final Language LANGUAGE = Language.ENGLISH;
    private static final CaseData CASE_DATA = CaseData.builder()
        .caseLocalAuthority(LA_CODE)
        .manageOrdersEventData(ManageOrdersEventData.builder()
            .manageOrdersCareOrderIssuedDate(LocalDate.of(2021, 6, 1))
            .manageOrdersCareOrderIssuedCourt(VENUE_ID)
            .manageOrdersType(Order.C32B_DISCHARGE_OF_CARE_ORDER)
            .manageOrdersDirections(DIRECTIONS)
            .manageOrdersFurtherDirections(FURTHER_DIRECTIONS)
            .build())
        .build();

    private static final HearingVenue HEARING_VENUE = HearingVenue.builder()
        .hearingVenueId(VENUE_ID)
        .venue(VENUE_NAME)
        .build();

    @Mock
    private LocalAuthorityNameLookupConfiguration laNameLookup;

    @Mock
    private HearingVenueLookUpService hearingVenueLookUpService;

    @InjectMocks
    private C32bDischargeOfCareOrderDocumentParameterGenerator underTest;

    @Test
    void accept() {
        assertThat(underTest.accept()).isEqualTo(Order.C32B_DISCHARGE_OF_CARE_ORDER);
    }

    @Test
    void generate() {
        when(hearingVenueLookUpService.getHearingVenue(VENUE_ID)).thenReturn(HEARING_VENUE);
        when(laNameLookup.getLocalAuthorityName(LA_CODE)).thenReturn(LA_NAME);

        DocmosisParameters generatedParameters = underTest.generate(CASE_DATA, LANGUAGE);
        assertThat(generatedParameters).isEqualTo(expectedCommonParameters());
    }

    @Test
    void generateWelsh() {
        when(hearingVenueLookUpService.getHearingVenue(VENUE_ID)).thenReturn(HEARING_VENUE);
        when(laNameLookup.getLocalAuthorityName(LA_CODE)).thenReturn(LA_NAME);

        DocmosisParameters generatedParameters = underTest.generate(CASE_DATA, Language.WELSH);
        assertThat(generatedParameters).isEqualTo(expectedCommonWelshParameters());
    }

    @Test
    void template() {
        assertThat(underTest.template()).isEqualTo(DocmosisTemplates.C32B);
    }

    private C32bDischargeOfCareOrderDocmosisParameters expectedCommonParameters() {
        return C32bDischargeOfCareOrderDocmosisParameters.builder()
            .orderTitle(Order.C32B_DISCHARGE_OF_CARE_ORDER.getTitle())
            .orderDetails(DIRECTIONS)
            .furtherDirections(FURTHER_DIRECTIONS)
            .localAuthorityName(LA_NAME)
            .build();
    }

    private C32bDischargeOfCareOrderDocmosisParameters expectedCommonWelshParameters() {
        return C32bDischargeOfCareOrderDocmosisParameters.builder()
            .orderTitle(Order.C32B_DISCHARGE_OF_CARE_ORDER.getTitle())
            .orderDetails(DIRECTIONS_WELSH)
            .furtherDirections(FURTHER_DIRECTIONS)
            .localAuthorityName(LA_NAME)
            .build();
    }
}
