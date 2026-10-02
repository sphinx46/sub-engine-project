package mordvinov_dev.common.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mordvinov_dev.common.dto.request.PageRequest;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Страница результатов в формате ответа API.
 *
 * @param <T> тип элементов страницы
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {
    private List<T> content;
    private Integer currentPage;
    private Integer totalPages;
    private Long totalElements;
    private Integer pageSize;
    private boolean first;
    private boolean last;

    /**
     * Создаёт ответ из страницы Spring Data.
     *
     * @param page страница Spring Data
     * @param <T> тип элементов
     * @return страница в формате ответа API
     */
    public static <T> PageResponse<T> of(Page<T> page) {
        return PageResponse.<T>builder()
                .content(page.getContent())
                .currentPage(page.getNumber())
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .pageSize(page.getSize())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    /**
     * Создаёт ответ из готового списка и параметров запроса.
     *
     * @param pageRequest параметры запроса страницы
     * @param content элементы страницы
     * @param totalElements общее число элементов
     * @param <T> тип элементов
     * @return страница в формате ответа API
     */
    public static <T> PageResponse<T> of(PageRequest pageRequest, List<T> content, Long totalElements) {
        int pageSize = pageRequest.getSize();
        int totalPages = (int) Math.ceil((double) totalElements / pageSize);
        boolean isFirst = pageRequest.getPageNumber() == 0;
        boolean isLast = pageRequest.getPageNumber() >= totalPages - 1;

        return PageResponse.<T>builder()
                .content(content)
                .currentPage(pageRequest.getPageNumber())
                .totalPages(totalPages)
                .totalElements(totalElements)
                .pageSize(pageSize)
                .first(isFirst)
                .last(isLast)
                .build();
    }

    /**
     * Создаёт ответ из полного списка (единственная страница).
     *
     * @param content все элементы
     * @param <T> тип элементов
     * @return страница в формате ответа API
     */
    public static <T> PageResponse<T> ofAll(List<T> content) {
        return PageResponse.<T>builder()
                .content(content)
                .currentPage(0)
                .totalPages(1)
                .totalElements((long) content.size())
                .pageSize(content.size())
                .first(true)
                .last(true)
                .build();
    }
}
