package com.green.university.controller;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.green.university.dto.NoticeFormDto;
import com.green.university.dto.NoticePageFormDto;
import com.green.university.handler.exception.CustomRestfullException;
import com.green.university.repository.model.Notice;
import com.green.university.service.NoticeService;
import com.green.university.utils.Define;

/**
 * @author 박성희
 * Notice REST Controller
 */
@RestController
@RequestMapping("/api/notice")
public class NoticeController {

    @Autowired
    private NoticeService noticeService;

    /**
     * 공지사항 목록 조회 (페이징)
     * GET /api/notice?page=0
     */
    @GetMapping("")
    public ResponseEntity<Map<String, Object>> getNoticeList(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type) {

        NoticePageFormDto dto = new NoticePageFormDto();
        dto.setPage(page);
        dto.setKeyword(keyword);
        dto.setType(type);

        Page<Notice> noticePage = noticeService.readNoticePage(dto);

        Map<String, Object> response = new HashMap<>();
        response.put("content", noticePage.getContent());
        response.put("currentPage", noticePage.getNumber());
        response.put("totalPages", noticePage.getTotalPages());
        response.put("totalElements", noticePage.getTotalElements());
        response.put("size", noticePage.getSize());
        response.put("hasNext", noticePage.hasNext());
        response.put("hasPrevious", noticePage.hasPrevious());

        return ResponseEntity.ok(response);
    }

    /**
     * 공지사항 상세 조회 (조회수 증가 없음)
     * GET /api/notice/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Notice> getNoticeDetail(@PathVariable Integer id) {
        Notice notice = noticeService.readByIdNotice(id);

        if (notice == null) {
            throw new CustomRestfullException("공지사항을 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
        }

        // 줄바꿈 처리
        if (notice.getContent() != null) {
            notice.setContent(notice.getContent().replace("\r\n", "<br>"));
        }

        return ResponseEntity.ok(notice);
    }

    /**
     * 조회수 증가 (별도 엔드포인트)
     * POST /api/notice/{id}/views
     */
    @PostMapping("/{id}/views")
    public ResponseEntity<Void> increaseViews(@PathVariable Integer id) {
        noticeService.increaseViews(id);
        return ResponseEntity.ok().build();
    }

    /**
     * 공지사항 등록
     * POST /api/notice
     */
    @PostMapping("")
    public ResponseEntity<Map<String, Object>> createNotice(@Validated NoticeFormDto noticeFormDto) {
        MultipartFile file = noticeFormDto.getFile();

        // 파일 업로드 처리
        if (file != null && !file.isEmpty()) {
            if (file.getSize() > Define.MAX_FILE_SIZE) {
                throw new CustomRestfullException("파일 크기는 20MB를 초과할 수 없습니다.", HttpStatus.BAD_REQUEST);
            }

            try {
                String saveDirectory = Define.UPLOAD_DIRECTORY;
                File dir = new File(saveDirectory);
                if (!dir.exists()) {
                    dir.mkdirs();
                }

                UUID uuid = UUID.randomUUID();
                String fileName = uuid + "_" + file.getOriginalFilename();
                String uploadPath = saveDirectory + File.separator + fileName;
                File destination = new File(uploadPath);
                file.transferTo(destination);

                noticeFormDto.setOriginFilename(file.getOriginalFilename());
                noticeFormDto.setUuidFilename(fileName);
            } catch (Exception e) {
                throw new CustomRestfullException("파일 업로드 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        noticeService.createNotice(noticeFormDto);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "공지사항이 등록되었습니다.");
        response.put("noticeId", noticeFormDto.getNoticeId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 공지사항 수정 페이지용 조회
     * GET /api/notice/{id}/edit
     */
    @GetMapping("/{id}/edit")
    public ResponseEntity<Notice> getNoticeForEdit(@PathVariable Integer id) {
        Notice notice = noticeService.readByIdNotice(id);

        if (notice == null) {
            throw new CustomRestfullException("공지사항을 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
        }

        return ResponseEntity.ok(notice);
    }

    /**
     * 공지사항 수정
     * PUT /api/notice/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, String>> updateNotice(
            @PathVariable Integer id,
            @RequestBody @Validated NoticeFormDto noticeFormDto) {

        noticeFormDto.setId(id);
        int result = noticeService.updateNotice(noticeFormDto);

        if (result == 0) {
            throw new CustomRestfullException("공지사항 수정에 실패했습니다.", HttpStatus.BAD_REQUEST);
        }

        Map<String, String> response = new HashMap<>();
        response.put("message", "공지사항이 수정되었습니다.");

        return ResponseEntity.ok(response);
    }

    /**
     * 공지사항 삭제
     * DELETE /api/notice/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteNotice(@PathVariable Integer id) {
        int result = noticeService.deleteNotice(id);

        if (result == 0) {
            throw new CustomRestfullException("공지사항 삭제에 실패했습니다.", HttpStatus.BAD_REQUEST);
        }

        Map<String, String> response = new HashMap<>();
        response.put("message", "공지사항이 삭제되었습니다.");

        return ResponseEntity.ok(response);
    }
}