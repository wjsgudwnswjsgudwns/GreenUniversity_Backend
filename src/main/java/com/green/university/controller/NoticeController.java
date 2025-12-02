package com.green.university.controller;

import java.io.File;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;
// Model import removed because REST controllers return JSON instead of views
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.green.university.dto.NoticeFormDto;
import com.green.university.dto.NoticePageFormDto;
import com.green.university.handler.exception.CustomRestfullException;
import com.green.university.repository.model.Notice;
import com.green.university.service.NoticeService;
import com.green.university.utils.Define;

/**
 * 
 * @author 박성희 
 * Notice Controller
 *
 */
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/notice")
public class NoticeController {
	@Autowired
	NoticeService noticeService;

	/**
	 * 
	 * @return 공지사항 페이지
	 */
	@GetMapping("")
    public ResponseEntity<Map<String, Object>> notice(@RequestParam(defaultValue = "select") String crud) {
        NoticePageFormDto noticePageFormDto = new NoticePageFormDto();
        noticePageFormDto.setPage(0);
        List<Notice> noticeList = noticeService.readNotice(noticePageFormDto);
        Integer amount = noticeService.readNoticeAmount(noticePageFormDto);
        Map<String, Object> response = new HashMap<>();
        response.put("crud", crud);
        response.put("listCount", Math.ceil(amount / 10.0));
        response.put("noticeList", noticeList.isEmpty() ? null : noticeList);
        return ResponseEntity.ok(response);
    }

	/**
	 * 
	 * @return 공지사항 입력 기능
	 */
    @PostMapping("/write")
    public ResponseEntity<?> insertNotice(@Validated NoticeFormDto noticeFormDto) {
        MultipartFile file = noticeFormDto.getFile();
        if (file != null && !file.isEmpty()) {
            if (file.getSize() > Define.MAX_FILE_SIZE) {
                throw new CustomRestfullException("파일 크기는 20MB 이상 클 수 없습니다.", HttpStatus.BAD_REQUEST);
            }
            try {
                String saveDirectory = Define.UPLOAD_DIRECTORY;
                File dir = new File(saveDirectory);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                UUID uuid = UUID.randomUUID();
                String fileName = uuid + "_" + file.getOriginalFilename();
                String uploadPath = Define.UPLOAD_DIRECTORY + File.separator + fileName;
                File destination = new File(uploadPath);
                file.transferTo(destination);
                noticeFormDto.setOriginFilename(file.getOriginalFilename());
                noticeFormDto.setUuidFilename(fileName);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        noticeService.readNotice(noticeFormDto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

	/**
	 * 
	 * @return 공지사항 상세 조회 기능
	 */
    @GetMapping("/read")
    public ResponseEntity<Notice> selectByIdNotice(@RequestParam Integer id) {
        Notice notice = noticeService.readByIdNotice(id);
        if (notice != null && notice.getContent() != null) {
            notice.setContent(notice.getContent().replace("\r\n", "<br>"));
        }
        return ResponseEntity.ok(notice);
    }

	/**
	 * 공지사항 페이지 이동
	 */
    @GetMapping("/list/{page}")
    public ResponseEntity<Map<String, Object>> showNoticeListByPage(
            @RequestParam(defaultValue = "select") String crud,
            @PathVariable Integer page) {
        NoticePageFormDto noticePageFormDto = new NoticePageFormDto();
        noticePageFormDto.setPage((page - 1) * 10);
        Integer amount = noticeService.readNoticeAmount(noticePageFormDto);
        List<Notice> noticeList = noticeService.readNotice(noticePageFormDto);
        Map<String, Object> response = new HashMap<>();
        response.put("crud", crud);
        response.put("listCount", Math.ceil(amount / 10.0));
        response.put("noticeList", noticeList.isEmpty() ? null : noticeList);
        return ResponseEntity.ok(response);
    }

	/**
	 * 공지사항 검색 기능
	 */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> showNoticeByKeyword(NoticePageFormDto noticePageFormDto) {
        noticePageFormDto.setPage(0);
        List<Notice> noticeList = noticeService.readNoticeByKeyword(noticePageFormDto);
        Integer amount = noticeService.readNoticeAmount(noticePageFormDto);
        Map<String, Object> response = new HashMap<>();
        response.put("keyword", noticePageFormDto.getKeyword());
        response.put("crud", "selectKeyword");
        response.put("listCount", Math.ceil(amount / 10.0));
        response.put("noticeList", noticeList.isEmpty() ? null : noticeList);
        return ResponseEntity.ok(response);
    }

	/**
	 * 공지사항 검색 기능 (키워드 검색 페이징 처리)
	 */
    @GetMapping("/search/{page}")
    public ResponseEntity<Map<String, Object>> showNoticeByKeywordAndPage(
            NoticePageFormDto noticePageFormDto,
            @PathVariable Integer page,
            @RequestParam String keyword) {
        noticePageFormDto.setPage((page - 1) * 10);
        List<Notice> noticeList = noticeService.readNoticeByKeyword(noticePageFormDto);
        Integer amount = noticeService.readNoticeAmount(noticePageFormDto);
        Map<String, Object> response = new HashMap<>();
        response.put("keyword", noticePageFormDto.getKeyword());
        response.put("crud", "selectKeyword");
        response.put("listCount", Math.ceil(amount / 10.0));
        response.put("noticeList", noticeList.isEmpty() ? null : noticeList);
        return ResponseEntity.ok(response);
    }

	/**
	 * 
	 * @return 공지사항 수정 페이지
	 */
    @GetMapping("/update")
    public ResponseEntity<Notice> update(@RequestParam Integer id) {
        Notice notice = noticeService.readByIdNotice(id);
        return ResponseEntity.ok(notice);
    }

	/**
	 * 
	 * @return 공지사항 수정 기능
	 */
    @PutMapping("/update")
    public ResponseEntity<?> update(@Validated NoticeFormDto noticeFormDto) {
        noticeService.updateNotice(noticeFormDto);
        return ResponseEntity.ok().build();
    }

	/**
	 * 
	 * @return 공지사항 삭제 조회 기능
	 */
    @GetMapping("/delete")
    public ResponseEntity<?> delete(@RequestParam Integer id) {
        noticeService.deleteNotice(id);
        return ResponseEntity.ok().build();
    }
}
