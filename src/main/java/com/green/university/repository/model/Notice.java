package com.green.university.repository.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.sql.Timestamp;
import java.util.List;

import lombok.Data;

@Data
@Entity
@Table(name = "notice_tb")
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "category", nullable = false)
    private String category;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "views")
    private Integer views;

    @Column(name = "created_time")
    private Timestamp createdTime;

    // 공지사항에 첨부된 파일 목록 (1:N 관계)
    @OneToMany(mappedBy = "notice", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<NoticeFile> files;

    // 첫 번째 첨부 이미지의 경로를 반환합니다.
    public String setUpImage() {
        if (files != null && !files.isEmpty()) {
            return "/images/uploads/" + files.get(0).getUuidFilename();
        }
        return null;
    }

    // 첫 번째 첨부파일의 UUID 파일명을 반환합니다.
    public String getUuidFilename() {
        if (files != null && !files.isEmpty()) {
            return files.get(0).getUuidFilename();
        }
        return null;
    }

    // 첫 번째 첨부파일의 원본 파일명을 반환합니다.
    public String getOriginFilename() {
        if (files != null && !files.isEmpty()) {
            return files.get(0).getOriginFilename();
        }
        return null;
    }
}