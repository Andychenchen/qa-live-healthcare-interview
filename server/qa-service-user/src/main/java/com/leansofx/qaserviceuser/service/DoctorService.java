package com.leansofx.qaserviceuser.service;

import com.leansofx.qaserviceuser.dto.DoctorResponse;
import com.leansofx.qaserviceuser.entity.Doctor;
import com.leansofx.qaserviceuser.repository.DoctorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 医生查询服务
 */
@Service
public class DoctorService {

    private final DoctorRepository doctorRepository;

    public DoctorService(DoctorRepository doctorRepository) {
        this.doctorRepository = doctorRepository;
    }

    /**
     * 查询全部医生
     */
    @Transactional(readOnly = true)
    public List<DoctorResponse> findAll() {
        return doctorRepository.findAllByOrderByIdAsc().stream()
                .map(DoctorResponse::from)
                .toList();
    }

    /**
     * 查询在线（isActive=true）的医生
     */
    @Transactional(readOnly = true)
    public List<DoctorResponse> findActive() {
        return doctorRepository.findByIsActiveTrueOrderByIdAsc().stream()
                .map(DoctorResponse::from)
                .toList();
    }
}
