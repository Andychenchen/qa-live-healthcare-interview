package com.leansofx.qaserviceuser.service;

import com.leansofx.qaserviceuser.dto.AuthResponse;
import com.leansofx.qaserviceuser.dto.LoginRequest;
import com.leansofx.qaserviceuser.dto.PatientResponse;
import com.leansofx.qaserviceuser.dto.RegisterRequest;
import com.leansofx.qaserviceuser.entity.Patient;
import com.leansofx.qaserviceuser.exception.BusinessException;
import com.leansofx.qaserviceuser.exception.ErrorCodes;
import com.leansofx.qaserviceuser.repository.PatientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 患者注册与登录服务（PRD REQ-001 / REQ-004）。
 */
@Service
public class PatientAuthService {

    private static final Logger log = LoggerFactory.getLogger(PatientAuthService.class);

    /**
     * 用于「用户不存在」场景的假哈希。
     *
     * <p>登录时若用户不存在，也执行一次 BCrypt 比对，使响应时间与「密码错误」接近，
     * 消除时间差带来的账号枚举风险（PRD SEC-03）。
     */
    private static final String DUMMY_HASH = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    private final PatientRepository patientRepository;
    private final PasswordEncoder passwordEncoder;
    private final PatientValidator validator;
    private final LoginAttemptService loginAttemptService;
    private final SessionService sessionService;

    public PatientAuthService(PatientRepository patientRepository,
                              PasswordEncoder passwordEncoder,
                              PatientValidator validator,
                              LoginAttemptService loginAttemptService,
                              SessionService sessionService) {
        this.patientRepository = patientRepository;
        this.passwordEncoder = passwordEncoder;
        this.validator = validator;
        this.loginAttemptService = loginAttemptService;
        this.sessionService = sessionService;
    }

    /**
     * 注册患者账号并返回登录态（PRD REQ-001）。
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        validator.validateRegister(request);

        String username = request.getUsername().trim();

        // V-07 用户名唯一（大小写不敏感）
        if (patientRepository.existsByUsernameIgnoreCase(username)) {
            throw new BusinessException(ErrorCodes.PATIENT_USERNAME_TAKEN, ErrorCodes.MSG_USERNAME_TAKEN, 409);
        }

        Patient patient = new Patient();
        patient.setId(generatePatientId());
        patient.setUsername(username);
        // 密码以 BCrypt 哈希存储，明文不落库
        patient.setPassword(passwordEncoder.encode(request.getPassword()));
        patient.setName(request.getName().trim());
        patient.setBirthday(blankToNull(request.getBirthday()));
        patient.setPhone(blankToNull(request.getPhone()));
        patient.setGender(blankToNull(request.getGender()));

        try {
            Patient saved = patientRepository.saveAndFlush(patient);
            String token = sessionService.createSession(saved.getId());
            log.info("患者注册成功: username={}, id={}", saved.getUsername(), saved.getId());
            return new AuthResponse(token, PatientResponse.from(saved));
        } catch (DataIntegrityViolationException e) {
            // 并发注册同一用户名时由数据库唯一索引兜底
            throw new BusinessException(ErrorCodes.PATIENT_USERNAME_TAKEN, ErrorCodes.MSG_USERNAME_TAKEN, 409);
        }
    }

    /**
     * 患者登录（PRD REQ-004）。
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        validator.validateLogin(request);

        String username = request.getUsername().trim();

        // V-09 限流：锁定期间直接拒绝
        if (loginAttemptService.isLocked(username)) {
            throw new BusinessException(ErrorCodes.PATIENT_LOGIN_LOCKED, ErrorCodes.MSG_LOGIN_LOCKED, 429);
        }

        Optional<Patient> found = patientRepository.findByUsernameIgnoreCase(username);
        if (found.isEmpty()) {
            // 用户不存在时同样执行一次哈希比对，消除时间差
            passwordEncoder.matches(request.getPassword(), DUMMY_HASH);
            loginAttemptService.recordFailure(username);
            throw new BusinessException(ErrorCodes.PATIENT_LOGIN_FAILED, ErrorCodes.MSG_LOGIN_FAILED, 401);
        }

        Patient patient = found.get();
        if (!passwordEncoder.matches(request.getPassword(), patient.getPassword())) {
            loginAttemptService.recordFailure(username);
            throw new BusinessException(ErrorCodes.PATIENT_LOGIN_FAILED, ErrorCodes.MSG_LOGIN_FAILED, 401);
        }

        // 登录成功，清空失败计数
        loginAttemptService.reset(username);
        String token = sessionService.createSession(patient.getId());
        log.info("患者登录成功: username={}", patient.getUsername());
        return new AuthResponse(token, PatientResponse.from(patient));
    }

    /**
     * 登出，使当前会话立即失效（PRD REQ-007）。
     */
    public void logout(String token) {
        sessionService.invalidate(token);
    }

    /**
     * 根据令牌查询当前患者（PRD REQ-006）。
     */
    @Transactional(readOnly = true)
    public PatientResponse currentPatient(String token) {
        String patientId = sessionService.resolvePatientId(token);
        if (patientId == null) {
            throw new BusinessException(ErrorCodes.PATIENT_TOKEN_INVALID, ErrorCodes.MSG_TOKEN_INVALID, 401);
        }
        return patientRepository.findById(patientId)
                .map(PatientResponse::from)
                .orElseThrow(() -> new BusinessException(
                        ErrorCodes.PATIENT_TOKEN_INVALID, ErrorCodes.MSG_TOKEN_INVALID, 401));
    }

    /**
     * 生成形如 patient006 的患者 ID
     */
    private String generatePatientId() {
        long count = patientRepository.count();
        String candidate;
        long suffix = count + 1;
        do {
            candidate = String.format("patient%03d", suffix);
            suffix++;
        } while (patientRepository.existsById(candidate));
        return candidate;
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
