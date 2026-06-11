package org.example.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;

/**
 * JSON 파싱과 파일 저장을 담당하는 핵심 핸들러.
 * 특정 모델에 의존하지 않고 JsonNode(트리) 단위로 동작한다.
 */
public class JsonHandler {

    private final ObjectMapper mapper;

    public JsonHandler() {
        mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT); // 사람이 읽기 좋은 포맷
    }

    // ── 파싱 ──────────────────────────────────────────────────────────────

    /** JSON 문자열 → JsonNode */
    public JsonNode parse(String json) throws IOException {
        return mapper.readTree(json);
    }

    /** JSON 파일 → JsonNode */
    public JsonNode parse(File file) throws IOException {
        return mapper.readTree(file);
    }

    // ── 저장 ──────────────────────────────────────────────────────────────

    /** JsonNode → JSON 파일 저장 */
    public void save(JsonNode node, File file) throws IOException {
        file.getParentFile().mkdirs();
        mapper.writeValue(file, node);
    }

    // ── 직렬화 ────────────────────────────────────────────────────────────

    /** JsonNode → JSON 문자열 */
    public String stringify(JsonNode node) throws IOException {
        return mapper.writeValueAsString(node);
    }

    /** JSON 문자열 유효성 검사 */
    public boolean isValid(String json) {
        try {
            mapper.readTree(json);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public ObjectMapper getMapper() {
        return mapper;
    }
}
