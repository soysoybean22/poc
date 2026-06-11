package org.example;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.example.model.Address;
import org.example.model.User;
import org.example.service.JsonService;

import java.io.File;
import java.util.List;

public class Main {

    public static void main(String[] args) throws Exception {
        JsonService jsonService = new JsonService();
        File outputDir = new File("output");
        outputDir.mkdirs();

        // ── 1. JSON 문자열 파싱 ──────────────────────────────────────────
        System.out.println("=== 1. JSON 문자열 → 객체 파싱 ===");
        String userJson = """
                {
                  "id": 1,
                  "name": "홍길동",
                  "email": "hong@example.com",
                  "age": 30,
                  "address": {
                    "street": "테헤란로 123",
                    "city": "서울",
                    "zip_code": "06234"
                  },
                  "phone_numbers": ["010-1234-5678", "010-9876-5432"],
                  "unknown_field": "이 필드는 @JsonIgnoreProperties로 무시됩니다"
                }
                """;

        User user = jsonService.parseFromString(userJson, User.class);
        System.out.println("파싱 결과: " + user);

        // ── 2. JSON 배열 파싱 ────────────────────────────────────────────
        System.out.println("\n=== 2. JSON 배열 → List<User> 파싱 ===");
        String usersJson = """
                [
                  {"id": 1, "name": "홍길동", "email": "hong@example.com", "age": 30,
                   "address": {"street": "테헤란로 1", "city": "서울", "zip_code": "06234"},
                   "phone_numbers": ["010-0000-0001"]},
                  {"id": 2, "name": "김영희", "email": "kim@example.com", "age": 25,
                   "address": {"street": "강남대로 2", "city": "서울", "zip_code": "06101"},
                   "phone_numbers": ["010-0000-0002"]}
                ]
                """;

        List<User> users = jsonService.parseListFromString(usersJson, new TypeReference<>() {});
        users.forEach(u -> System.out.println("  - " + u));

        // ── 3. JsonNode로 동적 탐색 ──────────────────────────────────────
        System.out.println("\n=== 3. JsonNode 트리 탐색 ===");
        JsonNode root = jsonService.parseToTree(userJson);
        System.out.println("name  : " + root.get("name").asText());
        System.out.println("city  : " + root.path("address").path("city").asText());
        System.out.println("phone0: " + root.path("phone_numbers").get(0).asText());

        // ── 4. 객체 → JSON 문자열 직렬화 ────────────────────────────────
        System.out.println("\n=== 4. 객체 → JSON 문자열 ===");
        User newUser = new User(
                99L, "이순신", "lee@example.com", 45,
                new Address("충무로 99", "서울", "04522"),
                List.of("010-5555-5555")
        );
        String serialized = jsonService.toJsonString(newUser);
        System.out.println(serialized);

        // ── 5. 객체 → JSON 파일 저장 ────────────────────────────────────
        System.out.println("\n=== 5. 객체 → JSON 파일 저장 ===");
        File singleFile = new File(outputDir, "user.json");
        jsonService.saveToFile(newUser, singleFile);
        System.out.println("저장 완료: " + singleFile.getAbsolutePath());

        // ── 6. List → JSON 파일 저장 ─────────────────────────────────────
        System.out.println("\n=== 6. List → JSON 파일 저장 ===");
        File listFile = new File(outputDir, "users.json");
        jsonService.saveListToFile(users, listFile);
        System.out.println("저장 완료: " + listFile.getAbsolutePath());

        // ── 7. JSON 파일에서 읽기 ────────────────────────────────────────
        System.out.println("\n=== 7. JSON 파일 → 객체 파싱 ===");
        User loadedUser = jsonService.parseFromFile(singleFile, User.class);
        System.out.println("로드 결과: " + loadedUser);

        List<User> loadedUsers = jsonService.parseListFromFile(listFile, new TypeReference<>() {});
        System.out.println("리스트 로드 건수: " + loadedUsers.size());

        // ── 8. 파일 내 특정 필드 업데이트 ───────────────────────────────
        System.out.println("\n=== 8. 파일 특정 필드 업데이트 ===");
        jsonService.updateFieldAndSave(singleFile, "email", "updated@example.com");
        User updatedUser = jsonService.parseFromFile(singleFile, User.class);
        System.out.println("업데이트된 email: " + updatedUser.getEmail());

        System.out.println("\nPOC 완료. output/ 디렉토리에서 생성된 JSON 파일을 확인하세요.");
    }
}
