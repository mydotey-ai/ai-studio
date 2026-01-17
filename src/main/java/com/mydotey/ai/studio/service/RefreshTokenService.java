package com.mydotey.ai.studio.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.mydotey.ai.studio.entity.RefreshToken;
import com.mydotey.ai.studio.mapper.RefreshTokenMapper;
import com.mydotey.ai.studio.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenMapper refreshTokenMapper;
    private final JwtUtil jwtUtil;

    /**
     * Create and save refresh token
     */
    public String createRefreshToken(Long userId) {
        // Revoke all previous refresh tokens for this user
        revokeUserTokens(userId);

        String tokenValue = UUID.randomUUID().toString();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(userId);
        refreshToken.setToken(tokenValue);
        refreshToken.setExpiresAt(Instant.now().plusMillis(jwtUtil.getRefreshTokenExpiration()));
        refreshToken.setIsRevoked(false);
        refreshToken.setCreatedAt(Instant.now());
        refreshToken.setUpdatedAt(Instant.now());

        refreshTokenMapper.insert(refreshToken);

        return tokenValue;
    }

    /**
     * Validate refresh token
     */
    public RefreshToken validateRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenMapper.selectOne(
                new LambdaQueryWrapper<RefreshToken>()
                        .eq(RefreshToken::getToken, token)
                        .eq(RefreshToken::getIsRevoked, false)
        );

        if (refreshToken == null) {
            return null;
        }

        if (refreshToken.getExpiresAt().isBefore(Instant.now())) {
            revokeToken(token);
            return null;
        }

        return refreshToken;
    }

    /**
     * Revoke specified refresh token
     */
    @Transactional
    public void revokeToken(String token) {
        refreshTokenMapper.update(null,
                new LambdaUpdateWrapper<RefreshToken>()
                        .eq(RefreshToken::getToken, token)
                        .set(RefreshToken::getIsRevoked, true)
                        .set(RefreshToken::getUpdatedAt, Instant.now())
        );
    }

    /**
     * Revoke all refresh tokens for a user
     */
    @Transactional
    public void revokeUserTokens(Long userId) {
        refreshTokenMapper.revokeByUserId(userId);
    }

    /**
     * Delete expired refresh tokens
     */
    @Transactional
    public void deleteExpiredTokens() {
        refreshTokenMapper.delete(
                new LambdaQueryWrapper<RefreshToken>()
                        .lt(RefreshToken::getExpiresAt, Instant.now())
        );
    }
}
