package com.leansofx.qaserviceuser.dto;

import java.time.LocalDateTime;

/**
 * 患者信息响应对象（PRD 5.3.1 / 5.3.4）。
 *
 * <p>与既有医生列表接口一致，响应中**绝不包含 password 字段**。
 */
public class PatientResponse {

    private String id;
    private String username;
    private String name;
    private String birthday;
    private String phone;
    private String gender;
    private LocalDateTime createdAt;

    public PatientResponse() {
    }

    public static PatientResponse from(com.leansofx.qaserviceuser.entity.Patient patient) {
        PatientResponse response = new PatientResponse();
        response.setId(patient.getId());
        response.setUsername(patient.getUsername());
        response.setName(patient.getName());
        response.setBirthday(patient.getBirthday());
        response.setPhone(patient.getPhone());
        response.setGender(patient.getGender());
        response.setCreatedAt(patient.getCreatedAt());
        return response;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBirthday() {
        return birthday;
    }

    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
