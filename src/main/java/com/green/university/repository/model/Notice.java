package com.green.university.repository.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.sql.Timestamp;

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

    @Column(name = "uuid_filename")
    private String uuidFilename;

    @Column(name = "origin_filename")
    private String originFilename;

    public String setUpImage() {
        return "/images/uploads/" + uuidFilename;
    }
}
