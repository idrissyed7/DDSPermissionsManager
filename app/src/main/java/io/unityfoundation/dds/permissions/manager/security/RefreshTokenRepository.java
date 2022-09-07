package io.unityfoundation.dds.permissions.manager.security;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.CrudRepository;

import javax.transaction.Transactional;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Optional;

@Repository
interface RefreshTokenRepository extends CrudRepository<RefreshToken, Long> {

    @Transactional
    RefreshToken save(@NonNull @NotBlank String username,
                      @NonNull @NotBlank String refreshToken,
                      @NonNull @NotNull Boolean revoked);

    @NonNull
    Optional<RefreshToken> findByRefreshToken(@NonNull @NotBlank String refreshToken);

    long updateByUsername(@NonNull @NotBlank String username,
                          boolean revoked);
}