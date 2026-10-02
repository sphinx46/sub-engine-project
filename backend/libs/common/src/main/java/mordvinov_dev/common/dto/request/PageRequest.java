package mordvinov_dev.common.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Sort;

/**
 * Параметры постраничной выборки, получаемые от клиента.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageRequest {
    private Integer size;
    private Integer pageNumber;

    @Builder.Default
    private String sortBy = "createdAt";

    @Builder.Default
    private Sort.Direction direction = Sort.Direction.DESC;

    /**
     * Создаёт параметры страницы из query-параметров запроса.
     *
     * @param size       размер страницы
     * @param pageNumber номер страницы (с нуля)
     * @param sortBy     поле сортировки
     * @param direction  направление сортировки: ASC или DESC
     * @return параметры пагинации
     */
    public static PageRequest of(Integer size, Integer pageNumber, String sortBy, String direction) {
        return PageRequest.builder()
                .size(size)
                .pageNumber(pageNumber)
                .sortBy(sortBy)
                .direction(Sort.Direction.fromString(direction))
                .build();
    }

    /**
     * Преобразует параметры в объект пагинации Spring Data.
     *
     * @return параметры страницы для репозитория
     */
    public org.springframework.data.domain.PageRequest toPageable() {
        return org.springframework.data.domain.PageRequest.of(pageNumber, size, direction, sortBy);
    }
}
