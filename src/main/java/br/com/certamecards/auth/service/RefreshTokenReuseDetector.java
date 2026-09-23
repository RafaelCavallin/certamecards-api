package br.com.certamecards.auth.service;

import br.com.certamecards.auth.domain.RefreshToken;
import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenReuseDetector {

    private final MeterRegistry meterRegistry;
    private final RefreshFamilyRevoker familyRevoker;

    public RefreshTokenReuseDetector(MeterRegistry meterRegistry, RefreshFamilyRevoker familyRevoker) {
        this.meterRegistry = meterRegistry;
        this.familyRevoker = familyRevoker;
    }

    public void reject(RefreshToken token, Instant now) {
        meterRegistry.counter("auth.refresh.reuse_detected").increment();
        familyRevoker.revoke(token.getFamilyId(), now);
        throw ApiException.of(ErrorCode.UNAUTHENTICATED);
    }
}
