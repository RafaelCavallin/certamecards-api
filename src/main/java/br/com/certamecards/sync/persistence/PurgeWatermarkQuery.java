package br.com.certamecards.sync.persistence;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class PurgeWatermarkQuery {

    private final JdbcClient jdbcClient;

    public PurgeWatermarkQuery(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public long currentWatermark() {
        return jdbcClient
                .sql("SELECT watermark_change_seq FROM sync_purge_state WHERE id = true")
                .query(Long.class)
                .single();
    }

    public void advanceWatermark(long changeSeq) {
        jdbcClient
                .sql("UPDATE sync_purge_state SET watermark_change_seq = GREATEST(watermark_change_seq, :seq) "
                        + "WHERE id = true")
                .param("seq", changeSeq)
                .update();
    }
}
