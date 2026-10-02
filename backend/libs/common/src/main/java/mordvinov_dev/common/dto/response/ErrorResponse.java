package mordvinov_dev.common.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Единый формат ответа об ошибке: {@code {timestamp, status, error, message}}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;

    /**
     * Создаёт ответ об ошибке для HTTP-статуса.
     *
     * @param status  HTTP-статус
     * @param message сообщение об ошибке для клиента
     * @return ответ об ошибке
     */
    public static ErrorResponse of(HttpStatus status, String message) {
        return new ErrorResponse(LocalDateTime.now(ZoneOffset.UTC), status.value(), status.getReasonPhrase(), message);
    }
}
