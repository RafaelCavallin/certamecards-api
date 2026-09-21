package br.com.certamecards.settings.persistence;

import br.com.certamecards.settings.domain.UserSettings;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSettingsRepository extends JpaRepository<UserSettings, UUID> {}
