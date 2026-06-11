package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import org.example.db.JsonDatabase;
import org.example.handler.JsonHandler;

import java.io.File;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;

/**
 * JSON 파일 DB 기반 CRUD 콘솔 앱.
 * 키를 입력하면 값을 조회하고, 키-값 쌍을 추가/수정/삭제한다.
 * 앱을 종료해도 data/db.json에 데이터가 유지된다.
 */
public class CrudApp {

    private static final Scanner sc = new Scanner(System.in);
    private static JsonHandler handler;
    private static JsonDatabase db;

    public static void main(String[] args) throws Exception {
        handler = new JsonHandler();
        db = new JsonDatabase(handler, new File("data/db.json"));
        System.out.println("DB 파일: data/db.json");

        while (true) {
            printMenu();
            String choice = sc.nextLine().trim();
            System.out.println();
            switch (choice) {
                case "1" -> readAll();
                case "2" -> readByKey();
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
        System.out.println("     JSON DB  (key-value CRUD)");
        System.out.println("══════════════════════════════════════");
        System.out.println(" 1. 전체 조회");
        System.out.println(" 2. 키로 조회");
        System.out.println(" 3. 추가");
        System.out.println(" 4. 수정");
        System.out.println(" 5. 삭제");
        System.out.println(" 0. 종료");
        System.out.println("══════════════════════════════════════");
        System.out.print("메뉴 선택 > ");
    }

    // ── 1. 전체 조회 ──────────────────────────────────────────────────────

    private static void readAll() throws Exception {
        Map<String, JsonNode> all = db.getAll();
        if (all.isEmpty()) {
            System.out.println("저장된 데이터가 없습니다.\n");
            return;
        }
        System.out.println("[전체 목록 - " + all.size() + "건]");
        all.forEach((key, value) ->
                System.out.printf("  %-15s → %s%n", key, value)
        );
        System.out.println();
    }

    // ── 2. 키로 조회 ──────────────────────────────────────────────────────

    private static void readByKey() throws Exception {
        System.out.print("키 입력 > ");
        String key = sc.nextLine().trim();

        Optional<JsonNode> result = db.get(key);
        if (result.isPresent()) {
            System.out.println(key + " → " + result.get().toPrettyString());
        } else {
            System.out.println("'" + key + "' 키가 존재하지 않습니다.");
        }
        System.out.println();
    }

    // ── 3. 추가 ───────────────────────────────────────────────────────────

    private static void create() throws Exception {
        System.out.print("키 > ");
        String key = sc.nextLine().trim();

        if (db.exists(key)) {
            System.out.println("'" + key + "' 키가 이미 존재합니다. 수정은 메뉴 4를 이용하세요.\n");
            return;
        }

        System.out.print("값 > ");
        String input = sc.nextLine().trim();
        JsonNode value = parseValue(input);

        db.put(key, value);
        System.out.println("추가 완료: " + key + " → " + value + "\n");
    }

    // ── 4. 수정 ───────────────────────────────────────────────────────────

    private static void update() throws Exception {
        System.out.print("수정할 키 > ");
        String key = sc.nextLine().trim();

        Optional<JsonNode> existing = db.get(key);
        if (existing.isEmpty()) {
            System.out.println("'" + key + "' 키가 존재하지 않습니다.\n");
            return;
        }

        System.out.println("현재 값: " + existing.get());
        System.out.print("새 값   > ");
        String input = sc.nextLine().trim();
        JsonNode newValue = parseValue(input);

        db.update(key, newValue);
        System.out.println("수정 완료: " + key + " → " + newValue + "\n");
    }

    // ── 5. 삭제 ───────────────────────────────────────────────────────────

    private static void delete() throws Exception {
        System.out.print("삭제할 키 > ");
        String key = sc.nextLine().trim();

        Optional<JsonNode> existing = db.get(key);
        if (existing.isEmpty()) {
            System.out.println("'" + key + "' 키가 존재하지 않습니다.\n");
            return;
        }

        System.out.println("삭제 대상: " + key + " → " + existing.get());
        System.out.print("정말 삭제하시겠습니까? (y/n) > ");
        if (!sc.nextLine().trim().equalsIgnoreCase("y")) {
            System.out.println("삭제가 취소되었습니다.\n");
            return;
        }

        db.delete(key);
        System.out.println("삭제 완료!\n");
    }

    // ── 값 파싱 헬퍼 ──────────────────────────────────────────────────────

    /**
     * 입력 문자열을 JsonNode로 변환한다.
     * - 유효한 JSON (숫자, true/false, 배열, 객체, 따옴표 문자열)이면 그대로 파싱
     * - 그 외 평문 텍스트는 JSON 문자열로 자동 변환
     */
    private static JsonNode parseValue(String input) throws Exception {
        if (handler.isValid(input)) {
            return handler.parse(input);
        }
        return handler.parse("\"" + input.replace("\"", "\\\"") + "\"");
    }
}
