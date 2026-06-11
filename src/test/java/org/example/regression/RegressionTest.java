package org.example.regression;

import com.fasterxml.jackson.databind.JsonNode;
import org.example.db.JsonDatabase;
import org.example.handler.JsonHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression Test — 기존에 정상 동작하던 기능이 변경 이후에도 동일하게 동작하는지 확인한다.
 * 리팩토링·기능 추가 후 이 테스트가 전부 통과하면 기존 동작이 보존된 것이다.
 */
@DisplayName("[Regression] 기존 기능 동작 보존 검증")
class RegressionTest {

    @TempDir
    Path tempDir;

    private JsonHandler handler;
    private JsonDatabase db;

    @BeforeEach
    void setUp() throws IOException {
        handler = new JsonHandler();
        db = new JsonDatabase(handler, tempDir.resolve("db.json").toFile());
    }

    // ── JsonHandler 기본 동작 ────────────────────────────────────────────

    @Test
    @DisplayName("[Handler] JSON 문자열 파싱 후 필드 값이 유지된다")
    void handler_parse_string_preservesValues() throws IOException {
        JsonNode node = handler.parse("{\"fruit\":\"사과\",\"count\":3}");
        assertEquals("사과", node.get("fruit").asText());
        assertEquals(3, node.get("count").asInt());
    }

    @Test
    @DisplayName("[Handler] 파일 저장 후 다시 파싱하면 동일한 값을 반환한다")
    void handler_saveAndParse_roundTrip() throws IOException {
        File file = tempDir.resolve("test.json").toFile();
        JsonNode original = handler.parse("{\"key\":\"value\"}");

        handler.save(original, file);
        JsonNode loaded = handler.parse(file);

        assertEquals("value", loaded.get("key").asText());
    }

    @Test
    @DisplayName("[Handler] isValid는 올바른 JSON에 true, 잘못된 JSON에 false를 반환한다")
    void handler_isValid_correctlyClassifies() {
        assertTrue(handler.isValid("{\"k\":1}"));
        assertTrue(handler.isValid("[1,2,3]"));
        assertFalse(handler.isValid("{k: 1}"));
        assertFalse(handler.isValid(""));
    }

    // ── JsonDatabase CRUD 반환값 계약 ────────────────────────────────────

    @Test
    @DisplayName("[DB] put은 신규 키 등록 시 true를 반환한다")
    void db_put_newKey_returnsTrue() throws IOException {
        assertTrue(db.put("사과", handler.parse("3")));
    }

    @Test
    @DisplayName("[DB] put은 중복 키 등록 시 false를 반환한다")
    void db_put_duplicateKey_returnsFalse() throws IOException {
        db.put("사과", handler.parse("3"));
        assertFalse(db.put("사과", handler.parse("99")));
    }

    @Test
    @DisplayName("[DB] update는 존재하는 키 수정 시 true를 반환한다")
    void db_update_existingKey_returnsTrue() throws IOException {
        db.put("사과", handler.parse("3"));
        assertTrue(db.update("사과", handler.parse("10")));
    }

    @Test
    @DisplayName("[DB] update는 존재하지 않는 키 수정 시 false를 반환한다")
    void db_update_missingKey_returnsFalse() throws IOException {
        assertFalse(db.update("없는키", handler.parse("1")));
    }

    @Test
    @DisplayName("[DB] delete는 존재하는 키 삭제 시 true를 반환한다")
    void db_delete_existingKey_returnsTrue() throws IOException {
        db.put("사과", handler.parse("3"));
        assertTrue(db.delete("사과"));
    }

    @Test
    @DisplayName("[DB] delete는 존재하지 않는 키 삭제 시 false를 반환한다")
    void db_delete_missingKey_returnsFalse() throws IOException {
        assertFalse(db.delete("없는키"));
    }

    // ── 값 보존 계약 ─────────────────────────────────────────────────────

    @Test
    @DisplayName("[DB] 숫자 값이 put → get 과정에서 변하지 않는다")
    void db_numberValue_preservedOnRoundTrip() throws IOException {
        db.put("count", handler.parse("42"));
        assertEquals(42, db.get("count").get().asInt());
    }

    @Test
    @DisplayName("[DB] 문자열 값이 put → get 과정에서 변하지 않는다")
    void db_stringValue_preservedOnRoundTrip() throws IOException {
        db.put("color", handler.parse("\"노란색\""));
        assertEquals("노란색", db.get("color").get().asText());
    }

    @Test
    @DisplayName("[DB] 중첩 객체 값이 put → get 과정에서 변하지 않는다")
    void db_objectValue_preservedOnRoundTrip() throws IOException {
        db.put("item", handler.parse("{\"name\":\"노트북\",\"price\":1500000}"));
        JsonNode value = db.get("item").get();
        assertEquals("노트북", value.get("name").asText());
        assertEquals(1500000, value.get("price").asInt());
    }

    @Test
    @DisplayName("[DB] put이 중복 키를 거부했을 때 기존 값이 그대로 유지된다")
    void db_put_duplicate_originalValueUnchanged() throws IOException {
        db.put("사과", handler.parse("3"));
        db.put("사과", handler.parse("99")); // 무시됨

        assertEquals(3, db.get("사과").get().asInt());
    }

    // ── 독립성 계약: 한 키의 연산이 다른 키에 영향을 주지 않는다 ─────────

    @Test
    @DisplayName("[DB] update는 지정한 키만 변경하고 나머지 키는 유지한다")
    void db_update_doesNotAffectOtherKeys() throws IOException {
        db.put("사과", handler.parse("3"));
        db.put("바나나", handler.parse("5"));

        db.update("사과", handler.parse("10"));

        assertEquals(10, db.get("사과").get().asInt());
        assertEquals(5,  db.get("바나나").get().asInt());
    }

    @Test
    @DisplayName("[DB] delete는 지정한 키만 제거하고 나머지 키는 유지한다")
    void db_delete_doesNotAffectOtherKeys() throws IOException {
        db.put("사과", handler.parse("3"));
        db.put("바나나", handler.parse("5"));
        db.put("오렌지", handler.parse("2"));

        db.delete("바나나");

        assertTrue(db.exists("사과"));
        assertFalse(db.exists("바나나"));
        assertTrue(db.exists("오렌지"));
    }

    // ── 순서 보존 계약 ────────────────────────────────────────────────────

    @Test
    @DisplayName("[DB] getAll은 삽입 순서대로 key-value 쌍을 반환한다")
    void db_getAll_preservesInsertionOrder() throws IOException {
        db.put("첫번째", handler.parse("1"));
        db.put("두번째", handler.parse("2"));
        db.put("세번째", handler.parse("3"));

        List<String> keys = List.copyOf(db.getAll().keySet());

        assertEquals("첫번째", keys.get(0));
        assertEquals("두번째", keys.get(1));
        assertEquals("세번째", keys.get(2));
    }

    // ── 영속성 계약 ───────────────────────────────────────────────────────

    @Test
    @DisplayName("[DB] put 후 새 인스턴스로 파일을 로드해도 값이 유지된다")
    void db_put_persistsAcrossInstances() throws IOException {
        db.put("사과", handler.parse("3"));

        JsonDatabase reloaded = new JsonDatabase(handler, tempDir.resolve("db.json").toFile());
        assertEquals(3, reloaded.get("사과").get().asInt());
    }

    @Test
    @DisplayName("[DB] update 후 새 인스턴스로 파일을 로드해도 변경 값이 유지된다")
    void db_update_persistsAcrossInstances() throws IOException {
        db.put("사과", handler.parse("3"));
        db.update("사과", handler.parse("10"));

        JsonDatabase reloaded = new JsonDatabase(handler, tempDir.resolve("db.json").toFile());
        assertEquals(10, reloaded.get("사과").get().asInt());
    }

    @Test
    @DisplayName("[DB] delete 후 새 인스턴스로 파일을 로드해도 키가 없다")
    void db_delete_persistsAcrossInstances() throws IOException {
        db.put("사과", handler.parse("3"));
        db.delete("사과");

        JsonDatabase reloaded = new JsonDatabase(handler, tempDir.resolve("db.json").toFile());
        assertTrue(reloaded.get("사과").isEmpty());
    }

    // ── 파일 형식 계약 ────────────────────────────────────────────────────

    @Test
    @DisplayName("[DB] 저장된 db.json 파일은 들여쓰기가 있는 사람이 읽기 좋은 형태다")
    void db_savedFile_isPrettyPrinted() throws IOException {
        db.put("사과", handler.parse("3"));

        String content = Files.readString(tempDir.resolve("db.json"));
        assertTrue(content.contains("\n"), "줄바꿈이 없으면 pretty-print가 아님");
        assertTrue(content.contains("  "),  "들여쓰기가 없으면 pretty-print가 아님");
    }

    @Test
    @DisplayName("[DB] 저장된 db.json 파일은 항상 유효한 JSON이다")
    void db_savedFile_isAlwaysValidJson() throws IOException {
        db.put("사과", handler.parse("3"));
        db.put("바나나", handler.parse("\"노란색\""));
        db.update("사과", handler.parse("10"));
        db.delete("바나나");

        String content = Files.readString(tempDir.resolve("db.json"));
        assertTrue(handler.isValid(content));
    }

    // ── 전체 흐름 계약 ────────────────────────────────────────────────────

    @Test
    @DisplayName("[DB] put → get → update → delete 전체 CRUD 흐름이 정상 동작한다")
    void db_fullCrudCycle() throws IOException {
        // Create
        assertTrue(db.put("사과", handler.parse("3")));
        assertTrue(db.exists("사과"));

        // Read
        assertEquals(3, db.get("사과").get().asInt());

        // Update
        assertTrue(db.update("사과", handler.parse("10")));
        assertEquals(10, db.get("사과").get().asInt());

        // Delete
        assertTrue(db.delete("사과"));
        assertFalse(db.exists("사과"));
        assertTrue(db.get("사과").isEmpty());
    }
}
