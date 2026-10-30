package com.leansofx.qaserviceuser.repository;

import com.leansofx.qaserviceuser.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 医生数据访问接口
 */
public interface DoctorRepository extends JpaRepository<Doctor, String> {

    /**
     * 查询所有在线（诊室开放）的医生
     */
    List<Doctor> findByIsActiveTrueOrderByIdAsc();

    /**
     * 查询全部医生，按 id 升序
     */
    List<Doctor> findAllByOrderByIdAsc();
}
