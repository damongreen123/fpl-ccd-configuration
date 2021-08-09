package uk.gov.hmcts.reform.fpl.handlers;

import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.fpl.config.CafcassLookupConfiguration;
import uk.gov.hmcts.reform.fpl.events.cmo.DraftOrdersApproved;
import uk.gov.hmcts.reform.fpl.handlers.cmo.DraftOrdersApprovedEventHandler;
import uk.gov.hmcts.reform.fpl.model.Address;
import uk.gov.hmcts.reform.fpl.model.CaseData;
import uk.gov.hmcts.reform.fpl.model.HearingBooking;
import uk.gov.hmcts.reform.fpl.model.Other;
import uk.gov.hmcts.reform.fpl.model.Others;
import uk.gov.hmcts.reform.fpl.model.Representative;
import uk.gov.hmcts.reform.fpl.model.Respondent;
import uk.gov.hmcts.reform.fpl.model.RespondentParty;
import uk.gov.hmcts.reform.fpl.model.common.Element;
import uk.gov.hmcts.reform.fpl.model.common.Party;
import uk.gov.hmcts.reform.fpl.model.notify.LocalAuthorityInboxRecipientsRequest;
import uk.gov.hmcts.reform.fpl.model.notify.cmo.ApprovedOrdersTemplate;
import uk.gov.hmcts.reform.fpl.model.order.HearingOrder;
import uk.gov.hmcts.reform.fpl.service.CourtService;
import uk.gov.hmcts.reform.fpl.service.FeatureToggleService;
import uk.gov.hmcts.reform.fpl.service.InboxLookupService;
import uk.gov.hmcts.reform.fpl.service.SendDocumentService;
import uk.gov.hmcts.reform.fpl.service.email.NotificationService;
import uk.gov.hmcts.reform.fpl.service.email.RepresentativesInbox;
import uk.gov.hmcts.reform.fpl.service.email.content.cmo.ReviewDraftOrdersEmailContentProvider;
import uk.gov.hmcts.reform.fpl.service.others.OtherRecipientsInbox;
import uk.gov.hmcts.reform.fpl.service.representative.RepresentativeNotificationService;
import uk.gov.hmcts.reform.fpl.utils.TestDataHelper;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static uk.gov.hmcts.reform.fpl.NotifyTemplates.JUDGE_APPROVES_DRAFT_ORDERS;
import static uk.gov.hmcts.reform.fpl.enums.RepresentativeServingPreferences.DIGITAL_SERVICE;
import static uk.gov.hmcts.reform.fpl.enums.RepresentativeServingPreferences.EMAIL;
import static uk.gov.hmcts.reform.fpl.enums.RepresentativeServingPreferences.POST;
import static uk.gov.hmcts.reform.fpl.handlers.NotificationEventHandlerTestData.CAFCASS_EMAIL_ADDRESS;
import static uk.gov.hmcts.reform.fpl.handlers.NotificationEventHandlerTestData.CTSC_INBOX;
import static uk.gov.hmcts.reform.fpl.handlers.NotificationEventHandlerTestData.LOCAL_AUTHORITY_CODE;
import static uk.gov.hmcts.reform.fpl.handlers.NotificationEventHandlerTestData.LOCAL_AUTHORITY_EMAIL_ADDRESS;
import static uk.gov.hmcts.reform.fpl.utils.CaseDataGeneratorHelper.createRepresentatives;
import static uk.gov.hmcts.reform.fpl.utils.ElementUtils.element;
import static uk.gov.hmcts.reform.fpl.utils.ElementUtils.unwrapElements;
import static uk.gov.hmcts.reform.fpl.utils.ElementUtils.wrapElements;
import static uk.gov.hmcts.reform.fpl.utils.TestDataHelper.testAddress;

@ExtendWith(SpringExtension.class)
class DraftOrdersApprovedEventHandlerTest {
    private static final UUID HEARING_ID = randomUUID();
    private static final Element<HearingBooking> HEARING = element(HEARING_ID, HearingBooking.builder().build());
    private static final ApprovedOrdersTemplate EXPECTED_TEMPLATE = ApprovedOrdersTemplate.builder().build();

    @Mock
    private SendDocumentService sendDocumentService;
    @Mock
    private CourtService courtService;
    @Mock
    private RepresentativeNotificationService representativeNotificationService;
    @Mock
    private CafcassLookupConfiguration cafcassLookupConfiguration;
    @Mock
    private InboxLookupService inboxLookupService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private ReviewDraftOrdersEmailContentProvider reviewDraftOrdersEmailContentProvider;
    @Mock
    private RepresentativesInbox representativesInbox;
    @Mock
    private OtherRecipientsInbox otherRecipientsInbox;
    @Mock
    private FeatureToggleService toggleService;

    @InjectMocks
    private DraftOrdersApprovedEventHandler underTest;

    @Test
    void shouldNotifyAdminAndLAOfApprovedOrders() {
        CaseData caseData = CaseData.builder()
            .id(12345L)
            .hearingDetails(List.of(HEARING))
            .lastHearingOrderDraftsHearingId(HEARING_ID)
            .build();
        List<HearingOrder> orders = List.of();
        ApprovedOrdersTemplate expectedTemplate = ApprovedOrdersTemplate.builder().build();

        given(courtService.getCourtEmail(caseData)).willReturn(CTSC_INBOX);
        given(inboxLookupService.getRecipients(
            LocalAuthorityInboxRecipientsRequest.builder().caseData(caseData).build()))
            .willReturn(Set.of(LOCAL_AUTHORITY_EMAIL_ADDRESS));

        given(reviewDraftOrdersEmailContentProvider.buildOrdersApprovedContent(
            caseData, HEARING.getValue(), orders, DIGITAL_SERVICE)).willReturn(EXPECTED_TEMPLATE);

        underTest.sendNotificationToAdminAndLA(new DraftOrdersApproved(caseData, orders));

        verify(notificationService).sendEmail(
            JUDGE_APPROVES_DRAFT_ORDERS,
            Set.of(LOCAL_AUTHORITY_EMAIL_ADDRESS),
            EXPECTED_TEMPLATE,
            caseData.getId().toString());

        verify(notificationService).sendEmail(
            JUDGE_APPROVES_DRAFT_ORDERS,
            CTSC_INBOX,
            EXPECTED_TEMPLATE,
            caseData.getId().toString());
    }

    @Test
    void shouldNotifyCafcass() {
        CaseData caseData = CaseData.builder()
            .id(12345L)
            .caseLocalAuthority(LOCAL_AUTHORITY_CODE)
            .hearingDetails(List.of(HEARING))
            .lastHearingOrderDraftsHearingId(HEARING_ID)
            .build();

        List<HearingOrder> orders = List.of(hearingOrder());
        CafcassLookupConfiguration.Cafcass cafcass =
            new CafcassLookupConfiguration.Cafcass(LOCAL_AUTHORITY_CODE, CAFCASS_EMAIL_ADDRESS);

        given(cafcassLookupConfiguration.getCafcass(LOCAL_AUTHORITY_CODE)).willReturn(cafcass);
        given(reviewDraftOrdersEmailContentProvider.buildOrdersApprovedContent(
            caseData, HEARING.getValue(), orders, EMAIL)).willReturn(EXPECTED_TEMPLATE);

        underTest.sendNotificationToCafcass(new DraftOrdersApproved(caseData, orders));

        verify(notificationService).sendEmail(
            JUDGE_APPROVES_DRAFT_ORDERS,
            CAFCASS_EMAIL_ADDRESS,
            EXPECTED_TEMPLATE,
            caseData.getId().toString());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @SuppressWarnings("unchecked")
    void shouldNotifyDigitalRepresentativesExcludingUnselectedOthersWhenServingOthersIsEnabled(
        boolean servingOthersEnabled) {
        List<Representative> digitalReps = unwrapElements(createRepresentatives(DIGITAL_SERVICE));
        CaseData caseData = CaseData.builder()
            .id(12345L)
            .caseLocalAuthority(LOCAL_AUTHORITY_CODE)
            .hearingDetails(List.of(HEARING))
            .representatives(wrapElements(digitalReps))
            .lastHearingOrderDraftsHearingId(HEARING_ID)
            .build();

        given(toggleService.isServeOrdersAndDocsToOthersEnabled()).willReturn(servingOthersEnabled);
        given(representativesInbox.getEmailsByPreference(caseData, DIGITAL_SERVICE))
            .willReturn(newHashSet("digital-rep1@test.com", "digital-rep2@test.com"));

        if (servingOthersEnabled) {
            given(otherRecipientsInbox.getNonSelectedRecipients(eq(DIGITAL_SERVICE), eq(caseData), any(), any()))
                .willReturn((Set) Set.of("digital-rep1@test.com"));
        }

        List<HearingOrder> orders = List.of(hearingOrder());
        given(reviewDraftOrdersEmailContentProvider.buildOrdersApprovedContent(
            caseData, HEARING.getValue(), orders, DIGITAL_SERVICE)).willReturn(EXPECTED_TEMPLATE);

        underTest.sendNotificationToDigitalRepresentatives(new DraftOrdersApproved(caseData, orders));

        if (servingOthersEnabled) {
            verify(representativeNotificationService).sendNotificationToRepresentatives(
                12345L,
                EXPECTED_TEMPLATE,
                Set.of("digital-rep2@test.com"),
                JUDGE_APPROVES_DRAFT_ORDERS
            );
        } else {
            verify(representativeNotificationService).sendNotificationToRepresentatives(
                12345L,
                EXPECTED_TEMPLATE,
                Set.of("digital-rep1@test.com", "digital-rep2@test.com"),
                JUDGE_APPROVES_DRAFT_ORDERS
            );
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @SuppressWarnings("unchecked")
    void shouldNotifyEmailRepresentativesExcludingUnselectedOthersWhenServingOthersIsEnabled(boolean othersToggle) {
        List<Representative> emailReps = unwrapElements(createRepresentatives(EMAIL));
        CaseData caseData = CaseData.builder()
            .id(12345L)
            .caseLocalAuthority(LOCAL_AUTHORITY_CODE)
            .hearingDetails(List.of(HEARING))
            .representatives(wrapElements(emailReps))
            .lastHearingOrderDraftsHearingId(HEARING_ID)
            .build();

        given(toggleService.isServeOrdersAndDocsToOthersEnabled()).willReturn(othersToggle);
        given(representativesInbox.getEmailsByPreference(caseData, EMAIL))
            .willReturn(newHashSet("rep1@test.com", "rep2@test.com"));

        if (othersToggle) {
            given(otherRecipientsInbox.getNonSelectedRecipients(eq(EMAIL), eq(caseData), any(), any()))
                .willReturn((Set) Set.of("rep2@test.com"));
        }

        List<HearingOrder> orders = List.of(hearingOrder());
        given(reviewDraftOrdersEmailContentProvider.buildOrdersApprovedContent(
            caseData, HEARING.getValue(), orders, EMAIL)).willReturn(EXPECTED_TEMPLATE);

        underTest.sendNotificationToEmailRepresentatives(new DraftOrdersApproved(caseData, orders));
        if (othersToggle) {
            verify(representativeNotificationService).sendNotificationToRepresentatives(
                12345L,
                EXPECTED_TEMPLATE,
                Set.of("rep1@test.com"),
                JUDGE_APPROVES_DRAFT_ORDERS
            );
        } else {
            verify(representativeNotificationService).sendNotificationToRepresentatives(
                12345L,
                EXPECTED_TEMPLATE,
                Set.of("rep1@test.com", "rep2@test.com"),
                JUDGE_APPROVES_DRAFT_ORDERS
            );
        }
    }

    @Test
    void shouldNotNotifyDigitalRepresentativesWhenNotPresent() {
        final CaseData caseData = CaseData.builder()
            .id(12345L)
            .caseLocalAuthority(LOCAL_AUTHORITY_CODE)
            .representatives(emptyList())
            .build();

        final List<HearingOrder> orders = List.of(hearingOrder());
        given(representativesInbox.getEmailsByPreference(caseData, DIGITAL_SERVICE)).willReturn(Set.of());

        underTest.sendNotificationToDigitalRepresentatives(new DraftOrdersApproved(caseData, orders));

        verifyNoInteractions(representativeNotificationService);
    }

    @Test
    void shouldNotNotifyEmailRepresentativesWhenNotPresent() {
        final CaseData caseData = CaseData.builder()
            .id(12345L)
            .caseLocalAuthority(LOCAL_AUTHORITY_CODE)
            .representatives(emptyList())
            .build();

        final List<HearingOrder> orders = List.of(hearingOrder());
        given(representativesInbox.getEmailsByPreference(caseData, EMAIL)).willReturn(Set.of());

        underTest.sendNotificationToEmailRepresentatives(new DraftOrdersApproved(caseData, orders));

        verifyNoInteractions(representativeNotificationService);
    }

    @Test
    void shouldSendOrderDocumentToRecipientsWhenServingOthersIsDisabled() {
        final HearingOrder hearingOrder1 = hearingOrder();
        final HearingOrder hearingOrder2 = hearingOrder();

        final Representative representative = Representative.builder()
            .fullName("Postal Rep")
            .servingPreferences(POST)
            .address(testAddress())
            .build();

        final RespondentParty respondent = RespondentParty.builder()
            .firstName("Postal")
            .lastName("Person")
            .address(testAddress())
            .build();

        final List<HearingOrder> orders = List.of(hearingOrder1, hearingOrder2);

        final CaseData caseData = CaseData.builder()
            .id(RandomUtils.nextLong())
            .representatives(wrapElements(representative))
            .respondents1(wrapElements(Respondent.builder().party(respondent).build()))
            .build();

        given(toggleService.isServeOrdersAndDocsToOthersEnabled()).willReturn(false);
        given(sendDocumentService.getStandardRecipients(caseData)).willReturn(List.of(representative, respondent));

        underTest.sendDocumentToPostRecipients(new DraftOrdersApproved(caseData, orders));

        verify(sendDocumentService).getStandardRecipients(caseData);
        verify(sendDocumentService).sendDocuments(caseData,
            List.of(hearingOrder1.getOrder(), hearingOrder2.getOrder()), List.of(representative, respondent));
        verifyNoMoreInteractions(sendDocumentService);
    }

    @Test
    void shouldPostOrderDocumentToRecipientsWhenServingOthersIsEnabled() {
        final Other firstOther = Other.builder().name("other1")
            .address(Address.builder().postcode("SE1").build()).build();

        final HearingOrder hearingOrder1 = hearingOrder(wrapElements(firstOther));
        final HearingOrder hearingOrder2 = hearingOrder();

        final Representative representative = Representative.builder()
            .fullName("Postal Rep")
            .servingPreferences(POST)
            .address(testAddress())
            .build();

        final RespondentParty respondent = RespondentParty.builder()
            .firstName("Postal")
            .lastName("Person")
            .address(testAddress())
            .build();

        final List<HearingOrder> orders = List.of(hearingOrder1, hearingOrder2);

        final CaseData caseData = CaseData.builder()
            .id(RandomUtils.nextLong())
            .representatives(wrapElements(representative))
            .respondents1(wrapElements(Respondent.builder().party(respondent).build()))
            .others(Others.builder().firstOther(firstOther).build())
            .build();

        given(toggleService.isServeOrdersAndDocsToOthersEnabled()).willReturn(true);
        Party otherParty = firstOther.toParty();
        given(sendDocumentService.getStandardRecipients(caseData))
            .willReturn(newArrayList(representative, respondent, otherParty));
        given(otherRecipientsInbox.getNonSelectedRecipients(eq(POST), eq(caseData), any(), any()))
            .willReturn(Set.of());
        given(otherRecipientsInbox.getSelectedRecipientsWithNoRepresentation(any())).willReturn(Set.of());

        underTest.sendDocumentToPostRecipients(new DraftOrdersApproved(caseData, orders));

        verify(sendDocumentService).getStandardRecipients(caseData);
        verify(sendDocumentService).sendDocuments(caseData,
            List.of(hearingOrder1.getOrder(), hearingOrder2.getOrder()),
            List.of(representative, respondent, otherParty));
    }

    private HearingOrder hearingOrder() {
        return HearingOrder.builder()
            .order(TestDataHelper.testDocumentReference())
            .build();
    }

    private HearingOrder hearingOrder(List<Element<Other>> selectedOthers) {
        return HearingOrder.builder()
            .order(TestDataHelper.testDocumentReference())
            .others(selectedOthers)
            .build();
    }
}
