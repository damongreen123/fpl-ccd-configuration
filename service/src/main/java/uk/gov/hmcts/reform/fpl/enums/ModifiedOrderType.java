package uk.gov.hmcts.reform.fpl.enums;

public enum ModifiedOrderType {
    URGENT_HEARING_ORDER("Urgent hearing order"),
    STANDARD_DIRECTION_ORDER("Standard direction order"),
    CASE_MANAGEMENT_ORDER("Case management order"),
    NOTICE_OF_PROCEEDINGS("Notice of Proceedings");

    private final String label;

    ModifiedOrderType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}