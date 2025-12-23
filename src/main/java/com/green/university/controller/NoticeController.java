package com.green.university.controller;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;

import com.green.university.dto.NoticeFormDto;
import com.green.university.dto.NoticePageFormDto;
import com.green.university.handler.exception.CustomRestfullException;
import com.green.university.repository.NoticeFileJpaRepository;
import com.green.university.repository.model.Notice;
import com.green.university.repository.model.NoticeFile;
import com.green.university.service.NoticeService;
import com.green.university.service.S3Service;
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

    @Autowired
    private NoticeFileJpaRepository noticeFileJpaRepository;

    @Autowired
    private S3Service s3Service;

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

        // 목록 조회 시 files를 null로 설정하여 순환 참조 방지
        noticePage.getContent().forEach(notice -> notice.setFiles(null));

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
        MultipartFile[] files = noticeFormDto.getFiles();
        MultipartFile file = noticeFormDto.getFile(); // 하위 호환성

        // 파일 크기 검증만 수행 (실제 업로드는 NoticeService에서 처리)
        if (files != null && files.length > 0) {
            for (MultipartFile uploadFile : files) {
                if (uploadFile != null && !uploadFile.isEmpty()) {
                    if (uploadFile.getSize() > Define.MAX_FILE_SIZE) {
                        throw new CustomRestfullException(
                                "파일 크기는 20MB를 초과할 수 없습니다: " + uploadFile.getOriginalFilename(),
                                HttpStatus.BAD_REQUEST);
                    }
                }
            }
        } else if (file != null && !file.isEmpty()) {
            if (file.getSize() > Define.MAX_FILE_SIZE) {
                throw new CustomRestfullException("파일 크기는 20MB를 초과할 수 없습니다.", HttpStatus.BAD_REQUEST);
            }
        }

        // NoticeService에서 공지사항 생성 및 파일 업로드 처리
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
            @Validated NoticeFormDto noticeFormDto) {

        MultipartFile[] files = noticeFormDto.getFiles();
        MultipartFile file = noticeFormDto.getFile(); // 하위 호환성

        // 파일 크기 검증만 수행 (실제 업로드는 NoticeService에서 처리)
        if (files != null && files.length > 0) {
            for (MultipartFile uploadFile : files) {
                if (uploadFile != null && !uploadFile.isEmpty()) {
                    if (uploadFile.getSize() > Define.MAX_FILE_SIZE) {
                        throw new CustomRestfullException(
                                "파일 크기는 20MB를 초과할 수 없습니다: " + uploadFile.getOriginalFilename(),
                                HttpStatus.BAD_REQUEST);
                    }
                }
            }
        } else if (file != null && !file.isEmpty()) {
            if (file.getSize() > Define.MAX_FILE_SIZE) {
                throw new CustomRestfullException("파일 크기는 20MB를 초과할 수 없습니다.", HttpStatus.BAD_REQUEST);
            }
        }

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

    /**
     * 공지사항 첨부파일 목록 조회
     * GET /api/notice/{id}/files
     */
    @GetMapping("/{id}/files")
    public ResponseEntity<List<NoticeFile>> getNoticeFiles(@PathVariable Integer id) {
        List<NoticeFile> files = noticeFileJpaRepository.findByNoticeId(id);
        return ResponseEntity.ok(files);
    }

    /**
     * 파일 다운로드
     * GET /api/notice/file/{uuidFilename}
     * GET /api/notice/file/public/{uuidFilename} (하위 호환성)
     */
    @GetMapping("/file/**")
    public ResponseEntity<Resource> downloadFile(HttpServletRequest request) {
        try {
            // 요청 경로에서 파일명 추출 (/api/notice/file/ 이후 부분)
            String requestPath = request.getRequestURI();
            String pathPrefix = "/api/notice/file/";
            String fileKey = requestPath.substring(requestPath.indexOf(pathPrefix) + pathPrefix.length());
            
            // URL 디코딩 (한글 파일명 지원)
            fileKey = java.net.URLDecoder.decode(fileKey, StandardCharsets.UTF_8.name());
            
            // DB에서 파일 찾기 (public/ 포함 여부와 관계없이)
            NoticeFile noticeFile = noticeFileJpaRepository.findById(fileKey)
                    .orElse(null);
            
            // fileKey에 public/이 없으면 추가해서 다시 시도
            if (noticeFile == null && !fileKey.startsWith("public/")) {
                noticeFile = noticeFileJpaRepository.findById("public/" + fileKey)
                        .orElse(null);
                if (noticeFile != null) {
                    fileKey = "public/" + fileKey;
                }
            }
            
            // fileKey에 public/이 있으면 제거해서 다시 시도
            if (noticeFile == null && fileKey.startsWith("public/")) {
                String keyWithoutPublic = fileKey.substring(7); // "public/".length()
                noticeFile = noticeFileJpaRepository.findById(keyWithoutPublic)
                        .orElse(null);
                if (noticeFile != null) {
                    fileKey = keyWithoutPublic;
                }
            }
            
            if (noticeFile == null) {
                throw new CustomRestfullException("파일을 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
            }

            // S3에서 파일 다운로드 (DB에 저장된 실제 경로 사용)
            String s3Key = noticeFile.getUuidFilename();
            InputStream inputStream = s3Service.downloadFile(s3Key);
            InputStreamResource resource = new InputStreamResource(inputStream);

            // 파일명 인코딩 (한글 파일명 지원)
            String encodedFilename = URLEncoder.encode(noticeFile.getOriginFilename(), StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFilename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (IOException e) {
            throw new CustomRestfullException("파일 다운로드 중 오류가 발생했습니다: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}