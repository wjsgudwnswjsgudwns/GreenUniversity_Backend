# 그린 대학교 학사정보시스템 
## Green University Learning Management System


<br>

## ⚙️ 개발 환경
- IDE : `IntelliJ`
- BackEnd :  `JDK 17`  `JSP`  `SpringBoot`  `MySQL`  `MyBatis`
- FrontEnd :  `HTML5`  `CSS`  `JavaScript`  `JSP`
- DataBase : `H2-Memory`
- VCS : `Git` `GitHub`

<br>

#### 의존성

    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.apache.tomcat.embed:tomcat-embed-jasper' 
    implementation 'javax.servlet:jstl' 
    implementation 'org.mybatis.spring.boot:mybatis-spring-boot-starter:2.3.0'  
    runtimeOnly 'com.mysql:mysql-connector-j'
    runtimeOnly 'com.h2database:h2'
    implementation 'org.springframework.security:spring-security-crypto'
    implementation 'org.springframework.boot:spring-boot-starter-web'
    compileOnly 'org.projectlombok:lombok'
    developmentOnly 'org.springframework.boot:spring-boot-devtools'
    annotationProcessor 'org.projectlombok:lombok'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'

<br>

## 📌 주요 기능

#### 예비 수강 신청 (수강 장바구니)
- 대상 : 현재 학기에 재학 상태가 되는 학생
- 신청/취소할 때마다 강의 현재 인원 변경
- 신청 강의의 정원 초과 가능
- 최대 수강 가능 학점 초과 불가능 (최대 18학점)
- 신청자 본인의 시간표와 겹치는 강의 신청 불가능
- 페이징 처리, 검색 기능

#### 예비 수강 신청 → 수강 신청
- 수강 신청 기간이 되면 예비 수강 신청 목록을 확인함 <br>
  → 정원 >= 신청인원인 강의 : 예비 수강 신청 내역이 수강 신청 내역으로 자동으로 이월됨 <br>
  → 정원 < 신청인원인 강의 : 신청인원이 0으로 초기화되며, 학생이 직접 신청하도록 함
- 예비 수강 신청 내역이 있는 경우, 수강 신청 탭에 가장 먼저 출력되도록 함

#### 수강 신청
- 대상 : 현재 학기에 재학 상태가 되는 학생
- 신청/취소할 때마다 강의 현재 인원 변경
- 신청 강의의 정원 초과 불가능
- 최대 수강 가능 학점 초과 불가능 (최대 18학점)
- 신청자 본인의 시간표와 겹치는 강의 신청 불가능
- 페이징 처리, 검색 기능

<br>

## 📖 기능 - 공통

#### 로그인
- 세션 처리
- 아이디 찾기
- 비밀번호 찾기
- 아이디 저장 (쿠키 활용)

#### 개인 정보
- 개인 정보 조회
- 개인 정보 변경
- 비밀번호 변경

#### 공지사항 및 학사일정
- 공지사항 조회
- 학사일정 조회


<br>

## 👨🏻‍💼 기능 - 교직원

#### 학사관리
- 학생, 교수, 직원 계정 생성
- 학생, 교수 명단 조회
- 등록금 고지서 발송
- 휴학 처리(승인)
- 수강 신청 기간 설정
- 공지 CRUD
- 학사일정 CRUD

#### 등록관리
- 단과대학 CRUD
- 학과 CRUD
- 강의 CRUD
- 강의실 CRUD
- 등록금 CRUD


<br>

## 👩🏻‍🎓 기능 - 학생

#### 등록 및 휴학
- 등록금 납부
- 등록금 납부 내역 조회
- 휴학 신청
- 휴학 신청 내역 조회

#### 수강 신청
- 강의 시간표 조회
- 예비 수강 신청
- 수강 신청
- 수강 신청 내역 조회

#### 성적
- 금학기 성적 조회
- 학기별 성적 조회
- 누계 성적

<br>


## 👨🏻‍🏫 기능 - 교수

#### 강의
- 내 강의 학기별 조회
- 강의계획서 수정
- 강의별 학생리스트 조회, 출결 및 성적 기입
- 강의평가 확인

<br>

