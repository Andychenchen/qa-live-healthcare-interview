package com.leansofx.qaserviceuser.init;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leansofx.qaserviceuser.entity.Patient;
import com.leansofx.qaserviceuser.repository.PatientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 患者数据初始化。
 *
 * <p>数据源为前端迁移前的 JSON 文件
 * {@code web/qa-web/src/data/patient-user.json}，应用启动时读取该文件，
 * 在 patients 表为空时写入种子数据（建表由 JPA ddl-auto 完成）。
 *
 * <p>存量记录没有密码字段，为其生成统一的初始密码并以 BCrypt 哈希存储，
 * 明文密码不会落库（参见 PRD 6.2 节「迁移要求」）。
 */
@Component
@Order(2)
public class PatientDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PatientDataInitializer.class);

    /** 默认种子数据源：仓库根目录下的前端 JSON 文件 */
    private static final String DEFAULT_SOURCE = "../../web/qa-web/src/data/patient-user.json";

    /** classpath 内的种子数据副本，用于无法访问仓库源码时的兜底 */
    private static final String CLASSPATH_SEED = "seed/patient-user.json";

    /**
     * 存量患者账号的初始密码（PRD Q-2 的默认方案）。
     * 仅用于生成本地测试账号的 BCrypt 哈希，不会以明文形式写入数据库。
     */
    static final String DEFAULT_INITIAL_PASSWORD = "qa123456";

    private final PatientRepository patientRepository;
    private final ObjectMapper objectMapper;
    private final PasswordEncoder passwordEncoder;
    private final String sourcePath;

    public PatientDataInitializer(PatientRepository patientRepository,
                                  ObjectMapper objectMapper,
                                  PasswordEncoder passwordEncoder,
                                  @Value("${app.patient.seed.source:" + DEFAULT_SOURCE + "}") String sourcePath) {
        this.patientRepository = patientRepository;
        this.objectMapper = objectMapper;
        this.passwordEncoder = passwordEncoder;
        this.sourcePath = sourcePath;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws IOException {
        long existing = patientRepository.count();
        if (existing > 0) {
            log.info("patients 表已有 {} 条数据，跳过种子数据初始化", existing);
            return;
        }

        List<Patient> patients = loadFromJson();
        if (patients.isEmpty()) {
            log.warn("未能读取到患者数据，跳过种子数据初始化");
            return;
        }

        patientRepository.saveAll(patients);
        log.info("已初始化 {} 条患者数据到 MySQL（初始密码已 BCrypt 哈希存储）", patients.size());
    }

    /**
     * 读取 JSON 种子文件并转换为 Patient 实体列表。
     *
     * <p>优先读取仓库中的前端 JSON 文件；若不可达则回落到 classpath 内的副本。
     */
    private List<Patient> loadFromJson() throws IOException {
        Path path = resolvePath();
        String content;
        if (path != null) {
            content = Files.readString(path);
            log.info("患者种子数据来源: {}", path.toAbsolutePath().normalize());
        } else {
            try (InputStream in = new ClassPathResource(CLASSPATH_SEED).getInputStream()) {
                content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                log.info("患者种子数据来源: classpath:{}", CLASSPATH_SEED);
            }
        }

        JsonNode root = objectMapper.readTree(content);
        JsonNode array = root.isArray() ? root : root.path("patients");
        if (!array.isArray()) {
            log.warn("种子文件格式不正确，期望 JSON 数组");
            return List.of();
        }

        List<Patient> patients = new ArrayList<>(array.size());
        for (JsonNode node : array) {
            patients.add(toPatient(node));
        }
        return patients;
    }

    private Patient toPatient(JsonNode node) {
        Patient patient = new Patient();
        patient.setId(node.path("id").asText());
        patient.setUsername(node.path("username").asText(node.path("id").asText()));
        // 存量数据无密码字段，统一生成初始密码并以 BCrypt 哈希存储
        patient.setPassword(passwordEncoder.encode(DEFAULT_INITIAL_PASSWORD));
        patient.setName(node.path("name").asText());
        patient.setBirthday(node.path("birthday").asText(null));
        patient.setPhone(node.path("phone").asText(null));
        patient.setGender(node.path("gender").asText(null));
        return patient;
    }

    /**
     * 解析种子文件路径，依次尝试相对工作目录与仓库根目录
     */
    private Path resolvePath() {
        List<Path> candidates = new ArrayList<>();
        candidates.add(Paths.get(sourcePath));
        candidates.add(Paths.get("").toAbsolutePath().resolve(sourcePath));
        candidates.add(Paths.get("").toAbsolutePath().getParent() == null
                ? Paths.get(sourcePath)
                : Paths.get("").toAbsolutePath().getParent().resolve(sourcePath));

        for (Path candidate : candidates) {
            File file = candidate.toAbsolutePath().normalize().toFile();
            if (file.isFile()) {
                return file.toPath();
            }
        }
        log.warn("未找到患者种子数据文件: {}", sourcePath);
        return null;
    }
}
