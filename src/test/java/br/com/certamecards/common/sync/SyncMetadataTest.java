package br.com.certamecards.common.sync;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SyncMetadataTest {

    @Test
    void givenNewInstance_whenReadingChangeSeq_thenNull() {
        SyncMetadata metadata = new SyncMetadata();
        assertThat(metadata.getChangeSeq()).isNull();
    }
}
