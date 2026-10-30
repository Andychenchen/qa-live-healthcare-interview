package com.leansofx.qaserviceuser.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 患者实体，对应 PRD 6.1 节的数据模型。
 *
 * <p>密码以 BCrypt 哈希形式存储，明文永不落库、永不出现在日志与接口响应中。
 */
@Entity
@Table(name = "patients")
public class Patient {

    /** 患者唯一标识，形如 patient001 */
    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    /** 登录用户名，大小写不敏感唯一 */
    @Column(name = "username", length = 64, nullable = false, unique = true)
    private String username;

    /** BCrypt 密码哈希（长度 60），绝不存明文 */
    @Column(name = "password", length = 100, nullable = false)
    private String password;

    /** 患者姓名 / 昵称 */
    @Column(name = "name", length = 64, nullable = false)
    private String name;

    /** 生日，格式 YYYY-MM-DD */
    @Column(name = "birthday", length = 10)
    private String birthday;

    /** 手机号 */
    @Column(name = "phone", length = 20)
    private String phone;

    /** 性别：男/女/其他 */
    @Column(name = "gender", length = 10)
    private String gender;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
