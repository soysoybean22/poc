package org.example.repository;

import com.fasterxml.jackson.databind.JsonNode;
import org.example.handler.JsonHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JsonRepository")
class JsonRepositoryTest {

    @TempDir
    Path tempDir;

    private JsonHandler handler;
    private JsonRepository repo;

    @BeforeEach
    void setUp() throws IOException {
        handler = new JsonHandler();
        repo = new JsonRepository(handler, tempDir.resolve("records.json").toFile());
    }

    // ── 초기화 ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("데이터 파일이 없으면 빈 배열로 자동 생성된다")
    void constructor_createsEmptyFileIfNotExists() throws IOException {
        File newFile = tempDir.resolve("new_records.json").toFile();
        assertFalse(newFile.exists());

        new JsonRepository(handler, newFile);

        assertTrue(newFile.exists());
        JsonNode content = handler.parse(newFile);
        assertTrue(content.isArray());
        assertEquals(0, content.size());
    }

    @Test
    @DisplayName("데이터 파일이 이미 존재하면 기존 데이터를 유지한다")
    void constructor_usesExistingFileWithoutOverwrite() throws IOException {
        File existingFile = tempDir.resolve("existing.json").toFile();
        JsonNode initial = handler.parse("[{\"id\":1,\"name\":\"기존데이터\"}]");
        handler.save(initial, existingFile);

        JsonRepository existingRepo = new JsonRepository(handler, existingFile);

        assertEquals(1, existingRepo.findAll().size());
        assertEquals("기존데이터", existingRepo.findAll().get(0).get("name").asText());
    }

    // ── Create ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("첫 번째 create는 id=1을 부여한다")
    void create_firstRecord_assignsIdOne() throws IOException {
        JsonNode data = handler.parse("{\"name\":\"홍길동\"}");

        JsonNode saved = repo.create(data);

        assertEquals(1L, saved.get("id").asLong());
    }

    @Test
    @DisplayName("두 번째 create는 id가 1 증가한다")
    void create_secondRecord_incrementsId() throws IOException {
        repo.create(handler.parse("{\"name\":\"첫번째\"}"));
        JsonNode saved = repo.create(handler.parse("{\"name\":\"두번째\"}"));

        assertEquals(2L, saved.get("id").asLong());
    }

    @Test
    @DisplayName("create 후 사용자가 입력한 필드가 저장된다")
    void create_preservesUserFields() throws IOException {
        JsonNode data = handler.parse("{\"name\":\"홍길동\",\"score\":95,\"grade\":\"A\"}");

        JsonNode saved = repo.create(data);

        assertEquals("홍길동", saved.get("name").asText());
        assertEquals(95, saved.get("score").asInt());
        assertEquals("A", saved.get("grade").asText());
    }

    @Test
    @DisplayName("입력 데이터에 id가 포함되어 있어도 자동 부여된 id로 덮어쓴다")
    void create_overridesUserProvidedId() throws IOException {
        JsonNode data = handler.parse("{\"id\":999,\"name\":\"테스트\"}");

        JsonNode saved = repo.create(data);

        assertEquals(1L, saved.get("id").asLong()); // 999가 아닌 자동 부여값
    }

    @Test
    @DisplayName("create된 레코드는 파일에 실제로 저장된다")
    void create_persistsToFile() throws IOException {
        repo.create(handler.parse("{\"name\":\"홍길동\"}"));

        // 같은 파일로 새 repo 인스턴스를 만들어 파일 내용을 검증
        JsonRepository freshRepo = new JsonRepository(handler, tempDir.resolve("records.json").toFile());
        List<JsonNode> records = freshRepo.findAll();

        assertEquals(1, records.size());
        assertEquals("홍길동", records.get(0).get("name").asText());
    }

    @Test
    @DisplayName("필드 구조가 서로 다른 데이터도 같은 파일에 저장된다")
    void create_differentSchemas_storedTogether() throws IOException {
        repo.create(handler.parse("{\"name\":\"홍길동\",\"score\":95}"));
        repo.create(handler.parse("{\"product\":\"노트북\",\"price\":1500000}"));

        List<JsonNode> all = repo.findAll();

        assertEquals(2, all.size());
        assertEquals("홍길동", all.get(0).get("name").asText());
        assertEquals("노트북", all.get(1).get("product").asText());
    }

    // ── Read ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("데이터가 없을 때 findAll은 빈 리스트를 반환한다")
    void findAll_empty_returnsEmptyList() throws IOException {
        assertTrue(repo.findAll().isEmpty());
    }

    @Test
    @DisplayName("findAll은 저장된 모든 레코드를 반환한다")
    void findAll_returnsAllRecords() throws IOException {
        repo.create(handler.parse("{\"name\":\"A\"}"));
        repo.create(handler.parse("{\"name\":\"B\"}"));
        repo.create(handler.parse("{\"name\":\"C\"}"));

        assertEquals(3, repo.findAll().size());
    }

    @Test
    @DisplayName("존재하는 id로 findById 호출 시 해당 레코드를 반환한다")
    void findById_existing_returnsRecord() throws IOException {
        repo.create(handler.parse("{\"name\":\"홍길동\"}"));
        repo.create(handler.parse("{\"name\":\"김영희\"}"));

        Optional<JsonNode> result = repo.findById(2L);

        assertTrue(result.isPresent());
        assertEquals("김영희", result.get().get("name").asText());
        assertEquals(2L, result.get().get("id").asLong());
    }

    @Test
    @DisplayName("존재하지 않는 id로 findById 호출 시 empty를 반환한다")
    void findById_notExisting_returnsEmpty() throws IOException {
        repo.create(handler.parse("{\"name\":\"홍길동\"}"));

        Optional<JsonNode> result = repo.findById(999L);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("빈 저장소에서 findById 호출 시 empty를 반환한다")
    void findById_emptyRepo_returnsEmpty() throws IOException {
        assertTrue(repo.findById(1L).isEmpty());
    }

    // ── Update ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("존재하는 id를 update하면 true를 반환하고 내용이 변경된다")
    void update_existing_returnsTrueAndChangesContent() throws IOException {
        repo.create(handler.parse("{\"name\":\"홍길동\",\"score\":80}"));

        boolean result = repo.update(1L, handler.parse("{\"name\":\"홍길동\",\"score\":100}"));

        assertTrue(result);
        assertEquals(100, repo.findById(1L).get().get("score").asInt());
    }

    @Test
    @DisplayName("update 후 id는 변경되지 않는다")
    void update_preservesId() throws IOException {
        repo.create(handler.parse("{\"name\":\"홍길동\"}"));
        repo.update(1L, handler.parse("{\"id\":999,\"name\":\"변경\"}"));

        assertEquals(1L, repo.findById(1L).get().get("id").asLong());
    }

    @Test
    @DisplayName("존재하지 않는 id를 update하면 false를 반환한다")
    void update_notExisting_returnsFalse() throws IOException {
        boolean result = repo.update(999L, handler.parse("{\"name\":\"없는사람\"}"));

        assertFalse(result);
    }

    @Test
    @DisplayName("update는 지정한 id의 레코드만 변경하고 나머지는 유지한다")
    void update_doesNotAffectOtherRecords() throws IOException {
        repo.create(handler.parse("{\"name\":\"홍길동\"}"));
        repo.create(handler.parse("{\"name\":\"김영희\"}"));

        repo.update(1L, handler.parse("{\"name\":\"이순신\"}"));

        assertEquals("이순신", repo.findById(1L).get().get("name").asText());
        assertEquals("김영희", repo.findById(2L).get().get("name").asText()); // 변경 없음
    }

    // ── Delete ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("존재하는 id를 delete하면 true를 반환한다")
    void delete_existing_returnsTrue() throws IOException {
        repo.create(handler.parse("{\"name\":\"홍길동\"}"));

        assertTrue(repo.delete(1L));
    }

    @Test
    @DisplayName("delete 후 해당 id로 findById하면 empty를 반환한다")
    void delete_existing_recordIsGone() throws IOException {
        repo.create(handler.parse("{\"name\":\"홍길동\"}"));
        repo.delete(1L);

        assertTrue(repo.findById(1L).isEmpty());
    }

    @Test
    @DisplayName("존재하지 않는 id를 delete하면 false를 반환한다")
    void delete_notExisting_returnsFalse() throws IOException {
        assertFalse(repo.delete(999L));
    }

    @Test
    @DisplayName("delete는 지정한 id의 레코드만 제거하고 나머지는 유지한다")
    void delete_doesNotAffectOtherRecords() throws IOException {
        repo.create(handler.parse("{\"name\":\"홍길동\"}"));
        repo.create(handler.parse("{\"name\":\"김영희\"}"));
        repo.create(handler.parse("{\"name\":\"이순신\"}"));

        repo.delete(2L);

        List<JsonNode> remaining = repo.findAll();
        assertEquals(2, remaining.size());
        assertTrue(repo.findById(1L).isPresent());
        assertTrue(repo.findById(2L).isEmpty());
        assertTrue(repo.findById(3L).isPresent());
    }

    @Test
    @DisplayName("delete 후 파일에서도 해당 레코드가 제거된다")
    void delete_persistsToFile() throws IOException {
        repo.create(handler.parse("{\"name\":\"홍길동\"}"));
        repo.delete(1L);

        JsonRepository freshRepo = new JsonRepository(handler, tempDir.resolve("records.json").toFile());
        assertTrue(freshRepo.findAll().isEmpty());
    }
}
