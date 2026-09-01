package Main;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Shared quantity helpers — BigDecimal only (no float/double for qty).
 */
public final class QuantityUtil {

    private QuantityUtil() {
    }

    public static BigDecimal parse(String raw) {
        if (raw == null) {
            throw new NumberFormatException("empty");
        }
        String s = raw.trim().replace(",", "");
        if (s.isEmpty()) {
            throw new NumberFormatException("empty");
        }
        return new BigDecimal(s);
    }

    /** Subtotal in whole rupees: qty × unitPrice, HALF_UP. */
    public static int moneySubtotal(BigDecimal qty, int unitPrice) {
        return qty.multiply(BigDecimal.valueOf(unitPrice))
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();
    }

    /**
     * Format for display/print: whole amounts without decimals ("2"),
     * fractional with up to 3 places ("1.5" / "1.500").
     */
    public static String format(BigDecimal qty, boolean allowDecimal) {
        if (qty == null) {
            return "0";
        }
        BigDecimal q = qty.setScale(3, RoundingMode.HALF_UP);
        if (!allowDecimal || q.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) == 0) {
            return q.setScale(0, RoundingMode.HALF_UP).toPlainString();
        }
        return q.stripTrailingZeros().toPlainString();
    }

    public static String formatWithUnit(BigDecimal qty, String unitName, boolean allowDecimal) {
        String u = unitName == null ? "" : unitName.trim();
        String q = format(qty, allowDecimal);
        if (u.isEmpty()) {
            return q;
        }
        return q + " " + u;
    }

    /** Reject decimals when unit does not allow them; max 3 dp when allowed. */
    public static String validate(BigDecimal qty, boolean allowDecimal) {
        if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
            return "Enter a valid quantity.";
        }
        if (!allowDecimal) {
            try {
                qty.toBigIntegerExact();
            } catch (ArithmeticException e) {
                return "This item is sold in whole units.";
            }
        } else if (qty.scale() > 3) {
            return "Quantity allows up to 3 decimal places.";
        }
        return null;
    }
}
