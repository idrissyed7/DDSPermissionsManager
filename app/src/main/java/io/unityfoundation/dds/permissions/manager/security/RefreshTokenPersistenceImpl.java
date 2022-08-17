package io.unityfoundation.dds.permissions.manager.security;

import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.errors.OauthErrorResponseException;
import io.micronaut.security.token.event.RefreshTokenGeneratedEvent;
import io.micronaut.security.token.refresh.RefreshTokenPersistence;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;

import java.util.Optional;

import static io.micronaut.security.errors.IssuingAnAccessTokenErrorCode.INVALID_GRANT;

@Singleton
public class RefreshTokenPersistenceImpl implements RefreshTokenPersistence {

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenPersistenceImpl(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Override
    public void persistToken(RefreshTokenGeneratedEvent event) {
        if (event.getRefreshToken() != null && event.getAuthentication() != null &&
                event.getAuthentication().getName() != null) {
            String payload = event.getRefreshToken();
            refreshTokenRepository.save(event.getAuthentication().getName(), payload, false);
        } else {
            System.out.println("DEBUG in persistToken");
        }
    }

    @Override
    public Publisher<Authentication> getAuthentication(String refreshToken) {
        Optional<RefreshToken> tokenOpt = refreshTokenRepository.findByRefreshToken(refreshToken);
        if (tokenOpt.isPresent()) {
            RefreshToken token = tokenOpt.get();
            if (token.getRevoked()) {
                throw new OauthErrorResponseException(INVALID_GRANT, "refresh token revoked", null);
            } else {
                return Publishers.just(Authentication.build(token.getUsername()));
            }
        } else {
            throw new OauthErrorResponseException(INVALID_GRANT, "refresh token not found", null);
        }
    }
}
