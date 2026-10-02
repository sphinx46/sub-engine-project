package mordvinov_dev.common.analytics;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Расчёт изменения показателя в процентах относительно прошлого периода.
 */
public final class PercentChange {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private PercentChange() {
    }

    /**
     * Считает изменение в процентах с одним знаком после запятой.
     *
     * @param current  текущее значение
     * @param previous значение за прошлый период
     * @return изменение в процентах или null, если за прошлый период было ноль (сравнивать не с чем)
     */
    public static BigDecimal of(BigDecimal current, BigDecimal previous) {
        if (previous.signum() == 0) {
            return null;
        }
        return current.subtract(previous)
                .multiply(HUNDRED)
                .divide(previous, 1, RoundingMode.HALF_UP);
    }

    /**
     * Считает изменение в процентах для целочисленных показателей.
     *
     * @param current  текущее значение
     * @param previous значение за прошлый период
     * @return изменение в процентах или null, если за прошлый период было ноль
     */
    public static BigDecimal of(long current, long previous) {
        return of(BigDecimal.valueOf(current), BigDecimal.valueOf(previous));
    }
}
