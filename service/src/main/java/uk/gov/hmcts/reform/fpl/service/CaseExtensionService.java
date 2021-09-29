package uk.gov.hmcts.reform.fpl.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.fpl.model.CaseData;
import uk.gov.hmcts.reform.fpl.model.Child;
import uk.gov.hmcts.reform.fpl.model.common.Element;
import uk.gov.hmcts.reform.fpl.model.order.selector.Selector;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static uk.gov.hmcts.reform.fpl.enums.CaseExtensionTime.EIGHT_WEEK_EXTENSION;
import static uk.gov.hmcts.reform.fpl.model.order.selector.Selector.newSelector;
import static uk.gov.hmcts.reform.fpl.utils.DateFormatterHelper.DATE;
import static uk.gov.hmcts.reform.fpl.utils.DateFormatterHelper.formatLocalDateToString;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class CaseExtensionService {


    public LocalDate getCaseCompletionDate(CaseData caseData) {
        if (caseData.getCaseExtensionTimeList().equals(EIGHT_WEEK_EXTENSION)) {
            if (caseData.getCaseExtensionTimeConfirmationList().equals(EIGHT_WEEK_EXTENSION)) {
                return getCaseCompletionDateFor8WeekExtension(caseData);
            }
            return caseData.getEightWeeksExtensionDateOther();
        }
        return caseData.getExtensionDateOther();
    }

    public LocalDate getCaseCompletionDateFor8WeekExtension(CaseData caseData) {
        return getCaseShouldBeCompletedByDate(caseData).plusWeeks(8);
    }

    private LocalDate getCaseShouldBeCompletedByDate(CaseData caseData) {
        return Optional.ofNullable(caseData.getCaseCompletionDate()).orElse(caseData.getDateSubmitted().plusWeeks(26));
    }

    public Map<String, Object> prePopulate(CaseData caseData) {
        return Map.of(
            "shouldBeCompletedByPerChild", getChildrenCaseCompletedDateLabel(caseData),
            "childTimelineSelector", getCaseTimelineSelector(caseData)
            );
    }

    private String getChildrenCaseCompletedDateLabel(CaseData caseData) {
        List<Element<Child>> children = caseData.getChildren1();

        if (isEmpty(children)) {
            return "No children in the case";
        }

        StringBuilder sb = new StringBuilder();

        for(int i = 0; i < children.size(); i++) {
            Child child = children.get(i).getValue();

            // Amend so that this checks the new date field
            LocalDate completedDate = getCaseShouldBeCompletedByDate(caseData);
            sb.append(String.format("Child %d: %s: %s <br/>", i + 1, child.asLabel(),
                formatLocalDateToString(completedDate, DATE)));
        }

        return sb.toString();
    }

    private Selector getCaseTimelineSelector(CaseData caseData) {
        return newSelector(caseData.getAllChildren().size());
    }

}
