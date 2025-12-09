package com.green.university.service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.green.university.dto.AllSubjectSearchFormDto;
import com.green.university.dto.CurrentSemesterSubjectSearchFormDto;
import com.green.university.dto.response.SubjectDto;
import com.green.university.handler.exception.CustomRestfullException;
import com.green.university.repository.SubjectJpaRepository;
import com.green.university.repository.DepartmentJpaRepository;
import com.green.university.repository.CollegeJpaRepository;
import com.green.university.repository.ProfessorJpaRepository;
import com.green.university.repository.RoomJpaRepository;
import com.green.university.repository.model.Subject;
import com.green.university.repository.model.Department;
import com.green.university.repository.model.College;
import com.green.university.repository.model.Professor;
import com.green.university.repository.model.Room;
import com.green.university.utils.Define;

/**
 * @author 서영
 */

@Service
public class SubjectService {

    @Autowired
    private SubjectJpaRepository subjectJpaRepository;
    @Autowired
    private DepartmentJpaRepository departmentJpaRepository;
    @Autowired
    private CollegeJpaRepository collegeJpaRepository;
    @Autowired
    private ProfessorJpaRepository professorJpaRepository;
    @Autowired
    private RoomJpaRepository roomJpaRepository;

	/**
	 * @return 전체 강의 조회에 사용할 강의 정보 (학생용) 전체 연도-학기에 해당하는 강의가 출력됨
	 */
	@Transactional
	public List<SubjectDto> readSubjectList() {
        // 모든 강의를 조회하여 DTO로 변환한다.
        List<Subject> subjects = subjectJpaRepository.findAll();
        List<SubjectDto> dtoList = new java.util.ArrayList<>();
        for (Subject sub : subjects) {
            SubjectDto dto = new SubjectDto();
            // 단과대/학과/교수/강의실 이름 정보 채우기
            Department dept = sub.getDepartment();
            if (dept != null) {
                dto.setDeptId(dept.getId());
                dto.setDeptName(dept.getName());
                College col = dept.getCollege();
                if (col != null) {
                    dto.setCollName(col.getName());
                }
            }
            // 과목 정보
            dto.setId(sub.getId());
            dto.setName(sub.getName());
            // 교수
            Professor prof = sub.getProfessor();
            if (prof != null) {
                dto.setProfessorId(prof.getId());
                dto.setProfessorName(prof.getName());
            }
            // 강의실
            Room room = sub.getRoom();
            if (room != null) {
                dto.setRoomId(room.getId());
            }
            // 기타 필드
            dto.setType(sub.getType());
            dto.setSubYear(sub.getSubYear());
            dto.setSemester(sub.getSemester());
            dto.setSubDay(sub.getSubDay());
            dto.setStartTime(sub.getStartTime());
            dto.setEndTime(sub.getEndTime());
            dto.setGrades(sub.getGrades());
            dto.setCapacity(sub.getCapacity());
            dto.setNumOfStudent(sub.getNumOfStudent());
            // status 는 기본 false 처리
            dto.setStatus(Boolean.FALSE);
            dtoList.add(dto);
        }
        return dtoList;
	}

	/**
	 * 페이징 처리
	 */
	@Transactional
	public List<SubjectDto> readSubjectListPage(Integer page) {
        // 페이지 번호는 1부터 시작. 20개씩 조회한다.
        if (page == null || page < 1) page = 1;
        int pageSize = 20;
        Page<Subject> pageResult = subjectJpaRepository
                .findAll(PageRequest.of(page - 1, pageSize));
        List<SubjectDto> dtoList = new java.util.ArrayList<>();
        for (Subject sub : pageResult.getContent()) {
            SubjectDto dto = new SubjectDto();
            Department dept = sub.getDepartment();
            if (dept != null) {
                dto.setDeptId(dept.getId());
                dto.setDeptName(dept.getName());
                College col = dept.getCollege();
                if (col != null) {
                    dto.setCollName(col.getName());
                }
            }
            dto.setId(sub.getId());
            dto.setName(sub.getName());
            Professor prof = sub.getProfessor();
            if (prof != null) {
                dto.setProfessorId(prof.getId());
                dto.setProfessorName(prof.getName());
            }
            Room room = sub.getRoom();
            if (room != null) {
                dto.setRoomId(room.getId());
            }
            dto.setType(sub.getType());
            dto.setSubYear(sub.getSubYear());
            dto.setSemester(sub.getSemester());
            dto.setSubDay(sub.getSubDay());
            dto.setStartTime(sub.getStartTime());
            dto.setEndTime(sub.getEndTime());
            dto.setGrades(sub.getGrades());
            dto.setCapacity(sub.getCapacity());
            dto.setNumOfStudent(sub.getNumOfStudent());
            dto.setStatus(Boolean.FALSE);
            dtoList.add(dto);
        }
        return dtoList;
	}

	/**
	 * @param allSubjectSearchFormDto
	 * @return 전체 강의 목록에서 필터링할 때 출력할 강의
	 */
    @Transactional
    public List<SubjectDto> readSubjectListSearch(AllSubjectSearchFormDto allSubjectSearchFormDto) {
        // 검색 조건에 따라 필터링한다. 우선 전체 목록을 가져와 메모리에서 필터링한다.
        List<SubjectDto> allList = readSubjectList();
        Stream<SubjectDto> stream = allList.stream();

        if (allSubjectSearchFormDto.getSubYear() != null) {
            stream = stream.filter(s -> allSubjectSearchFormDto.getSubYear().equals(s.getSubYear()));
        }
        if (allSubjectSearchFormDto.getSemester() != null) {
            stream = stream.filter(s -> allSubjectSearchFormDto.getSemester().equals(s.getSemester()));
        }
        // ⭐ deptId가 -1이 아닐 때만 필터링
        if (allSubjectSearchFormDto.getDeptId() != null && allSubjectSearchFormDto.getDeptId() != -1) {
            stream = stream.filter(s -> allSubjectSearchFormDto.getDeptId().equals(s.getDeptId()));
        }
        if (allSubjectSearchFormDto.getName() != null && !allSubjectSearchFormDto.getName().isEmpty()) {
            String name = allSubjectSearchFormDto.getName();
            stream = stream.filter(s -> s.getName() != null && s.getName().contains(name));
        }
        List<SubjectDto> filtered = stream.collect(Collectors.toList());

        // 검색 결과에서는 페이징을 제거하거나, 페이지 파라미터가 없으면 전체 반환
        if (allSubjectSearchFormDto.getPage() != null && allSubjectSearchFormDto.getPage() > 0) {
            int page = allSubjectSearchFormDto.getPage();
            int pageSize = 20;
            int fromIndex = (page - 1) * pageSize;
            int toIndex = Math.min(filtered.size(), fromIndex + pageSize);
            if (fromIndex < filtered.size()) {
                return filtered.subList(fromIndex, toIndex);
            } else {
                return java.util.Collections.emptyList();
            }
        }
        return filtered;
    }
	/**
	 * @return 수강 신청에 사용할 강의 정보 (학생용) 현재 연도-학기에 해당하는 강의만 출력됨
	 */
	@Transactional
	public List<SubjectDto> readSubjectListByCurrentSemester() {
        // 현재 학기의 강의만 필터링 (Define.CURRENT_YEAR, CURRENT_SEMESTER)
        List<SubjectDto> allList = readSubjectList();
        return allList.stream()
                .filter(s ->
                        s.getSubYear() != null
                        && s.getSemester() != null
                        && s.getSubYear() == Define.CURRENT_YEAR
                        && s.getSemester() == Define.CURRENT_SEMESTER)
                .collect(Collectors.toList());
	}

	/**
	 * 페이징 처리
	 */
	@Transactional
	public List<SubjectDto> readSubjectListByCurrentSemesterPage(Integer page) {
        List<SubjectDto> filtered = readSubjectListByCurrentSemester();
        if (page == null || page < 1) page = 1;
        int pageSize = 20;
        int fromIndex = (page - 1) * pageSize;
        int toIndex = Math.min(filtered.size(), fromIndex + pageSize);
        if (fromIndex < filtered.size()) {
            return filtered.subList(fromIndex, toIndex);
        }
        return java.util.Collections.emptyList();
	}

	/**
	 * @return 강의 시간표에서 필터링할 때 출력할 강의
	 */
    @Transactional
    public List<SubjectDto> readSubjectListSearchByCurrentSemester(CurrentSemesterSubjectSearchFormDto dto) {
        List<SubjectDto> filtered = readSubjectListByCurrentSemester();
        System.out.println("현재 학기 전체 강의 개수: " + filtered.size());

        Stream<SubjectDto> stream = filtered.stream();

        if (dto.getType() != null && !dto.getType().isEmpty() && !dto.getType().equals("전체")) {
            stream = stream.filter(s -> s.getType() != null && s.getType().equals(dto.getType()));
        }

        if (dto.getDeptId() != null && dto.getDeptId() != -1) {
            stream = stream.filter(s -> dto.getDeptId().equals(s.getDeptId()));
        }

        if (dto.getName() != null && !dto.getName().isEmpty()) {
            String name = dto.getName();
            System.out.println("강의명으로 필터링: " + name);
            stream = stream.filter(s -> {
                boolean match = s.getName() != null && s.getName().contains(name);
                if (match) {
                    System.out.println("매칭된 강의: " + s.getName());
                }
                return match;
            });
        }

        List<SubjectDto> result = stream.collect(Collectors.toList());
        System.out.println("최종 검색 결과: " + result.size());

        return result;
    }

	/**
	 * 현재 인원을 1명 추가함
	 */
    @Transactional
    public void updatePlusNumOfStudent(Integer subjectId) {
        Subject subject = subjectJpaRepository.findById(subjectId)
                .orElseThrow(() -> new CustomRestfullException("과목 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        subject.setNumOfStudent(subject.getNumOfStudent() + 1);
        subjectJpaRepository.save(subject);
    }
	/**
	 * 현재 인원을 1명 삭제함
	 */
    @Transactional
    public void updateMinusNumOfStudent(Integer subjectId) {
        Subject subject = subjectJpaRepository.findById(subjectId)
                .orElseThrow(() -> new CustomRestfullException("과목 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        if (subject.getNumOfStudent() > 0) {
            subject.setNumOfStudent(subject.getNumOfStudent() - 1);
            subjectJpaRepository.save(subject);
        }
    }

	@Transactional
	public Subject readBySubjectId(Integer id) {
        return subjectJpaRepository.findById(id)
                .orElseThrow(() -> new CustomRestfullException("과목을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
	}

    // 예비 수강 신청 인원 +1
    @Transactional
    public void updatePlusPreNumOfStudent(Integer subjectId) {
        Subject subject = subjectJpaRepository.findById(subjectId)
                .orElseThrow(() -> new CustomRestfullException("과목 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        subject.setPreNumOfStudent(subject.getPreNumOfStudent() + 1);
        subjectJpaRepository.save(subject);
    }

    // 예비 수강 신청 인원 -1
    @Transactional
    public void updateMinusPreNumOfStudent(Integer subjectId) {
        Subject subject = subjectJpaRepository.findById(subjectId)
                .orElseThrow(() -> new CustomRestfullException("과목 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        if (subject.getPreNumOfStudent() > 0) {
            subject.setPreNumOfStudent(subject.getPreNumOfStudent() - 1);
            subjectJpaRepository.save(subject);
        }
    }

    //
}
