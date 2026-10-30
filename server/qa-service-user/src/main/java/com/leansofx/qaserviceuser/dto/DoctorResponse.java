package com.leansofx.qaserviceuser.dto;

import com.leansofx.qaserviceuser.entity.Doctor;

import java.util.List;

/**
 * 医生列表接口的响应对象。
 *
 * <p>与实体字段保持一致，但出于安全考虑不返回 password 字段。
 */
public class DoctorResponse {

    private String id;
    private String username;
    private String name;
    private String title;
    private String department;
    private String avatar;
    private String experience;
    private List<String> specialties;
    private Boolean isActive;

    public DoctorResponse() {
    }

    public static DoctorResponse from(Doctor doctor) {
        DoctorResponse response = new DoctorResponse();
        response.setId(doctor.getId());
        response.setUsername(doctor.getUsername());
        response.setName(doctor.getName());
        response.setTitle(doctor.getTitle());
        response.setDepartment(doctor.getDepartment());
        response.setAvatar(doctor.getAvatar());
        response.setExperience(doctor.getExperience());
        response.setSpecialties(doctor.getSpecialties());
        response.setIsActive(doctor.getIsActive());
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getExperience() {
        return experience;
    }

    public void setExperience(String experience) {
        this.experience = experience;
    }

    public List<String> getSpecialties() {
        return specialties;
    }

    public void setSpecialties(List<String> specialties) {
        this.specialties = specialties;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
