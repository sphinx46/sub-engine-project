package mordvinov_dev.billing_service.mapping;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Component for mapping entities to DTOs and vice versa.
 * Provides object mapping between different application layers.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EntityMapper {
    private final ModelMapper modelMapper;

    /**
     * Maps an object of one type to an object of another type.
     * @param source source object
     * @param targetClass target object class
     * @param <S> source object type
     * @param <T> target object type
     * @return mapped object or null if source is null
     */
    public <S, T> T map(final S source, final Class<T> targetClass) {
        if (source == null) {
            log.debug("ENTITY_MAPPER_ПРЕОБРАЗОВАНИЕ: " +
                    "исходный объект null, возвращается null");
            return null;
        }

        log.debug("ENTITY_MAPPER_ПРЕОБРАЗОВАНИЕ: преобразование из {} в {}",
                source.getClass().getSimpleName(), targetClass.getSimpleName());
        T result = modelMapper.map(source, targetClass);
        return result;
    }

    /**
     * Maps a list of objects of one type to a list of objects of another type.
     * @param source source list
     * @param targetClass target object class
     * @param <S> source object type
     * @param <T> target object type
     * @return mapped list or empty list if source is null
     */
    public <S, T> List<T> mapList(
            final List<S> source, final Class<T> targetClass) {
        if (source == null) {
            log.debug("ENTITY_MAPPER_ПРЕОБРАЗОВАНИЕ_СПИСКА: " +
                    "исходный список null, возвращается пустой список");
            return List.of();
        }

        log.debug("ENTITY_MAPPER_ПРЕОБРАЗОВАНИЕ_СПИСКА: " +
                        "преобразование списка из {} элементов из {} в {}",
                source.size(), source.get(0).getClass().getSimpleName(), targetClass.getSimpleName());

        List<T> result = source.stream()
                .map(element -> modelMapper.map(element, targetClass))
                .collect(Collectors.toList());
        return result;
    }
}