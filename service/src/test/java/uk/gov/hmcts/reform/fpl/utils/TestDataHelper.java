package uk.gov.hmcts.reform.fpl.utils;

import feign.FeignException;
import feign.Request;
import feign.Response;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.tuple.Pair;
import uk.gov.hmcts.reform.calendar.model.BankHolidays;
import uk.gov.hmcts.reform.document.domain.Document;
import uk.gov.hmcts.reform.fnp.model.fee.FeeResponse;
import uk.gov.hmcts.reform.fpl.enums.ChildGender;
import uk.gov.hmcts.reform.fpl.enums.HearingType;
import uk.gov.hmcts.reform.fpl.enums.RepresentativeRole;
import uk.gov.hmcts.reform.fpl.enums.RepresentativeServingPreferences;
import uk.gov.hmcts.reform.fpl.enums.SolicitorRole;
import uk.gov.hmcts.reform.fpl.model.Address;
import uk.gov.hmcts.reform.fpl.model.Child;
import uk.gov.hmcts.reform.fpl.model.ChildParty;
import uk.gov.hmcts.reform.fpl.model.Court;
import uk.gov.hmcts.reform.fpl.model.HearingBooking;
import uk.gov.hmcts.reform.fpl.model.Judge;
import uk.gov.hmcts.reform.fpl.model.Other;
import uk.gov.hmcts.reform.fpl.model.Recipient;
import uk.gov.hmcts.reform.fpl.model.Representative;
import uk.gov.hmcts.reform.fpl.model.Respondent;
import uk.gov.hmcts.reform.fpl.model.RespondentParty;
import uk.gov.hmcts.reform.fpl.model.RespondentSolicitor;
import uk.gov.hmcts.reform.fpl.model.SentDocument;
import uk.gov.hmcts.reform.fpl.model.common.DocmosisDocument;
import uk.gov.hmcts.reform.fpl.model.common.DocumentReference;
import uk.gov.hmcts.reform.fpl.model.common.Element;
import uk.gov.hmcts.reform.fpl.model.common.EmailAddress;
import uk.gov.hmcts.reform.fpl.model.common.JudgeAndLegalAdvisor;
import uk.gov.hmcts.reform.fpl.model.common.Telephone;
import uk.gov.hmcts.reform.fpl.model.common.dynamic.DynamicList;
import uk.gov.hmcts.reform.fpl.model.common.dynamic.DynamicListElement;
import uk.gov.hmcts.reform.fpl.model.docmosis.DocmosisJudge;
import uk.gov.hmcts.reform.rd.model.Organisation;
import uk.gov.hmcts.reform.sendletter.api.LetterWithPdfsRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static feign.Request.HttpMethod.GET;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.LocalDate.now;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static uk.gov.hmcts.reform.fpl.enums.ChildGender.BOY;
import static uk.gov.hmcts.reform.fpl.enums.HearingType.CASE_MANAGEMENT;
import static uk.gov.hmcts.reform.fpl.enums.JudgeOrMagistrateTitle.HER_HONOUR_JUDGE;
import static uk.gov.hmcts.reform.fpl.enums.JudgeOrMagistrateTitle.MAGISTRATES;
import static uk.gov.hmcts.reform.fpl.utils.DateFormatterHelper.formatLocalDateTimeBaseUsingFormat;
import static uk.gov.hmcts.reform.fpl.utils.ElementUtils.element;

public class TestDataHelper {
    public static final String ALLOCATED_JUDGE_KEY = "allocatedJudge";
    public static final byte[] DOCUMENT_CONTENT = {1, 2, 3, 4, 5};

    private TestDataHelper() {
    }

    public static Court testCourt() {
        final String name = randomAlphanumeric(5);

        return Court.builder()
            .code(randomNumeric(3))
            .name(name)
            .email(String.format("%s@test.com", name))
            .build();
    }

    public static Organisation testOrganisation() {
        return testOrganisation(RandomStringUtils.randomAlphanumeric(5));
    }

    public static Organisation testOrganisation(String organisationCode) {
        return Organisation.builder()
            .organisationIdentifier(organisationCode)
            .build();
    }

    public static byte[] testDocumentBinary() {
        return RandomUtils.nextBytes(RandomUtils.nextInt(3, 10));
    }

    public static DocumentReference testDocumentReference() {
        return testDocumentReference(randomAlphanumeric(10));
    }

    public static DocumentReference testDocumentReference(String filename) {
        return DocumentReference.builder()
            .filename(filename)
            .url(randomAlphanumeric(10))
            .binaryUrl(randomAlphanumeric(10))
            .build();
    }

    public static Document testDocument() {
        final Document.Link binaryLink = new Document.Link();
        binaryLink.href = randomAlphanumeric(10);
        final Document.Link selfLink = new Document.Link();
        selfLink.href = randomAlphanumeric(10);

        final Document.Links links = new Document.Links();
        links.binary = binaryLink;
        links.self = selfLink;

        final Document document = new Document();
        document.links = links;
        document.originalDocumentName = randomAlphanumeric(10);

        return document;
    }

    public static DocmosisDocument testDocmosisDocument(byte[] binaries) {
        return DocmosisDocument.builder()
            .documentTitle(randomAlphanumeric(10))
            .bytes(binaries)
            .build();
    }

    public static byte[] testDocumentBinaries() {
        return randomAlphanumeric(10).getBytes();
    }

    public static Element<Child> testChild() {
        return testChild(null);
    }

    public static Element<Child> testChild(ChildGender childGender) {
        return testChild(randomAlphanumeric(10), randomAlphanumeric(10), childGender, now());
    }

    public static Element<Child> testChild(String firstName, String lastName) {
        return testChild(firstName, lastName, null, now());
    }

    public static Element<Child> testChild(String firstName, String lastName, ChildGender gender, LocalDate dob) {
        return element(Child.builder()
            .party(testChildParty(firstName, lastName, gender, dob))
            .build());
    }

    public static ChildParty testChildParty(String firstName, String lastName, ChildGender gender, LocalDate dob) {
        return ChildParty.builder()
            .firstName(firstName)
            .lastName(lastName)
            .gender(ofNullable(gender).map(ChildGender::getLabel).orElse(null))
            .dateOfBirth(dob)
            .build();
    }

    public static ChildParty testChildParty() {
        return testChildParty(
            randomAlphanumeric(10),
            randomAlphanumeric(10),
            BOY,
            LocalDate.now().minusDays(RandomUtils.nextInt(1, 1000)));
    }

    public static List<Element<Child>> testChildren() {
        return List.of(testChild(), testChild(), testChild());
    }

    public static Element<Respondent> testRespondent(String firstName, String lastName) {
        return testRespondent(firstName, lastName, "");
    }

    public static Element<Respondent> testRespondent(String firstName, String lastName, String relationshipToChild) {
        return element(Respondent.builder()
            .party(RespondentParty.builder()
                .firstName(firstName)
                .lastName(lastName)
                .relationshipToChild(relationshipToChild)
                .build())
            .build());
    }

    public static Element<Respondent> testRespondentWithAddress(String firstName, String lastName) {
        return testRepresentedRespondentWithAddress(firstName, lastName, null);
    }

    public static Element<Respondent> testRepresentedRespondentWithAddress(String firstName,
                                                                           String lastName,
                                                                           String solicitorEmail) {
        Respondent.RespondentBuilder respondentBuilder = Respondent.builder()
            .party(RespondentParty.builder()
                .firstName(firstName)
                .lastName(lastName)
                .address(testAddress())
                .build()
            );

        if (solicitorEmail != null) {
            respondentBuilder.solicitor(RespondentSolicitor.builder().email(solicitorEmail).build());
        }

        return element(respondentBuilder.build());
    }

    public static Representative testRepresentative() {
        return testRepresentative(RepresentativeServingPreferences.POST);
    }

    public static Representative testRepresentative(RepresentativeServingPreferences servingPreferences) {
        return Representative.builder()
            .fullName(randomAlphanumeric(10))
            .positionInACase("Position")
            .role(RepresentativeRole.REPRESENTING_PERSON_1)
            .servingPreferences(servingPreferences)
            .address(Address.builder().build())
            .build();
    }

    public static Address testAddress() {
        return Address.builder()
            .addressLine1("Flat 1001")
            .addressLine2("Pinnacle Apartments")
            .addressLine3("Saffron Central Square")
            .postcode("CRO 2GE")
            .build();
    }

    public static Telephone testTelephone() {
        return Telephone.builder()
            .telephoneNumber("0795554444")
            .contactDirection("Harry Kane")
            .build();
    }

    public static EmailAddress testEmail() {
        return EmailAddress.builder().email("test@test.com").build();
    }

    public static Other testOther() {
        return Other.builder()
            .name(randomAlphanumeric(20))
            .birthPlace(randomAlphanumeric(10))
            .address(testAddress())
            .build();
    }

    public static Other testOther(String name) {
        return Other.builder()
            .name(name)
            .birthPlace(randomAlphanumeric(10))
            .address(testAddress())
            .build();
    }

    public static Judge testJudge() {
        return Judge.builder()
            .judgeTitle(MAGISTRATES)
            .judgeLastName("Stark")
            .judgeFullName("Brandon Stark")
            .build();
    }

    public static JudgeAndLegalAdvisor testJudgeAndLegalAdviser() {
        return JudgeAndLegalAdvisor.builder()
            .judgeTitle(HER_HONOUR_JUDGE)
            .judgeLastName("Judy")
            .legalAdvisorName("Peter Parker")
            .build();
    }

    public static DocmosisJudge testDocmosisJudge() {
        return DocmosisJudge.builder()
            .judgeTitleAndName("Brandon Stark (JP)")
            .build();
    }

    public static HearingBooking testHearing() {
        return testHearing(CASE_MANAGEMENT);
    }

    public static HearingBooking testHearing(HearingType type) {
        return HearingBooking.builder()
            .type(type)
            .startDate(LocalDateTime.now())
            .endDate(LocalDateTime.now().plusHours(1))
            .venue("EXAMPLE")
            .build();
    }

    public static LetterWithPdfsRequest printRequest(Long caseId, DocumentReference order, byte[]... binaries) {
        List<String> documents = Stream.of(binaries)
            .map(Base64.getEncoder()::encodeToString)
            .collect(toList());
        Map<String, Object> parameters = Map.of("caseId", caseId, "documentName", order.getFilename());
        return new LetterWithPdfsRequest(documents, "FPLA001", parameters);
    }

    public static SentDocument documentSent(Recipient recipient, Document coversheet, Document document,
                                            UUID letterId, LocalDateTime sentAt) {
        return SentDocument.builder()
            .partyName(recipient.getFullName())
            .letterId(letterId.toString())
            .document(DocumentReference.buildFromDocument(document))
            .coversheet(DocumentReference.buildFromDocument(coversheet))
            .sentAt(formatLocalDateTimeBaseUsingFormat(sentAt, "h:mma, d MMMM yyyy"))
            .build();
    }

    public static FeignException feignException(int status) {
        return feignException(status, "Test");
    }

    public static FeignException feignException(int status, String message) {
        return FeignException.errorStatus(message, Response.builder()
            .status(status)
            .request(Request.create(GET, EMPTY, Map.of(), new byte[]{}, UTF_8, null))
            .build());
    }

    public static DynamicList buildDynamicList(List<Pair<UUID, String>> listElements) {
        return buildDynamicList(-1, listElements);
    }

    @SafeVarargs
    public static DynamicList buildDynamicList(Pair<UUID, String>... listElements) {
        return buildDynamicList(-1, listElements);
    }

    @SafeVarargs
    public static DynamicList buildDynamicList(int selected, Pair<UUID, String>... listElements) {
        Stream<Pair<UUID, String>> pairStream = Arrays.stream(listElements);
        return buildDynamicList(selected, pairStream);
    }

    public static DynamicList buildDynamicList(int selected, List<Pair<UUID, String>> listElements) {
        Stream<Pair<UUID, String>> pairStream = listElements.stream();
        return buildDynamicList(selected, pairStream);
    }

    private static DynamicList buildDynamicList(int selected, Stream<Pair<UUID, String>> pairStream) {
        List<DynamicListElement> listItems = pairStream
            .map(listElement -> DynamicListElement.builder()
                .code(listElement.getKey())
                .label(listElement.getValue())
                .build())
            .collect(Collectors.toList());

        return DynamicList.builder()
            .listItems(listItems)
            .value(selected != -1 && selected < listItems.size() ? listItems.get(selected) : DynamicListElement.EMPTY)
            .build();
    }

    public static DynamicList caseRoleDynamicList(String role) {
        final DynamicListElement dynamicListElement = DynamicListElement.builder()
            .code(role)
            .label(role)
            .build();

        return DynamicList.builder()
            .value(dynamicListElement)
            .listItems(List.of(dynamicListElement))
            .build();
    }

    public static DynamicList caseRoleDynamicList(SolicitorRole role) {
        return caseRoleDynamicList(role.getCaseRoleLabel());
    }

    public static BankHolidays bankHolidays(LocalDate... days) {
        final List<BankHolidays.Division.Event> events = Stream.of(days)
            .map(day -> BankHolidays.Division.Event.builder()
                .date(day)
                .build())
            .collect(toList());

        return BankHolidays.builder()
            .englandAndWales(BankHolidays.Division.builder()
                .events(events)
                .build())
            .build();
    }

    public static FeeResponse feeResponse(double amount) {
        final FeeResponse feeResponse = new FeeResponse();
        feeResponse.setAmount(BigDecimal.valueOf(amount));
        return feeResponse;
    }
}
