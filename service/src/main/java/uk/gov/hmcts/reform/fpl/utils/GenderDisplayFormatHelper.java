package uk.gov.hmcts.reform.fpl.utils;

import org.apache.commons.lang3.StringUtils;

public class GenderDisplayFormatHelper {

    private static final String DEFAULT_STRING = "-";

    public static String formatGenderDisplay(final String gender, final String genderIdentification) {
        if (StringUtils.isNotEmpty(gender)) {
            if ("They identify in another way".equalsIgnoreCase(gender)
                && StringUtils.isNotEmpty(genderIdentification)) {
                return genderIdentification;
            }
            if ("Maent yn uniaethu mewn ffordd arall".equalsIgnoreCase(gender)
                && StringUtils.isNotEmpty(genderIdentification)) {
                return genderIdentification;
            }
            return gender;
        }
        return DEFAULT_STRING;
    }

}
