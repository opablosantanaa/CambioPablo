package com.opablosantanaa.cambiopablo;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

final class AmountInputFormatter {
    private AmountInputFormatter() {
    }

    static String format(CharSequence input) {
        String raw = input.toString();
        String digits = raw.replaceAll("[^0-9]", "").replaceFirst("^0+(?=\\d)", "");
        if (digits.isEmpty()){
            return "";
        }

        BigDecimal value = new BigDecimal(digits).movePointLeft(2);
        NumberFormat format = NumberFormat.getNumberInstance(new Locale("pt", "BR"));
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        format.setMinimumIntegerDigits(1);
        return format.format(value);
    }
}
