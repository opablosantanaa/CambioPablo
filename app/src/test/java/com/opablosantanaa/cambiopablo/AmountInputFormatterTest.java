package com.opablosantanaa.cambiopablo;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class AmountInputFormatterTest {
    @Test public void typedDigitsShiftCentsExactlyAsRequested() {
        String amount = "";
        amount = AmountInputFormatter.format(amount + "1");
        assertEquals("0,01", amount);
        amount = AmountInputFormatter.format(amount + "0");
        assertEquals("0,10", amount);
        amount = AmountInputFormatter.format(amount + "5");
        assertEquals("1,05", amount);
    }

    @Test public void backspaceReversesTheSequenceAndRestoresTheHint() {
        String amount = "1,05";
        amount = AmountInputFormatter.format(amount.substring(0, amount.length() - 1));
        assertEquals("0,10", amount);
        amount = AmountInputFormatter.format(amount.substring(0, amount.length() - 1));
        assertEquals("0,01", amount);
        amount = AmountInputFormatter.format(amount.substring(0, amount.length() - 1));
        assertEquals("", amount);
    }

    @Test public void emptyAndZeroLeaveTheFieldEmpty() {
        assertEquals("", AmountInputFormatter.format(""));
        assertEquals("", AmountInputFormatter.format("0"));
        assertEquals("", AmountInputFormatter.format("0,00"));
    }

    @Test public void pastedAmountsAndLargeValuesKeepExactCents() {
        assertEquals("10,50", AmountInputFormatter.format("10,50"));
        assertEquals("1.234,56", AmountInputFormatter.format("123456"));
        assertEquals("999.999.999.999,99", AmountInputFormatter.format("99999999999999"));
        assertEquals("1,05", AmountInputFormatter.format("01,05"));
        assertEquals("5,80", AmountInputFormatter.format("0,58" + "0"));
    }
}
