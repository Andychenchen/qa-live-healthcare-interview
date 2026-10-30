package com.leansofx.qaserviceuser.repository;

import com.leansofx.qaserviceuser.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 患者数据访问接口
 */
public interface PatientRepository extends JpaRepository<Patient, String> {

    /**
     * 按用户名查询（大小写不敏感），用于登录与注册查重
     */
    Optional<Patient> findByUsernameIgnoreCase(String username);

    /**
     * 判断用户名是否已存在（大小写不敏感）
     */
    boolean existsByUsernameIgnoreCase(String username);
}
