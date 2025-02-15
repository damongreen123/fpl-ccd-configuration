package uk.gov.hmcts.reform.fpl.service.orders;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.reform.fpl.model.order.Order;
import uk.gov.hmcts.reform.fpl.model.order.OrderSourceType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.fpl.model.order.Order.A70_PLACEMENT_ORDER;
import static uk.gov.hmcts.reform.fpl.model.order.Order.AMENED_ORDER;
import static uk.gov.hmcts.reform.fpl.model.order.Order.C21_BLANK_ORDER;
import static uk.gov.hmcts.reform.fpl.model.order.Order.C23_EMERGENCY_PROTECTION_ORDER;
import static uk.gov.hmcts.reform.fpl.model.order.Order.C26_SECURE_ACCOMMODATION_ORDER;
import static uk.gov.hmcts.reform.fpl.model.order.Order.C32A_CARE_ORDER;
import static uk.gov.hmcts.reform.fpl.model.order.Order.C32B_DISCHARGE_OF_CARE_ORDER;
import static uk.gov.hmcts.reform.fpl.model.order.Order.C35A_SUPERVISION_ORDER;
import static uk.gov.hmcts.reform.fpl.model.order.Order.C37_EDUCATION_SUPERVISION_ORDER;
import static uk.gov.hmcts.reform.fpl.model.order.Order.C43A_SPECIAL_GUARDIANSHIP_ORDER;
import static uk.gov.hmcts.reform.fpl.model.order.Order.C43_CHILD_ARRANGEMENTS_SPECIFIC_ISSUE_PROHIBITED_STEPS_ORDER;
import static uk.gov.hmcts.reform.fpl.model.order.Order.C45A_PARENTAL_RESPONSIBILITY_ORDER;
import static uk.gov.hmcts.reform.fpl.model.order.Order.C47A_APPOINTMENT_OF_A_CHILDRENS_GUARDIAN;
import static uk.gov.hmcts.reform.fpl.model.order.Order.OTHER_ORDER;

class OrderShowHideQuestionsCalculatorTest {

    private static final Set<Order> MANUAL_ORDERS_WITH_IS_FINAL_ORDER_QUESTION = Set.of(
        C37_EDUCATION_SUPERVISION_ORDER,
        OTHER_ORDER
    );

    private final OrderShowHideQuestionsCalculator underTest = new OrderShowHideQuestionsCalculator();

    @Test
    void calculateAmendment() {
        assertThat(underTest.calculate(AMENED_ORDER)).containsExactlyInAnyOrderEntriesOf(Map.ofEntries(
            Map.entry("orderToAmend", "YES"),
            Map.entry("uploadAmendedOrder", "YES"),
            Map.entry("uploadOrderFile", "NO"),
            Map.entry("approvalDateTime", "NO"),
            Map.entry("approver", "NO"),
            Map.entry("previewOrder", "NO"),
            Map.entry("approvalDate", "NO"),
            Map.entry("whichChildren", "NO"),
            Map.entry("epoIncludePhrase", "NO"),
            Map.entry("manageOrdersExpiryDateWithMonth", "NO"),
            Map.entry("hearingDetails", "NO"),
            Map.entry("dischargeOfCareDetails", "NO"),
            Map.entry("manageOrdersExclusionRequirementDetails", "NO"),
            Map.entry("furtherDirections", "NO"),
            Map.entry("orderDetails", "NO"),
            Map.entry("epoChildrenDescription", "NO"),
            Map.entry("epoExpiryDate", "NO"),
            Map.entry("needSealing", "NO"),
            Map.entry("linkApplication", "NO"),
            Map.entry("isFinalOrder", "NO"),
            Map.entry("cafcassJurisdictions", "NO"),
            Map.entry("translationRequirements", "NO"),
            Map.entry("epoTypeAndPreventRemoval", "NO"),
            Map.entry("manageOrdersExpiryDateWithEndOfProceedings", "NO"),
            Map.entry("closeCase", "NO"),
            Map.entry("orderIsByConsent", "NO"),
            Map.entry("whichOthers", "YES"),
            Map.entry("appointedGuardian", "NO"),
            Map.entry("orderTitle", "NO"),
            Map.entry("childArrangementSpecificIssueProhibitedSteps", "NO"),
            Map.entry("reasonForSecureAccommodation", "NO"),
            Map.entry("orderJurisdiction", "NO"),
            Map.entry("childLegalRepresentation", "NO"),
            Map.entry("selectSingleChild", "NO"),
            Map.entry("parentResponsible", "NO"),
            Map.entry("childPlacementApplications", "NO"),
            Map.entry("childPlacementQuestions", "NO")
        ));
    }

    @ParameterizedTest(name = "Show hide map for {0}")
    @MethodSource("orderWithExpectedMap")
    void calculate(Order order, Map<String, String> expectedShowHideMap) {
        assertThat(underTest.calculate(order))
            .containsExactlyInAnyOrderEntriesOf(expectedShowHideMap);
    }

    @ParameterizedTest(name = "Show hide map for upload order {0}")
    @MethodSource("finalManualUploadOrders")
    void calculateManualUploadWithFinalOrderQuestion(Order order, Map<String, String> expectedShowHideMap) {
        assertThat(underTest.calculate(order))
            .containsExactlyInAnyOrderEntriesOf(expectedShowHideMap);
    }

    @ParameterizedTest(name = "Show hide map for upload order {0}")
    @MethodSource("nonFinalManualUploadOrders")
    void calculateManualUpload(Order order, Map<String, String> expectedShowHideMap) {
        assertThat(underTest.calculate(order))
            .containsExactlyInAnyOrderEntriesOf(expectedShowHideMap);
    }

    private static Stream<Arguments> orderWithExpectedMap() {
        Map<String, String> commonQuestions = Map.of(
            "hearingDetails", "YES",
            "linkApplication", "YES",
            "approver", "YES",
            "previewOrder", "YES",
            "orderToAmend", "NO",
            "translationRequirements", "NO",
            "uploadAmendedOrder", "NO"
        );

        Map<String, String> careOrderQuestions = new HashMap<>(commonQuestions);
        careOrderQuestions.put("orderTitle", "NO");
        careOrderQuestions.put("furtherDirections", "YES");
        careOrderQuestions.put("approvalDate", "YES");
        careOrderQuestions.put("approvalDateTime", "NO");
        careOrderQuestions.put("epoIncludePhrase", "NO");
        careOrderQuestions.put("uploadOrderFile", "NO");
        careOrderQuestions.put("needSealing", "NO");
        careOrderQuestions.put("epoChildrenDescription", "NO");
        careOrderQuestions.put("epoExpiryDate", "NO");
        careOrderQuestions.put("epoTypeAndPreventRemoval", "NO");
        careOrderQuestions.put("orderDetails", "NO");
        careOrderQuestions.put("cafcassJurisdictions", "NO");
        careOrderQuestions.put("whichChildren", "YES");
        careOrderQuestions.put("selectSingleChild", "NO");
        careOrderQuestions.put("reasonForSecureAccommodation", "NO");
        careOrderQuestions.put("childLegalRepresentation", "NO");
        careOrderQuestions.put("orderJurisdiction", "NO");
        careOrderQuestions.put("manageOrdersExpiryDateWithMonth", "NO");
        careOrderQuestions.put("manageOrdersExclusionRequirementDetails", "NO");
        careOrderQuestions.put("manageOrdersExpiryDateWithEndOfProceedings", "NO");
        careOrderQuestions.put("childArrangementSpecificIssueProhibitedSteps", "NO");
        careOrderQuestions.put("isFinalOrder", "NO");
        careOrderQuestions.put("closeCase", "YES");
        careOrderQuestions.put("whichOthers", "YES");
        careOrderQuestions.put("dischargeOfCareDetails", "NO");
        careOrderQuestions.put("orderIsByConsent", "NO");
        careOrderQuestions.put("appointedGuardian", "NO");
        careOrderQuestions.put("parentResponsible", "NO");
        careOrderQuestions.put("childPlacementApplications", "NO");
        careOrderQuestions.put("childPlacementQuestions", "NO");

        Map<String, String> dischargeOfCareQuestions = new HashMap<>(commonQuestions);
        dischargeOfCareQuestions.put("orderTitle", "NO");
        dischargeOfCareQuestions.put("furtherDirections", "YES");
        dischargeOfCareQuestions.put("approvalDate", "YES");
        dischargeOfCareQuestions.put("approvalDateTime", "NO");
        dischargeOfCareQuestions.put("epoIncludePhrase", "NO");
        dischargeOfCareQuestions.put("uploadOrderFile", "NO");
        dischargeOfCareQuestions.put("needSealing", "NO");
        dischargeOfCareQuestions.put("epoChildrenDescription", "NO");
        dischargeOfCareQuestions.put("epoExpiryDate", "NO");
        dischargeOfCareQuestions.put("epoTypeAndPreventRemoval", "NO");
        dischargeOfCareQuestions.put("orderDetails", "NO");
        dischargeOfCareQuestions.put("cafcassJurisdictions", "NO");
        dischargeOfCareQuestions.put("whichChildren", "YES");
        dischargeOfCareQuestions.put("selectSingleChild", "NO");
        dischargeOfCareQuestions.put("reasonForSecureAccommodation", "NO");
        dischargeOfCareQuestions.put("childLegalRepresentation", "NO");
        dischargeOfCareQuestions.put("orderJurisdiction", "NO");
        dischargeOfCareQuestions.put("dischargeOfCareDetails", "YES");
        dischargeOfCareQuestions.put("manageOrdersExpiryDateWithMonth", "NO");
        dischargeOfCareQuestions.put("manageOrdersExclusionRequirementDetails", "NO");
        dischargeOfCareQuestions.put("manageOrdersExpiryDateWithEndOfProceedings", "NO");
        dischargeOfCareQuestions.put("childArrangementSpecificIssueProhibitedSteps", "NO");
        dischargeOfCareQuestions.put("isFinalOrder", "YES");
        dischargeOfCareQuestions.put("whichOthers", "YES");
        dischargeOfCareQuestions.put("closeCase", "YES");
        dischargeOfCareQuestions.put("orderIsByConsent", "NO");
        dischargeOfCareQuestions.put("appointedGuardian", "NO");
        dischargeOfCareQuestions.put("parentResponsible", "NO");
        dischargeOfCareQuestions.put("childPlacementApplications", "NO");
        dischargeOfCareQuestions.put("childPlacementQuestions", "NO");

        Map<String, String> epoQuestions = new HashMap<>(commonQuestions);
        epoQuestions.put("orderTitle", "NO");
        epoQuestions.put("furtherDirections", "YES");
        epoQuestions.put("approvalDate", "NO");
        epoQuestions.put("approvalDateTime", "YES");
        epoQuestions.put("epoIncludePhrase", "YES");
        epoQuestions.put("uploadOrderFile", "NO");
        epoQuestions.put("needSealing", "NO");
        epoQuestions.put("epoChildrenDescription", "YES");
        epoQuestions.put("epoExpiryDate", "YES");
        epoQuestions.put("epoTypeAndPreventRemoval", "YES");
        epoQuestions.put("orderDetails", "NO");
        epoQuestions.put("cafcassJurisdictions", "NO");
        epoQuestions.put("whichChildren", "YES");
        epoQuestions.put("selectSingleChild", "NO");
        epoQuestions.put("reasonForSecureAccommodation", "NO");
        epoQuestions.put("childLegalRepresentation", "NO");
        epoQuestions.put("orderJurisdiction", "NO");
        epoQuestions.put("dischargeOfCareDetails", "NO");
        epoQuestions.put("manageOrdersExpiryDateWithMonth", "NO");
        epoQuestions.put("manageOrdersExclusionRequirementDetails", "NO");
        epoQuestions.put("manageOrdersExpiryDateWithEndOfProceedings", "NO");
        epoQuestions.put("childArrangementSpecificIssueProhibitedSteps", "NO");
        epoQuestions.put("closeCase", "NO");
        epoQuestions.put("whichOthers", "YES");
        epoQuestions.put("isFinalOrder", "NO");
        epoQuestions.put("orderIsByConsent", "NO");
        epoQuestions.put("appointedGuardian", "NO");
        epoQuestions.put("parentResponsible", "NO");
        epoQuestions.put("childPlacementApplications", "NO");
        epoQuestions.put("childPlacementQuestions", "NO");

        Map<String, String> blankOrderQuestions = new HashMap<>(commonQuestions);
        blankOrderQuestions.put("orderTitle", "YES");
        blankOrderQuestions.put("approvalDate", "YES");
        blankOrderQuestions.put("furtherDirections", "NO");
        blankOrderQuestions.put("orderDetails", "YES");
        blankOrderQuestions.put("approvalDateTime", "NO");
        blankOrderQuestions.put("epoIncludePhrase", "NO");
        blankOrderQuestions.put("uploadOrderFile", "NO");
        blankOrderQuestions.put("needSealing", "NO");
        blankOrderQuestions.put("epoChildrenDescription", "NO");
        blankOrderQuestions.put("epoExpiryDate", "NO");
        blankOrderQuestions.put("epoTypeAndPreventRemoval", "NO");
        blankOrderQuestions.put("whichChildren", "YES");
        blankOrderQuestions.put("selectSingleChild", "NO");
        blankOrderQuestions.put("reasonForSecureAccommodation", "NO");
        blankOrderQuestions.put("childLegalRepresentation", "NO");
        blankOrderQuestions.put("orderJurisdiction", "NO");
        blankOrderQuestions.put("dischargeOfCareDetails", "NO");
        blankOrderQuestions.put("cafcassJurisdictions", "NO");
        blankOrderQuestions.put("manageOrdersExpiryDateWithMonth", "NO");
        blankOrderQuestions.put("manageOrdersExclusionRequirementDetails", "NO");
        blankOrderQuestions.put("manageOrdersExpiryDateWithEndOfProceedings", "NO");
        blankOrderQuestions.put("childArrangementSpecificIssueProhibitedSteps", "NO");
        blankOrderQuestions.put("closeCase", "NO");
        blankOrderQuestions.put("whichOthers", "YES");
        blankOrderQuestions.put("isFinalOrder", "NO");
        blankOrderQuestions.put("orderIsByConsent", "NO");
        blankOrderQuestions.put("appointedGuardian", "NO");
        blankOrderQuestions.put("parentResponsible", "NO");
        blankOrderQuestions.put("childPlacementApplications", "NO");
        blankOrderQuestions.put("childPlacementQuestions", "NO");

        Map<String, String> supervisionOrderQuestions = new HashMap<>(commonQuestions);
        supervisionOrderQuestions.put("orderTitle", "NO");
        supervisionOrderQuestions.put("approvalDate", "YES");
        supervisionOrderQuestions.put("furtherDirections", "YES");
        supervisionOrderQuestions.put("orderDetails", "NO");
        supervisionOrderQuestions.put("approvalDateTime", "NO");
        supervisionOrderQuestions.put("epoIncludePhrase", "NO");
        supervisionOrderQuestions.put("uploadOrderFile", "NO");
        supervisionOrderQuestions.put("needSealing", "NO");
        supervisionOrderQuestions.put("epoChildrenDescription", "NO");
        supervisionOrderQuestions.put("epoExpiryDate", "NO");
        supervisionOrderQuestions.put("epoTypeAndPreventRemoval", "NO");
        supervisionOrderQuestions.put("cafcassJurisdictions", "NO");
        supervisionOrderQuestions.put("whichChildren", "YES");
        supervisionOrderQuestions.put("selectSingleChild", "NO");
        supervisionOrderQuestions.put("reasonForSecureAccommodation", "NO");
        supervisionOrderQuestions.put("childLegalRepresentation", "NO");
        supervisionOrderQuestions.put("orderJurisdiction", "NO");
        supervisionOrderQuestions.put("dischargeOfCareDetails", "NO");
        supervisionOrderQuestions.put("manageOrdersExpiryDateWithMonth", "YES");
        supervisionOrderQuestions.put("manageOrdersExclusionRequirementDetails", "NO");
        supervisionOrderQuestions.put("manageOrdersExpiryDateWithEndOfProceedings", "NO");
        supervisionOrderQuestions.put("childArrangementSpecificIssueProhibitedSteps", "NO");
        supervisionOrderQuestions.put("closeCase", "YES");
        supervisionOrderQuestions.put("whichOthers", "YES");
        supervisionOrderQuestions.put("isFinalOrder", "NO");
        supervisionOrderQuestions.put("orderIsByConsent", "NO");
        supervisionOrderQuestions.put("appointedGuardian", "NO");
        supervisionOrderQuestions.put("parentResponsible", "NO");
        supervisionOrderQuestions.put("childPlacementApplications", "NO");
        supervisionOrderQuestions.put("childPlacementQuestions", "NO");

        Map<String, String> specialGuardianshipOrderQuestions = new HashMap<>(commonQuestions);
        specialGuardianshipOrderQuestions.put("orderTitle", "NO");
        specialGuardianshipOrderQuestions.put("approvalDate", "NO");
        specialGuardianshipOrderQuestions.put("furtherDirections", "YES");
        specialGuardianshipOrderQuestions.put("orderDetails", "NO");
        specialGuardianshipOrderQuestions.put("approvalDateTime", "YES");
        specialGuardianshipOrderQuestions.put("epoIncludePhrase", "NO");
        specialGuardianshipOrderQuestions.put("epoChildrenDescription", "NO");
        specialGuardianshipOrderQuestions.put("uploadOrderFile", "NO");
        specialGuardianshipOrderQuestions.put("needSealing", "NO");
        specialGuardianshipOrderQuestions.put("epoExpiryDate", "NO");
        specialGuardianshipOrderQuestions.put("epoTypeAndPreventRemoval", "NO");
        specialGuardianshipOrderQuestions.put("cafcassJurisdictions", "NO");
        specialGuardianshipOrderQuestions.put("whichChildren", "YES");
        specialGuardianshipOrderQuestions.put("selectSingleChild", "NO");
        specialGuardianshipOrderQuestions.put("reasonForSecureAccommodation", "NO");
        specialGuardianshipOrderQuestions.put("childLegalRepresentation", "NO");
        specialGuardianshipOrderQuestions.put("orderJurisdiction", "NO");
        specialGuardianshipOrderQuestions.put("dischargeOfCareDetails", "NO");
        specialGuardianshipOrderQuestions.put("manageOrdersExpiryDateWithMonth", "NO");
        specialGuardianshipOrderQuestions.put("manageOrdersExclusionRequirementDetails", "NO");
        specialGuardianshipOrderQuestions.put("manageOrdersExpiryDateWithEndOfProceedings", "NO");
        specialGuardianshipOrderQuestions.put("childArrangementSpecificIssueProhibitedSteps", "NO");
        specialGuardianshipOrderQuestions.put("closeCase", "NO");
        specialGuardianshipOrderQuestions.put("isFinalOrder", "YES");
        specialGuardianshipOrderQuestions.put("orderIsByConsent", "YES");
        specialGuardianshipOrderQuestions.put("whichOthers", "YES");
        specialGuardianshipOrderQuestions.put("appointedGuardian", "YES");
        specialGuardianshipOrderQuestions.put("parentResponsible", "NO");
        specialGuardianshipOrderQuestions.put("childPlacementApplications", "NO");
        specialGuardianshipOrderQuestions.put("childPlacementQuestions", "NO");

        Map<String, String> appointmentOfChildrensGuardianQuestions = new HashMap<>(commonQuestions);
        appointmentOfChildrensGuardianQuestions.put("orderTitle", "NO");
        appointmentOfChildrensGuardianQuestions.put("approvalDate", "YES");
        appointmentOfChildrensGuardianQuestions.put("furtherDirections", "YES");
        appointmentOfChildrensGuardianQuestions.put("orderDetails", "NO");
        appointmentOfChildrensGuardianQuestions.put("approvalDateTime", "NO");
        appointmentOfChildrensGuardianQuestions.put("epoIncludePhrase", "NO");
        appointmentOfChildrensGuardianQuestions.put("uploadOrderFile", "NO");
        appointmentOfChildrensGuardianQuestions.put("needSealing", "NO");
        appointmentOfChildrensGuardianQuestions.put("epoChildrenDescription", "NO");
        appointmentOfChildrensGuardianQuestions.put("epoExpiryDate", "NO");
        appointmentOfChildrensGuardianQuestions.put("epoTypeAndPreventRemoval", "NO");
        appointmentOfChildrensGuardianQuestions.put("cafcassJurisdictions", "YES");
        appointmentOfChildrensGuardianQuestions.put("whichChildren", "NO");
        appointmentOfChildrensGuardianQuestions.put("selectSingleChild", "NO");
        appointmentOfChildrensGuardianQuestions.put("reasonForSecureAccommodation", "NO");
        appointmentOfChildrensGuardianQuestions.put("childLegalRepresentation", "NO");
        appointmentOfChildrensGuardianQuestions.put("orderJurisdiction", "NO");
        appointmentOfChildrensGuardianQuestions.put("dischargeOfCareDetails", "NO");
        appointmentOfChildrensGuardianQuestions.put("manageOrdersExpiryDateWithMonth", "NO");
        appointmentOfChildrensGuardianQuestions.put("manageOrdersExclusionRequirementDetails", "NO");
        appointmentOfChildrensGuardianQuestions.put("manageOrdersExpiryDateWithEndOfProceedings", "NO");
        appointmentOfChildrensGuardianQuestions.put("childArrangementSpecificIssueProhibitedSteps", "NO");
        appointmentOfChildrensGuardianQuestions.put("closeCase", "NO");
        appointmentOfChildrensGuardianQuestions.put("isFinalOrder", "NO");
        appointmentOfChildrensGuardianQuestions.put("orderIsByConsent", "NO");
        appointmentOfChildrensGuardianQuestions.put("appointedGuardian", "NO");
        appointmentOfChildrensGuardianQuestions.put("whichOthers", "YES");
        appointmentOfChildrensGuardianQuestions.put("parentResponsible", "NO");
        appointmentOfChildrensGuardianQuestions.put("childPlacementApplications", "NO");
        appointmentOfChildrensGuardianQuestions.put("childPlacementQuestions", "NO");

        Map<String, String> childArrangementSpecificOrder = new HashMap<>(commonQuestions);
        childArrangementSpecificOrder.put("orderTitle", "NO");
        childArrangementSpecificOrder.put("approvalDate", "YES");
        childArrangementSpecificOrder.put("furtherDirections", "YES");
        childArrangementSpecificOrder.put("orderDetails", "YES");
        childArrangementSpecificOrder.put("approvalDateTime", "NO");
        childArrangementSpecificOrder.put("epoIncludePhrase", "NO");
        childArrangementSpecificOrder.put("uploadOrderFile", "NO");
        childArrangementSpecificOrder.put("needSealing", "NO");
        childArrangementSpecificOrder.put("epoChildrenDescription", "NO");
        childArrangementSpecificOrder.put("epoExpiryDate", "NO");
        childArrangementSpecificOrder.put("epoTypeAndPreventRemoval", "NO");
        childArrangementSpecificOrder.put("cafcassJurisdictions", "NO");
        childArrangementSpecificOrder.put("whichChildren", "YES");
        childArrangementSpecificOrder.put("selectSingleChild", "NO");
        childArrangementSpecificOrder.put("reasonForSecureAccommodation", "NO");
        childArrangementSpecificOrder.put("childLegalRepresentation", "NO");
        childArrangementSpecificOrder.put("orderJurisdiction", "NO");
        childArrangementSpecificOrder.put("dischargeOfCareDetails", "NO");
        childArrangementSpecificOrder.put("manageOrdersExpiryDateWithMonth", "NO");
        childArrangementSpecificOrder.put("manageOrdersExclusionRequirementDetails", "NO");
        childArrangementSpecificOrder.put("manageOrdersExpiryDateWithEndOfProceedings", "NO");
        childArrangementSpecificOrder.put("childArrangementSpecificIssueProhibitedSteps", "YES");
        childArrangementSpecificOrder.put("closeCase", "YES");
        childArrangementSpecificOrder.put("isFinalOrder", "YES");
        childArrangementSpecificOrder.put("orderIsByConsent", "YES");
        childArrangementSpecificOrder.put("appointedGuardian", "NO");
        childArrangementSpecificOrder.put("whichOthers", "YES");
        childArrangementSpecificOrder.put("parentResponsible", "NO");
        childArrangementSpecificOrder.put("childPlacementApplications", "NO");
        childArrangementSpecificOrder.put("childPlacementQuestions", "NO");

        Map<String, String> secureAccommodationOrderQuestions = new HashMap<>(commonQuestions);
        secureAccommodationOrderQuestions.put("orderTitle", "NO");
        secureAccommodationOrderQuestions.put("approvalDate", "NO");
        secureAccommodationOrderQuestions.put("furtherDirections", "YES");
        secureAccommodationOrderQuestions.put("orderDetails", "NO");
        secureAccommodationOrderQuestions.put("approvalDateTime", "YES");
        secureAccommodationOrderQuestions.put("epoIncludePhrase", "NO");
        secureAccommodationOrderQuestions.put("uploadOrderFile", "NO");
        secureAccommodationOrderQuestions.put("needSealing", "NO");
        secureAccommodationOrderQuestions.put("epoChildrenDescription", "NO");
        secureAccommodationOrderQuestions.put("epoExpiryDate", "NO");
        secureAccommodationOrderQuestions.put("epoTypeAndPreventRemoval", "NO");
        secureAccommodationOrderQuestions.put("cafcassJurisdictions", "NO");
        secureAccommodationOrderQuestions.put("whichChildren", "NO");
        secureAccommodationOrderQuestions.put("selectSingleChild", "YES");
        secureAccommodationOrderQuestions.put("reasonForSecureAccommodation", "YES");
        secureAccommodationOrderQuestions.put("childLegalRepresentation", "YES");
        secureAccommodationOrderQuestions.put("orderIsByConsent", "YES");
        secureAccommodationOrderQuestions.put("orderJurisdiction", "YES");
        secureAccommodationOrderQuestions.put("manageOrdersExpiryDateWithMonth", "YES");
        secureAccommodationOrderQuestions.put("manageOrdersExclusionRequirementDetails", "NO");
        secureAccommodationOrderQuestions.put("manageOrdersExpiryDateWithEndOfProceedings", "NO");
        secureAccommodationOrderQuestions.put("childArrangementSpecificIssueProhibitedSteps", "NO");
        secureAccommodationOrderQuestions.put("dischargeOfCareDetails", "NO");
        secureAccommodationOrderQuestions.put("closeCase", "YES");
        secureAccommodationOrderQuestions.put("isFinalOrder", "YES");
        secureAccommodationOrderQuestions.put("appointedGuardian", "NO");
        secureAccommodationOrderQuestions.put("whichOthers", "YES");
        secureAccommodationOrderQuestions.put("parentResponsible", "NO");
        secureAccommodationOrderQuestions.put("childPlacementApplications", "NO");
        secureAccommodationOrderQuestions.put("childPlacementQuestions", "NO");

        Map<String, String> parentalResponsibilityOrder = new HashMap<>(commonQuestions);
        parentalResponsibilityOrder.put("orderTitle", "NO");
        parentalResponsibilityOrder.put("approvalDate", "NO");
        parentalResponsibilityOrder.put("orderDetails", "NO");
        parentalResponsibilityOrder.put("approvalDateTime", "YES");
        parentalResponsibilityOrder.put("epoIncludePhrase", "NO");
        parentalResponsibilityOrder.put("uploadOrderFile", "NO");
        parentalResponsibilityOrder.put("needSealing", "NO");
        parentalResponsibilityOrder.put("epoChildrenDescription", "NO");
        parentalResponsibilityOrder.put("epoExpiryDate", "NO");
        parentalResponsibilityOrder.put("epoTypeAndPreventRemoval", "NO");
        parentalResponsibilityOrder.put("cafcassJurisdictions", "NO");
        parentalResponsibilityOrder.put("selectSingleChild", "NO");
        parentalResponsibilityOrder.put("reasonForSecureAccommodation", "NO");
        parentalResponsibilityOrder.put("childLegalRepresentation", "NO");
        parentalResponsibilityOrder.put("orderJurisdiction", "NO");
        parentalResponsibilityOrder.put("manageOrdersExpiryDateWithMonth", "NO");
        parentalResponsibilityOrder.put("manageOrdersExclusionRequirementDetails", "NO");
        parentalResponsibilityOrder.put("manageOrdersExpiryDateWithEndOfProceedings", "NO");
        parentalResponsibilityOrder.put("childArrangementSpecificIssueProhibitedSteps", "NO");
        parentalResponsibilityOrder.put("whichOthers", "YES");
        parentalResponsibilityOrder.put("dischargeOfCareDetails", "NO");
        parentalResponsibilityOrder.put("closeCase", "YES");
        parentalResponsibilityOrder.put("whichChildren", "YES");
        parentalResponsibilityOrder.put("orderIsByConsent", "YES");
        parentalResponsibilityOrder.put("furtherDirections", "YES");
        parentalResponsibilityOrder.put("isFinalOrder", "YES");
        parentalResponsibilityOrder.put("appointedGuardian", "NO");
        parentalResponsibilityOrder.put("parentResponsible", "YES");
        parentalResponsibilityOrder.put("childPlacementApplications", "NO");
        parentalResponsibilityOrder.put("childPlacementQuestions", "NO");

        Map<String, String> placementOrder = new HashMap<>(commonQuestions);
        placementOrder.put("orderTitle", "NO");
        placementOrder.put("hearingDetails", "NO");
        placementOrder.put("linkApplication", "NO");
        placementOrder.put("approvalDate", "YES");
        placementOrder.put("orderDetails", "NO");
        placementOrder.put("approvalDateTime", "NO");
        placementOrder.put("epoIncludePhrase", "NO");
        placementOrder.put("uploadOrderFile", "NO");
        placementOrder.put("needSealing", "NO");
        placementOrder.put("epoChildrenDescription", "NO");
        placementOrder.put("epoExpiryDate", "NO");
        placementOrder.put("epoTypeAndPreventRemoval", "NO");
        placementOrder.put("cafcassJurisdictions", "NO");
        placementOrder.put("selectSingleChild", "NO");
        placementOrder.put("reasonForSecureAccommodation", "NO");
        placementOrder.put("childLegalRepresentation", "NO");
        placementOrder.put("orderJurisdiction", "NO");
        placementOrder.put("manageOrdersExpiryDateWithMonth", "NO");
        placementOrder.put("manageOrdersExclusionRequirementDetails", "NO");
        placementOrder.put("manageOrdersExpiryDateWithEndOfProceedings", "NO");
        placementOrder.put("childArrangementSpecificIssueProhibitedSteps", "NO");
        placementOrder.put("whichOthers", "NO");
        placementOrder.put("dischargeOfCareDetails", "NO");
        placementOrder.put("closeCase", "NO");
        placementOrder.put("whichChildren", "NO");
        placementOrder.put("orderIsByConsent", "NO");
        placementOrder.put("furtherDirections", "NO");
        placementOrder.put("isFinalOrder", "YES");
        placementOrder.put("appointedGuardian", "NO");
        placementOrder.put("parentResponsible", "NO");
        placementOrder.put("childPlacementApplications", "YES");
        placementOrder.put("childPlacementQuestions", "YES");

        return Stream.of(
            Arguments.of(C21_BLANK_ORDER, blankOrderQuestions),
            Arguments.of(C23_EMERGENCY_PROTECTION_ORDER, epoQuestions),
            Arguments.of(C26_SECURE_ACCOMMODATION_ORDER, secureAccommodationOrderQuestions),
            Arguments.of(C32A_CARE_ORDER, careOrderQuestions),
            Arguments.of(C32B_DISCHARGE_OF_CARE_ORDER, dischargeOfCareQuestions),
            Arguments.of(C35A_SUPERVISION_ORDER, supervisionOrderQuestions),
            Arguments.of(C43A_SPECIAL_GUARDIANSHIP_ORDER, specialGuardianshipOrderQuestions),
            Arguments.of(C43_CHILD_ARRANGEMENTS_SPECIFIC_ISSUE_PROHIBITED_STEPS_ORDER, childArrangementSpecificOrder),
            Arguments.of(C45A_PARENTAL_RESPONSIBILITY_ORDER, parentalResponsibilityOrder),
            Arguments.of(C47A_APPOINTMENT_OF_A_CHILDRENS_GUARDIAN, appointmentOfChildrensGuardianQuestions),
            Arguments.of(A70_PLACEMENT_ORDER, placementOrder)
        );
    }

    private static Stream<Arguments> finalManualUploadOrders() {
        return MANUAL_ORDERS_WITH_IS_FINAL_ORDER_QUESTION.stream()
            .map(order -> Arguments.of(order, Map.ofEntries(
                Map.entry("approver", "NO"),
                Map.entry("previewOrder", "YES"),
                Map.entry("orderTitle", "NO"),
                Map.entry("furtherDirections", "NO"),
                Map.entry("orderDetails", "NO"),
                Map.entry("whichChildren", "YES"),
                Map.entry("hearingDetails", "NO"),
                Map.entry("linkApplication", "NO"),
                Map.entry("approvalDate", "YES"),
                Map.entry("approvalDateTime", "NO"),
                Map.entry("dischargeOfCareDetails", "NO"),
                Map.entry("childArrangementSpecificIssueProhibitedSteps", "NO"),
                Map.entry("epoIncludePhrase", "NO"),
                Map.entry("epoExpiryDate", "NO"),
                Map.entry("isFinalOrder", "YES"),
                Map.entry("epoTypeAndPreventRemoval", "NO"),
                Map.entry("epoChildrenDescription", "NO"),
                Map.entry("manageOrdersExclusionRequirementDetails", "NO"),
                Map.entry("manageOrdersExpiryDateWithEndOfProceedings", "NO"),
                Map.entry("manageOrdersExpiryDateWithMonth", "NO"),
                Map.entry("cafcassJurisdictions", "NO"),
                Map.entry("needSealing", "YES"),
                Map.entry("uploadOrderFile", "YES"),
                Map.entry("translationRequirements", "YES"),
                Map.entry("closeCase", "YES"),
                Map.entry("orderIsByConsent", "NO"),
                Map.entry("appointedGuardian", "NO"),
                Map.entry("whichOthers", "YES"),
                Map.entry("parentResponsible", "NO"),
                Map.entry("selectSingleChild", "NO"),
                Map.entry("reasonForSecureAccommodation", "NO"),
                Map.entry("childLegalRepresentation", "NO"),
                Map.entry("orderJurisdiction", "NO"),
                Map.entry("orderToAmend", "NO"),
                Map.entry("uploadAmendedOrder", "NO"),
                Map.entry("childPlacementApplications", "NO"),
                Map.entry("childPlacementQuestions", "NO")
            )));
    }

    private static Stream<Arguments> nonFinalManualUploadOrders() {
        return Arrays.stream(Order.values())
            .filter(order -> OrderSourceType.MANUAL_UPLOAD == order.getSourceType())
            .filter(order -> !MANUAL_ORDERS_WITH_IS_FINAL_ORDER_QUESTION.contains(order))
            .map(order -> Arguments.of(order, Map.ofEntries(
                Map.entry("approver", "NO"),
                Map.entry("previewOrder", "YES"),
                Map.entry("orderTitle", "NO"),
                Map.entry("furtherDirections", "NO"),
                Map.entry("orderDetails", "NO"),
                Map.entry("whichChildren", "YES"),
                Map.entry("hearingDetails", "NO"),
                Map.entry("linkApplication", "NO"),
                Map.entry("reasonForSecureAccommodation", "NO"),
                Map.entry("childLegalRepresentation", "NO"),
                Map.entry("selectSingleChild", "NO"),
                Map.entry("orderJurisdiction", "NO"),
                Map.entry("approvalDate", "YES"),
                Map.entry("approvalDateTime", "NO"),
                Map.entry("dischargeOfCareDetails", "NO"),
                Map.entry("childArrangementSpecificIssueProhibitedSteps", "NO"),
                Map.entry("epoIncludePhrase", "NO"),
                Map.entry("epoExpiryDate", "NO"),
                Map.entry("isFinalOrder", "NO"),
                Map.entry("epoTypeAndPreventRemoval", "NO"),
                Map.entry("translationRequirements", "YES"),
                Map.entry("epoChildrenDescription", "NO"),
                Map.entry("manageOrdersExclusionRequirementDetails", "NO"),
                Map.entry("manageOrdersExpiryDateWithEndOfProceedings", "NO"),
                    Map.entry("manageOrdersExpiryDateWithMonth", "NO"),
                    Map.entry("cafcassJurisdictions", "NO"),
                    Map.entry("needSealing", "YES"),
                    Map.entry("uploadOrderFile", "YES"),
                    Map.entry("closeCase", "YES"),
                    Map.entry("orderIsByConsent", "NO"),
                    Map.entry("appointedGuardian", "NO"),
                    Map.entry("whichOthers", "YES"),
                    Map.entry("orderToAmend", "NO"),
                    Map.entry("uploadAmendedOrder", "NO"),
                    Map.entry("parentResponsible", "NO"),
                    Map.entry("childPlacementApplications", "NO"),
                    Map.entry("childPlacementQuestions", "NO")
                )
            ));
    }
}
