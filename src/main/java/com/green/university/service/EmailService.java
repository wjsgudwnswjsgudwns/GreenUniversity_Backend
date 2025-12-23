package com.green.university.service;

import com.green.university.handler.exception.CustomRestfullException;
import com.green.university.repository.EmailVerificationRepository;
import com.green.university.repository.model.EmailVerification;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final EmailVerificationRepository emailVerificationRepository;
    private static final SecureRandom random = new SecureRandom();

    /**
     * 6자리 랜덤 인증 코드 생성
     */
    private String generateVerificationCode() {
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    /**
     * 이메일 인증 코드 발송
     */
    @Transactional
    public void sendVerificationEmail(Integer userId, String email) {
        // 기존 미인증 코드가 있으면 삭제
        Optional<EmailVerification> existingVerification =
                emailVerificationRepository.findByUserIdAndEmailAndIsVerifiedFalse(userId, email);
        existingVerification.ifPresent(emailVerificationRepository::delete);

        // 새로운 인증 코드 생성
        String verificationCode = generateVerificationCode();

        // DB에 저장
        EmailVerification emailVerification = EmailVerification.builder()
                .email(email)
                .verificationCode(verificationCode)
                .userId(userId)
                .isVerified(false)
                .build();

        emailVerificationRepository.save(emailVerification);

        // 이메일 발송
        try {
            sendEmail(email, verificationCode);
        } catch (MessagingException e) {
            throw new CustomRestfullException("이메일 발송에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 실제 이메일 발송 로직
     */
    private void sendEmail(String to, String verificationCode) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(to);
        helper.setSubject("[Green University] 이메일 인증 코드");

        String htmlContent = String.format("""
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                <h2 style="color: #2c3e50;">이메일 인증 코드</h2>
                <p>안녕하세요, Green University입니다.</p>
                <p>아래의 인증 코드를 입력하여 이메일 인증을 완료해주세요.</p>
                <div style="background-color: #f8f9fa; padding: 20px; text-align: center; margin: 20px 0;">
                    <h1 style="color: #28a745; letter-spacing: 5px; margin: 0;">%s</h1>
                </div>
                <p style="color: #dc3545;">※ 인증 코드는 5분간 유효합니다.</p>
                <hr style="border: none; border-top: 1px solid #dee2e6; margin: 20px 0;">
                <p style="color: #6c757d; font-size: 12px;">
                    본인이 요청하지 않은 경우, 이 이메일을 무시하셔도 됩니다.
                </p>
            </div>
            """, verificationCode);

        helper.setText(htmlContent, true);

        mailSender.send(message);
    }

    /**
     * 이메일 인증 코드 확인
     */
    @Transactional
    public boolean verifyEmailCode(Integer userId, String email, String code) {
        Optional<EmailVerification> verificationOpt =
                emailVerificationRepository.findByEmailAndVerificationCode(email, code);

        if (verificationOpt.isEmpty()) {
            throw new CustomRestfullException("유효하지 않은 인증 코드입니다.", HttpStatus.BAD_REQUEST);
        }

        EmailVerification verification = verificationOpt.get();

        // 사용자 ID 확인
        if (!verification.getUserId().equals(userId)) {
            throw new CustomRestfullException("인증 권한이 없습니다.", HttpStatus.FORBIDDEN);
        }

        // 이미 인증된 코드인지 확인
        if (verification.getIsVerified()) {
            throw new CustomRestfullException("이미 사용된 인증 코드입니다.", HttpStatus.BAD_REQUEST);
        }

        // 만료 확인
        if (verification.isExpired()) {
            throw new CustomRestfullException("인증 코드가 만료되었습니다. 새로운 코드를 요청해주세요.", HttpStatus.BAD_REQUEST);
        }

        // 인증 완료 처리
        verification.setIsVerified(true);
        emailVerificationRepository.save(verification);

        return true;
    }

    /**
     * 만료된 인증 코드 정리 (스케줄러에서 호출)
     */
    @Transactional
    public void cleanupExpiredVerifications() {
        emailVerificationRepository.deleteByExpiresAtBefore(LocalDateTime.now());
    }

    /**
     * 이메일 인증 여부 확인
     */
    public boolean isEmailVerified(Integer userId, String email) {
        Optional<EmailVerification> verificationOpt =
                emailVerificationRepository.findByUserIdAndEmailAndIsVerifiedFalse(userId, email);

        return verificationOpt.isEmpty(); // 미인증 코드가 없으면 인증된 것으로 간주
    }
}