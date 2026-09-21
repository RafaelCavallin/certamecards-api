package br.com.certamecards.events.service;

import br.com.certamecards.events.domain.ProductEvent;
import br.com.certamecards.events.domain.ProductEventLimits;
import br.com.certamecards.events.domain.ProductEventName;
import br.com.certamecards.events.persistence.ProductEventRepository;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
public class ProductEventService {

    private final ProductEventRepository repository;
    private final ObjectMapper objectMapper;

    public ProductEventService(ProductEventRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void submit(UUID userId, List<SubmitEventCommand> events) {
        List<ProductEvent> accepted = events.stream()
                .map(event -> toEntity(userId, event))
                .flatMap(Optional::stream)
                .toList();
        repository.saveAll(accepted);
    }

    private Optional<ProductEvent> toEntity(UUID userId, SubmitEventCommand command) {
        Optional<ProductEventName> name = parseName(command.name());
        if (name.isEmpty()) {
            return Optional.empty();
        }
        String props = objectMapper.writeValueAsString(command.props());
        if (props.getBytes(StandardCharsets.UTF_8).length > ProductEventLimits.MAX_PROPS_BYTES) {
            return Optional.empty();
        }
        return Optional.of(new ProductEvent(command.id(), userId, name.get(), props, command.occurredAt()));
    }

    private Optional<ProductEventName> parseName(String name) {
        try {
            return Optional.of(ProductEventName.fromCode(name));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
