package com.green.university.service;

import com.green.university.dto.*;
import com.green.university.dto.response.CounselingReservationResDto;
import com.green.university.dto.response.MeetingSimpleResDto;
import com.green.university.dto.response.PrincipalDto;
import com.green.university.enums.CounselingReservationStatus;
import com.green.university.enums.CounselingSlotStatus;
import com.green.university.handler.exception.CustomRestfullException;
import com.green.university.repository.*;
import com.green.university.repository.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CounselingService {

    @Autowired
    private CounselingSlotJpaRepository slotRepo;

    @Autowired
    private CounselingReservationJpaRepository reservationRepo;

    @Autowired
    private ProfessorJpaRepository professorRepo;

    @Autowired
    private StudentJpaRepository studentRepo;

    @Autowired
    private SubjectJpaRepository subjectRepo; // 다른 메서드에서 사용할 가능성 고려해서 유지

    @Autowired
    private MeetingService meetingService;

    // ============= 공통 유틸/검증 =============



    private boolean isPastSlot(CounselingSlot slot) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = slot.getEndAt().toLocalDateTime();
        return end.isBefore(now);
    }

    private String getUpperRole(PrincipalDto principal) {
        if (principal == null) return null;
        String role = principal.getUserRole();
        return role != null ? role.toUpperCase() : null;
    }

    private void validateRole(PrincipalDto principal, String required, String message) {
        String role = getUpperRole(principal);
        if (principal == null || role == null || !required.equals(role)) {
            throw new CustomRestfullException(message, HttpStatus.FORBIDDEN);
        }
    }

    private void validateStudent(PrincipalDto principal) {
        validateRole(principal, "STUDENT", "학생만 이용 가능합니다.");
    }

    private void validateProfessor(PrincipalDto principal) {
        validateRole(principal, "PROFESSOR", "교수만 이용 가능합니다.");
    }

    private void validateSlotOwnerOrAdmin(PrincipalDto principal, CounselingSlot slot) {
        if (principal == null) {
            throw new CustomRestfullException("권한 없음", HttpStatus.FORBIDDEN);
        }

        String role = getUpperRole(principal);
        boolean isOwnerProfessor =
                "PROFESSOR".equals(role) &&
                        slot.getProfessor().getId().equals(principal.getId());
        boolean isAdmin = "ADMIN".equals(role);

        if (!isOwnerProfessor && !isAdmin) {
            throw new CustomRestfullException("권한 없음", HttpStatus.FORBIDDEN);
        }
    }

    private void validateDateRange(LocalDate from, LocalDate to, String label) {
        if (from == null || to == null) {
            throw new CustomRestfullException(label + " 날짜가 필요합니다.", HttpStatus.BAD_REQUEST);
        }
        if (to.isBefore(from)) {
            throw new CustomRestfullException(label + " 종료 날짜가 시작 날짜보다 빠릅니다.", HttpStatus.BAD_REQUEST);
        }
    }

    private void validateSlotTime(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            throw new CustomRestfullException("시간이 필요합니다.", HttpStatus.BAD_REQUEST);
        }
        if (!end.isAfter(start)) {
            throw new CustomRestfullException("종료 시간이 시작 시간보다 빨라야 합니다.", HttpStatus.BAD_REQUEST);
        }
        if (!Duration.between(start, end).equals(Duration.ofHours(1))) {
            throw new CustomRestfullException("상담 시간은 1시간만 가능합니다.", HttpStatus.BAD_REQUEST);
        }
    }

    private Timestamp toStartTs(LocalDate date) {
        return Timestamp.valueOf(date.atStartOfDay());
    }

    private Timestamp toEndTs(LocalDate date) {
        return Timestamp.valueOf(date.atTime(LocalTime.MAX));
    }

    private boolean hasOverlapSlot(Integer professorId, LocalDateTime start, LocalDateTime end) {
        return slotRepo.existsByProfessor_IdAndStartAtLessThanAndEndAtGreaterThan(
                professorId,
                Timestamp.valueOf(end),
                Timestamp.valueOf(start)
        );
    }

    private CounselingSlot findSlot(Long id) {
        return slotRepo.findById(id)
                .orElseThrow(() -> new CustomRestfullException("상담 슬롯 없음", HttpStatus.NOT_FOUND));
    }

    private CounselingReservation findReservation(Long id) {
        return reservationRepo.findById(id)
                .orElseThrow(() -> new CustomRestfullException("예약 없음", HttpStatus.NOT_FOUND));
    }

    private Professor findProfessor(Integer id) {
        return professorRepo.findById(id)
                .orElseThrow(() -> new CustomRestfullException("교수 없음", HttpStatus.NOT_FOUND));
    }

    private Student findStudent(Integer id) {
        return studentRepo.findById(id)
                .orElseThrow(() -> new CustomRestfullException("학생 없음", HttpStatus.NOT_FOUND));
    }

    private List<CounselingSlotResDto> mapSlotsToDtos(List<CounselingSlot> slots) {
        return slots.stream()
                .map(this::toSlotDto)
                .collect(Collectors.toList());
    }

    private List<CounselingReservationResDto> mapReservationsToDtos(List<CounselingReservation> list) {
        return list.stream()
                .map(this::toReservationDto)
                .collect(Collectors.toList());
    }

    private List<CounselingSlotResDto> findSlotsByProfessorAndRange(
            Integer professorId,
            LocalDate from,
            LocalDate to,
            String label
    ) {
        validateDateRange(from, to, label);
        Timestamp fromTs = toStartTs(from);
        Timestamp toTs = toEndTs(to);

        List<CounselingSlot> slots =
                slotRepo.findByProfessor_IdAndStartAtBetweenOrderByStartAt(professorId, fromTs, toTs);

        return mapSlotsToDtos(slots);
    }


    // ============= DTO 변환 =============

    private CounselingSlotResDto toSlotDto(CounselingSlot slot) {
        CounselingSlotResDto dto = new CounselingSlotResDto();
        dto.setSlotId(slot.getId());
        dto.setProfessorId(slot.getProfessor().getId());
        dto.setProfessorName(slot.getProfessor().getName());
        dto.setStartAt(slot.getStartAt().toLocalDateTime());
        dto.setEndAt(slot.getEndAt().toLocalDateTime());
        dto.setStatus(slot.getStatus());
        dto.setMeetingId(slot.getMeetingId() != null ? slot.getMeetingId() : null);
        return dto;
    }

    private CounselingReservationResDto toReservationDto(CounselingReservation r) {
        CounselingReservationResDto dto = new CounselingReservationResDto();
        dto.setReservationId(r.getId());
        dto.setSlotId(r.getSlot().getId());
        dto.setStudentId(r.getStudent().getId());
        dto.setStudentName(r.getStudent().getName());
        dto.setStatus(r.getStatus());
        dto.setStudentMemo(r.getStudentMemo());
        dto.setMeetingId(r.getMeetingId());

        if (r.getSlot() != null) {
            if (r.getSlot().getStartAt() != null) {
                dto.setSlotStartAt(r.getSlot().getStartAt().toLocalDateTime());
            }
            if (r.getSlot().getEndAt() != null) {
                dto.setSlotEndAt(r.getSlot().getEndAt().toLocalDateTime());
            }
            if (r.getSlot().getProfessor() != null) {
                dto.setProfessorName(r.getSlot().getProfessor().getName());
            }
        }

        if (r.getCreatedAt() != null) {
            dto.setCreatedAt(r.getCreatedAt().toLocalDateTime());
        }
        if (r.getCanceledAt() != null) {
            dto.setCanceledAt(r.getCanceledAt().toLocalDateTime());
        }
        return dto;
    }


    // ============= 슬롯 삭제(공통) =============

    @Transactional
    public void deleteSlot(Long slotId, PrincipalDto principal) {
        CounselingSlot slot = findSlot(slotId);
        validateSlotOwnerOrAdmin(principal, slot);

        if (isPastSlot(slot)) {
            throw new CustomRestfullException(
                    "이미 지난 상담 시간은 수정/삭제할 수 없습니다.",
                    HttpStatus.BAD_REQUEST
            );
        }

        boolean hasAnyReservation = reservationRepo.existsBySlot_Id(slotId);
        if (hasAnyReservation) {
            throw new CustomRestfullException(
                    "예약 이력이 있는 시간은 삭제할 수 없습니다. 먼저 예약을 취소하세요.",
                    HttpStatus.BAD_REQUEST
            );
        }

        slotRepo.delete(slot);
    }

    @Transactional(readOnly = true)
    public List<CounselingSlotResDto> getSlotsForGrid(
            PrincipalDto principal,
            Integer professorId,   // 학생: 필수, 교수: 생략 가능
            LocalDate from,
            LocalDate to
    ) {
        validateDateRange(from, to, "슬롯 조회");

        Integer targetProfessorId = professorId;

        // 교수 본인이 자기 시간표 보는 경우: professorId 안 넘기면 principal 기준
        if (targetProfessorId == null) {
            String role = getUpperRole(principal);
            if (!"PROFESSOR".equals(role)) {
                throw new CustomRestfullException("교수 정보가 필요합니다.", HttpStatus.BAD_REQUEST);
            }
            targetProfessorId = principal.getId();
        }

        return findSlotsByProfessorAndRange(targetProfessorId, from, to, "슬롯 조회");
    }
    // ============= 학생 기능 =============

    @Transactional(readOnly = true)
    public List<Professor> getMyMajorProfessors(PrincipalDto principal) {
        validateStudent(principal);

        Student s = findStudent(principal.getId());
        return professorRepo.findByDepartment_Id(s.getDepartment().getId());
    }

    @Transactional(readOnly = true)
    public List<CounselingSlotResDto> getOpenSlots(Integer professorId, LocalDate from, LocalDate to) {
        // 상태까지 포함된 레포 메서드를 쓰지 않고, 서비스 레벨에서 필터
        validateDateRange(from, to, "슬롯 조회");

        Timestamp fromTs = toStartTs(from);
        Timestamp toTs = toEndTs(to);

        List<CounselingSlot> slots =
                slotRepo.findByProfessor_IdAndStartAtBetweenOrderByStartAt(professorId, fromTs, toTs);

        return slots.stream()
                .filter(slot -> slot.getStatus() == CounselingSlotStatus.OPEN)
                .map(this::toSlotDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public CounselingReservationResDto reserveSlot(PrincipalDto principal, Long slotId, String memo) {
        validateStudent(principal);

        CounselingSlot slot = findSlot(slotId);
        if (slot.getStatus() != CounselingSlotStatus.OPEN) {
            throw new CustomRestfullException("이미 예약된 슬롯", HttpStatus.BAD_REQUEST);
        }

        Student student = findStudent(principal.getId());

        LocalDateTime start = slot.getStartAt().toLocalDateTime();
        LocalDateTime end = slot.getEndAt().toLocalDateTime();

        boolean overlaps = reservationRepo
                .existsByStudent_IdAndStatusAndSlot_StartAtLessThanAndSlot_EndAtGreaterThan(
                        student.getId(),
                        CounselingReservationStatus.RESERVED,
                        Timestamp.valueOf(end),
                        Timestamp.valueOf(start)
                );

        if (overlaps) {
            throw new CustomRestfullException("해당 시간대에 이미 예약 있음", HttpStatus.CONFLICT);
        }

        LocalDateTime now = LocalDateTime.now();

        CounselingReservation r = new CounselingReservation();
        r.setSlot(slot);
        r.setStudent(student);
        r.setStatus(CounselingReservationStatus.RESERVED);
        r.setStudentMemo(memo);
        r.setCreatedAt(Timestamp.valueOf(now));
        r.setUpdatedAt(Timestamp.valueOf(now));

        CounselingReservation saved = reservationRepo.save(r);

        slot.setStatus(CounselingSlotStatus.RESERVED);
        slot.setUpdatedAt(Timestamp.valueOf(now));
        slotRepo.save(slot);

        return toReservationDto(saved);
    }

    @Transactional
    public void cancelReservation(PrincipalDto principal, Long reservationId) {
        validateStudent(principal);

        CounselingReservation r = findReservation(reservationId);
        if (!r.getStudent().getId().equals(principal.getId())) {
            throw new CustomRestfullException("본인 예약만 취소 가능", HttpStatus.FORBIDDEN);
        }

        LocalDateTime now = LocalDateTime.now();

        // 교수님이 이미 승인해서 회의가 연결된 예약은 학생 취소 불가
        if (r.getMeetingId() != null ||
                (r.getSlot() != null && r.getSlot().getMeetingId() != null)) {
            throw new CustomRestfullException("이미 교수님이 승인한 예약은 직접 취소할 수 없습니다.", HttpStatus.BAD_REQUEST);
        }

        if (r.getSlot().getStartAt().toLocalDateTime().isBefore(now)) {
            throw new CustomRestfullException("이미 지난 상담은 취소 불가", HttpStatus.BAD_REQUEST);
        }

        CounselingSlot slot = r.getSlot();
        Long slotId = slot.getId();

        // 1) 예약 삭제
        reservationRepo.delete(r);

        // 2) 이 슬롯에 아직 RESERVED 상태 예약이 남아있는지 체크
        boolean stillReserved = reservationRepo
                .existsBySlot_IdAndStatus(slotId, CounselingReservationStatus.RESERVED);

        // 3) 하나도 없으면 슬롯 상태를 OPEN 으로 되돌림
        if (!stillReserved) {
            slot.setStatus(CounselingSlotStatus.OPEN);
            slot.setUpdatedAt(Timestamp.valueOf(now));
            slotRepo.save(slot);
        }
    }


// ============= 학생 전용: 교수 슬롯 조회 =============

    @Transactional(readOnly = true)
    public List<CounselingSlotResDto> getStudentSlotsForGrid(
            PrincipalDto principal,
            Integer professorId,
            LocalDate from,
            LocalDate to
    ) {
        // 학생만 사용
        validateStudent(principal);

        if (professorId == null) {
            throw new CustomRestfullException("교수 정보가 필요합니다.", HttpStatus.BAD_REQUEST);
        }

        return findSlotsByProfessorAndRange(professorId, from, to, "학생용 슬롯 조회");
    }

    // ============= 교수 기능 =============

    @Transactional(readOnly = true)
    public List<CounselingSlotResDto> getMySlots(PrincipalDto principal, LocalDate from, LocalDate to) {
        validateProfessor(principal);
        return findSlotsByProfessorAndRange(principal.getId(), from, to, "내 슬롯 조회");
    }

    @Transactional
    public CounselingSlotResDto createSingleSlot(PrincipalDto principal, CreateSingleSlotReqDto dto) {
        validateProfessor(principal);

        validateSlotTime(dto.getStartAt(), dto.getEndAt());

        Integer professorId = principal.getId();
        if (hasOverlapSlot(professorId, dto.getStartAt(), dto.getEndAt())) {
            throw new CustomRestfullException("이미 다른 상담과 겹칩니다.", HttpStatus.CONFLICT);
        }

        LocalDateTime now = LocalDateTime.now();
        Professor professor = findProfessor(professorId);

        CounselingSlot slot = new CounselingSlot();
        slot.setProfessor(professor);
        slot.setStartAt(Timestamp.valueOf(dto.getStartAt()));
        slot.setEndAt(Timestamp.valueOf(dto.getEndAt()));
        slot.setStatus(CounselingSlotStatus.OPEN);
        slot.setCreatedAt(Timestamp.valueOf(now));
        slot.setUpdatedAt(Timestamp.valueOf(now));

        return toSlotDto(slotRepo.save(slot));
    }

    @Transactional
    public List<CounselingSlotResDto> createWeeklyPattern(
            PrincipalDto principal,
            CreateWeeklyPatternReqDto dto
    ) {
        validateProfessor(principal);

        LocalDate weekStart = dto.getWeekStartDate();
        LocalDate repeatEnd = dto.getRepeatEndDate();
        validateDateRange(weekStart, repeatEnd, "패턴 반복");

        List<CounselingSlotResDto> results = new ArrayList<>();
        Professor professor = findProfessor(principal.getId());
        LocalDateTime now = LocalDateTime.now();

        LocalDate cursor = weekStart;
        while (!cursor.isAfter(repeatEnd)) {
            for (CreateWeeklyPatternReqDto.WeeklyPatternItem item : dto.getItems()) {
                LocalDate targetDate =
                        cursor.with(TemporalAdjusters.nextOrSame(item.getDayOfWeek()));

                if (targetDate.isBefore(cursor) || targetDate.isAfter(cursor.plusDays(6))) {
                    continue;
                }
                if (targetDate.isAfter(repeatEnd)) {
                    continue;
                }

                LocalDateTime start = LocalDateTime.of(targetDate, item.getStartTime());
                LocalDateTime end = LocalDateTime.of(targetDate, item.getEndTime());

                validateSlotTime(start, end);

                if (hasOverlapSlot(principal.getId(), start, end)) {
                    continue;
                }

                CounselingSlot slot = new CounselingSlot();
                slot.setProfessor(professor);
                slot.setStartAt(Timestamp.valueOf(start));
                slot.setEndAt(Timestamp.valueOf(end));
                slot.setStatus(CounselingSlotStatus.OPEN);
                slot.setCreatedAt(Timestamp.valueOf(now));
                slot.setUpdatedAt(Timestamp.valueOf(now));

                results.add(toSlotDto(slotRepo.save(slot)));
            }

            cursor = cursor.plusWeeks(1);
        }

        if (results.isEmpty()) {
            throw new CustomRestfullException("생성된 슬롯이 없습니다.", HttpStatus.BAD_REQUEST);
        }

        return results;
    }

    @Transactional(readOnly = true)
    public List<CounselingReservationResDto> getProfessorReservations(
            PrincipalDto principal,
            LocalDate from,
            LocalDate to
    ) {
        validateProfessor(principal);
        validateDateRange(from, to, "교수 예약 조회");

        // 🔥 과거 필터링 제거: clampFromToday 사용 안 함
        Timestamp fromTs = toStartTs(from);
        Timestamp toTs = toEndTs(to);

        Integer professorId = principal.getId();

        List<CounselingReservation> reservations =
                reservationRepo.findBySlot_Professor_IdAndStatusNotAndSlot_StartAtBetweenOrderBySlot_StartAt(
                        professorId,
                        CounselingReservationStatus.CANCELED,
                        fromTs,
                        toTs
                );

        return mapReservationsToDtos(reservations);
    }




    @Transactional(readOnly = true)
    public List<CounselingReservationResDto> getSlotReservations(
            PrincipalDto principal,
            Long slotId
    ) {
        CounselingSlot slot = findSlot(slotId);
        validateSlotOwnerOrAdmin(principal, slot);

        List<CounselingReservation> reservations = reservationRepo.findBySlot_Id(slotId);
        return mapReservationsToDtos(reservations);
    }

    @Transactional(readOnly = true)
    public List<CounselingReservationResDto> getMyReservations(
            PrincipalDto principal,
            LocalDate from,
            LocalDate to
    ) {
        validateStudent(principal);
        validateDateRange(from, to, "예약 조회");

        // 🔥 과거 필터링 제거: clampFromToday 사용 안 함
        Timestamp fromTs = toStartTs(from);
        Timestamp toTs = toEndTs(to);

        Integer studentId = principal.getId();

        List<CounselingReservation> reservations =
                reservationRepo.findByStudent_IdAndStatusNotAndSlot_StartAtBetweenOrderBySlot_StartAt(
                        studentId,
                        CounselingReservationStatus.CANCELED,
                        fromTs,
                        toTs
                );

        return mapReservationsToDtos(reservations);
    }


    /**
     * 교수: 예약 승인
     * - 본인 슬롯인지 확인
     * - 이미 지난 슬롯이면 승인 불가
     * - 예약 상태가 RESERVED 인 경우 그대로 두고,
     *   WebRTC SCHEDULED Meeting 생성 후 slot.meetingId 세팅
     */
    /**
     * 교수: 예약 승인
     */
    @Transactional
    public void approveReservationByProfessor(
            PrincipalDto principal,
            Long reservationId,
            CreateMeetingReqDto approveReq
    ) {
        validateProfessor(principal);

        CounselingReservation r = findReservation(reservationId);
        CounselingSlot slot = r.getSlot();

        validateSlotOwnerOrAdmin(principal, slot);

        // 🔒 이미 회의 연결된 예약/슬롯이면 재승인 막기
        if (r.getMeetingId() != null || slot.getMeetingId() != null) {
            return;
        }

        // 🔒 승인 가능한 상태만 허용
        if (r.getStatus() != CounselingReservationStatus.RESERVED) {
            throw new CustomRestfullException("승인할 수 없는 예약 상태입니다.", HttpStatus.BAD_REQUEST);
        }

        // 🔒 지난 슬롯은 승인 불가 (선택사항이지만 보통 이렇게 막음)
        if (isPastSlot(slot)) {
            throw new CustomRestfullException("이미 지난 상담은 승인할 수 없습니다.", HttpStatus.BAD_REQUEST);
        }

        if (approveReq == null) {
            approveReq = new CreateMeetingReqDto();
        }

        if (approveReq.getTitle() == null || approveReq.getTitle().isBlank()) {
            approveReq.setTitle(
                    "상담 - " + slot.getProfessor().getName() + " / " + r.getStudent().getName()
            );
        }
        if (approveReq.getDescription() == null || approveReq.getDescription().isBlank()) {
            approveReq.setDescription("상담 예약으로 자동 생성된 회의입니다.");
        }

        // 🔥 Timestamp → LocalDateTime
        approveReq.setStartAt(slot.getStartAt());
        approveReq.setEndAt(slot.getEndAt());

        MeetingSimpleResDto simpleResDto =
                meetingService.createScheduledMeeting(approveReq, principal);
        Integer meetingId = simpleResDto.getMeetingId();

        LocalDateTime now = LocalDateTime.now();

        // 예약에 meetingId + 상태 반영
        r.setMeetingId(meetingId);
        r.setStatus(CounselingReservationStatus.APPROVED);
        r.setUpdatedAt(Timestamp.valueOf(now));
        reservationRepo.save(r);

        // 슬롯에도 meetingId 반영 (있으면 프론트에서 "연결된 회의" 표시 가능)
        slot.setMeetingId(meetingId);
        slot.setUpdatedAt(Timestamp.valueOf(now));
        slotRepo.save(slot);

        // 학생을 회의 참가자로 등록
        meetingService.addGuestParticipant(
                meetingId,
                r.getStudent().getEmail(),
                r.getStudent().getId()
        );
    }

    /**
     * 교수: 예약 취소
     * - 예약 상태를 CANCELED 로 변경
     * - 같은 슬롯에 남아있는 RESERVED 예약 없으면 슬롯을 OPEN 으로
     * - 슬롯에 연결된 meetingId 가 있으면 Meeting도 취소 + meetingId 제거
     */
    @Transactional
    public void cancelReservationByProfessor(PrincipalDto principal, Long reservationId) {
        validateProfessor(principal);

        CounselingReservation r = findReservation(reservationId);
        CounselingSlot slot = r.getSlot();

        validateSlotOwnerOrAdmin(principal, slot);


        // 이미 지난 상담은 취소 불가
        if (isPastSlot(slot)) {
            throw new CustomRestfullException("이미 지난 상담은 취소할 수 없습니다.", HttpStatus.BAD_REQUEST);
        }

        LocalDateTime now = LocalDateTime.now();

        // 예약 상태 변경
        if (r.getStatus() == CounselingReservationStatus.CANCELED) {
            // 이미 취소면 그냥 종료
            return;
        }
        r.setStatus(CounselingReservationStatus.CANCELED);
        r.setCanceledAt(Timestamp.valueOf(now));
        r.setUpdatedAt(Timestamp.valueOf(now));
        reservationRepo.save(r);

        // 같은 슬롯에 아직 RESERVED 상태 예약이 남아 있는지 확인
        boolean stillReserved = reservationRepo
                .findBySlot_Id(slot.getId())
                .stream()
                .anyMatch(x -> x.getStatus() == CounselingReservationStatus.RESERVED);

        if (!stillReserved) {
            slot.setStatus(CounselingSlotStatus.OPEN);

            // WebRTC Meeting 이 연결되어 있으면 같이 취소
            if (slot.getMeetingId() != null) {
                try {
                    meetingService.cancelMeeting(slot.getMeetingId(), principal);
                } catch (CustomRestfullException e) {
                    // meeting 취소 실패해도 슬롯 상태 변경은 진행
                    // 필요하면 로그만 남기고 무시
                }
                slot.setMeetingId(null);
            }

            slot.setUpdatedAt(Timestamp.valueOf(now));
            slotRepo.save(slot);
        }
    }
}
