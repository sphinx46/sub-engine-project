package mordvinov_dev.subscription_service.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Ответ со списком подписок с пагинацией")
public class SubscriptionPageResponse {

    @Schema(description = "Список подписок")
    private List<SubscriptionResponse> content;

    @Schema(description = "Номер страницы", example = "0")
    private int pageNumber;

    @Schema(description = "Размер страницы", example = "20")
    private int pageSize;

    @Schema(description = "Общее количество элементов", example = "100")
    private long totalElements;

    @Schema(description = "Общее количество страниц", example = "5")
    private int totalPages;

    @Schema(description = "Последняя ли страница", example = "false")
    private boolean last;

    @Schema(description = "Первая ли страница", example = "true")
    private boolean first;

    @Schema(description = "Пустая ли страница", example = "false")
    private boolean empty;
}