package com.leansofx.qaserviceuser.init;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leansofx.qaserviceuser.entity.Doctor;
import com.leansofx.qaserviceuser.repository.DoctorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
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
 * 医生数据初始化。
 *
 * <p>数据源为前端迁移前的 JSON 文件
 * {@code web/qa-web/src/data/doctor-user-list.json}，应用启动时读取该文件，
 * 在 doctors 表为空时写入种子数据（建表由 JPA ddl-auto 完成）。
 *
 * <p>文件路径可通过配置 {@code app.doctor.seed.source} 覆盖。
 */
@Component
@Order(1)
public class DoctorDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DoctorDataInitializer.class);

    /** 默认种子数据源：仓库根目录下的前端 JSON 文件 */
    private static final String DEFAULT_SOURCE = "../../web/qa-web/src/data/doctor-user-list.json";

    /** classpath 内的种子数据副本，用于无法访问仓库源码时的兜底 */
    private static final String CLASSPATH_SEED = "seed/doctor-user-list.json";

    private final DoctorRepository doctorRepository;
    private final ObjectMapper objectMapper;
    private final String sourcePath;

    public DoctorDataInitializer(DoctorRepository doctorRepository,
                                 ObjectMapper objectMapper,
                                 @Value("${app.doctor.seed.source:" + DEFAULT_SOURCE + "}") String sourcePath) {
        this.doctorRepository = doctorRepository;
        this.objectMapper = objectMapper;
        this.sourcePath = sourcePath;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws IOException {
        long existing = doctorRepository.count();
        if (existing > 0) {
            log.info("doctors 表已有 {} 条数据，跳过种子数据初始化", existing);
            return;
        }

        List<Doctor> doctors = loadFromJson();
        if (doctors.isEmpty()) {
            log.warn("未能读取到医生数据，跳过种子数据初始化");
            return;
        }

        doctorRepository.saveAll(doctors);
        log.info("已初始化 {} 条医生数据到 MySQL", doctors.size());
    }

    /**
     * 读取 JSON 种子文件并转换为 Doctor 实体列表。
     *
     * <p>优先读取仓库中的前端 JSON 文件；若不可达则回落到 classpath 内的副本。
     */
    private List<Doctor> loadFromJson() throws IOException {
        Path path = resolvePath();
        String content;
        if (path != null) {
            content = Files.readString(path);
            log.info("医生种子数据来源: {}", path.toAbsolutePath().normalize());
        } else {
            try (InputStream in = new ClassPathResource(CLASSPATH_SEED).getInputStream()) {
                content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                log.info("医生种子数据来源: classpath:{}", CLASSPATH_SEED);
            }
        }

        JsonNode root = objectMapper.readTree(content);
        JsonNode array = root.isArray() ? root : root.path("doctors");
        if (!array.isArray()) {
            log.warn("种子文件格式不正确，期望 JSON 数组");
            return List.of();
        }

        List<Doctor> doctors = new ArrayList<>(array.size());
        for (JsonNode node : array) {
            doctors.add(toDoctor(node));
        }
        return doctors;
    }

    private Doctor toDoctor(JsonNode node) {
        Doctor doctor = new Doctor();
        doctor.setId(node.path("id").asText());
        doctor.setUsername(node.path("username").asText());
        doctor.setPassword(node.path("password").asText());
        doctor.setName(node.path("name").asText());
        doctor.setTitle(node.path("title").asText(null));
        doctor.setDepartment(node.path("department").asText(null));
        doctor.setAvatar(node.path("avatar").asText(null));
        doctor.setExperience(node.path("experience").asText(null));
        doctor.setIsActive(node.path("isActive").asBoolean(false));

        List<String> specialties = new ArrayList<>();
        JsonNode specialtiesNode = node.path("specialties");
        if (specialtiesNode.isArray()) {
            specialtiesNode.forEach(item -> specialties.add(item.asText()));
        }
        doctor.setSpecialties(specialties);
        return doctor;
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
        log.warn("未找到医生种子数据文件: {}", sourcePath);
        return null;
    }
}
