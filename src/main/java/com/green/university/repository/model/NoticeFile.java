package com.green.university.repository.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import com.green.university.repository.model.Notice;

import lombok.Data;

/**
 * 공지사항 첨부파일 엔티티입니다.
 *
 * 파일명(uuidFilename)을 기본키로 사용하며, 공지사항과 연관관계를 갖습니다.
 */
@Data
@Entity
@Table(name = "notice_file_tb")
public class NoticeFile {

    /**
     * 첨부파일의 랜덤 문자열이 포함된 파일명. 기본키로 사용한다.
     */
    @Id
    @Column(name = "uuid_filename")
    private String uuidFilename;

    @Column(name = "origin_filename", nullable = false)
    private String originFilename;

    /**
     * 첨부파일이 속한 공지사항. {@link Notice}와 N:1 관계를 맺는다.
     */
    @ManyToOne
    @JoinColumn(name = "notice_id", nullable = false)
    @JsonIgnore
    private Notice notice;

    @Column(name = "notice_id", insertable = false, updatable = false)
    private Integer noticeId;
}
