package br.com.certamecards.common.sync;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class SyncSequenceQuery {

    private final JdbcClient jdbcClient;

    public SyncSequenceQuery(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public long currentValue() {
        return jdbcClient
                .sql("SELECT last_value FROM sync_seq")
                .query(Long.class)
                .single();
    }
}
