package uk.gov.hmcts.reform.fpl.utils;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.RandomStringUtils;
import uk.gov.hmcts.reform.fpl.enums.CMOStatus;
import uk.gov.hmcts.reform.fpl.enums.DirectionAssignee;
import uk.gov.hmcts.reform.fpl.enums.DocumentStatus;
import uk.gov.hmcts.reform.fpl.enums.JudgeOrMagistrateTitle;
import uk.gov.hmcts.reform.fpl.enums.OrderStatus;
import uk.gov.hmcts.reform.fpl.enums.RepresentativeServingPreferences;
import uk.gov.hmcts.reform.fpl.model.Address;
import uk.gov.hmcts.reform.fpl.model.Applicant;
import uk.gov.hmcts.reform.fpl.model.ApplicantParty;
import uk.gov.hmcts.reform.fpl.model.CaseData;
import uk.gov.hmcts.reform.fpl.model.CaseManagementOrder;
import uk.gov.hmcts.reform.fpl.model.Child;
import uk.gov.hmcts.reform.fpl.model.ChildParty;
import uk.gov.hmcts.reform.fpl.model.Direction;
import uk.gov.hmcts.reform.fpl.model.HearingBooking;
import uk.gov.hmcts.reform.fpl.model.NextHearing;
import uk.gov.hmcts.reform.fpl.model.Order;
import uk.gov.hmcts.reform.fpl.model.OrderAction;
import uk.gov.hmcts.reform.fpl.model.Other;
import uk.gov.hmcts.reform.fpl.model.Others;
import uk.gov.hmcts.reform.fpl.model.Representative;
import uk.gov.hmcts.reform.fpl.model.Respondent;
import uk.gov.hmcts.reform.fpl.model.RespondentParty;
import uk.gov.hmcts.reform.fpl.model.Solicitor;
import uk.gov.hmcts.reform.fpl.model.common.Document;
import uk.gov.hmcts.reform.fpl.model.common.DocumentReference;
import uk.gov.hmcts.reform.fpl.model.common.Element;
import uk.gov.hmcts.reform.fpl.model.common.EmailAddress;
import uk.gov.hmcts.reform.fpl.model.common.JudgeAndLegalAdvisor;
import uk.gov.hmcts.reform.fpl.model.common.Recital;
import uk.gov.hmcts.reform.fpl.model.common.Schedule;
import uk.gov.hmcts.reform.fpl.model.common.Telephone;
import uk.gov.hmcts.reform.fpl.model.common.dynamic.DynamicList;
import uk.gov.hmcts.reform.fpl.model.common.dynamic.DynamicListElement;
import uk.gov.hmcts.reform.fpl.model.order.generated.GeneratedOrder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.StringUtils.left;
import static uk.gov.hmcts.reform.fpl.enums.ActionType.SEND_TO_ALL_PARTIES;
import static uk.gov.hmcts.reform.fpl.enums.DirectionAssignee.ALL_PARTIES;
import static uk.gov.hmcts.reform.fpl.enums.DirectionAssignee.CAFCASS;
import static uk.gov.hmcts.reform.fpl.enums.DirectionAssignee.COURT;
import static uk.gov.hmcts.reform.fpl.enums.DirectionAssignee.LOCAL_AUTHORITY;
import static uk.gov.hmcts.reform.fpl.enums.DirectionAssignee.OTHERS;
import static uk.gov.hmcts.reform.fpl.enums.DirectionAssignee.PARENTS_AND_RESPONDENTS;
import static uk.gov.hmcts.reform.fpl.enums.JudgeOrMagistrateTitle.DEPUTY_DISTRICT_JUDGE;
import static uk.gov.hmcts.reform.fpl.enums.JudgeOrMagistrateTitle.HER_HONOUR_JUDGE;
import static uk.gov.hmcts.reform.fpl.enums.JudgeOrMagistrateTitle.HIS_HONOUR_JUDGE;
import static uk.gov.hmcts.reform.fpl.enums.OtherPartiesDirectionAssignee.OTHER_1;
import static uk.gov.hmcts.reform.fpl.enums.ParentsAndRespondentsDirectionAssignee.RESPONDENT_1;
import static uk.gov.hmcts.reform.fpl.enums.YesNo.NO;
import static uk.gov.hmcts.reform.fpl.enums.YesNo.YES;
import static uk.gov.hmcts.reform.fpl.utils.DateFormatterHelper.TIME_DATE;
import static uk.gov.hmcts.reform.fpl.utils.DateFormatterHelper.formatLocalDateTimeBaseUsingFormat;
import static uk.gov.hmcts.reform.fpl.utils.DateFormatterHelper.formatLocalDateToString;
import static uk.gov.hmcts.reform.fpl.utils.ElementUtils.element;
import static uk.gov.hmcts.reform.fpl.utils.ElementUtils.wrapElements;

public class CaseDataGeneratorHelper {

    private static final String TELEPHONE = "02838882404";

    private CaseDataGeneratorHelper() {
        // NO-OP
    }

    public static HearingBooking createHearingBooking(LocalDateTime startDate, LocalDateTime endDate) {
        return HearingBooking.builder()
            .startDate(startDate)
            .venue("Venue")
            .endDate(endDate)
            .judgeAndLegalAdvisor(JudgeAndLegalAdvisor.builder()
                .judgeTitle(HER_HONOUR_JUDGE)
                .judgeLastName("Law")
                .legalAdvisorName("Peter Parker")
                .build())
            .build();
    }

    public static List<Element<Applicant>> createPopulatedApplicants() {
        return ImmutableList.of(
            Element.<Applicant>builder()
                .id(randomUUID())
                .value(Applicant.builder()
                    .leadApplicantIndicator("No")
                    .party(ApplicantParty.builder()
                        .organisationName("Bran Stark")
                        .jobTitle("Judge")
                        .address(address())
                        .email(EmailAddress.builder()
                            .email("BranStark@hMCTS.net")
                            .build())
                        .telephoneNumber(Telephone.builder()
                            .telephoneNumber(TELEPHONE)
                            .contactDirection("Bran Stark")
                            .build())
                        .build())
                    .build())
                .build(),
            Element.<Applicant>builder()
                .id(randomUUID())
                .value(Applicant.builder()
                    .leadApplicantIndicator("No")
                    .party(ApplicantParty.builder()
                        .organisationName("Sansa Stark")
                        .jobTitle("Judge")
                        .address(address())
                        .email(EmailAddress.builder()
                            .email("Harrykane@hMCTS.net")
                            .build())
                        .telephoneNumber(Telephone.builder()
                            .telephoneNumber(TELEPHONE)
                            .contactDirection("Sansa Stark")
                            .build())
                        .build())
                    .build())
                .build());
    }

    public static List<Element<Child>> createPopulatedChildren(LocalDate dateOfBirth) {
        return ElementUtils.wrapElements(
            Child.builder()
                .party(ChildParty.builder()
                    .firstName("Bran")
                    .lastName("Stark")
                    .gender("Boy")
                    .dateOfBirth(dateOfBirth)
                    .build())
                .build(),
            Child.builder()
                .party(ChildParty.builder()
                    .firstName("Sansa")
                    .lastName("Stark")
                    .gender("Boy")
                    .dateOfBirth(dateOfBirth)
                    .build())
                .build(),
            Child.builder()
                .party(ChildParty.builder()
                    .firstName("Jon")
                    .lastName("Snow")
                    .gender("Girl")
                    .dateOfBirth(dateOfBirth)
                    .build())
                .build()
        );
    }

    public static Order createStandardDirectionOrders(LocalDateTime today, OrderStatus status) {
        return Order.builder()
            .dateOfIssue("29 November 2019")
            .directions(wrapElements(Direction.builder()
                    .directionType("Test SDO type 1")
                    .directionText("Test body 1")
                    .directionNeeded(YES.getValue())
                    .dateToBeCompletedBy(today)
                    .assignee(ALL_PARTIES)
                    .build(),
                Direction.builder()
                    .directionType("Test SDO type 2")
                    .directionText("Test body 2")
                    .directionNeeded(YES.getValue())
                    .dateToBeCompletedBy(today)
                    .assignee(ALL_PARTIES)
                    .build(),
                Direction.builder()
                    .directionNeeded(NO.getValue())
                    .build()
            ))
            .orderStatus(status)
            .judgeAndLegalAdvisor(JudgeAndLegalAdvisor.builder()
                .judgeTitle(HER_HONOUR_JUDGE)
                .judgeLastName("Smith")
                .legalAdvisorName("Bob Ross")
                .build())
            .build();
    }

    public static List<Element<Respondent>> createRespondents() {
        return wrapElements(
            Respondent.builder().party(
                RespondentParty.builder()
                    .firstName("Timothy")
                    .lastName("Jones")
                    .relationshipToChild("Father")
                    .build())
                .build(),
            Respondent.builder().party(
                RespondentParty.builder()
                    .firstName("Sarah")
                    .lastName("Simpson")
                    .relationshipToChild("Mother")
                    .build())
                .build()
        );
    }

    public static Document createDocumentWithStatus(DocumentStatus status) {
        return Document.builder()
            .documentStatus(ofNullable(status).map(DocumentStatus::getLabel).orElse(null))
            .typeOfDocument(DocumentReference.builder()
                .filename("Mock file")
                .build())
            .build();
    }

    public static Others createOthers() {
        return Others.builder()
            .firstOther(Other.builder()
                .birthPlace("Newry")
                .childInformation("Child suffers from ADD")
                .DOB("02/02/05")
                .gender("Male")
                .name("Kyle Stafford")
                .telephone(TELEPHONE)
                .address(address())
                .build())
            .additionalOthers(ImmutableList.of(
                Element.<Other>builder()
                    .value(Other.builder()
                        .birthPlace("Craigavon")
                        .DOB("02/02/05")
                        .gender("Female")
                        .name("Sarah Simpson")
                        .telephone(TELEPHONE)
                        .address(address())
                        .build())
                    .build()
            )).build();
    }

    public static List<Element<GeneratedOrder>> createOrders(DocumentReference lastOrderDocumentReference) {
        final String orderType = "Blank order (C21)";
        return ImmutableList.of(
            Element.<GeneratedOrder>builder()
                .value(GeneratedOrder.builder()
                    .type(orderType)
                    .title("Example Order")
                    .details(
                        "Example order details here - Lorem ipsum dolor sit amet, consectetur adipiscing elit")
                    .date(formatLocalDateTimeBaseUsingFormat(LocalDateTime.now().plusDays(57), TIME_DATE))
                    .judgeAndLegalAdvisor(createJudgeAndLegalAdvisor("Peter Parker",
                        "Judy", null, HER_HONOUR_JUDGE))
                    .build())
                .build(),
            Element.<GeneratedOrder>builder()
                .id(UUID.randomUUID())
                .value(GeneratedOrder.builder()
                    .type(orderType)
                    .title("Winter is here")
                    .details("Westeros")
                    .date(formatLocalDateTimeBaseUsingFormat(LocalDateTime.now().plusDays(59), TIME_DATE))
                    .judgeAndLegalAdvisor(createJudgeAndLegalAdvisor("Baratheon",
                        "Tyrion Lannister", "Lannister", HIS_HONOUR_JUDGE))
                    .document(createDocumentReference(randomUUID().toString()))
                    .build())
                .build(),
            Element.<GeneratedOrder>builder()
                .id(UUID.randomUUID())
                .value(GeneratedOrder.builder()
                    .type(orderType)
                    .title("Black Sails")
                    .details("Long John Silver")
                    .date(formatLocalDateTimeBaseUsingFormat(LocalDateTime.now().plusDays(60), TIME_DATE))
                    .judgeAndLegalAdvisor(createJudgeAndLegalAdvisor("Edward Teach",
                        "Captain Flint", "Scott", DEPUTY_DISTRICT_JUDGE))
                    .document(lastOrderDocumentReference)
                    .build())
                .build());
    }

    public static List<Element<HearingBooking>> createHearingBookings(final LocalDateTime startDate,
                                                                      final LocalDateTime endDate) {
        return ImmutableList.of(Element.<HearingBooking>builder()
            .id(randomUUID())
            .value(createHearingBooking(startDate, endDate))
            .build());
    }

    public static List<Element<HearingBooking>> createHearingBookingsFromInitialDate(LocalDateTime date) {
        return ImmutableList.of(
            Element.<HearingBooking>builder()
                .id(fromString("b15eb00f-e151-47f2-8e5f-374cc6fc2657"))
                .value(createHearingBooking(date.plusDays(5), date.plusDays(6)))
                .build(),
            Element.<HearingBooking>builder()
                .id(fromString("6b3ee98f-acff-4b64-bb00-cc3db02a24b2"))
                .value(createHearingBooking(date.plusDays(2), date.plusDays(3)))
                .build(),
            Element.<HearingBooking>builder()
                .id(fromString("ecac3668-8fa6-4ba0-8894-2114601a3e31"))
                .value(createHearingBooking(date, date.plusDays(1)))
                .build()
        );
    }

    public static DocumentReference createDocumentReference(final String id) {
        final String documentUrl = "http://" + String.join("/", "dm-store:8080", "documents", id);
        return DocumentReference.builder()
            .filename(RandomStringUtils.randomAlphabetic(12) + ".pdf")
            .url(documentUrl)
            .binaryUrl(documentUrl + "/binary")
            .build();
    }

    public static JudgeAndLegalAdvisor createJudgeAndLegalAdvisor(final String legalAdvisorName,
                                                                  final String judgeFullName,
                                                                  final String judgeLastName,
                                                                  final JudgeOrMagistrateTitle judgeTitle) {
        return JudgeAndLegalAdvisor.builder()
            .legalAdvisorName(legalAdvisorName)
            .judgeLastName(judgeLastName)
            .judgeFullName(judgeFullName)
            .judgeTitle(judgeTitle)
            .build();
    }

    public static List<Element<Direction>> createElementCollection(Direction direction) {
        return ImmutableList.of(
            Element.<Direction>builder()
                .value(direction)
                .build()
        );
    }

    private static Direction createCustomDirection(DirectionAssignee assignee) {
        return Direction.builder()
            .directionType("Direction title")
            .directionText("Mock direction text")
            .directionNeeded(YES.getValue())
            .assignee(assignee)
            .readOnly("No")
            .custom("Yes")
            .dateToBeCompletedBy(LocalDateTime.of(2099, 1, 1, 10, 0, 0))
            .build();
    }

    public static Direction createUnassignedDirection() {
        return Direction.builder()
            .directionType("Direction title")
            .directionText("Mock direction text")
            .directionNeeded(YES.getValue())
            .dateToBeCompletedBy(LocalDateTime.of(2099, 1, 1, 10, 0, 0))
            .assignee(null)
            .build();
    }

    public static List<Element<Direction>> createCmoDirections() {
        return wrapElements(
            createCustomDirection(ALL_PARTIES),
            createCustomDirection(LOCAL_AUTHORITY),
            createCustomDirection(CAFCASS),
            createCustomDirection(COURT),
            createCustomDirection(PARENTS_AND_RESPONDENTS),
            createCustomDirection(OTHERS)
        );
    }

    public static List<Element<Recital>> createRecitals() {
        return wrapElements(Recital.builder()
            .title("A title")
            .description("A description")
            .build());
    }

    public static Schedule createSchedule(boolean includeSchedule) {
        if (!includeSchedule) {
            return Schedule.builder()
                .includeSchedule("No")
                .build();
        }

        return Schedule.builder()
            .allocation("An allocation")
            .alternativeCarers("Alternatives")
            .application("An application")
            .childrensCurrentArrangement("Current arrangement")
            .includeSchedule("Yes")
            .keyIssues("Key Issues")
            .partiesPositions("Some positions")
            .threshold("threshold")
            .timetableForChildren("time goes by")
            .timetableForProceedings("so slowly")
            .todaysHearing("slowly")
            .build();
    }

    public static CaseData buildCaseDataForCMODocmosisGeneration(LocalDateTime dateTime) {
        final List<Element<Direction>> cmoDirections = createCmoDirections();

        List<Element<Respondent>> respondents = createRespondents();
        Others others = createOthers();

        List<Element<Representative>> representatives = List.of(
            createRepresentativesFor(respondents.get(0).getValue()),
            createRepresentativesFor(respondents.get(0).getValue()),
            createRepresentativesFor(respondents.get(1).getValue()),
            createRepresentativesFor(others.getFirstOther()));

        return CaseData.builder()
            .caseLocalAuthority("example")
            .familyManCaseNumber("123")
            .children1(createPopulatedChildren(dateTime.toLocalDate()))
            .hearingDetails(createHearingBookingsFromInitialDate(dateTime))
            .dateSubmitted(LocalDate.now())
            .respondents1(respondents)
            .others(others)
            .solicitor(createSolicitor())
            .applicants(createPopulatedApplicants())
            .cmoHearingDateList(DynamicList.builder()
                .value(DynamicListElement.builder()
                    .code(fromString("ecac3668-8fa6-4ba0-8894-2114601a3e31"))
                    .label(formatLocalDateToString(dateTime.plusDays(5).toLocalDate(), FormatStyle.MEDIUM))
                    .build())
                .build())
            .schedule(createSchedule(true))
            .recitals(createRecitals())
            .representatives(representatives)
            .allPartiesCustomCMO(getDirectionByAssignee(cmoDirections, ALL_PARTIES))
            .localAuthorityDirectionsCustomCMO(getDirectionByAssignee(cmoDirections, LOCAL_AUTHORITY))
            .courtDirectionsCustomCMO(getDirectionByAssignee(cmoDirections, COURT))
            .cafcassDirectionsCustomCMO(getDirectionByAssignee(cmoDirections, CAFCASS))
            .otherPartiesDirectionsCustomCMO(getDirectionByAssignee(cmoDirections, OTHERS))
            .respondentDirectionsCustomCMO(getDirectionByAssignee(cmoDirections, PARENTS_AND_RESPONDENTS))
            .caseManagementOrder(createApprovedCMO())
            .servedCaseManagementOrders(wrapElements(CaseManagementOrder.builder().build()))
            .build();
    }

    private static Element<Representative> createRepresentativesFor(Respondent respondent) {
        RespondentParty respondentParty = respondent.getParty();
        String initials = left(respondentParty.getFirstName(), 1) + left(respondentParty.getLastName(), 1);

        int representativeSequence = respondent.getRepresentedBy().size() + 1;

        Element<Representative> representative = element(Representative.builder()
            .fullName(String.format("George Rep %s (%s)", representativeSequence, initials))
            .email(String.format("%s%s@representatives.com", representativeSequence, initials))
            .telephoneNumber(String.format("+44 7900000%s", representativeSequence))
            .build());

        respondent.addRepresentative(representative.getId());

        return representative;
    }

    private static Element<Representative> createRepresentativesFor(Other other) {
        String respondentInitials = left(other.getName(), 1);
        int representativeSequence = other.getRepresentedBy().size() + 1;

        Element<Representative> representative = element(Representative.builder()
            .fullName(String.format("Barbara Rep %s (%s)", representativeSequence, respondentInitials))
            .email(String.format("%s%s@representatives.com", representativeSequence, respondentInitials))
            .telephoneNumber(String.format("+44 7100000%s", representativeSequence))
            .build());

        other.addRepresentative(representative.getId());

        return representative;
    }

    public static CaseManagementOrder createCaseManagementOrder(CMOStatus status) {
        return CaseManagementOrder.builder()
            .status(status)
            .schedule(createSchedule(true))
            .recitals(createRecitals())
            .directions(createCmoDirections())
            .build();
    }

    public static List<Element<Representative>> createRepresentatives(
        RepresentativeServingPreferences servingPreferences) {
        return wrapElements(Representative.builder()
            .email("abc@example.com")
            .fullName("Jon Snow")
            .servingPreferences(servingPreferences)
            .build());
    }

    private static Solicitor createSolicitor() {
        return Solicitor.builder()
            .name("Bruce Wayne")
            .email("bruce-wayne@notbatman.com")
            .mobile("07700900304")
            .build();
    }

    private static List<Element<Direction>> getDirectionByAssignee(List<Element<Direction>> list,
                                                                   DirectionAssignee assignee) {
        return list.stream()
            .filter(element -> element.getValue().getAssignee().equals(assignee))
            .map(element -> {
                Element<Direction> prepared = element;
                final UUID elementId = element.getId();
                final Direction direction = element.getValue();

                if (assignee.equals(OTHERS)) {
                    prepared = Element.<Direction>builder()
                        .id(elementId)
                        .value(direction.toBuilder()
                            .otherPartiesAssignee(OTHER_1)
                            .build())
                        .build();
                }

                if (assignee.equals(PARENTS_AND_RESPONDENTS)) {
                    prepared = Element.<Direction>builder()
                        .id(elementId)
                        .value(direction.toBuilder()
                            .parentsAndRespondentsAssignee(RESPONDENT_1)
                            .build())
                        .build();
                }

                return prepared;
            })
            .collect(Collectors.toList());
    }

    public static DynamicList createHearingBookingDynamicList() {
        return DynamicList.builder()
            .value(DynamicListElement.builder()
                .code(fromString("b15eb00f-e151-47f2-8e5f-374cc6fc2657"))
                .label("15th Dec 2019")
                .build())
            .listItems(List.of(DynamicListElement.builder().code(UUID.randomUUID()).label("test").build()))
            .build();
    }

    private static CaseManagementOrder createApprovedCMO() {
        return CaseManagementOrder.builder()
            .action(OrderAction.builder()
                .type(SEND_TO_ALL_PARTIES)
                .build())
            .nextHearing(NextHearing.builder()
                .id(fromString("ecac3668-8fa6-4ba0-8894-2114601a3e31"))
                .build())
            .build();
    }

    private static Address address() {
        return Address.builder()
            .addressLine1("1 Some street")
            .addressLine2("Some road")
            .postTown("some town")
            .postcode("BT66 7RR")
            .county("Some county")
            .country("UK")
            .build();
    }
}
