package com.opablosantanaa.cambiopablo;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/** Formats digits as cents, without turning the empty field's hint into text. */
final class AmountInputFormatter {
    private AmountInputFormatter() { }

    static String format(CharSequence input) {
        String digits = input.toString().replaceAll("[^0-9]", "").replaceFirst("^0+", "");
        if (digits.isEmpty()) return "";

        BigDecimal value = new BigDecimal(digits).movePointLeft(2);
        NumberFormat format = NumberFormat.getNumberInstance(new Locale("pt", "BR"));
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        format.setMinimumIntegerDigits(1);
        return format.format(value);
    }
}
