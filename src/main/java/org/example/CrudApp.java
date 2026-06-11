package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import org.example.handler.JsonHandler;
import org.example.repository.JsonRepository;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

/**
 * JSON 파일 기반 CRUD 콘솔 애플리케이션.
 * 레코드는 사용자가 직접 JSON 문자열로 입력하며, id는 자동 부여된다.
 */
public class CrudApp {

    private static final Scanner sc = new Scanner(System.in);
    private static JsonHandler handler;
    private static JsonRepository repo;

    public static void main(String[] args) throws Exception {
        handler = new JsonHandler();
        repo = new JsonRepository(handler, new File("data/records.json"));
        System.out.println("데이터 파일: data/records.json");

        while (true) {
            printMenu();
            String choice = sc.nextLine().trim();
            System.out.println();
            switch (choice) {
                case "1" -> readAll();
                case "2" -> readById();
                case "3" -> create();
                case "4" -> update();
                case "5" -> delete();
                case "0" -> { System.out.println("종료합니다."); return; }
                default  -> System.out.println("[오류] 올바른 번호를 입력하세요.\n");
            }
        }
    }

    // ── 메뉴 ──────────────────────────────────────────────────────────────

    private static void printMenu() {
        System.out.println("══════════════════════════════════════");
        System.out.println("     JSON 데이터 관리  (CRUD)");
        System.out.println("══════════════════════════════════════");
        System.out.println(" 1. 전체 조회");
        System.out.println(" 2. ID 조회");
        System.out.println(" 3. 등록  (JSON 입력)");
        System.out.println(" 4. 수정  (JSON 입력)");
        System.out.println(" 5. 삭제");
        System.out.println(" 0. 종료");
        System.out.println("══════════════════════════════════════");
        System.out.print("메뉴 선택 > ");
    }

    // ── 1. 전체 조회 ──────────────────────────────────────────────────────

    private static void readAll() throws Exception {
        List<JsonNode> records = repo.findAll();
        if (records.isEmpty()) {
            System.out.println("저장된 데이터가 없습니다.\n");
            return;
        }
        System.out.println("[전체 목록 - " + records.size() + "건]");
        records.forEach(r -> System.out.println(r.toPrettyString()));
        System.out.println();
    }

    // ── 2. ID 조회 ────────────────────────────────────────────────────────

    private static void readById() throws Exception {
        System.out.print("조회할 ID > ");
        Long id = parseLong(sc.nextLine());
        if (id == null) return;

        Optional<JsonNode> result = repo.findById(id);
        if (result.isPresent()) {
            System.out.println("[조회 결과]");
            System.out.println(result.get().toPrettyString());
        } else {
            System.out.println("ID " + id + " 에 해당하는 데이터가 없습니다.");
        }
        System.out.println();
    }

    // ── 3. 등록 ───────────────────────────────────────────────────────────

    private static void create() throws Exception {
        System.out.println("[데이터 등록]");
        System.out.println("저장할 JSON을 입력하세요. (id 필드는 자동 부여)");
        System.out.println("예시: {\"name\":\"홍길동\",\"score\":95}");
        System.out.print("입력 > ");
        String input = sc.nextLine().trim();

        if (!handler.isValid(input)) {
            System.out.println("[오류] 올바른 JSON 형식이 아닙니다.\n");
            return;
        }

        JsonNode data = handler.parse(input);
        JsonNode saved = repo.create(data);
        System.out.println("등록 완료! (부여된 ID: " + saved.get("id").asLong() + ")");
        System.out.println(saved.toPrettyString() + "\n");
    }

    // ── 4. 수정 ───────────────────────────────────────────────────────────

    private static void update() throws Exception {
        System.out.print("수정할 ID > ");
        Long id = parseLong(sc.nextLine());
        if (id == null) return;

        Optional<JsonNode> existing = repo.findById(id);
        if (existing.isEmpty()) {
            System.out.println("ID " + id + " 에 해당하는 데이터가 없습니다.\n");
            return;
        }

        System.out.println("[현재 데이터]");
        System.out.println(existing.get().toPrettyString());

        System.out.println("새 JSON을 입력하세요. (id 필드는 유지됩니다)");
        System.out.print("입력 > ");
        String input = sc.nextLine().trim();

        if (!handler.isValid(input)) {
            System.out.println("[오류] 올바른 JSON 형식이 아닙니다.\n");
            return;
        }

        JsonNode newData = handler.parse(input);
        repo.update(id, newData);
        System.out.println("수정 완료!\n");
    }

    // ── 5. 삭제 ───────────────────────────────────────────────────────────

    private static void delete() throws Exception {
        System.out.print("삭제할 ID > ");
        Long id = parseLong(sc.nextLine());
        if (id == null) return;

        Optional<JsonNode> existing = repo.findById(id);
        if (existing.isEmpty()) {
            System.out.println("ID " + id + " 에 해당하는 데이터가 없습니다.\n");
            return;
        }

        System.out.println("[삭제 대상]");
        System.out.println(existing.get().toPrettyString());
        System.out.print("정말 삭제하시겠습니까? (y/n) > ");

        if (!sc.nextLine().trim().equalsIgnoreCase("y")) {
            System.out.println("삭제가 취소되었습니다.\n");
            return;
        }

        repo.delete(id);
        System.out.println("삭제 완료!\n");
    }

    // ── 입력 헬퍼 ─────────────────────────────────────────────────────────

    private static Long parseLong(String input) {
        try {
            return Long.parseLong(input.trim());
        } catch (NumberFormatException e) {
            System.out.println("[오류] 숫자를 입력해주세요.\n");
            return null;
        }
    }
}
