package br.com.certamecards.settings.service;

import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import br.com.certamecards.settings.domain.UpdateSettingsCommand;
import br.com.certamecards.settings.domain.UserSettings;
import br.com.certamecards.settings.persistence.UserSettingsRepository;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserSettingsService {

    private static final String DEFAULT_TIME_ZONE = "America/Sao_Paulo";

    private final UserSettingsRepository repository;

    public UserSettingsService(UserSettingsRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public UserSettings createDefault(UUID userId, String timeZone) {
        return repository.save(new UserSettings(userId, timeZone));
    }

    public UserSettings get(UUID userId) {
        return repository.findById(userId).orElseGet(() -> createDefault(userId, DEFAULT_TIME_ZONE));
    }

    @Transactional
    public UserSettings update(UUID userId, UpdateSettingsCommand command) {
        ensureValidTimeZone(command.timeZone());
        UserSettings settings = get(userId);
        settings.apply(command);
        return repository.save(settings);
    }

    private void ensureValidTimeZone(String timeZone) {
        try {
            ZoneId.of(timeZone);
        } catch (DateTimeException e) {
            throw ApiException.withDetail(ErrorCode.VALIDATION_FAILED, "Zona de fuso inválida.");
        }
    }
}
