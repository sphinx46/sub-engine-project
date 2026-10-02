package mordvinov_dev.common.analytics;

import lombok.Getter;
import mordvinov_dev.common.exception.InvalidAnalyticsPeriodException;

import java.util.Arrays;

/**
 * Допустимые периоды админской аналитики.
 */
@Getter
public enum AnalyticsPeriod {

    WEEK(7),
    MONTH(30),
    QUARTER(90);

    private final int days;

    AnalyticsPeriod(int days) {
        this.days = days;
    }

    /**
     * Находит период по числу дней.
     *
     * @param days число дней
     * @return период аналитики
     * @throws InvalidAnalyticsPeriodException если такого периода нет
     */
    public static AnalyticsPeriod ofDays(int days) {
        return Arrays.stream(values())
                .filter(period -> period.days == days)
                .findFirst()
                .orElseThrow(() -> new InvalidAnalyticsPeriodException(days));
    }
}
