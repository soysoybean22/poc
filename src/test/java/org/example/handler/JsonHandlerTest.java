package org.example.handler;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JsonHandler")
class JsonHandlerTest {

    private JsonHandler handler;

    @BeforeEach
    void setUp() {
        handler = new JsonHandler();
    }

    // ── parse(String) ─────────────────────────────────────────────────────

    @Test
    @DisplayName("JSON 객체 문자열을 파싱하면 올바른 필드 값을 반환한다")
    void parse_string_object() throws IOException {
        String json = "{\"name\":\"홍길동\",\"score\":95}";

        JsonNode node = handler.parse(json);

        assertEquals("홍길동", node.get("name").asText());
        assertEquals(95, node.get("score").asInt());
    }

    @Test
    @DisplayName("JSON 배열 문자열을 파싱하면 ArrayNode를 반환한다")
    void parse_string_array() throws IOException {
        String json = "[{\"id\":1},{\"id\":2}]";

        JsonNode node = handler.parse(json);

        assertTrue(node.isArray());
        assertEquals(2, node.size());
        assertEquals(1, node.get(0).get("id").asInt());
        assertEquals(2, node.get(1).get("id").asInt());
    }

    @Test
    @DisplayName("중첩된 JSON 객체를 파싱하면 중첩 필드에 접근할 수 있다")
    void parse_string_nested() throws IOException {
        String json = "{\"user\":{\"address\":{\"city\":\"서울\"}}}";

        JsonNode node = handler.parse(json);

        assertEquals("서울", node.path("user").path("address").path("city").asText());
    }

    @Test
    @DisplayName("유효하지 않은 JSON 문자열을 파싱하면 IOException이 발생한다")
    void parse_string_invalidJson_throwsIOException() {
        assertThrows(IOException.class, () -> handler.parse("{name: 홍길동}"));
    }

    // ── parse(File) ───────────────────────────────────────────────────────

    @Test
    @DisplayName("JSON 파일을 파싱하면 올바른 JsonNode를 반환한다")
    void parse_file_returnsCorrectNode(@TempDir Path tempDir) throws IOException {
        File file = tempDir.resolve("test.json").toFile();
        JsonNode original = handler.parse("{\"product\":\"노트북\",\"price\":1500000}");
        handler.save(original, file);

        JsonNode loaded = handler.parse(file);

        assertEquals("노트북", loaded.get("product").asText());
        assertEquals(1500000, loaded.get("price").asInt());
    }

    @Test
    @DisplayName("존재하지 않는 파일을 파싱하면 IOException이 발생한다")
    void parse_file_notExist_throwsIOException(@TempDir Path tempDir) {
        File missing = tempDir.resolve("missing.json").toFile();

        assertThrows(IOException.class, () -> handler.parse(missing));
    }

    // ── save(JsonNode, File) ──────────────────────────────────────────────

    @Test
    @DisplayName("save 호출 후 파일이 생성되고 파싱하면 동일한 내용을 반환한다")
    void save_createsFileWithCorrectContent(@TempDir Path tempDir) throws IOException {
        File file = tempDir.resolve("output.json").toFile();
        JsonNode node = handler.parse("{\"key\":\"value\"}");

        handler.save(node, file);
        JsonNode loaded = handler.parse(file);

        assertTrue(file.exists());
        assertEquals("value", loaded.get("key").asText());
    }

    @Test
    @DisplayName("저장 경로의 부모 디렉토리가 없어도 save가 자동으로 생성한다")
    void save_createsParentDirectoriesAutomatically(@TempDir Path tempDir) throws IOException {
        File file = tempDir.resolve("subdir/nested/output.json").toFile();
        JsonNode node = handler.parse("{\"key\":\"value\"}");

        assertDoesNotThrow(() -> handler.save(node, file));
        assertTrue(file.exists());
    }

    // ── stringify(JsonNode) ───────────────────────────────────────────────

    @Test
    @DisplayName("stringify는 JsonNode를 JSON 문자열로 변환한다")
    void stringify_returnsJsonString() throws IOException {
        JsonNode node = handler.parse("{\"name\":\"테스트\"}");

        String result = handler.stringify(node);

        assertNotNull(result);
        assertTrue(result.contains("테스트"));
        // 다시 파싱해도 동일한 값을 가져야 한다
        assertEquals("테스트", handler.parse(result).get("name").asText());
    }

    // ── isValid(String) ───────────────────────────────────────────────────

    @Test
    @DisplayName("올바른 JSON 객체 문자열이면 true를 반환한다")
    void isValid_validObject_returnsTrue() {
        assertTrue(handler.isValid("{\"key\":\"value\"}"));
    }

    @Test
    @DisplayName("올바른 JSON 배열 문자열이면 true를 반환한다")
    void isValid_validArray_returnsTrue() {
        assertTrue(handler.isValid("[1,2,3]"));
    }

    @Test
    @DisplayName("올바른 JSON 빈 배열이면 true를 반환한다")
    void isValid_emptyArray_returnsTrue() {
        assertTrue(handler.isValid("[]"));
    }

    @Test
    @DisplayName("키에 따옴표가 없는 JSON이면 false를 반환한다")
    void isValid_unquotedKey_returnsFalse() {
        assertFalse(handler.isValid("{name: \"홍길동\"}"));
    }

    @Test
    @DisplayName("빈 문자열이면 false를 반환한다")
    void isValid_emptyString_returnsFalse() {
        assertFalse(handler.isValid(""));
    }

    @Test
    @DisplayName("완전히 잘못된 문자열이면 false를 반환한다")
    void isValid_randomString_returnsFalse() {
        assertFalse(handler.isValid("not json at all"));
    }
}
