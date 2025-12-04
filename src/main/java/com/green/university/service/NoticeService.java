package com.green.university.service;

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

/**
 * @author 박성희
 * notice 관련 서비스
 */
@Service
public class NoticeService {

    @Autowired
    private NoticeJpaRepository noticeJpaRepository;

    @Autowired
    private NoticeFileJpaRepository noticeFileJpaRepository;

    /**
     * 새로운 공지를 등록한다.
     */
    public void createNotice(@Validated NoticeFormDto noticeFormDto) {
        Notice notice = new Notice();
        notice.setCategory(noticeFormDto.getCategory());
        notice.setTitle(noticeFormDto.getTitle());
        notice.setContent(noticeFormDto.getContent());
        notice.setViews(0);
        notice.setCreatedTime(new Timestamp(System.currentTimeMillis()));

        Notice savedNotice = noticeJpaRepository.save(notice);

        // 첨부파일 정보가 있는 경우
        if (noticeFormDto.getOriginFilename() != null && noticeFormDto.getUuidFilename() != null) {
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
        return noticeJpaRepository.findById(id).orElse(null);
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
            notice.setContent(noticeFormDto.getContent());
            noticeJpaRepository.save(notice);

            // 파일 정보 업데이트 (새로 업로드된 경우)
            if (noticeFormDto.getOriginFilename() != null && noticeFormDto.getUuidFilename() != null) {
                // 기존 파일 삭제
                noticeFileJpaRepository.findAll().stream()
                        .filter(f -> f.getNoticeId() != null && f.getNoticeId().equals(notice.getId()))
                        .forEach(f -> noticeFileJpaRepository.deleteById(f.getUuidFilename()));

                // 새 파일 저장
                NoticeFile noticeFile = new NoticeFile();
                noticeFile.setUuidFilename(noticeFormDto.getUuidFilename());
                noticeFile.setOriginFilename(noticeFormDto.getOriginFilename());
                noticeFile.setNotice(notice);
                noticeFile.setNoticeId(notice.getId());
                noticeFileJpaRepository.save(noticeFile);
            }
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

        // 첨부파일 삭제
        noticeFileJpaRepository.findAll().stream()
                .filter(f -> f.getNoticeId() != null && f.getNoticeId().equals(id))
                .forEach(f -> noticeFileJpaRepository.deleteById(f.getUuidFilename()));

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