package com.green.university.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.base-url}")
    private String baseUrl;

    /**
     * 파일을 S3에 업로드
     * @param file 업로드할 파일
     * @return S3에 저장된 파일명 (UUID_원본파일명)
     */
    public String uploadFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어있습니다.");
        }

        // UUID를 사용한 고유 파일명 생성
        String originalFilename = file.getOriginalFilename();
        String uuid = UUID.randomUUID().toString();
        String fileName = uuid + "_" + originalFilename;
        
        // public 폴더 안에 저장
        String s3Key = "public/" + fileName;

        try {
            // S3에 파일 업로드
            // ACL은 버킷에서 비활성화되어 있을 수 있으므로, 버킷 정책으로 공개 접근 제어
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key) // public/ 폴더 안에 저장
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    // ACL 제거 - 버킷 정책으로 공개 접근 제어
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            log.info("파일 업로드 성공: {} -> s3://{}/{}", originalFilename, bucketName, s3Key);
            // DB에는 public/을 포함한 전체 경로 저장
            return s3Key;
        } catch (Exception e) {
            log.error("파일 업로드 실패: {}", originalFilename, e);
            throw new IOException("S3 파일 업로드 실패: " + e.getMessage(), e);
        }
    }

    /**
     * S3에서 파일 다운로드
     * @param fileName S3에 저장된 파일명
     * @return 파일 InputStream
     */
    public InputStream downloadFile(String fileName) throws IOException {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .build();

            return s3Client.getObject(getObjectRequest);
        } catch (NoSuchKeyException e) {
            log.error("파일을 찾을 수 없습니다: {}", fileName);
            throw new IOException("파일을 찾을 수 없습니다: " + fileName, e);
        } catch (Exception e) {
            log.error("파일 다운로드 실패: {}", fileName, e);
            throw new IOException("S3 파일 다운로드 실패: " + e.getMessage(), e);
        }
    }

    /**
     * S3에서 파일 삭제
     * @param fileName 삭제할 파일명
     */
    public void deleteFile(String fileName) {
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("파일 삭제 성공: s3://{}/{}", bucketName, fileName);
        } catch (Exception e) {
            log.error("파일 삭제 실패: {}", fileName, e);
            // 삭제 실패는 예외를 던지지 않고 로그만 남김 (기존 파일이 없을 수 있음)
        }
    }

    /**
     * S3 파일 URL 생성
     * @param fileName S3에 저장된 파일명
     * @return 파일 접근 URL
     */
    public String getFileUrl(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return null;
        }
        try {
            // S3 URL 생성 - 특수문자만 선택적으로 인코딩
            // 일반적인 파일명 문자(a-z, A-Z, 0-9, -, _, .)는 인코딩하지 않음
            // 공백, 한글, 특수문자만 인코딩
            StringBuilder encoded = new StringBuilder();
            for (char c : fileName.toCharArray()) {
                if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || 
                    (c >= '0' && c <= '9') || c == '-' || c == '_' || c == '.' || c == '/') {
                    // 일반 문자는 그대로 사용
                    encoded.append(c);
                } else {
                    // 특수문자만 인코딩
                    try {
                        String encodedChar = java.net.URLEncoder.encode(String.valueOf(c), java.nio.charset.StandardCharsets.UTF_8);
                        encoded.append(encodedChar);
                    } catch (Exception e) {
                        // 인코딩 실패 시 원본 문자 사용
                        encoded.append(c);
                    }
                }
            }
            // 공백을 %20으로 변환 (URLEncoder는 +로 변환하므로)
            String encodedFileName = encoded.toString().replace("+", "%20");
            String url = baseUrl + "/" + encodedFileName;
            log.debug("S3 URL 생성: {} -> {}", fileName, url);
            return url;
        } catch (Exception e) {
            log.error("파일 URL 생성 실패: {}", fileName, e);
            // 인코딩 실패 시 원본 파일명 사용
            return baseUrl + "/" + fileName;
        }
    }

    /**
     * 파일 존재 여부 확인
     * @param fileName 확인할 파일명
     * @return 파일 존재 여부
     */
    public boolean fileExists(String fileName) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .build();

            s3Client.headObject(headObjectRequest);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            log.error("파일 존재 확인 실패: {}", fileName, e);
            return false;
        }
    }
}

