package br.com.certamecards.sync.persistence;

import br.com.certamecards.sync.domain.EventOrder;
import java.sql.Timestamp;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class SettingFieldClockRepository {

    private static final String UPSERT =
            """
            INSERT INTO sync_setting_field_clocks (user_id, field_name, operation_id, event_at, event_counter, event_device_id)
            VALUES (:userId, :fieldName, :operationId, :eventAt, :eventCounter, :deviceId)
            ON CONFLICT (user_id, field_name) DO UPDATE SET operation_id = EXCLUDED.operation_id,
                event_at = EXCLUDED.event_at, event_counter = EXCLUDED.event_counter,
                event_device_id = EXCLUDED.event_device_id, updated_at = now()
            WHERE (sync_setting_field_clocks.event_at, sync_setting_field_clocks.event_counter,
                   sync_setting_field_clocks.event_device_id, sync_setting_field_clocks.operation_id)
                < (EXCLUDED.event_at, EXCLUDED.event_counter, EXCLUDED.event_device_id, EXCLUDED.operation_id)
            """;
    private final JdbcClient jdbcClient;

    public SettingFieldClockRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public boolean advance(UUID userId, String fieldName, EventOrder order) {
        return jdbcClient
                        .sql(UPSERT)
                        .param("userId", userId)
                        .param("fieldName", fieldName)
                        .param("operationId", order.operationId())
                        .param("eventAt", Timestamp.from(order.eventAt()))
                        .param("eventCounter", order.logicalCounter())
                        .param("deviceId", order.deviceId())
                        .update()
                > 0;
    }
}
