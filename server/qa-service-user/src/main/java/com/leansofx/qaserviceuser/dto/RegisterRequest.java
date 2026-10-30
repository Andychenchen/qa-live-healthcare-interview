package com.leansofx.qaserviceuser.dto;

/**
 * 患者注册请求体（PRD REQ-001）。
 *
 * <p>服务端会独立执行全部校验，不信任前端。
 */
public class RegisterRequest {

    private String username;
    private String password;
    private String name;
    private String birthday;
    private String phone;
    private String gender;

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
}
