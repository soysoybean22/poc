package org.example.db;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.example.handler.JsonHandler;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * JSON 파일을 DB 서버처럼 사용하는 key-value 저장소.
 *
 * db.json 구조:
 * {
 *   "사과": 3,
 *   "바나나": "노란색",
 *   "상품": { "price": 1000, "stock": 5 }
 * }
 *
 * 앱이 종료되어도 db.json 파일에 데이터가 유지된다.
 */
public class JsonDatabase {

    private final File dbFile;
    private final JsonHandler handler;

    public JsonDatabase(JsonHandler handler, File dbFile) throws IOException {
        this.handler = handler;
        this.dbFile = dbFile;
        initFile();
    }

    private void initFile() throws IOException {
        if (!dbFile.exists()) {
            dbFile.getParentFile().mkdirs();
            handler.save(handler.getMapper().createObjectNode(), dbFile);
        }
    }

    // ── Create ────────────────────────────────────────────────────────────

    /** key가 이미 존재하면 false, 신규 등록 시 true */
    public boolean put(String key, JsonNode value) throws IOException {
        ObjectNode db = load();
        if (db.has(key)) return false;
        db.set(key, value);
        save(db);
        return true;
    }

    // ── Read ──────────────────────────────────────────────────────────────

    /** key에 해당하는 값 반환. 없으면 empty */
    public Optional<JsonNode> get(String key) throws IOException {
        JsonNode value = load().get(key);
        return Optional.ofNullable(value);
    }

    /** 전체 key-value 쌍을 순서 보존 Map으로 반환 */
    public Map<String, JsonNode> getAll() throws IOException {
        ObjectNode db = load();
        Map<String, JsonNode> result = new LinkedHashMap<>();
        Iterator<Map.Entry<String, JsonNode>> fields = db.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    public boolean exists(String key) throws IOException {
        return load().has(key);
    }

    // ── Update ────────────────────────────────────────────────────────────

    /** key가 존재하면 값을 교체하고 true, 없으면 false */
    public boolean update(String key, JsonNode newValue) throws IOException {
        ObjectNode db = load();
        if (!db.has(key)) return false;
        db.set(key, newValue);
        save(db);
        return true;
    }

    // ── Delete ────────────────────────────────────────────────────────────

    /** key가 존재하면 제거하고 true, 없으면 false */
    public boolean delete(String key) throws IOException {
        ObjectNode db = load();
        if (!db.has(key)) return false;
        db.remove(key);
        save(db);
        return true;
    }

    // ── 내부 유틸 ─────────────────────────────────────────────────────────

    private ObjectNode load() throws IOException {
        return (ObjectNode) handler.parse(dbFile);
    }

    private void save(ObjectNode db) throws IOException {
        handler.save(db, dbFile);
    }
}
