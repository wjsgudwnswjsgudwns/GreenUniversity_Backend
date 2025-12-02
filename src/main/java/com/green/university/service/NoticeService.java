package com.green.university.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
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
 * 
 * @author 박성희 notice 관련 서비스
 *
 */

@Service
public class NoticeService {

    @Autowired
    private NoticeJpaRepository noticeJpaRepository;

    @Autowired
    private NoticeFileJpaRepository noticeFileJpaRepository;

    /**
     * 새로운 공지를 등록한다. 공지 본문과 첨부파일 정보가 DTO에 포함되어 있으면 함께 저장한다.
     *
     * @param noticeFormDto 등록할 공지 정보
     */
    public void readNotice(@Validated NoticeFormDto noticeFormDto) {
        // DTO를 엔티티로 변환하여 저장
        Notice notice = new Notice();
        notice.setCategory(noticeFormDto.getCategory());
        notice.setTitle(noticeFormDto.getTitle());
        notice.setContent(noticeFormDto.getContent());
        // 조회수는 최초 0으로 설정하거나 DTO 값이 있으면 사용
        notice.setViews(noticeFormDto.getViews() != null ? noticeFormDto.getViews() : 0);
        // 생성일시가 전달되지 않았다면 현재 시각 저장
        Timestamp now = noticeFormDto.getCreatedTime();
        notice.setCreatedTime(now != null ? now : new Timestamp(System.currentTimeMillis()));
        notice.setUuidFilename(noticeFormDto.getUuidFilename());
        notice.setOriginFilename(noticeFormDto.getOriginFilename());
        Notice savedNotice = noticeJpaRepository.save(notice);
        // 첨부파일 정보가 있는 경우 NoticeFile 엔티티 저장
        if (noticeFormDto.getOriginFilename() != null && noticeFormDto.getUuidFilename() != null) {
            NoticeFile noticeFile = new NoticeFile();
            noticeFile.setUuidFilename(noticeFormDto.getUuidFilename());
            noticeFile.setOriginFilename(noticeFormDto.getOriginFilename());
            noticeFile.setNotice(savedNotice);
            noticeFile.setNoticeId(savedNotice.getId());
            noticeFileJpaRepository.save(noticeFile);
        }
        // 저장된 공지의 ID를 DTO에 반영
        noticeFormDto.setNoticeId(savedNotice.getId());
    }

    /**
     * 공지 조회 서비스. 키워드가 없으면 전체 공지를 반환하고, 검색 유형과 키워드가 있으면 해당 조건에 맞는 공지를 반환한다.
     *
     * @param noticePageFormDto 검색 및 페이징 정보
     * @return 조회된 공지 목록
     */
    public List<Notice> readNotice(NoticePageFormDto noticePageFormDto) {
        String keyword = noticePageFormDto.getKeyword();
        String type = noticePageFormDto.getType();
        // 키워드가 없으면 전체 반환
        if (keyword == null || keyword.isEmpty()) {
            return noticeJpaRepository.findAll();
        }
        // 키워드가 있고 type이 "title"이면 제목으로 검색
        if ("title".equals(type)) {
            return noticeJpaRepository.findByTitleContainingIgnoreCase(keyword);
        }
        // 그 외에는 본문으로 검색
        return noticeJpaRepository.findByContentContainingIgnoreCase(keyword);
    }

	/**
	 * 
	 * @param noticePageFormDto
	 * @return 공지 갯수 확인 서비스
	 */
    public Integer readNoticeAmount(NoticePageFormDto noticePageFormDto) {
        // 검색 조건에 맞는 공지 목록의 크기를 반환한다.
        List<Notice> notices = readNotice(noticePageFormDto);
        return notices != null ? notices.size() : 0;
    }

	/**
	 * 공지 검색 서비스
	 */
    public List<Notice> readNoticeByKeyword(NoticePageFormDto noticePageFormDto) {
        // readNotice 메서드를 재사용하여 키워드 검색 결과를 반환한다.
        return readNotice(noticePageFormDto);
    }

	/**
	 * 공지 상세 조회 서비스
	 */
    public Notice readByIdNotice(Integer id) {
        // ID로 공지를 조회하고 조회수를 1 증가시킨 후 저장한다.
        return noticeJpaRepository.findById(id).map(notice -> {
            Integer currentViews = notice.getViews();
            notice.setViews(currentViews == null ? 1 : currentViews + 1);
            // 저장된 객체를 반환하여 반영
            return noticeJpaRepository.save(notice);
        }).orElse(null);
    }

	/**
	 * 공지 수정 서비스
	 */
    public int updateNotice(NoticeFormDto noticeFormDto) {
        // 공지 ID가 존재하는 경우에만 수정 처리한다.
        if (noticeFormDto.getId() == null) {
            return 0;
        }
        return noticeJpaRepository.findById(noticeFormDto.getId()).map(notice -> {
            // 공지 내용 업데이트
            notice.setCategory(noticeFormDto.getCategory());
            notice.setTitle(noticeFormDto.getTitle());
            notice.setContent(noticeFormDto.getContent());
            notice.setOriginFilename(noticeFormDto.getOriginFilename());
            notice.setUuidFilename(noticeFormDto.getUuidFilename());
            noticeJpaRepository.save(notice);
            // 파일 정보가 있다면 NoticeFile 업데이트 또는 추가
            if (noticeFormDto.getOriginFilename() != null && noticeFormDto.getUuidFilename() != null) {
                NoticeFile noticeFile = noticeFileJpaRepository.findById(noticeFormDto.getUuidFilename())
                    .orElse(new NoticeFile());
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
	 * 공지 삭제 서비스
	 */
    public int deleteNotice(Integer id) {
        if (id == null) {
            return 0;
        }
        // 먼저 첨부파일 삭제
        noticeFileJpaRepository.findAll().stream()
            .filter(f -> f.getNoticeId() != null && f.getNoticeId().equals(id))
            .forEach(f -> noticeFileJpaRepository.deleteById(f.getUuidFilename()));
        noticeJpaRepository.deleteById(id);
        return 1;
    }
	
	/**
	 * 최근 글 5개 조회
	 */
    public List<NoticeFormDto> readCurrentNotice() {
        // 생성일시가 null이 아닌 공지들을 최신순으로 5개 추출
        List<Notice> all = noticeJpaRepository.findAll();
        // 정렬 후 상위 5개만 DTO 변환하여 반환
        return all.stream()
            .sorted((a, b) -> {
                Timestamp at = a.getCreatedTime();
                Timestamp bt = b.getCreatedTime();
                if (at == null && bt == null) return 0;
                if (at == null) return 1;
                if (bt == null) return -1;
                return bt.compareTo(at);
            })
            .limit(5)
            .map(notice -> {
                NoticeFormDto dto = new NoticeFormDto();
                dto.setNoticeId(notice.getId());
                dto.setCategory(notice.getCategory());
                dto.setTitle(notice.getTitle());
                dto.setContent(notice.getContent());
                dto.setViews(notice.getViews());
                dto.setCreatedTime(notice.getCreatedTime());
                dto.setOriginFilename(notice.getOriginFilename());
                dto.setUuidFilename(notice.getUuidFilename());
                return dto;
            })
            .collect(Collectors.toList());
    }
	
}
