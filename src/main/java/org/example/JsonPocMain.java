package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import org.example.db.JsonDatabase;
import org.example.handler.JsonHandler;

import java.io.File;
import java.util.Map;

/**
 * JSON DB POC 진입점.
 * 목표: JSON 파일이 DB 서버처럼 동작함을 검증한다.
 *       앱을 재시작해도 데이터가 유지되는(영속성) 구조를 확인한다.
 */
public class JsonPocMain {

    public static void main(String[] args) throws Exception {
        JsonHandler handler = new JsonHandler();
        JsonDatabase db = new JsonDatabase(handler, new File("data/db.json"));

        // ── Step 1. 데이터 저장 (JSON 파일 = DB 서버에 쓰기) ─────────────
        System.out.println("=== Step 1. DB에 데이터 저장 ===");
        db.put("사과",   handler.parse("3"));
        db.put("바나나", handler.parse("5"));
        db.put("오렌지", handler.parse("2"));
        db.put("상품",   handler.parse("{\"name\":\"노트북\",\"price\":1500000}"));
        System.out.println("저장 완료 → data/db.json 확인");

        // ── Step 2. 키로 조회 (key 입력 → value 출력) ─────────────────────
        System.out.println("\n=== Step 2. 키로 조회 ===");
        System.out.println("사과   → " + db.get("사과").map(JsonNode::asText).orElse("없음"));
        System.out.println("바나나 → " + db.get("바나나").map(JsonNode::asText).orElse("없음"));
        System.out.println("없는키 → " + db.get("포도").map(JsonNode::asText).orElse("없음"));

        // ── Step 3. 전체 조회 ────────────────────────────────────────────
        System.out.println("\n=== Step 3. 전체 조회 ===");
        for (Map.Entry<String, JsonNode> entry : db.getAll().entrySet()) {
            System.out.println("  " + entry.getKey() + " → " + entry.getValue());
        }

        // ── Step 4. 값 수정 ───────────────────────────────────────────────
        System.out.println("\n=== Step 4. 값 수정 (사과: 3 → 10) ===");
        db.update("사과", handler.parse("10"));
        System.out.println("사과 → " + db.get("사과").map(JsonNode::asText).orElse("없음"));

        // ── Step 5. 삭제 ──────────────────────────────────────────────────
        System.out.println("\n=== Step 5. 삭제 (바나나) ===");
        db.delete("바나나");
        System.out.println("바나나 존재 여부: " + db.exists("바나나"));

        // ── Step 6. 영속성 확인 ──────────────────────────────────────────
        System.out.println("\n=== Step 6. 영속성 확인 (파일에서 새로 로드) ===");
        JsonDatabase reloaded = new JsonDatabase(handler, new File("data/db.json"));
        for (Map.Entry<String, JsonNode> entry : reloaded.getAll().entrySet()) {
            System.out.println("  " + entry.getKey() + " → " + entry.getValue());
        }

        System.out.println("\nPOC 완료. data/db.json 에서 최종 상태를 확인하세요.");
    }
}
