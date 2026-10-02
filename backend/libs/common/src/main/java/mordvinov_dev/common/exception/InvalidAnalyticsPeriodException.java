package mordvinov_dev.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Период аналитики не входит в допустимый набор (7, 30, 90 дней).
 */
public class InvalidAnalyticsPeriodException extends ApiException {

    public InvalidAnalyticsPeriodException(int days) {
        super(HttpStatus.BAD_REQUEST, "Недопустимый период: " + days + ". Допустимые значения: 7, 30, 90");
    }
}
