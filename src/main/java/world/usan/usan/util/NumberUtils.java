package world.usan.usan.util;

import java.math.BigDecimal;

public final class NumberUtils {

    public static BigDecimal toBigDecimal(Object v) {
        if (v == null) return null;
        return new BigDecimal(String.valueOf(v));
    }
}
