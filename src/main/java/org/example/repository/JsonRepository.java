package org.example.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.example.handler.JsonHandler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JsonHandler를 기반으로 JSON 배열 파일에 대한 CRUD를 제공한다.
 * 레코드 구조에 제약이 없으며, id 필드만 자동으로 관리한다.
 */
public class JsonRepository {

    private final File dataFile;
    private final JsonHandler handler;

    public JsonRepository(JsonHandler handler, File dataFile) throws IOException {
        this.handler = handler;
        this.dataFile = dataFile;
        initFile();
    }

    private void initFile() throws IOException {
        if (!dataFile.exists()) {
            dataFile.getParentFile().mkdirs();
            handler.save(handler.getMapper().createArrayNode(), dataFile);
        }
    }

    // ── Create ────────────────────────────────────────────────────────────

    public JsonNode create(JsonNode data) throws IOException {
        ArrayNode records = loadAll();
        long nextId = nextId(records);

        ObjectNode record = handler.getMapper().createObjectNode();
        record.put("id", nextId);
        record.setAll((ObjectNode) data);   // 사용자 입력 필드 병합

        records.add(record);
        save(records);
        return record;
    }

    // ── Read ──────────────────────────────────────────────────────────────

    public List<JsonNode> findAll() throws IOException {
        List<JsonNode> list = new ArrayList<>();
        loadAll().forEach(list::add);
        return list;
    }

    public Optional<JsonNode> findById(long id) throws IOException {
        for (JsonNode record : loadAll()) {
            if (record.get("id").asLong() == id) return Optional.of(record);
        }
        return Optional.empty();
    }

    // ── Update ────────────────────────────────────────────────────────────

    public boolean update(long id, JsonNode newData) throws IOException {
        ArrayNode records = loadAll();
        for (int i = 0; i < records.size(); i++) {
            if (records.get(i).get("id").asLong() == id) {
                ObjectNode updated = handler.getMapper().createObjectNode();
                updated.put("id", id);
                updated.setAll((ObjectNode) newData);   // id를 덮어쓰지 않도록 순서 유지
                updated.put("id", id);                  // id 보존 보장
                records.set(i, updated);
                save(records);
                return true;
            }
        }
        return false;
    }

    // ── Delete ────────────────────────────────────────────────────────────

    public boolean delete(long id) throws IOException {
        ArrayNode records = loadAll();
        ArrayNode filtered = handler.getMapper().createArrayNode();
        boolean found = false;
        for (JsonNode record : records) {
            if (record.get("id").asLong() == id) {
                found = true;
            } else {
                filtered.add(record);
            }
        }
        if (found) save(filtered);
        return found;
    }

    // ── 내부 유틸 ─────────────────────────────────────────────────────────

    private ArrayNode loadAll() throws IOException {
        return (ArrayNode) handler.parse(dataFile);
    }

    private void save(ArrayNode records) throws IOException {
        handler.save(records, dataFile);
    }

    private long nextId(ArrayNode records) {
        long max = 0;
        for (JsonNode record : records) {
            long id = record.get("id").asLong();
            if (id > max) max = id;
        }
        return max + 1;
    }
}
