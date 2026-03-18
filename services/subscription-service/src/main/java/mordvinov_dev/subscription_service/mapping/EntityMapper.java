package mordvinov_dev.subscription_service.mapping;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Component for mapping entities to DTOs and vice versa using ModelMapper.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EntityMapper {
    private final ModelMapper modelMapper;


    /**
     * Maps an object of one type to another type using ModelMapper.
     * @param source the source object to map
     * @param targetClass the target class type
     * @param <S> the source type
     * @param <T> the target type
     * @return the mapped object or null if source is null
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
     * Maps a list of objects of one type to a list of objects of another type.
     * @param source the source list to map
     * @param targetClass the target class type for list elements
     * @param <S> the source type
     * @param <T> the target type
     * @return the mapped list or empty list if source is null or empty
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