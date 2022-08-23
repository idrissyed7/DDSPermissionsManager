package io.unityfoundation.dds.permissions.manager.security;

import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.errors.OauthErrorResponseException;
import io.micronaut.security.token.event.RefreshTokenGeneratedEvent;
import io.micronaut.security.token.refresh.RefreshTokenPersistence;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static io.micronaut.security.errors.IssuingAnAccessTokenErrorCode.INVALID_GRANT;

@Singleton
public class RefreshTokenPersistenceImpl implements RefreshTokenPersistence {

    private static final Logger LOG = LoggerFactory.getLogger(RefreshTokenPersistenceImpl.class);

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenPersistenceImpl(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Override
    public void persistToken(RefreshTokenGeneratedEvent event) {
        String refreshToken = event.getRefreshToken();
        Authentication authentication = event.getAuthentication();
        if (refreshToken != null && authentication != null &&
                authentication.getName() != null) {
            refreshTokenRepository.save(authentication.getName(), refreshToken, false);
        } else {
            LOG.debug("DEBUG in persistToken");
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
