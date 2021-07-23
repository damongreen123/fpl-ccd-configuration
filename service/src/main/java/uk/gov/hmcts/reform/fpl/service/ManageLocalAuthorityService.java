package uk.gov.hmcts.reform.fpl.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.model.ChangeOrganisationRequest;
import uk.gov.hmcts.reform.ccd.model.Organisation;
import uk.gov.hmcts.reform.ccd.model.OrganisationPolicy;
import uk.gov.hmcts.reform.fpl.config.LocalAuthorityCodeLookupConfiguration;
import uk.gov.hmcts.reform.fpl.config.LocalAuthorityEmailLookupConfiguration;
import uk.gov.hmcts.reform.fpl.config.LocalAuthorityIdLookupConfiguration;
import uk.gov.hmcts.reform.fpl.config.LocalAuthorityNameLookupConfiguration;
import uk.gov.hmcts.reform.fpl.enums.LocalAuthorityAction;
import uk.gov.hmcts.reform.fpl.events.SecondaryLocalAuthorityAdded;
import uk.gov.hmcts.reform.fpl.events.SecondaryLocalAuthorityRemoved;
import uk.gov.hmcts.reform.fpl.model.CaseData;
import uk.gov.hmcts.reform.fpl.model.LocalAuthority;
import uk.gov.hmcts.reform.fpl.model.common.Element;
import uk.gov.hmcts.reform.fpl.model.common.dynamic.DynamicList;
import uk.gov.hmcts.reform.fpl.model.common.dynamic.DynamicListElement;
import uk.gov.hmcts.reform.fpl.model.event.LocalAuthoritiesEventData;
import uk.gov.hmcts.reform.fpl.service.time.Time;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static uk.gov.hmcts.reform.ccd.model.ChangeOrganisationApprovalStatus.APPROVED;
import static uk.gov.hmcts.reform.fpl.enums.CaseRole.LASHARED;
import static uk.gov.hmcts.reform.fpl.enums.LocalAuthorityAction.ADD;
import static uk.gov.hmcts.reform.fpl.enums.LocalAuthorityAction.REMOVE;
import static uk.gov.hmcts.reform.fpl.utils.ElementUtils.element;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ManageLocalAuthorityService {

    private final Time time;
    private final DynamicListService dynamicListService;
    private final LocalAuthorityNameLookupConfiguration localAuthorities;
    private final LocalAuthorityEmailLookupConfiguration localAuthorityEmails;
    private final LocalAuthorityIdLookupConfiguration localAuthorityId;
    private final LocalAuthorityCodeLookupConfiguration localAuthCodes;
    private final ValidateEmailService emailService;
    private final ApplicantLocalAuthorityService applicantLocalAuthorityService;
    private final OrganisationService organisationService;

    public List<String> validateAction(CaseData caseData) {

        boolean isCaseShared = ofNullable(caseData.getSharedLocalAuthorityPolicy())
            .map(OrganisationPolicy::getOrganisation)
            .map(Organisation::getOrganisationID)
            .isPresent();

        final LocalAuthorityAction action = caseData.getLocalAuthoritiesEventData().getLocalAuthorityAction();

        if (ADD.equals(action) && isCaseShared) {
            return List.of("Case access has already been given to local authority. Remove their access to continue");
        }

        if (REMOVE.equals(action) && !isCaseShared) {
            return List.of("There are no other local authorities to remove from this case");
        }

        return emptyList();
    }

    public DynamicList getLocalAuthoritiesToShare(CaseData caseData) {
        final List<Map.Entry<String, String>> entries = this.localAuthorities.getLocalAuthoritiesNames().entrySet()
            .stream()
            .filter(e -> !e.getKey().equals(caseData.getCaseLocalAuthority()))
            .sorted(Comparator.comparing(Map.Entry::getValue))
            .collect(Collectors.toList());

        return dynamicListService.asDynamicList(entries, Map.Entry::getKey, Map.Entry::getValue);
    }

    public String getEmail(LocalAuthoritiesEventData eventData) {
        final DynamicList dynamicList = eventData.getLocalAuthoritiesToShare();
        final String localAuthorityCode = dynamicList.getValueCode();

        return localAuthorityEmails.getLocalAuthority(localAuthorityCode)
            .map(LocalAuthorityEmailLookupConfiguration.LocalAuthority::getEmail)
            .orElseGet(() -> localAuthCodes.getLocalAuthorityDomain(localAuthorityCode)
                .map("@"::concat)
                .orElse(null));
    }

    public List<Element<LocalAuthority>> removeSharedLocalAuthority(CaseData caseData) {
        final OrganisationPolicy oldOrgPolicy = caseData.getSharedLocalAuthorityPolicy();

        caseData.getLocalAuthorities()
            .removeIf(la -> la.getValue().getId().equals(oldOrgPolicy.getOrganisation().getOrganisationID()));

        return applicantLocalAuthorityService.updateDesignatedLocalAuthority(caseData);
    }

    public List<Element<LocalAuthority>> addSharedLocalAuthority(CaseData caseData) {

        final LocalAuthority localAuthorityToAdd = ofNullable(caseData.getLocalAuthoritiesEventData())
            .map(LocalAuthoritiesEventData::getLocalAuthoritiesToShare)
            .map(DynamicList::getValueCode)
            .map(localAuthorityId::getLocalAuthorityId)
            .map(organisationService::getOrganisation)
            .map(applicantLocalAuthorityService::getLocalAuthority)
            .orElseThrow();

        localAuthorityToAdd.setEmail(caseData.getLocalAuthoritiesEventData().getLocalAuthorityEmail());

        caseData.getLocalAuthorities().add(element(localAuthorityToAdd));

        return applicantLocalAuthorityService.updateDesignatedLocalAuthority(caseData);
    }


    public List<String> validateEmail(LocalAuthoritiesEventData eventData) {
        final List<String> errors = new ArrayList<>();
        emailService.validate(eventData.getLocalAuthorityEmail()).ifPresent(errors::add);
        return errors;
    }


    public ChangeOrganisationRequest getOrgRemovalRequest(CaseData caseData) {
        final OrganisationPolicy organisationPolicy = caseData.getSharedLocalAuthorityPolicy();

        final DynamicListElement roleItem = DynamicListElement.builder()
            .code(LASHARED.formattedName())
            .label(LASHARED.formattedName())
            .build();

        return ChangeOrganisationRequest.builder()
            .approvalStatus(APPROVED)
            .requestTimestamp(time.now())
            .caseRoleId(DynamicList.builder()
                .value(roleItem)
                .listItems(List.of(roleItem))
                .build())
            .organisationToRemove(organisationPolicy.getOrganisation())
            .build();
    }

    public OrganisationPolicy buildSharedLocalAuthorityPolicy(CaseData caseData) {

        final DynamicListElement selectedLocalAuthority = ofNullable(caseData.getLocalAuthoritiesEventData())
            .map(LocalAuthoritiesEventData::getLocalAuthoritiesToShare)
            .map(DynamicList::getValue)
            .orElseThrow();

        final String selectedLocalAuthorityId = localAuthorityId.getLocalAuthorityId(selectedLocalAuthority.getCode());

        return OrganisationPolicy.builder()
            .organisation(Organisation.builder()
                .organisationID(selectedLocalAuthorityId)
                .organisationName(selectedLocalAuthority.getLabel())
                .build())
            .orgPolicyCaseAssignedRole(LASHARED.formattedName())
            .build();
    }

    public Optional<Object> getChangeEvent(CaseData caseData, CaseData caseDataBefore) {
        if (getShareOrganisationId(caseData).isPresent() && getShareOrganisationId(caseDataBefore).isEmpty()) {
            return Optional.of(new SecondaryLocalAuthorityAdded(caseData));
        }

        if (getShareOrganisationId(caseData).isEmpty() && getShareOrganisationId(caseDataBefore).isPresent()) {
            return Optional.of(new SecondaryLocalAuthorityRemoved(caseData, caseDataBefore));
        }

        return Optional.empty();
    }

    private Optional<Organisation> getShareOrganisationId(CaseData caseData) {
        return Optional.ofNullable(caseData.getSharedLocalAuthorityPolicy())
            .map(OrganisationPolicy::getOrganisation)
            .filter(org -> nonNull(org.getOrganisationID()));
    }
}
