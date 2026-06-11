package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import org.example.handler.JsonHandler;

import java.io.File;

/**
 * JSON POC 진입점.
 * 목표: JSON 데이터를 받아 파싱하고 파일로 저장하는 핵심 흐름을 검증한다.
 */
public class JsonPocMain {

    public static void main(String[] args) throws Exception {
        JsonHandler handler = new JsonHandler();
        File outputDir = new File("data");
        outputDir.mkdirs();

        // ── Step 1. JSON 문자열 파싱 ─────────────────────────────────────
        System.out.println("=== Step 1. JSON 문자열 파싱 ===");

        String jsonString = """
                {
                  "id": 1,
                  "product": "노트북",
                  "price": 1500000,
                  "stock": 10,
                  "tags": ["전자제품", "IT"]
                }
                """;

        JsonNode node = handler.parse(jsonString);

        System.out.println("product : " + node.get("product").asText());
        System.out.println("price   : " + node.get("price").asInt());
        System.out.println("stock   : " + node.get("stock").asInt());
        System.out.println("tags[0] : " + node.get("tags").get(0).asText());

        // ── Step 2. 파싱한 데이터를 JSON 파일로 저장 ─────────────────────
        System.out.println("\n=== Step 2. JSON 파일 저장 ===");

        File saveFile = new File(outputDir, "sample.json");
        handler.save(node, saveFile);
        System.out.println("저장 완료: " + saveFile.getAbsolutePath());

        // ── Step 3. 저장된 파일을 다시 파싱 ─────────────────────────────
        System.out.println("\n=== Step 3. JSON 파일 파싱 ===");

        JsonNode loaded = handler.parse(saveFile);
        System.out.println("파일에서 읽은 product : " + loaded.get("product").asText());
        System.out.println("파일에서 읽은 price   : " + loaded.get("price").asInt());

        // ── Step 4. JSON 배열 파싱 ───────────────────────────────────────
        System.out.println("\n=== Step 4. JSON 배열 파싱 ===");

        String arrayJson = """
                [
                  {"id": 1, "name": "사과", "price": 1000},
                  {"id": 2, "name": "바나나", "price": 500},
                  {"id": 3, "name": "오렌지", "price": 1500}
                ]
                """;

        JsonNode array = handler.parse(arrayJson);
        for (JsonNode item : array) {
            System.out.printf("  id=%-3s name=%-8s price=%s%n",
                    item.get("id").asText(),
                    item.get("name").asText(),
                    item.get("price").asText());
        }

        // ── Step 5. JSON 배열을 파일로 저장 ─────────────────────────────
        System.out.println("\n=== Step 5. JSON 배열 파일 저장 ===");

        File arrayFile = new File(outputDir, "items.json");
        handler.save(array, arrayFile);
        System.out.println("저장 완료: " + arrayFile.getAbsolutePath());

        // ── Step 6. 유효성 검사 ──────────────────────────────────────────
        System.out.println("\n=== Step 6. JSON 유효성 검사 ===");

        System.out.println("정상 JSON  : " + handler.isValid("{\"key\":\"value\"}"));
        System.out.println("비정상 JSON: " + handler.isValid("{key: value}"));

        System.out.println("\nPOC 완료. data/ 디렉토리에서 결과를 확인하세요.");
    }
}
