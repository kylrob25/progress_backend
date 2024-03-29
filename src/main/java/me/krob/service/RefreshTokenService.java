package me.krob.service;

import me.krob.model.token.RefreshToken;
import me.krob.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

@Service
public class RefreshTokenService {

    @Value("${progress.jwt.refreshExpiration}")
    private long refreshExpiration;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    public RefreshToken create(String username) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUsername(username);

        Instant now = Instant.now();
        refreshToken.setCreation(now);
        refreshToken.setExpiry(now.plusMillis(refreshExpiration));
        refreshToken.setToken(UUID.randomUUID().toString());

        return refreshTokenRepository.save(refreshToken);
    }

    public RefreshToken verify(RefreshToken token) {
        if (token.getExpiry().compareTo(Instant.now()) < 0) {
            Logger.getGlobal().info(token.getUsername() + " token has expired, deleting..");
            refreshTokenRepository.delete(token);
            return null;
        }
        Logger.getGlobal().info("Valid token");
        return token;
    }

    public void deleteByUsername(String username) {
        refreshTokenRepository.deleteByUsername(username);
    }
}
