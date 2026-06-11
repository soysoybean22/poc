package org.example.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import org.example.model.User;
import org.example.service.JsonService;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserRepository {

    private static final File DATA_FILE = new File("data/users.json");
    private final JsonService jsonService;

    public UserRepository(JsonService jsonService) throws IOException {
        this.jsonService = jsonService;
        initDataFile();
    }

    private void initDataFile() throws IOException {
        DATA_FILE.getParentFile().mkdirs();
        if (!DATA_FILE.exists()) {
            jsonService.saveListToFile(new ArrayList<>(), DATA_FILE);
        }
    }

    // ── 전체 조회 ─────────────────────────────────────────────────────────

    public List<User> findAll() throws IOException {
        return jsonService.parseListFromFile(DATA_FILE, new TypeReference<>() {});
    }

    // ── ID 검색 ──────────────────────────────────────────────────────────

    public Optional<User> findById(Long id) throws IOException {
        return findAll().stream()
                .filter(u -> u.getId().equals(id))
                .findFirst();
    }

    // ── 이름 검색 (부분 일치) ─────────────────────────────────────────────

    public List<User> findByName(String keyword) throws IOException {
        return findAll().stream()
                .filter(u -> u.getName().contains(keyword))
                .toList();
    }

    // ── 생성 ─────────────────────────────────────────────────────────────

    public User create(User user) throws IOException {
        List<User> users = findAll();
        long nextId = users.stream()
                .mapToLong(User::getId)
                .max()
                .orElse(0L) + 1;
        user.setId(nextId);
        users.add(user);
        persist(users);
        return user;
    }

    // ── 수정 ─────────────────────────────────────────────────────────────

    public boolean update(User updated) throws IOException {
        List<User> users = findAll();
        boolean found = false;
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getId().equals(updated.getId())) {
                users.set(i, updated);
                found = true;
                break;
            }
        }
        if (found) persist(users);
        return found;
    }

    // ── 삭제 ─────────────────────────────────────────────────────────────

    public boolean delete(Long id) throws IOException {
        List<User> users = findAll();
        boolean removed = users.removeIf(u -> u.getId().equals(id));
        if (removed) persist(users);
        return removed;
    }

    // ── ID 존재 여부 ──────────────────────────────────────────────────────

    public boolean existsById(Long id) throws IOException {
        return findById(id).isPresent();
    }

    private void persist(List<User> users) throws IOException {
        jsonService.saveListToFile(users, DATA_FILE);
    }
}
