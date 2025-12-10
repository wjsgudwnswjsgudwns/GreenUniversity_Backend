package com.green.university.controller;

import com.green.university.dto.*;
import com.green.university.dto.CounselingSlotResDto;
import com.green.university.dto.response.CounselingReservationResDto;

import com.green.university.dto.response.PrincipalDto;
import com.green.university.repository.model.Professor;
import com.green.university.service.CounselingService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/counseling")
@RequiredArgsConstructor
public class CounselingController {

    private final CounselingService counselingService;

    // ================== 학생용 ==================

    /**
     * 내가 속한 학과의 교수 목록 조회
     * GET /api/counseling/professors/my
     */
    @GetMapping("/professors/my")
    public List<Professor> getMyMajorProfessors(
            @AuthenticationPrincipal PrincipalDto principal
    ) {
        return counselingService.getMyMajorProfessors(principal);
    }

    /**
     * 특정 교수의 OPEN 슬롯 조회
     * GET /api/counseling/slots/open?professorId=1&from=2025-12-08&to=2025-12-31
     */
    @GetMapping("/slots/open")
    public List<CounselingSlotResDto> getOpenSlots(
            @RequestParam Integer professorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return counselingService.getOpenSlots(professorId, from, to);
    }

    /**
     * 슬롯 예약
     * POST /api/counseling/slots/{slotId}/reserve
     * body: { "memo": "상담 내용 메모" }
     */
    @PostMapping("/slots/{slotId}/reserve")
    @ResponseStatus(HttpStatus.CREATED)
    public CounselingReservationResDto reserveSlot(
            @AuthenticationPrincipal PrincipalDto principal,
            @PathVariable Long slotId,
            @RequestBody(required = false) ReserveSlotReqDto body
    ) {
        String memo = (body != null) ? body.getMemo() : null;
        return counselingService.reserveSlot(principal, slotId, memo);
    }

    /**
     * 내 예약 취소
     * DELETE /api/counseling/reservations/{reservationId}
     */
    @DeleteMapping("/reservations/{reservationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelReservation(
            @AuthenticationPrincipal PrincipalDto principal,
            @PathVariable Long reservationId
    ) {
        counselingService.cancelReservation(principal, reservationId);
    }

    /**
     * 내 상담 예약 목록 조회 (학생용)
     * GET /api/counseling/my-reservations?from=2025-12-08&to=2025-12-31
     */
    @GetMapping("/my-reservations")
    public List<CounselingReservationResDto> getMyReservations(
            @AuthenticationPrincipal PrincipalDto principal,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return counselingService.getMyReservations(principal, from, to);
    }

    // ================== 교수용 ==================
    /**
     * 내 상담 "예약" 목록 조회 (교수용)
     * GET /api/counseling/professor-reservations?from=2025-12-08&to=2025-12-31
     */
    @GetMapping("/professor-reservations")
    public List<CounselingReservationResDto> getProfessorReservations(
            @AuthenticationPrincipal PrincipalDto principal,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return counselingService.getProfessorReservations(principal, from, to);
    }

    /**
     * 내 상담 슬롯 목록 조회
     * GET /api/counseling/my-slots?from=2025-12-08&to=2025-12-31
     */
    @GetMapping("/my-slots")
    public List<CounselingSlotResDto> getMySlots(
            @AuthenticationPrincipal PrincipalDto principal,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return counselingService.getMySlots(principal, from, to);
    }

    /**
     * 단일 슬롯 생성 (1시간짜리)
     * POST /api/counseling/slots/single
     */
    @PostMapping("/slots/single")
    @ResponseStatus(HttpStatus.CREATED)
    public CounselingSlotResDto createSingleSlot(
            @AuthenticationPrincipal PrincipalDto principal,
            @RequestBody CreateSingleSlotReqDto dto
    ) {
        return counselingService.createSingleSlot(principal, dto);
    }



    /**
     * 특정 슬롯에 대한 예약 목록 조회
     * (슬롯의 교수 or ADMIN만 가능)
     * GET /api/counseling/slots/{slotId}/reservations
     */
    @GetMapping("/slots/{slotId}/reservations")
    public List<CounselingReservationResDto> getSlotReservations(
            @AuthenticationPrincipal PrincipalDto principal,
            @PathVariable Long slotId
    ) {
        return counselingService.getSlotReservations(principal, slotId);
    }


    // ================== 내부용 DTO ==================

    /**
     * 슬롯 예약 시 메모 받기용 간단 DTO
     */
    public static class ReserveSlotReqDto {
        private String memo;

        public String getMemo() {
            return memo;
        }

        public void setMemo(String memo) {
            this.memo = memo;
        }
    }
    @DeleteMapping("/slots/{slotId}")
    public ResponseEntity<Void> deleteSlot(
            @PathVariable Long slotId,
            @AuthenticationPrincipal PrincipalDto principal
    ) {
        counselingService.deleteSlot(slotId, principal);
        return ResponseEntity.noContent().build(); // 204
    }

    @GetMapping("/student/slots")
    public List<CounselingSlotResDto> getStudentSlotsForGrid(
            @AuthenticationPrincipal PrincipalDto principal,
            @RequestParam Integer professorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return counselingService.getStudentSlotsForGrid(principal, professorId, from, to);
    }

    @PostMapping("/professor/reservations/{id}/approve")
    public ResponseEntity<Void> approve(@PathVariable Long id, @AuthenticationPrincipal PrincipalDto principal,@RequestBody CreateMeetingReqDto reqDto) {
        counselingService.approveReservationByProfessor(principal, id, reqDto);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/professor/reservations/{id}/cancel")
    public ResponseEntity<Void> cancel(@PathVariable Long id, @AuthenticationPrincipal PrincipalDto principal) {
        counselingService.cancelReservationByProfessor(principal, id);
        return ResponseEntity.ok().build();
    }
}



