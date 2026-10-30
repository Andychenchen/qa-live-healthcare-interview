package com.leansofx.qaserviceuser.entity.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.List;

/**
 * List&lt;String&gt; 与 JSON 字符串之间的 JPA 转换器。
 *
 * <p>用于把医生专长列表以单个 JSON 字符串列（如 ["高血压","冠心病"]）存储到 MySQL，
 * 而实体层仍保持强类型的 List&lt;String&gt;。
 */
@Converter
public class StringListJsonConverter implements AttributeConverter<List<String>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final TypeReference<List<String>> LIST_TYPE = new TypeReference<List<String>>() {
    };

    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "[]";
        }
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (Exception e) {
            throw new IllegalStateException("序列化 specialties 失败: " + attribute, e);
        }
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return new ArrayList<>();
        }
        try {
            List<String> parsed = MAPPER.readValue(dbData, LIST_TYPE);
            return parsed == null ? new ArrayList<>() : parsed;
        } catch (Exception e) {
            throw new IllegalStateException("反序列化 specialties 失败: " + dbData, e);
        }
    }
}
