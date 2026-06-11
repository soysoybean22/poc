package org.example.safety;

import com.fasterxml.jackson.databind.JsonNode;
import org.example.db.JsonDatabase;
import org.example.handler.JsonHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Safety Test — edge 케이스와 오용(misuse) 시나리오에서 시스템이 안전하게 반응하는지 확인한다.
 * 예상치 못한 입력·상태에서도 데이터 손상 없이 명확한 결과를 반환해야 한다.
 */
@DisplayName("[Safety] Edge 케이스 및 오용 시나리오 검증")
class SafetyTest {

    @TempDir
    Path tempDir;

    private JsonHandler handler;
    private JsonDatabase db;

    @BeforeEach
    void setUp() throws IOException {
        handler = new JsonHandler();
        db = new JsonDatabase(handler, tempDir.resolve("db.json").toFile());
    }

    // ── 키 edge 케이스 ────────────────────────────────────────────────────

    @Test
    @DisplayName("[Key] 빈 문자열 키도 정상 저장·조회된다")
    void key_emptyString_worksCorrectly() throws IOException {
        db.put("", handler.parse("1"));

        assertTrue(db.exists(""));
        assertEquals(1, db.get("").get().asInt());
    }

    @Test
    @DisplayName("[Key] 공백을 포함한 키도 정상 저장·조회된다")
    void key_withSpaces_worksCorrectly() throws IOException {
        db.put("my key", handler.parse("1"));
        assertEquals(1, db.get("my key").get().asInt());
    }

    @Test
    @DisplayName("[Key] 특수문자를 포함한 키도 정상 저장·조회된다")
    void key_withSpecialChars_worksCorrectly() throws IOException {
        db.put("key!@#$%^&*()", handler.parse("1"));
        assertTrue(db.exists("key!@#$%^&*()"));
    }

    @Test
    @DisplayName("[Key] 한국어·유니코드 키도 정상 저장·조회된다")
    void key_unicode_worksCorrectly() throws IOException {
        db.put("사과🍎", handler.parse("3"));
        assertEquals(3, db.get("사과🍎").get().asInt());
    }

    @ParameterizedTest(name = "[Key] 다양한 특수 키: \"{0}\"")
    @ValueSource(strings = {".", "/", "\\", "\"quoted\"", "key:colon", "key\ttab"})
    @DisplayName("[Key] 다양한 특수 형태의 키가 손상 없이 저장·조회된다")
    void key_variousSpecialForms_storedAndRetrievedCorrectly(String key) throws IOException {
        db.put(key, handler.parse("1"));
        assertTrue(db.exists(key));
        assertEquals(1, db.get(key).get().asInt());
    }

    // ── 값 타입 edge 케이스 ───────────────────────────────────────────────

    @Test
    @DisplayName("[Value] boolean true 값이 정상 저장·조회된다")
    void value_booleanTrue_storedCorrectly() throws IOException {
        db.put("활성화", handler.parse("true"));
        assertTrue(db.get("활성화").get().asBoolean());
    }

    @Test
    @DisplayName("[Value] boolean false 값이 정상 저장·조회된다")
    void value_booleanFalse_storedCorrectly() throws IOException {
        db.put("비활성화", handler.parse("false"));
        assertFalse(db.get("비활성화").get().asBoolean());
    }

    @Test
    @DisplayName("[Value] JSON null 값이 정상 저장되고, 키는 존재하는 것으로 확인된다")
    void value_jsonNull_keyExistsButValueIsNull() throws IOException {
        db.put("없는값", handler.parse("null"));

        assertTrue(db.exists("없는값"));
        assertTrue(db.get("없는값").isPresent());
        assertTrue(db.get("없는값").get().isNull());
    }

    @Test
    @DisplayName("[Value] 빈 JSON 객체 값이 정상 저장·조회된다")
    void value_emptyObject_storedCorrectly() throws IOException {
        db.put("empty", handler.parse("{}"));
        assertTrue(db.get("empty").get().isObject());
        assertEquals(0, db.get("empty").get().size());
    }

    @Test
    @DisplayName("[Value] 빈 JSON 배열 값이 정상 저장·조회된다")
    void value_emptyArray_storedCorrectly() throws IOException {
        db.put("list", handler.parse("[]"));
        assertTrue(db.get("list").get().isArray());
        assertEquals(0, db.get("list").get().size());
    }

    @Test
    @DisplayName("[Value] JSON 배열 값이 정상 저장·조회된다")
    void value_array_storedCorrectly() throws IOException {
        db.put("tags", handler.parse("[\"사과\",\"바나나\",\"오렌지\"]"));
        JsonNode tags = db.get("tags").get();
        assertTrue(tags.isArray());
        assertEquals("사과",   tags.get(0).asText());
        assertEquals("바나나", tags.get(1).asText());
    }

    @Test
    @DisplayName("[Value] 음수 값이 정상 저장·조회된다")
    void value_negativeNumber_storedCorrectly() throws IOException {
        db.put("온도", handler.parse("-15"));
        assertEquals(-15, db.get("온도").get().asInt());
    }

    @Test
    @DisplayName("[Value] 소수점 값이 정밀도 손실 없이 저장·조회된다")
    void value_floatNumber_storedWithoutPrecisionLoss() throws IOException {
        db.put("비율", handler.parse("3.14"));
        assertEquals(3.14, db.get("비율").get().asDouble(), 0.0001);
    }

    @Test
    @DisplayName("[Value] 깊이 중첩된 객체도 손상 없이 저장·조회된다")
    void value_deeplyNestedObject_storedCorrectly() throws IOException {
        String nested = "{\"a\":{\"b\":{\"c\":{\"d\":\"깊은값\"}}}}";
        db.put("nested", handler.parse(nested));

        JsonNode value = db.get("nested").get();
        assertEquals("깊은값", value.path("a").path("b").path("c").path("d").asText());
    }

    // ── 오용 시나리오 ─────────────────────────────────────────────────────

    @Test
    @DisplayName("[Misuse] delete 후 같은 키로 put하면 정상 등록된다")
    void misuse_putAfterDelete_keyCanBeReused() throws IOException {
        db.put("사과", handler.parse("3"));
        db.delete("사과");

        boolean result = db.put("사과", handler.parse("99"));

        assertTrue(result);
        assertEquals(99, db.get("사과").get().asInt());
    }

    @Test
    @DisplayName("[Misuse] 같은 키를 연속으로 두 번 delete해도 두 번째는 false만 반환하고 예외가 없다")
    void misuse_deleteCalledTwice_secondReturnsFalse() throws IOException {
        db.put("사과", handler.parse("3"));

        assertTrue(db.delete("사과"));
        assertFalse(db.delete("사과")); // 이미 없는 키 — 예외 없이 false 반환
    }

    @Test
    @DisplayName("[Misuse] 존재하지 않는 키를 연속으로 update해도 DB가 변하지 않는다")
    void misuse_updateNonExistentKey_dbUnchanged() throws IOException {
        db.put("사과", handler.parse("3"));

        db.update("없는키", handler.parse("99"));
        db.update("없는키", handler.parse("100"));

        assertEquals(1, db.getAll().size()); // 사과 하나만 남아있어야 함
        assertEquals(3, db.get("사과").get().asInt());
    }

    @Test
    @DisplayName("[Misuse] update로 값 타입을 변경해도 정상 동작한다 (숫자 → 객체)")
    void misuse_updateChangesValueType_worksCorrectly() throws IOException {
        db.put("사과", handler.parse("3"));
        db.update("사과", handler.parse("{\"count\":3,\"color\":\"빨강\"}"));

        JsonNode value = db.get("사과").get();
        assertTrue(value.isObject());
        assertEquals("빨강", value.get("color").asText());
    }

    @Test
    @DisplayName("[Misuse] 모든 키를 삭제하면 DB가 빈 객체가 된다")
    void misuse_deleteAllKeys_dbBecomesEmpty() throws IOException {
        db.put("사과", handler.parse("1"));
        db.put("바나나", handler.parse("2"));
        db.delete("사과");
        db.delete("바나나");

        assertTrue(db.getAll().isEmpty());
        // 빈 상태에서도 새 데이터를 추가할 수 있어야 한다
        assertTrue(db.put("오렌지", handler.parse("3")));
        assertEquals(3, db.get("오렌지").get().asInt());
    }

    @Test
    @DisplayName("[Misuse] 동일 키에 put → update → delete를 반복해도 DB가 일관성을 유지한다")
    void misuse_repeatedOperationsOnSameKey_dbRemainsConsistent() throws IOException {
        for (int i = 1; i <= 5; i++) {
            db.put("키", handler.parse(String.valueOf(i)));
            db.update("키", handler.parse(String.valueOf(i * 10)));
            db.delete("키");
        }

        // 루프 후 DB는 완전히 비어있어야 한다
        assertTrue(db.getAll().isEmpty());
    }

    // ── 손상된 파일 시나리오 ──────────────────────────────────────────────

    @Test
    @DisplayName("[Corruption] db.json이 유효하지 않은 JSON이면 get 시 IOException이 발생한다")
    void corruption_invalidJsonFile_throwsIOException() throws IOException {
        File dbFile = tempDir.resolve("db.json").toFile();
        Files.writeString(dbFile.toPath(), "{ this is not valid json }");

        JsonDatabase corruptDb = new JsonDatabase(handler, dbFile) {
            // initFile은 파일이 이미 있으므로 실행되지 않아 손상된 내용이 유지됨
        };

        assertThrows(IOException.class, () -> corruptDb.get("key"));
    }

    @Test
    @DisplayName("[Corruption] db.json이 완전히 비어있으면 get 시 IOException이 발생한다")
    void corruption_emptyFile_throwsIOException() throws IOException {
        File dbFile = tempDir.resolve("db.json").toFile();
        Files.writeString(dbFile.toPath(), "");

        JsonDatabase corruptDb = new JsonDatabase(handler, dbFile) {};

        assertThrows(IOException.class, () -> corruptDb.get("key"));
    }

    // ── JsonHandler null 입력 안전성 ──────────────────────────────────────

    @Test
    @DisplayName("[Handler] isValid에 null을 전달하면 예외 없이 false를 반환한다")
    void handler_isValid_null_returnsFalse() {
        assertFalse(handler.isValid(null));
    }

    @Test
    @DisplayName("[Handler] parse에 유효한 JSON 문자열을 전달하면 항상 non-null을 반환한다")
    void handler_parse_validJson_returnsNonNull() throws IOException {
        assertNotNull(handler.parse("{}"));
        assertNotNull(handler.parse("[]"));
        assertNotNull(handler.parse("\"text\""));
        assertNotNull(handler.parse("42"));
    }
}
