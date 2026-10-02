package mordvinov_dev.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Базовое исключение бизнес-логики, которое превращается в ответ API с заданным HTTP-статусом.
 * Новые виды ошибок добавляются наследованием, без правок обработчика исключений.
 */
@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    /**
     * Создаёт исключение с HTTP-статусом и сообщением для клиента.
     *
     * @param status  HTTP-статус ответа
     * @param message сообщение об ошибке для клиента
     */
    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    /**
     * Создаёт исключение с HTTP-статусом, сообщением и причиной.
     *
     * @param status  HTTP-статус ответа
     * @param message сообщение об ошибке для клиента
     * @param cause   причина ошибки
     */
    public ApiException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }
}
