package com.green.university.repository;

import com.green.university.repository.model.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Integer> {

    /**
     * 이메일과 인증 코드로 인증 정보 조회
     */
    Optional<EmailVerification> findByEmailAndVerificationCode(String email, String verificationCode);

    /**
     * 사용자 ID로 가장 최근 인증 정보 조회
     */
    Optional<EmailVerification> findTopByUserIdOrderByCreatedAtDesc(Integer userId);

    /**
     * 이메일로 가장 최근 인증 정보 조회
     */
    Optional<EmailVerification> findTopByEmailOrderByCreatedAtDesc(String email);

    /**
     * 만료된 인증 코드 삭제
     */
    void deleteByExpiresAtBefore(LocalDateTime dateTime);

    /**
     * 사용자 ID와 이메일로 미인증 코드 조회
     */
    Optional<EmailVerification> findByUserIdAndEmailAndIsVerifiedFalse(Integer userId, String email);
}