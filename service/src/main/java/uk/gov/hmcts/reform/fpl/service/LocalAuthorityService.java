package uk.gov.hmcts.reform.fpl.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.fpl.config.EpsLookupConfiguration;
import uk.gov.hmcts.reform.fpl.config.LocalAuthorityCodeLookupConfiguration;
import uk.gov.hmcts.reform.fpl.config.LocalAuthorityIdLookupConfiguration;
import uk.gov.hmcts.reform.fpl.config.LocalAuthorityNameLookupConfiguration;
import uk.gov.hmcts.reform.fpl.config.MlaLookupConfiguration;
import uk.gov.hmcts.reform.fpl.enums.OutsourcingType;
import uk.gov.hmcts.reform.fpl.model.LocalAuthorityName;
import uk.gov.hmcts.reform.fpl.request.RequestData;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.rd.model.Organisation;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class LocalAuthorityService {

    private final IdamClient idam;
    private final RequestData requestData;
    private final EpsLookupConfiguration epsConfig;
    private final MlaLookupConfiguration mlaConfig;
    private final LocalAuthorityIdLookupConfiguration idsConfig;
    private final LocalAuthorityCodeLookupConfiguration codesConfig;
    private final LocalAuthorityNameLookupConfiguration namesConfig;
    private final OrganisationService organisationService;

    public Optional<String> getLocalAuthorityCode() {
        UserInfo userInfo = idam.getUserInfo(requestData.authorisation());
        String email = userInfo.getSub();
        String domain = extractEmailDomain(email);

        return codesConfig.getLocalAuthorityCode(domain).or(this::getLocalAuthorityFromOrganisation);
    }

    public String getLocalAuthorityName(String localAuthorityCode) {
        return namesConfig.getLocalAuthorityName(localAuthorityCode);
    }

    public Optional<LocalAuthorityName> getUserLocalAuthority() {
        return getLocalAuthorityCode()
            .map(code -> LocalAuthorityName.builder().code(code).name(getLocalAuthorityName(code)).build());
    }

    public String getLocalAuthorityId(String localAuthorityCode) {
        return idsConfig.getLocalAuthorityId(localAuthorityCode);
    }

    public List<LocalAuthorityName> getOutsourcingLocalAuthorities(String organisationId, OutsourcingType type) {
        return getOutsourcingLocalAuthoritiesCodes(organisationId, type).stream()
            .map(localAuthorityCode -> LocalAuthorityName.builder()
                .code(localAuthorityCode)
                .name(getLocalAuthorityName(localAuthorityCode))
                .build())
            .collect(Collectors.toList());
    }

    private Optional<String> getLocalAuthorityFromOrganisation() {
        return organisationService.findOrganisation()
            .map(Organisation::getOrganisationIdentifier)
            .flatMap(idsConfig::getLocalAuthorityCode);
    }

    private List<String> getOutsourcingLocalAuthoritiesCodes(String organisationId, OutsourcingType type) {
        switch (type) {
            case EPS:
                return epsConfig.getLocalAuthorities(organisationId);
            case MLA:
                return mlaConfig.getLocalAuthorities(organisationId);
            default:
                throw new UnsupportedOperationException(String.format("Outsourcing type %s not supported", type));
        }
    }

    private String extractEmailDomain(String email) {
        int start = email.indexOf('@');

        return email.toLowerCase().substring(start + 1);
    }
}
