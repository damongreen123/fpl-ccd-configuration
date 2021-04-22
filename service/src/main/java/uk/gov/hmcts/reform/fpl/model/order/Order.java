package uk.gov.hmcts.reform.fpl.model.order;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.reform.fpl.enums.docmosis.RenderFormat;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.fpl.model.order.OrderQuestionBlock.APPROVAL_DATE;
import static uk.gov.hmcts.reform.fpl.model.order.OrderQuestionBlock.APPROVAL_DATE_TIME;
import static uk.gov.hmcts.reform.fpl.model.order.OrderQuestionBlock.APPROVER;
import static uk.gov.hmcts.reform.fpl.model.order.OrderQuestionBlock.EPO_CHILDREN_DESCRIPTION;
import static uk.gov.hmcts.reform.fpl.model.order.OrderQuestionBlock.EPO_EXPIRY_DATE;
import static uk.gov.hmcts.reform.fpl.model.order.OrderQuestionBlock.EPO_INCLUDE_PHRASE;
import static uk.gov.hmcts.reform.fpl.model.order.OrderQuestionBlock.EPO_PREVENT_REMOVAL;
import static uk.gov.hmcts.reform.fpl.model.order.OrderQuestionBlock.EPO_TYPE;
import static uk.gov.hmcts.reform.fpl.model.order.OrderQuestionBlock.FURTHER_DIRECTIONS;
import static uk.gov.hmcts.reform.fpl.model.order.OrderQuestionBlock.REVIEW_DRAFT_ORDER;
import static uk.gov.hmcts.reform.fpl.model.order.OrderQuestionBlock.WHICH_CHILDREN;

@Getter
@RequiredArgsConstructor
public enum Order {
    C32_CARE_ORDER(
        "Care order", "Section 31 Children Act 1989", "C32 - Care order",
        List.of(APPROVER, APPROVAL_DATE, WHICH_CHILDREN, FURTHER_DIRECTIONS, REVIEW_DRAFT_ORDER)
    ),
    C23_EMERGENCY_PROTECTION_ORDER(
        "Emergency protection order", "Section 44 Children Act 1989", "C23 - Emergency protection order",
        List.of(APPROVER, APPROVAL_DATE_TIME, WHICH_CHILDREN, EPO_TYPE, EPO_INCLUDE_PHRASE, EPO_CHILDREN_DESCRIPTION,
            EPO_EXPIRY_DATE, EPO_PREVENT_REMOVAL, FURTHER_DIRECTIONS, REVIEW_DRAFT_ORDER)
    );

    private final String title;
    private final String childrenAct;
    private final String historyTitle;
    private final List<OrderQuestionBlock> questions;

    public String fileName(RenderFormat format) {
        return String.format("%s.%s", this.name().toLowerCase(), format.getExtension());
    }

    public OrderSection firstSection() {
        return this.getQuestions().get(0).getSection();
    }

    public Optional<OrderSection> nextSection(OrderSection currentSection) {
        Set<OrderSection> sectionsForOrder = this.getQuestions()
            .stream()
            .map(OrderQuestionBlock::getSection)
            .collect(Collectors.toSet());

        for (int i = 0; i < OrderSection.values().length - 1; i++) {
            if (currentSection.equals(OrderSection.values()[i])) { // current section found
                for (int j = i + 1; j < OrderSection.values().length; j++) { // assume sections in order
                    if (sectionsForOrder.contains(OrderSection.values()[j])) { // question sections contain section
                        return Optional.of(OrderSection.values()[j]);
                    }
                }
            }
        }

        return Optional.empty();
    }
}
