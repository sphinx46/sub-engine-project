package mordvinov_dev.subscription_service.mapping;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Компонент для преобразования сущностей в DTO и обратно.
 * Обеспечивает маппинг объектов между различными слоями приложения.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EntityMapper {
    private final ModelMapper modelMapper;

    /**
     * Преобразует объект одного типа в объект другого типа.
     *
     * @param source исходный объект
     * @param targetClass класс целевого объекта
     * @param <S> тип исходного объекта
     * @param <T> тип целевого объекта
     * @return преобразованный объект или null, если source равен null
     */
    public <S, T> T map(final S source, final Class<T> targetClass) {
        if (source == null) {
            log.debug("ENTITY_MAPPER_ПРЕОБРАЗОВАНИЕ: исходный объект null, возвращается null");
            return null;
        }

        log.debug("ENTITY_MAPPER_ПРЕОБРАЗОВАНИЕ: преобразование из {} в {}",
                source.getClass().getSimpleName(), targetClass.getSimpleName());
        return modelMapper.map(source, targetClass);
    }

    /**
     * Преобразует список объектов одного типа в список объектов другого типа.
     *
     * @param source исходный список
     * @param targetClass класс целевых объектов
     * @param <S> тип исходных объектов
     * @param <T> тип целевых объектов
     * @return преобразованный список или пустой список, если source равен null или пуст
     */
    public <S, T> List<T> mapList(final List<S> source, final Class<T> targetClass) {
        if (source == null || source.isEmpty()) {
            log.debug("ENTITY_MAPPER_ПРЕОБРАЗОВАНИЕ_СПИСКА: исходный список {} - возвращается пустой список",
                    source == null ? "null" : "пуст");
            return Collections.emptyList();
        }

        log.debug("ENTITY_MAPPER_ПРЕОБРАЗОВАНИЕ_СПИСКА: преобразование списка из {} элементов из {} в {}",
                source.size(), source.get(0).getClass().getSimpleName(), targetClass.getSimpleName());

        return source.stream()
                .map(element -> modelMapper.map(element, targetClass))
                .collect(Collectors.toList());
    }
}