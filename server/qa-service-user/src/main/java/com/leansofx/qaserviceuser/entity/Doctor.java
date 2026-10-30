package com.leansofx.qaserviceuser.entity;

import com.leansofx.qaserviceuser.entity.converter.StringListJsonConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 医生实体，对应前端迁移前的 web/qa-web/src/data/doctor-user-list.json
 *
 * <p>字段与 JSON 完全一致；specialties 以 JSON 字符串形式存储在单列中
 * （由 {@link StringListJsonConverter} 完成 List&lt;String&gt; 与 JSON 字符串的互转）。
 */
@Entity
@Table(name = "doctors")
public class Doctor {

    /** 医生唯一标识，对应 JSON 中的 id */
    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    /** 登录用户名 */
    @Column(name = "username", length = 64, nullable = false, unique = true)
    private String username;

    /** 登录密码 */
    @Column(name = "password", length = 128, nullable = false)
    private String password;

    /** 姓名 */
    @Column(name = "name", length = 64, nullable = false)
    private String name;

    /** 职称，如 主任医师 */
    @Column(name = "title", length = 64)
    private String title;

    /** 科室，如 心内科 */
    @Column(name = "department", length = 64)
    private String department;

    /** 头像地址 */
    @Column(name = "avatar", length = 512)
    private String avatar;

    /** 从业经验描述 */
    @Column(name = "experience", length = 255)
    private String experience;

    /** 专长列表，以 JSON 字符串形式存储，如 ["高血压","冠心病","心律失常"] */
    @Column(name = "specialties", columnDefinition = "TEXT")
    @Convert(converter = StringListJsonConverter.class)
    private java.util.List<String> specialties;

    /** 是否在线（诊室是否开放） */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    public Doctor() {
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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

    public java.util.List<String> getSpecialties() {
        return specialties;
    }

    public void setSpecialties(java.util.List<String> specialties) {
        this.specialties = specialties;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
