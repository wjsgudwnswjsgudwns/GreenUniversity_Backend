package com.green.university.utils;

import java.time.LocalDate;

public class Define {

    public final static String PRINCIPAL = "principal";

    public final static String CREATE_FAIL = "생성에 실패하였습니다.";

    public final static String UPDATE_FAIL = "수정에 실패하였습니다.";

    public final static String NOT_FOUND_ID = "아이디를 찾을 수 없습니다.";

    public final static String WRONG_PASSWORD = "비밀번호가 틀렸습니다.";

    // 동적으로 현재 연도/학기 계산
    /**
     * 현재 연도를 반환
     * @return 현재 연도
     */
    public static int getCurrentYear() {
        return LocalDate.now().getYear();
    }

    /**
     * 현재 학기를 반환 (1학기: 1~6월, 2학기: 7~12월)
     * @return 현재 학기 (1 또는 2)
     */
    public static int getCurrentSemester() {
        int month = LocalDate.now().getMonthValue();
        return (month <= 6) ? 1 : 2;
    }

    // 하위 호환성을 위해 상수로도 제공 (deprecated 표시)
    /**
     * @deprecated getCurrentYear() 메서드 사용 권장
     */
    @Deprecated
    public final static int CURRENT_YEAR = getCurrentYear();

    /**
     * @deprecated getCurrentSemester() 메서드 사용 권장
     */
    @Deprecated
    public final static int CURRENT_SEMESTER = getCurrentSemester();

    // 이미지 처리 관련
    // 1KB = 1024byte
    // 1MB = 1024*1024 = 1,048,476 byte
    public final static String UPLOAD_DIRECTORY = "C:\\spring_upload\\universityManagement\\upload";

    public final static int MAX_FILE_SIZE = 1024 * 1024 * 20;

    /**
     * 로그인 해야 접속 가능한 페이지 목록
     *
     * @author 김지현
     */
    public final static String[] PATHS = { "/update", "/password", "/info/**", "/guide", "/notice/**"};
    public final static String[] PROFESSOR_PATHS = { "/professor/**" };
    public final static String[] STUDENT_PATHS = {"/grade/**"};
    public final static String[] STAFF_PATHS = { "/user/**" };

    // 수강 가능한 최대 학점
    public final static int MAX_GRADES = 18;

}