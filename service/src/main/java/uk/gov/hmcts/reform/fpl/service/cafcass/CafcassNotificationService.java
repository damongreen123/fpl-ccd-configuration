package uk.gov.hmcts.reform.fpl.service.cafcass;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.fpl.config.cafcass.CafcassEmailConfiguration;
import uk.gov.hmcts.reform.fpl.model.CaseData;
import uk.gov.hmcts.reform.fpl.model.common.DocumentReference;
import uk.gov.hmcts.reform.fpl.model.email.EmailAttachment;
import uk.gov.hmcts.reform.fpl.model.email.EmailData;
import uk.gov.hmcts.reform.fpl.service.DocumentDownloadService;
import uk.gov.hmcts.reform.fpl.service.email.EmailService;

import java.net.URLConnection;

import static java.util.Set.of;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static uk.gov.hmcts.reform.fpl.model.email.EmailAttachment.document;

@Service
@Slf4j
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class CafcassNotificationService {
    private static final String SUBJECT = "Court Ref. %s.- %s";
    private final EmailService emailService;
    private final DocumentDownloadService documentDownloadService;
    private final CafcassEmailConfiguration configuration;

    public void sendRequest(CaseData caseData, DocumentReference documentReference,
                            CafcassRequestEmailContentProvider provider) {
        byte[] documentContent = documentDownloadService.downloadDocument(documentReference.getBinaryUrl());

        EmailAttachment emailAttachment = document(
            defaultIfNull(URLConnection.guessContentTypeFromName(documentReference.getFilename()),
                "application/octet-stream"),
            documentContent,
            documentReference.getFilename());

        String subject = String.format(SUBJECT, caseData.getFamilyManCaseNumber(), provider.getType());

        emailService.sendEmail(configuration.getSender(),
            EmailData.builder()
                .recipient(configuration.getRecipient())
                .subject(subject)
                .attachments(of(emailAttachment))
                .message(String.format(provider.getContent(),
                    documentReference.getFilename()))
                .build());

    }
}
