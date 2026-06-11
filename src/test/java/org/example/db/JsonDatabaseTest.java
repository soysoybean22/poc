package org.example.db;

import com.fasterxml.jackson.databind.JsonNode;
import org.example.handler.JsonHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JsonDatabase")
class JsonDatabaseTest {

    @TempDir
    Path tempDir;

    private JsonHandler handler;
    private JsonDatabase db;

    @BeforeEach
    void setUp() throws IOException {
        handler = new JsonHandler();
        db = new JsonDatabase(handler, tempDir.resolve("db.json").toFile());
    }

    // ── 초기화 ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("DB 파일이 없으면 빈 JSON 객체로 자동 생성된다")
    void init_createsEmptyObjectFileIfNotExists() throws IOException {
        File newFile = tempDir.resolve("new_db.json").toFile();
        assertFalse(newFile.exists());

        new JsonDatabase(handler, newFile);

        assertTrue(newFile.exists());
        JsonNode content = handler.parse(newFile);
        assertTrue(content.isObject());
        assertEquals(0, content.size());
    }

    @Test
    @DisplayName("DB 파일이 이미 존재하면 기존 데이터를 유지한다")
    void init_preservesExistingData() throws IOException {
        File existingFile = tempDir.resolve("existing.json").toFile();
        handler.save(handler.parse("{\"사과\":3}"), existingFile);

        JsonDatabase existing = new JsonDatabase(handler, existingFile);

        assertEquals(3, existing.get("사과").get().asInt());
    }

    // ── Create (put) ──────────────────────────────────────────────────────

    @Test
    @DisplayName("새 키를 put하면 true를 반환하고 값이 저장된다")
    void put_newKey_returnsTrueAndStores() throws IOException {
        boolean result = db.put("사과", handler.parse("3"));

        assertTrue(result);
        assertEquals(3, db.get("사과").get().asInt());
    }

    @Test
    @DisplayName("이미 존재하는 키를 put하면 false를 반환하고 기존 값이 유지된다")
    void put_duplicateKey_returnsFalseAndKeepsOriginal() throws IOException {
        db.put("사과", handler.parse("3"));

        boolean result = db.put("사과", handler.parse("99"));

        assertFalse(result);
        assertEquals(3, db.get("사과").get().asInt()); // 기존 값 유지
    }

    @Test
    @DisplayName("문자열 값을 put하면 그대로 저장된다")
    void put_stringValue_stores() throws IOException {
        db.put("색깔", handler.parse("\"노란색\""));

        assertEquals("노란색", db.get("색깔").get().asText());
    }

    @Test
    @DisplayName("JSON 객체 값을 put하면 중첩 구조로 저장된다")
    void put_objectValue_stores() throws IOException {
        db.put("상품", handler.parse("{\"name\":\"노트북\",\"price\":1500000}"));

        JsonNode value = db.get("상품").get();
        assertEquals("노트북", value.get("name").asText());
        assertEquals(1500000, value.get("price").asInt());
    }

    @Test
    @DisplayName("put 후 파일을 새로 로드해도 데이터가 유지된다 (영속성)")
    void put_persistsToFile() throws IOException {
        db.put("사과", handler.parse("3"));

        JsonDatabase reloaded = new JsonDatabase(handler, tempDir.resolve("db.json").toFile());

        assertEquals(3, reloaded.get("사과").get().asInt());
    }

    // ── Read (get) ────────────────────────────────────────────────────────

    @Test
    @DisplayName("존재하는 키로 get하면 해당 값을 반환한다")
    void get_existingKey_returnsValue() throws IOException {
        db.put("바나나", handler.parse("5"));

        Optional<JsonNode> result = db.get("바나나");

        assertTrue(result.isPresent());
        assertEquals(5, result.get().asInt());
    }

    @Test
    @DisplayName("존재하지 않는 키로 get하면 empty를 반환한다")
    void get_missingKey_returnsEmpty() throws IOException {
        assertTrue(db.get("없는키").isEmpty());
    }

    @Test
    @DisplayName("getAll은 저장된 모든 key-value 쌍을 반환한다")
    void getAll_returnsAllEntries() throws IOException {
        db.put("사과",   handler.parse("3"));
        db.put("바나나", handler.parse("5"));
        db.put("오렌지", handler.parse("2"));

        Map<String, JsonNode> all = db.getAll();

        assertEquals(3, all.size());
        assertEquals(3, all.get("사과").asInt());
        assertEquals(5, all.get("바나나").asInt());
        assertEquals(2, all.get("오렌지").asInt());
    }

    @Test
    @DisplayName("빈 DB에서 getAll은 빈 Map을 반환한다")
    void getAll_empty_returnsEmptyMap() throws IOException {
        assertTrue(db.getAll().isEmpty());
    }

    @Test
    @DisplayName("exists는 존재하는 키에 true를 반환한다")
    void exists_presentKey_returnsTrue() throws IOException {
        db.put("사과", handler.parse("3"));
        assertTrue(db.exists("사과"));
    }

    @Test
    @DisplayName("exists는 존재하지 않는 키에 false를 반환한다")
    void exists_missingKey_returnsFalse() throws IOException {
        assertFalse(db.exists("없는키"));
    }

    // ── Update ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("존재하는 키를 update하면 true를 반환하고 값이 교체된다")
    void update_existingKey_returnsTrueAndReplacesValue() throws IOException {
        db.put("사과", handler.parse("3"));

        boolean result = db.update("사과", handler.parse("10"));

        assertTrue(result);
        assertEquals(10, db.get("사과").get().asInt());
    }

    @Test
    @DisplayName("존재하지 않는 키를 update하면 false를 반환하고 DB에 변화가 없다")
    void update_missingKey_returnsFalseAndNoChange() throws IOException {
        boolean result = db.update("없는키", handler.parse("99"));

        assertFalse(result);
        assertTrue(db.getAll().isEmpty());
    }

    @Test
    @DisplayName("update는 지정한 키만 변경하고 다른 키는 유지한다")
    void update_doesNotAffectOtherKeys() throws IOException {
        db.put("사과",   handler.parse("3"));
        db.put("바나나", handler.parse("5"));

        db.update("사과", handler.parse("10"));

        assertEquals(10, db.get("사과").get().asInt());
        assertEquals(5,  db.get("바나나").get().asInt()); // 변경 없음
    }

    @Test
    @DisplayName("update 후 파일을 새로 로드해도 변경된 값이 유지된다 (영속성)")
    void update_persistsToFile() throws IOException {
        db.put("사과", handler.parse("3"));
        db.update("사과", handler.parse("10"));

        JsonDatabase reloaded = new JsonDatabase(handler, tempDir.resolve("db.json").toFile());

        assertEquals(10, reloaded.get("사과").get().asInt());
    }

    // ── Delete ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("존재하는 키를 delete하면 true를 반환하고 키가 제거된다")
    void delete_existingKey_returnsTrueAndRemoves() throws IOException {
        db.put("사과", handler.parse("3"));

        boolean result = db.delete("사과");

        assertTrue(result);
        assertTrue(db.get("사과").isEmpty());
    }

    @Test
    @DisplayName("존재하지 않는 키를 delete하면 false를 반환한다")
    void delete_missingKey_returnsFalse() throws IOException {
        assertFalse(db.delete("없는키"));
    }

    @Test
    @DisplayName("delete는 지정한 키만 제거하고 다른 키는 유지한다")
    void delete_doesNotAffectOtherKeys() throws IOException {
        db.put("사과",   handler.parse("3"));
        db.put("바나나", handler.parse("5"));

        db.delete("사과");

        assertTrue(db.get("사과").isEmpty());
        assertEquals(5, db.get("바나나").get().asInt()); // 유지
    }

    @Test
    @DisplayName("delete 후 파일을 새로 로드해도 키가 제거되어 있다 (영속성)")
    void delete_persistsToFile() throws IOException {
        db.put("사과", handler.parse("3"));
        db.delete("사과");

        JsonDatabase reloaded = new JsonDatabase(handler, tempDir.resolve("db.json").toFile());

        assertTrue(reloaded.get("사과").isEmpty());
    }
}
