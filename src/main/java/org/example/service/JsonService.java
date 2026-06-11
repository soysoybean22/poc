package org.example.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class JsonService {

    private final ObjectMapper objectMapper;

    public JsonService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT); // pretty print
    }

    /** JSON 문자열 → 객체 역직렬화 */
    public <T> T parseFromString(String json, Class<T> clazz) throws IOException {
        return objectMapper.readValue(json, clazz);
    }

    /** JSON 문자열 → 리스트 역직렬화 */
    public <T> List<T> parseListFromString(String json, TypeReference<List<T>> typeRef) throws IOException {
        return objectMapper.readValue(json, typeRef);
    }

    /** JSON 파일 → 객체 역직렬화 */
    public <T> T parseFromFile(File file, Class<T> clazz) throws IOException {
        return objectMapper.readValue(file, clazz);
    }

    /** JSON 파일 → 리스트 역직렬화 */
    public <T> List<T> parseListFromFile(File file, TypeReference<List<T>> typeRef) throws IOException {
        return objectMapper.readValue(file, typeRef);
    }

    /** JSON 문자열 → 트리 구조(JsonNode)로 파싱 — 스키마 없이 탐색할 때 사용 */
    public JsonNode parseToTree(String json) throws IOException {
        return objectMapper.readTree(json);
    }

    /** 객체 → JSON 문자열 직렬화 */
    public String toJsonString(Object obj) throws IOException {
        return objectMapper.writeValueAsString(obj);
    }

    /** 객체 → JSON 파일 저장 */
    public void saveToFile(Object obj, File file) throws IOException {
        objectMapper.writeValue(file, obj);
    }

    /** 리스트 → JSON 파일 저장 */
    public <T> void saveListToFile(List<T> list, File file) throws IOException {
        objectMapper.writeValue(file, list);
    }

    /** JsonNode 동적 조작 후 파일 저장 */
    public void saveNodeToFile(JsonNode node, File file) throws IOException {
        objectMapper.writeValue(file, node);
    }

    /** 기존 JSON 파일의 특정 필드 값을 업데이트하여 다시 저장 */
    public void updateFieldAndSave(File file, String fieldName, String newValue) throws IOException {
        JsonNode root = objectMapper.readTree(file);
        ((ObjectNode) root).put(fieldName, newValue);
        objectMapper.writeValue(file, root);
    }
}
