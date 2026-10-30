package com.leansofx.qaserviceuser.controller;

import com.leansofx.qaserviceuser.dto.DoctorResponse;
import com.leansofx.qaserviceuser.service.DoctorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 医生列表接口
 *
 * <p>供前端 Doctors.vue / 首页开放诊室区调用；跨域由现有 CorsConfig 统一处理。
 */
@RestController
@RequestMapping("/api/doctors")
public class DoctorController {

    private final DoctorService doctorService;

    public DoctorController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    /**
     * GET /api/doctors —— 返回全部医生
     */
    @GetMapping
    public ResponseEntity<List<DoctorResponse>> listAll() {
        return ResponseEntity.ok(doctorService.findAll());
    }

    /**
     * GET /api/doctors/active —— 仅返回 isActive=true 的医生
     */
    @GetMapping("/active")
    public ResponseEntity<List<DoctorResponse>> listActive() {
        return ResponseEntity.ok(doctorService.findActive());
    }
}
