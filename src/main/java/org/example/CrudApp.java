package org.example;

import org.example.model.Address;
import org.example.model.User;
import org.example.repository.UserRepository;
import org.example.service.JsonService;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public class CrudApp {

    private static final Scanner sc = new Scanner(System.in);
    private static UserRepository repo;

    public static void main(String[] args) throws Exception {
        repo = new UserRepository(new JsonService());
        System.out.println("데이터 파일: data/users.json");

        while (true) {
            printMenu();
            String choice = sc.nextLine().trim();
            System.out.println();

            switch (choice) {
                case "1" -> listAll();
                case "2" -> searchById();
                case "3" -> searchByName();
                case "4" -> createUser();
                case "5" -> updateUser();
                case "6" -> deleteUser();
                case "0" -> {
                    System.out.println("종료합니다.");
                    return;
                }
                default -> System.out.println("[오류] 올바른 메뉴 번호를 입력하세요.\n");
            }
        }
    }

    // ── 메뉴 ──────────────────────────────────────────────────────────────

    private static void printMenu() {
        System.out.println("══════════════════════════════════════");
        System.out.println("   사용자 관리 시스템  (JSON CRUD)");
        System.out.println("══════════════════════════════════════");
        System.out.println(" 1. 전체 목록 조회");
        System.out.println(" 2. ID로 검색");
        System.out.println(" 3. 이름으로 검색");
        System.out.println(" 4. 새 사용자 등록");
        System.out.println(" 5. 사용자 정보 수정");
        System.out.println(" 6. 사용자 삭제");
        System.out.println(" 0. 종료");
        System.out.println("══════════════════════════════════════");
        System.out.print("메뉴 선택 > ");
    }

    // ── 1. 전체 조회 ──────────────────────────────────────────────────────

    private static void listAll() throws Exception {
        List<User> users = repo.findAll();
        if (users.isEmpty()) {
            System.out.println("등록된 사용자가 없습니다.\n");
            return;
        }
        System.out.println("[전체 목록 - " + users.size() + "명]");
        users.forEach(CrudApp::printUser);
        System.out.println();
    }

    // ── 2. ID 검색 ────────────────────────────────────────────────────────

    private static void searchById() throws Exception {
        System.out.print("검색할 ID > ");
        Long id = parseLong(sc.nextLine());
        if (id == null) return;

        Optional<User> result = repo.findById(id);
        if (result.isPresent()) {
            System.out.println("[검색 결과]");
            printUser(result.get());
        } else {
            System.out.println("ID " + id + "에 해당하는 사용자가 없습니다.");
        }
        System.out.println();
    }

    // ── 3. 이름 검색 ──────────────────────────────────────────────────────

    private static void searchByName() throws Exception {
        System.out.print("검색할 이름 (부분 일치) > ");
        String keyword = sc.nextLine().trim();

        List<User> results = repo.findByName(keyword);
        if (results.isEmpty()) {
            System.out.println("'" + keyword + "'을(를) 포함한 사용자가 없습니다.");
        } else {
            System.out.println("[검색 결과 - " + results.size() + "명]");
            results.forEach(CrudApp::printUser);
        }
        System.out.println();
    }

    // ── 4. 생성 ───────────────────────────────────────────────────────────

    private static void createUser() throws Exception {
        System.out.println("[새 사용자 등록]");

        System.out.print("이름 > ");
        String name = sc.nextLine().trim();

        System.out.print("이메일 > ");
        String email = sc.nextLine().trim();

        System.out.print("나이 > ");
        Integer age = parseInt(sc.nextLine());
        if (age == null) return;

        System.out.print("주소 - 도로명 > ");
        String street = sc.nextLine().trim();

        System.out.print("주소 - 도시 > ");
        String city = sc.nextLine().trim();

        System.out.print("주소 - 우편번호 > ");
        String zipCode = sc.nextLine().trim();

        System.out.print("전화번호 (쉼표로 구분, 예: 010-1234-5678,010-9999-0000) > ");
        List<String> phones = Arrays.stream(sc.nextLine().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        User user = new User(null, name, email, age, new Address(street, city, zipCode), phones);
        User saved = repo.create(user);

        System.out.println("등록 완료! (부여된 ID: " + saved.getId() + ")\n");
    }

    // ── 5. 수정 ───────────────────────────────────────────────────────────

    private static void updateUser() throws Exception {
        System.out.print("수정할 사용자 ID > ");
        Long id = parseLong(sc.nextLine());
        if (id == null) return;

        Optional<User> opt = repo.findById(id);
        if (opt.isEmpty()) {
            System.out.println("ID " + id + "에 해당하는 사용자가 없습니다.\n");
            return;
        }

        User user = opt.get();
        System.out.println("[현재 정보]");
        printUser(user);

        System.out.println("\n수정할 필드를 선택하세요:");
        System.out.println("  1. 이름       현재: " + user.getName());
        System.out.println("  2. 이메일     현재: " + user.getEmail());
        System.out.println("  3. 나이       현재: " + user.getAge());
        System.out.println("  4. 도로명     현재: " + user.getAddress().getStreet());
        System.out.println("  5. 도시       현재: " + user.getAddress().getCity());
        System.out.println("  6. 우편번호   현재: " + user.getAddress().getZipCode());
        System.out.println("  7. 전화번호   현재: " + user.getPhoneNumbers());
        System.out.print("필드 선택 > ");

        String field = sc.nextLine().trim();
        System.out.print("새 값 > ");
        String newValue = sc.nextLine().trim();

        switch (field) {
            case "1" -> user.setName(newValue);
            case "2" -> user.setEmail(newValue);
            case "3" -> {
                Integer age = parseInt(newValue);
                if (age == null) return;
                user.setAge(age);
            }
            case "4" -> user.getAddress().setStreet(newValue);
            case "5" -> user.getAddress().setCity(newValue);
            case "6" -> user.getAddress().setZipCode(newValue);
            case "7" -> user.setPhoneNumbers(
                    Arrays.stream(newValue.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList()
            );
            default -> {
                System.out.println("[오류] 올바른 필드 번호를 입력하세요.\n");
                return;
            }
        }

        repo.update(user);
        System.out.println("수정 완료!\n");
    }

    // ── 6. 삭제 ───────────────────────────────────────────────────────────

    private static void deleteUser() throws Exception {
        System.out.print("삭제할 사용자 ID > ");
        Long id = parseLong(sc.nextLine());
        if (id == null) return;

        Optional<User> opt = repo.findById(id);
        if (opt.isEmpty()) {
            System.out.println("ID " + id + "에 해당하는 사용자가 없습니다.\n");
            return;
        }

        System.out.println("[삭제 대상]");
        printUser(opt.get());

        System.out.print("정말 삭제하시겠습니까? (y/n) > ");
        if (!sc.nextLine().trim().equalsIgnoreCase("y")) {
            System.out.println("삭제가 취소되었습니다.\n");
            return;
        }

        repo.delete(id);
        System.out.println("삭제 완료!\n");
    }

    // ── 출력 헬퍼 ─────────────────────────────────────────────────────────

    private static void printUser(User u) {
        System.out.println("┌─────────────────────────────────────");
        System.out.println("│ ID       : " + u.getId());
        System.out.println("│ 이름     : " + u.getName());
        System.out.println("│ 이메일   : " + u.getEmail());
        System.out.println("│ 나이     : " + u.getAge());
        if (u.getAddress() != null) {
            System.out.println("│ 주소     : " + u.getAddress().getCity()
                    + " " + u.getAddress().getStreet()
                    + " (" + u.getAddress().getZipCode() + ")");
        }
        System.out.println("│ 전화번호 : " + u.getPhoneNumbers());
        System.out.println("└─────────────────────────────────────");
    }

    // ── 입력 파싱 헬퍼 ────────────────────────────────────────────────────

    private static Long parseLong(String input) {
        try {
            return Long.parseLong(input.trim());
        } catch (NumberFormatException e) {
            System.out.println("[오류] 숫자를 입력해주세요.\n");
            return null;
        }
    }

    private static Integer parseInt(String input) {
        try {
            return Integer.parseInt(input.trim());
        } catch (NumberFormatException e) {
            System.out.println("[오류] 숫자를 입력해주세요.\n");
            return null;
        }
    }
}
