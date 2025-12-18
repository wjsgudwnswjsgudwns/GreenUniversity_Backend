package com.green.university.service;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.green.university.dto.NoticeFormDto;
import com.green.university.dto.NoticePageFormDto;
import java.sql.Timestamp;
import java.util.stream.Collectors;

import com.green.university.repository.NoticeFileJpaRepository;
import com.green.university.repository.NoticeJpaRepository;
import com.green.university.repository.model.Notice;
import com.green.university.repository.model.NoticeFile;
import com.green.university.service.S3Service;

import lombok.extern.slf4j.Slf4j;

/**
 * @author 박성희
 * notice 관련 서비스
 */
@Slf4j
@Service
public class NoticeService {

    @Autowired
    private NoticeJpaRepository noticeJpaRepository;

    @Autowired
    private NoticeFileJpaRepository noticeFileJpaRepository;

    @Autowired
    private S3Service s3Service;

    /**
     * 새로운 공지를 등록한다.
     */
    @org.springframework.transaction.annotation.Transactional
    public void createNotice(@Validated NoticeFormDto noticeFormDto) {
        Notice notice = new Notice();
        notice.setCategory(noticeFormDto.getCategory());
        notice.setTitle(noticeFormDto.getTitle());
        notice.setContent(noticeFormDto.getContent());
        notice.setViews(0);
        notice.setCreatedTime(new Timestamp(System.currentTimeMillis()));

        Notice savedNotice = noticeJpaRepository.save(notice);

        // 여러 파일 처리
        // DTO에서 직접 content 가져오기 (notice 객체의 content는 이미 DTO에서 설정됨)
        String content = noticeFormDto.getContent();
        if (content == null) {
            content = notice.getContent();
        }
        
        // 디버깅: 받은 content 확인
        if (content != null) {
            log.info("받은 content 길이: {}", content.length());
            // content에서 img 태그 찾기
            java.util.regex.Pattern imgPattern = java.util.regex.Pattern.compile("<img[^>]*>", java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher imgMatcher = imgPattern.matcher(content);
            int imgCount = 0;
            while (imgMatcher.find()) {
                imgCount++;
                String imgTag = imgMatcher.group();
                log.info("받은 content의 img 태그 #{}: {}", imgCount, imgTag);
            }
            log.info("받은 content에서 총 {}개의 img 태그 발견", imgCount);
        }
        if (noticeFormDto.getFiles() != null && noticeFormDto.getFiles().length > 0) {
            for (org.springframework.web.multipart.MultipartFile file : noticeFormDto.getFiles()) {
                if (file != null && !file.isEmpty()) {
                    try {
                        // S3에 업로드 (파일명은 S3Service에서 UUID 생성)
                        String fileName = s3Service.uploadFile(file);
                        String originalFilename = file.getOriginalFilename();
                        
                        // content에서 임시 이미지 태그를 UUID 파일명으로 교체
                        // __IMAGE_원본파일명__ 패턴을 UUID로 교체
                        String tempPattern = "__IMAGE_" + originalFilename + "__";
                        String imageUrl = s3Service.getFileUrl(fileName);
                        
                        if (content != null) {
                            // 디버깅: 교체 전 content 확인
                            boolean containsPattern = content.contains(tempPattern);
                            log.info("이미지 태그 교체 시도 - 파일명: {}, 패턴 포함 여부: {}, tempPattern: {}", originalFilename, containsPattern, tempPattern);
                            
                            // 여러 패턴으로 시도: src 속성에 tempPattern이 있는 경우
                            // 1. src="__IMAGE_파일명__" 또는 src='__IMAGE_파일명__' 패턴
                            String escapedPattern = java.util.regex.Pattern.quote(tempPattern);
                            String regex1 = "(<img[^>]*src=[\"'])" + escapedPattern + "([\"'][^>]*>)";
                            String beforeReplace = content;
                            content = content.replaceAll(regex1, "$1" + imageUrl + "$2");
                            
                            // 2. src 속성이 따옴표 없이 있는 경우 (src=__IMAGE_파일명__)
                            if (beforeReplace.equals(content)) {
                                String regex2 = "(<img[^>]*src=)" + escapedPattern + "([\\s>])";
                                content = content.replaceAll(regex2, "$1\"" + imageUrl + "\"$2");
                            }
                            
                            // 3. 단순 문자열 교체 (최후의 수단)
                            if (beforeReplace.equals(content) && content.contains(tempPattern)) {
                                content = content.replace(tempPattern, imageUrl);
                                log.info("단순 문자열 교체로 이미지 태그 교체 성공: {} -> {}", tempPattern, imageUrl);
                            }
                            
                            // 디버깅: 교체 결과 확인
                            if (!beforeReplace.equals(content)) {
                                log.info("정규식으로 이미지 태그 교체 성공: {} -> {}", tempPattern, imageUrl);
                                // 교체 후 content 일부 로그 출력
                                int imgIndex = content.indexOf("<img");
                                if (imgIndex >= 0) {
                                    String imgTagPreview = content.substring(imgIndex, Math.min(imgIndex + 200, content.length()));
                                    log.info("교체된 이미지 태그 (일부): {}", imgTagPreview);
                                }
                            } else {
                                log.warn("이미지 태그 교체 실패 - 패턴을 찾을 수 없음: {}", tempPattern);
                                // 디버깅: content에서 img 태그 찾기
                                int imgIndex = content.indexOf("<img");
                                if (imgIndex >= 0) {
                                    String imgTagPreview = content.substring(imgIndex, Math.min(imgIndex + 300, content.length()));
                                    log.warn("content에서 발견된 img 태그 (일부): {}", imgTagPreview);
                                }
                            }
                            
                            // data-temp-id 속성 제거 (모든 경우에 대해 처리)
                            // 여러 패턴 시도: data-temp-id="..." 또는 data-temp-id='...' 또는 공백 포함
                            String beforeDataRemoval = content;
                            content = content.replaceAll("data-temp-id\\s*=\\s*\"[^\"]*\"", ""); // 큰따옴표
                            content = content.replaceAll("data-temp-id\\s*=\\s*'[^']*'", ""); // 작은따옴표
                            content = content.replaceAll("\\s+", " "); // 연속된 공백 정리
                            if (!beforeDataRemoval.equals(content)) {
                                log.info("data-temp-id 속성 제거 완료");
                            }
                        }
                        
                        NoticeFile noticeFile = new NoticeFile();
                        noticeFile.setUuidFilename(fileName);
                        noticeFile.setOriginFilename(originalFilename);
                        noticeFile.setNotice(savedNotice);
                        noticeFile.setNoticeId(savedNotice.getId());
                        noticeFileJpaRepository.save(noticeFile);
                    } catch (IOException e) {
                        throw new RuntimeException("파일 업로드 중 오류가 발생했습니다: " + e.getMessage(), e);
                    }
                }
            }
            // content 업데이트 (이미지 태그 교체 후)
            // 최종적으로 모든 data-temp-id 속성 제거 (파일 처리 후 한 번 더)
            if (content != null) {
                String beforeFinalCleanup = content;
                content = content.replaceAll("data-temp-id\\s*=\\s*\"[^\"]*\"", ""); // 큰따옴표
                content = content.replaceAll("data-temp-id\\s*=\\s*'[^']*'", ""); // 작은따옴표
                content = content.replaceAll("\\s+", " "); // 연속된 공백 정리
                if (!beforeFinalCleanup.equals(content)) {
                    log.info("최종 data-temp-id 속성 제거 완료");
                }
                
                if (!content.equals(notice.getContent())) {
                    notice.setContent(content);
                    noticeJpaRepository.save(notice);
                    // 디버깅: 저장된 content 확인 (일부만 표시)
                    String contentPreview = content.length() > 500 ? content.substring(0, 500) + "..." : content;
                    log.info("저장된 content (최종, 일부): {}", contentPreview);
                    
                    // 디버깅: 모든 img 태그 찾기
                    java.util.regex.Pattern imgPattern = java.util.regex.Pattern.compile("<img[^>]*>", java.util.regex.Pattern.CASE_INSENSITIVE);
                    java.util.regex.Matcher imgMatcher = imgPattern.matcher(content);
                    int imgCount = 0;
                    while (imgMatcher.find()) {
                        imgCount++;
                        String imgTag = imgMatcher.group();
                        log.info("저장된 img 태그 #{}: {}", imgCount, imgTag);
                    }
                    log.info("총 {}개의 img 태그 발견", imgCount);
                }
            }
        }
        // 단일 파일 처리 (하위 호환성)
        else if (noticeFormDto.getOriginFilename() != null && noticeFormDto.getUuidFilename() != null) {
            NoticeFile noticeFile = new NoticeFile();
            noticeFile.setUuidFilename(noticeFormDto.getUuidFilename());
            noticeFile.setOriginFilename(noticeFormDto.getOriginFilename());
            noticeFile.setNotice(savedNotice);
            noticeFile.setNoticeId(savedNotice.getId());
            noticeFileJpaRepository.save(noticeFile);
        }

        noticeFormDto.setNoticeId(savedNotice.getId());
    }

    /**
     * 공지 목록 조회 (페이징)
     */
    public Page<Notice> readNoticePage(NoticePageFormDto dto) {
        int page = dto.getPage() != null ? dto.getPage() : 0;
        Pageable pageable = PageRequest.of(page, 10);

        String keyword = dto.getKeyword();
        String type = dto.getType();

        // 키워드가 없으면 전체 조회 (ID 내림차순 - 최신순)
        if (keyword == null || keyword.trim().isEmpty()) {
            return noticeJpaRepository.findAllByOrderByIdDesc(pageable);
        }

        // 검색 타입에 따른 조회
        if ("title".equals(type)) {
            return noticeJpaRepository.findByTitleContainingIgnoreCaseOrderByIdDesc(keyword, pageable);
        } else if ("keyword".equals(type)) {
            return noticeJpaRepository.findByTitleOrContentContainingIgnoreCase(keyword, pageable);
        } else {
            return noticeJpaRepository.findByContentContainingIgnoreCaseOrderByIdDesc(keyword, pageable);
        }
    }

    /**
     * 공지 상세 조회 (조회수 증가 없음)
     */
    public Notice readByIdNotice(Integer id) {
        Notice notice = noticeJpaRepository.findById(id).orElse(null);
        if (notice != null && notice.getFiles() != null) {
            // files를 명시적으로 초기화하여 LAZY 로딩
            notice.getFiles().size();
        }
        return notice;
    }

    /**
     * 조회수 증가 (별도 메서드)
     */
    public void increaseViews(Integer id) {
        noticeJpaRepository.findById(id).ifPresent(notice -> {
            Integer currentViews = notice.getViews();
            notice.setViews(currentViews == null ? 1 : currentViews + 1);
            noticeJpaRepository.save(notice);
        });
    }

    /**
     * 공지 수정
     */
    public int updateNotice(NoticeFormDto noticeFormDto) {
        if (noticeFormDto.getId() == null) {
            return 0;
        }

        return noticeJpaRepository.findById(noticeFormDto.getId()).map(notice -> {
            notice.setCategory(noticeFormDto.getCategory());
            notice.setTitle(noticeFormDto.getTitle());
            
            // 여러 파일 처리
            String content = noticeFormDto.getContent();
            if (noticeFormDto.getFiles() != null && noticeFormDto.getFiles().length > 0) {
                // 기존 파일 삭제 (S3에서도 삭제)
                noticeFileJpaRepository.findByNoticeId(notice.getId())
                        .forEach(f -> {
                            s3Service.deleteFile(f.getUuidFilename());
                            noticeFileJpaRepository.deleteById(f.getUuidFilename());
                        });

                // 새 파일들 S3에 업로드 및 DB 저장
                for (org.springframework.web.multipart.MultipartFile file : noticeFormDto.getFiles()) {
                    if (file != null && !file.isEmpty()) {
                        try {
                            // S3에 업로드 (파일명은 S3Service에서 UUID 생성)
                            String fileName = s3Service.uploadFile(file);
                            String originalFilename = file.getOriginalFilename();
                            
                            // content에서 임시 이미지 태그를 UUID 파일명으로 교체
                            String tempPattern = "__IMAGE_" + originalFilename + "__";
                            String imageUrl = s3Service.getFileUrl(fileName);
                            
                            if (content != null) {
                                // 디버깅: 교체 전 content 확인
                                boolean containsPattern = content.contains(tempPattern);
                                log.info("이미지 태그 교체 시도 (update) - 파일명: {}, 패턴 포함 여부: {}, tempPattern: {}", originalFilename, containsPattern, tempPattern);
                                
                                // 여러 패턴으로 시도: src 속성에 tempPattern이 있는 경우
                                // 1. src="__IMAGE_파일명__" 또는 src='__IMAGE_파일명__' 패턴
                                String escapedPattern = java.util.regex.Pattern.quote(tempPattern);
                                String regex1 = "(<img[^>]*src=[\"'])" + escapedPattern + "([\"'][^>]*>)";
                                String beforeReplace = content;
                                content = content.replaceAll(regex1, "$1" + imageUrl + "$2");
                                
                                // 2. src 속성이 따옴표 없이 있는 경우 (src=__IMAGE_파일명__)
                                if (beforeReplace.equals(content)) {
                                    String regex2 = "(<img[^>]*src=)" + escapedPattern + "([\\s>])";
                                    content = content.replaceAll(regex2, "$1\"" + imageUrl + "\"$2");
                                }
                                
                                // 3. 단순 문자열 교체 (최후의 수단)
                                if (beforeReplace.equals(content) && content.contains(tempPattern)) {
                                    content = content.replace(tempPattern, imageUrl);
                                    log.info("단순 문자열 교체로 이미지 태그 교체 성공 (update): {} -> {}", tempPattern, imageUrl);
                                }
                                
                                // 디버깅: 교체 결과 확인
                                if (!beforeReplace.equals(content)) {
                                    log.info("정규식으로 이미지 태그 교체 성공 (update): {} -> {}", tempPattern, imageUrl);
                                    // 교체 후 content 일부 로그 출력
                                    int imgIndex = content.indexOf("<img");
                                    if (imgIndex >= 0) {
                                        String imgTagPreview = content.substring(imgIndex, Math.min(imgIndex + 200, content.length()));
                                        log.info("교체된 이미지 태그 (일부, update): {}", imgTagPreview);
                                    }
                                } else {
                                    log.warn("이미지 태그 교체 실패 (update) - 패턴을 찾을 수 없음: {}", tempPattern);
                                    // 디버깅: content에서 img 태그 찾기
                                    int imgIndex = content.indexOf("<img");
                                    if (imgIndex >= 0) {
                                        String imgTagPreview = content.substring(imgIndex, Math.min(imgIndex + 300, content.length()));
                                        log.warn("content에서 발견된 img 태그 (일부, update): {}", imgTagPreview);
                                    }
                                }
                                
                                // data-temp-id 속성 제거 (모든 경우에 대해 처리)
                                // 여러 패턴 시도: data-temp-id="..." 또는 data-temp-id='...' 또는 공백 포함
                                String beforeDataRemoval = content;
                                content = content.replaceAll("data-temp-id\\s*=\\s*\"[^\"]*\"", ""); // 큰따옴표
                                content = content.replaceAll("data-temp-id\\s*=\\s*'[^']*'", ""); // 작은따옴표
                                content = content.replaceAll("\\s+", " "); // 연속된 공백 정리
                                if (!beforeDataRemoval.equals(content)) {
                                    log.info("data-temp-id 속성 제거 완료 (update)");
                                }
                            }
                            
                            NoticeFile noticeFile = new NoticeFile();
                            noticeFile.setUuidFilename(fileName);
                            noticeFile.setOriginFilename(originalFilename);
                            noticeFile.setNotice(notice);
                            noticeFile.setNoticeId(notice.getId());
                            noticeFileJpaRepository.save(noticeFile);
                        } catch (IOException e) {
                            throw new RuntimeException("파일 업로드 중 오류가 발생했습니다: " + e.getMessage(), e);
                        }
                    }
                }
            }
            // 단일 파일 처리 (하위 호환성)
            else if (noticeFormDto.getOriginFilename() != null && noticeFormDto.getUuidFilename() != null) {
                // 기존 파일 삭제
                noticeFileJpaRepository.findByNoticeId(notice.getId())
                        .forEach(f -> noticeFileJpaRepository.deleteById(f.getUuidFilename()));

                // 새 파일 저장
                NoticeFile noticeFile = new NoticeFile();
                noticeFile.setUuidFilename(noticeFormDto.getUuidFilename());
                noticeFile.setOriginFilename(noticeFormDto.getOriginFilename());
                noticeFile.setNotice(notice);
                noticeFile.setNoticeId(notice.getId());
                noticeFileJpaRepository.save(noticeFile);
            }
            
            // content 업데이트 (이미지 태그 교체 후)
            // 최종적으로 모든 data-temp-id 속성 제거 (파일 처리 후 한 번 더)
            if (content != null) {
                String beforeFinalCleanup = content;
                content = content.replaceAll("data-temp-id\\s*=\\s*\"[^\"]*\"", ""); // 큰따옴표
                content = content.replaceAll("data-temp-id\\s*=\\s*'[^']*'", ""); // 작은따옴표
                content = content.replaceAll("\\s+", " "); // 연속된 공백 정리
                if (!beforeFinalCleanup.equals(content)) {
                    log.info("최종 data-temp-id 속성 제거 완료 (update)");
                }
            }
            
            notice.setContent(content);
            noticeJpaRepository.save(notice);
            return 1;
        }).orElse(0);
    }

    /**
     * 공지 삭제
     */
    public int deleteNotice(Integer id) {
        if (id == null) {
            return 0;
        }

        // 첨부파일 삭제 (S3에서도 삭제)
        noticeFileJpaRepository.findAll().stream()
                .filter(f -> f.getNoticeId() != null && f.getNoticeId().equals(id))
                .forEach(f -> {
                    s3Service.deleteFile(f.getUuidFilename());
                    noticeFileJpaRepository.deleteById(f.getUuidFilename());
                });

        noticeJpaRepository.deleteById(id);
        return 1;
    }

    /**
     * 최근 공지 5개 조회 (메인 페이지용)
     */
    public List<NoticeFormDto> readRecentNotices() {
        Pageable pageable = PageRequest.of(0, 5);
        Page<Notice> page = noticeJpaRepository.findAllByOrderByIdDesc(pageable);

        return page.getContent().stream()
                .map(notice -> {
                    NoticeFormDto dto = new NoticeFormDto();
                    dto.setNoticeId(notice.getId());
                    dto.setCategory(notice.getCategory());
                    dto.setTitle(notice.getTitle());
                    dto.setContent(notice.getContent());
                    dto.setViews(notice.getViews());
                    dto.setCreatedTime(notice.getCreatedTime());
                    // 파일 정보는 관계를 통해 가져옴
                    dto.setOriginFilename(notice.getOriginFilename());
                    dto.setUuidFilename(notice.getUuidFilename());
                    return dto;
                })
                .collect(Collectors.toList());
    }
}