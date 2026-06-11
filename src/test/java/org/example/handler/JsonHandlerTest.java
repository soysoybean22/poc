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

    @Test
    @DisplayName("JSON 객체 문자열을 파싱하면 올바른 필드 값을 반환한다")
    void parse_string_object() throws IOException {
        JsonNode node = handler.parse("{\"name\":\"사과\",\"count\":3}");

        assertEquals("사과", node.get("name").asText());
        assertEquals(3, node.get("count").asInt());
    }

    @Test
    @DisplayName("JSON 배열 문자열을 파싱하면 ArrayNode를 반환한다")
    void parse_string_array() throws IOException {
        JsonNode node = handler.parse("[1, 2, 3]");

        assertTrue(node.isArray());
        assertEquals(3, node.size());
    }

    @Test
    @DisplayName("유효하지 않은 JSON 문자열을 파싱하면 IOException이 발생한다")
    void parse_string_invalid_throwsIOException() {
        assertThrows(IOException.class, () -> handler.parse("{name: 사과}"));
    }

    @Test
    @DisplayName("JSON 파일을 파싱하면 저장 당시와 동일한 값을 반환한다")
    void parse_file_roundTrip(@TempDir Path tempDir) throws IOException {
        File file = tempDir.resolve("test.json").toFile();
        JsonNode original = handler.parse("{\"key\":\"value\"}");
        handler.save(original, file);

        JsonNode loaded = handler.parse(file);

        assertEquals("value", loaded.get("key").asText());
    }

    @Test
    @DisplayName("존재하지 않는 파일을 파싱하면 IOException이 발생한다")
    void parse_file_missing_throwsIOException(@TempDir Path tempDir) {
        assertThrows(IOException.class,
                () -> handler.parse(tempDir.resolve("missing.json").toFile()));
    }

    @Test
    @DisplayName("save 후 파일이 생성되고 내용이 일치한다")
    void save_createsFileWithContent(@TempDir Path tempDir) throws IOException {
        File file = tempDir.resolve("out.json").toFile();
        JsonNode node = handler.parse("{\"fruit\":\"사과\"}");

        handler.save(node, file);

        assertTrue(file.exists());
        assertEquals("사과", handler.parse(file).get("fruit").asText());
    }

    @Test
    @DisplayName("save는 존재하지 않는 부모 디렉토리를 자동으로 생성한다")
    void save_createsParentDirectories(@TempDir Path tempDir) throws IOException {
        File file = tempDir.resolve("a/b/c/out.json").toFile();

        assertDoesNotThrow(() -> handler.save(handler.parse("{}"), file));
        assertTrue(file.exists());
    }

    @Test
    @DisplayName("stringify는 JsonNode를 JSON 문자열로 변환하며, 재파싱해도 동일한 값을 가진다")
    void stringify_roundTrip() throws IOException {
        JsonNode node = handler.parse("{\"사과\":3}");

        String json = handler.stringify(node);
        JsonNode reparsed = handler.parse(json);

        assertEquals(3, reparsed.get("사과").asInt());
    }

    @Test
    @DisplayName("isValid는 올바른 JSON에 대해 true를 반환한다")
    void isValid_validJson_returnsTrue() {
        assertTrue(handler.isValid("{\"key\":\"value\"}"));
        assertTrue(handler.isValid("[1,2,3]"));
        assertTrue(handler.isValid("42"));
        assertTrue(handler.isValid("[]"));
    }

    @Test
    @DisplayName("isValid는 잘못된 JSON에 대해 false를 반환한다")
    void isValid_invalidJson_returnsFalse() {
        assertFalse(handler.isValid("{name: 홍길동}"));
        assertFalse(handler.isValid(""));
        assertFalse(handler.isValid("not json"));
    }
}
