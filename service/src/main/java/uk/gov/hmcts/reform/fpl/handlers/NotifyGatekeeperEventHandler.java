package uk.gov.hmcts.reform.fpl.handlers;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.fpl.events.NotifyGatekeepersEvent;
import uk.gov.hmcts.reform.fpl.model.CaseData;
import uk.gov.hmcts.reform.fpl.model.notify.sendtogatekeeper.NotifyGatekeeperTemplate;
import uk.gov.hmcts.reform.fpl.service.FeatureToggleService;
import uk.gov.hmcts.reform.fpl.service.email.NotificationService;
import uk.gov.hmcts.reform.fpl.service.email.content.GatekeeperEmailContentProvider;

import java.util.List;

import static uk.gov.hmcts.reform.fpl.NotifyTemplates.GATEKEEPER_SUBMISSION_TEMPLATE;
import static uk.gov.hmcts.reform.fpl.NotifyTemplates.GATEKEEPER_SUBMISSION_TEMPLATE_CHILD_NAME;
import static uk.gov.hmcts.reform.fpl.utils.EmailNotificationHelper.getDistinctGatekeeperEmails;

@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class NotifyGatekeeperEventHandler {
    private final NotificationService notificationService;
    private final GatekeeperEmailContentProvider contentProvider;
    private final FeatureToggleService toggleService;

    @EventListener
    public void notifyGatekeeper(NotifyGatekeepersEvent event) {
        CaseData caseData = event.getCaseData();

        NotifyGatekeeperTemplate parameters = contentProvider.buildGatekeeperNotification(caseData);

        List<String> emailList = getDistinctGatekeeperEmails(caseData.getGatekeeperEmails());

        String template = toggleService.isEldestChildLastNameEnabled() ? GATEKEEPER_SUBMISSION_TEMPLATE_CHILD_NAME
                                                                       : GATEKEEPER_SUBMISSION_TEMPLATE;

        emailList.forEach(recipientEmail -> {
            NotifyGatekeeperTemplate notifyData = parameters.duplicate();
            notificationService.sendEmail(template, recipientEmail, notifyData, caseData.getId());
        });
    }
}
