package com.mydotey.ai.studio.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mydotey.ai.studio.entity.LoginAttempt;
import com.mydotey.ai.studio.mapper.LoginAttemptMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private final LoginAttemptMapper loginAttemptMapper;

    @Value("${auth.max-login-attempts:5}")
    private int maxLoginAttempts;

    @Value("${auth.lock-duration:900000}") // 15 minutes default
    private long lockDuration;

    /**
     * 检查是否被锁定
     */
    public boolean isLocked(String identifier) {
        LoginAttempt attempt = loginAttemptMapper.selectOne(
                new LambdaQueryWrapper<LoginAttempt>()
                        .eq(LoginAttempt::getIdentifier, identifier)
        );

        if (attempt == null) {
            return false;
        }

        if (attempt.getLockedUntil() != null) {
            if (attempt.getLockedUntil().isAfter(Instant.now())) {
                return true;
            } else {
                // 锁定已过期，重置
                resetAttempt(identifier);
                return false;
            }
        }

        return false;
    }

    /**
     * 记录失败的登录尝试
     */
    @Transactional
    public void recordFailedAttempt(String identifier, Long userId) {
        LoginAttempt attempt = loginAttemptMapper.selectOne(
                new LambdaQueryWrapper<LoginAttempt>()
                        .eq(LoginAttempt::getIdentifier, identifier)
        );

        if (attempt == null) {
            attempt = new LoginAttempt();
            attempt.setIdentifier(identifier);
            attempt.setUserId(userId);
            attempt.setAttemptCount(1);
            attempt.setLastAttemptAt(Instant.now());
            attempt.setCreatedAt(Instant.now());
            attempt.setUpdatedAt(Instant.now());
            loginAttemptMapper.insert(attempt);
        } else {
            attempt.setAttemptCount(attempt.getAttemptCount() + 1);
            attempt.setLastAttemptAt(Instant.now());
            attempt.setUpdatedAt(Instant.now());

            // 检查是否需要锁定
            if (attempt.getAttemptCount() >= maxLoginAttempts) {
                attempt.setLockedUntil(Instant.now().plusMillis(lockDuration));
            }

            loginAttemptMapper.updateById(attempt);
        }
    }

    /**
     * 记录成功的登录，重置尝试次数
     */
    @Transactional
    public void recordSuccessfulAttempt(String identifier) {
        resetAttempt(identifier);
    }

    /**
     * 重置登录尝试
     */
    @Transactional
    public void resetAttempt(String identifier) {
        LoginAttempt attempt = loginAttemptMapper.selectOne(
                new LambdaQueryWrapper<LoginAttempt>()
                        .eq(LoginAttempt::getIdentifier, identifier)
        );

        if (attempt != null) {
            attempt.setAttemptCount(0);
            attempt.setLockedUntil(null);
            attempt.setUpdatedAt(Instant.now());
            loginAttemptMapper.updateById(attempt);
        }
    }

    /**
     * 获取剩余锁定时间（秒）
     */
    public long getRemainingLockTime(String identifier) {
        LoginAttempt attempt = loginAttemptMapper.selectOne(
                new LambdaQueryWrapper<LoginAttempt>()
                        .eq(LoginAttempt::getIdentifier, identifier)
        );

        if (attempt == null || attempt.getLockedUntil() == null) {
            return 0;
        }

        long remaining = attempt.getLockedUntil().toEpochMilli() - Instant.now().toEpochMilli();
        return Math.max(0, remaining / 1000);
    }
}
