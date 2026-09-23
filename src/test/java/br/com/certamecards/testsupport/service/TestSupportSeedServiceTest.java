package br.com.certamecards.testsupport.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

class TestSupportSeedServiceTest {

    @Test
    void QA_BUG01_seed_nao_mantem_uma_transacao_unica_que_prende_o_xmin_da_sincronizacao()
            throws NoSuchMethodException {
        Method seed = TestSupportSeedService.class.getMethod("seed", SeedCommand.class);

        boolean methodIsTransactional = seed.isAnnotationPresent(Transactional.class);
        boolean classIsTransactional = TestSupportSeedService.class.isAnnotationPresent(Transactional.class);

        assertThat(methodIsTransactional).isFalse();
        assertThat(classIsTransactional).isFalse();
    }
}
