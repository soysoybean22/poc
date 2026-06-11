# JSON DB 테스트 가이드

> **대상 독자:** 본 애플리케이션을 처음 사용하거나 기능 검증이 필요한 분

---

## 목차

1. [사전 준비](#1-사전-준비)
2. [전체 구조 이해](#2-전체-구조-이해)
3. [JSON POC 실행 및 검증](#3-json-poc-실행-및-검증)
4. [CRUD 앱 수동 테스트](#4-crud-앱-수동-테스트)
   - [TC-01 데이터 추가 (Create)](#tc-01-데이터-추가-create)
   - [TC-02 전체 조회 (Read)](#tc-02-전체-조회-read)
   - [TC-03 키로 조회 (Read)](#tc-03-키로-조회-read)
   - [TC-04 데이터 수정 (Update)](#tc-04-데이터-수정-update)
   - [TC-05 데이터 삭제 (Delete)](#tc-05-데이터-삭제-delete)
5. [예외 처리 테스트](#5-예외-처리-테스트)
6. [영속성 테스트](#6-영속성-테스트)
7. [자동화 테스트 실행](#7-자동화-테스트-실행)
   - [단위 테스트 (Unit Test)](#단위-테스트-unit-test)
   - [Regression Test](#regression-test)
   - [Safety Test](#safety-test)
8. [테스트 체크리스트](#8-테스트-체크리스트)

---

## 1. 사전 준비

### 필수 환경

| 항목 | 요구 버전 | 확인 명령어 |
|------|-----------|-------------|
| Java JDK | 17 이상 | `java -version` |
| Gradle | 프로젝트 내 포함 (별도 설치 불필요) | - |

### 프로젝트 디렉토리로 이동

```powershell
cd C:\Reviewer\poc
```

---

## 2. 전체 구조 이해

이 프로젝트는 **JSON 파일을 DB 서버처럼 사용**하는 key-value 저장소입니다.

### 핵심 개념

```
"사과" 입력  →  db.json 조회  →  3 출력
```

앱을 종료해도 `data/db.json` 파일에 데이터가 남아 있어, 재시작 시 그대로 사용할 수 있습니다.

### DB 파일 구조 (`data/db.json`)

```json
{
  "사과": 3,
  "바나나": "노란색",
  "상품": {
    "name": "노트북",
    "price": 1500000
  }
}
```

키(key)를 입력하면 그에 해당하는 값(value)이 반환됩니다. 값의 타입은 숫자·문자열·JSON 객체 모두 가능합니다.

### 코드 구성

```
src/main/java/org/example/
├── handler/
│   └── JsonHandler.java       # JSON 파싱 & 파일 저장 핵심 유틸
├── db/
│   └── JsonDatabase.java      # JSON 파일을 DB처럼 다루는 key-value 저장소
├── JsonPocMain.java            # POC 실행 진입점 (DB 동작 검증)
└── CrudApp.java                # CRUD 콘솔 앱 진입점

src/test/java/org/example/
├── handler/
│   └── JsonHandlerTest.java   # JsonHandler 단위 테스트
├── db/
│   └── JsonDatabaseTest.java  # JsonDatabase 단위 테스트
├── regression/
│   └── RegressionTest.java    # 기존 기능 동작 보존 검증
└── safety/
    └── SafetyTest.java        # Edge 케이스 및 오용 시나리오 검증
```

### 실행 명령어 요약

| 목적 | 명령어 |
|------|--------|
| POC 검증 | `.\gradlew.bat runPoc --console=plain` |
| CRUD 앱 실행 | `.\gradlew.bat run --console=plain` |
| 전체 자동화 테스트 | `.\gradlew.bat test --console=plain` |

---

## 3. JSON POC 실행 및 검증

**목표:** JSON 파일이 DB처럼 동작하는 핵심 흐름(저장 → 조회 → 수정 → 삭제 → 영속성)을 자동으로 실행하고 확인합니다.

### 실행

```powershell
.\gradlew.bat runPoc --console=plain
```

### 기대 출력

```
=== Step 1. DB에 데이터 저장 ===
저장 완료 → data/db.json 확인

=== Step 2. 키로 조회 ===
사과   → 3
바나나 → 5
없는키 → 없음

=== Step 3. 전체 조회 ===
  사과   → 3
  바나나 → 5
  오렌지 → 2
  상품   → {"name":"노트북","price":1500000}

=== Step 4. 값 수정 (사과: 3 → 10) ===
사과 → 10

=== Step 5. 삭제 (바나나) ===
바나나 존재 여부: false

=== Step 6. 영속성 확인 (파일에서 새로 로드) ===
  사과   → 10
  오렌지 → 2
  상품   → {"name":"노트북","price":1500000}

POC 완료. data/db.json 에서 최종 상태를 확인하세요.
```

### 단계별 검증 포인트

| 단계 | 확인 항목 |
|------|-----------|
| Step 1 | `data/db.json` 파일이 생성되고 4개의 키가 저장되었는지 |
| Step 2 | 존재하는 키는 값이 반환되고, 없는 키는 "없음"이 출력되는지 |
| Step 3 | 저장된 모든 key-value가 출력되는지 |
| Step 4 | 사과의 값이 `3`에서 `10`으로 변경되었는지 |
| Step 5 | 바나나 삭제 후 존재 여부가 `false`인지 |
| Step 6 | 새 인스턴스로 파일을 로드했을 때 수정·삭제가 반영되어 있는지 (영속성) |

---

## 4. CRUD 앱 수동 테스트

### 실행

```powershell
.\gradlew.bat run --console=plain
```

> **참고:** `--console=plain` 옵션을 반드시 붙여야 Gradle의 진행 표시가 입력을 방해하지 않습니다.

### 메뉴 화면

```
══════════════════════════════════════
     JSON DB  (key-value CRUD)
══════════════════════════════════════
 1. 전체 조회
 2. 키로 조회
 3. 추가
 4. 수정
 5. 삭제
 0. 종료
══════════════════════════════════════
메뉴 선택 >
```

> 최초 실행 시 `data/db.json`이 자동으로 빈 JSON 객체(`{}`)로 생성됩니다.

---

### TC-01 데이터 추가 (Create)

**목적:** 키와 값을 입력하면 `db.json`에 저장되는지 확인합니다.

#### 테스트 1-1: 숫자 값 추가

메뉴 `3` 선택 후 아래와 같이 입력합니다.

```
키 > 사과
값 > 3
```

**기대 결과:**
```
추가 완료: 사과 → 3
```

#### 테스트 1-2: 문자열 값 추가 (평문 입력)

```
키 > 색깔
값 > 노란색
```

**기대 결과:**
```
추가 완료: 색깔 → "노란색"
```

> 따옴표 없이 평문을 입력하면 자동으로 JSON 문자열로 변환됩니다.

#### 테스트 1-3: JSON 객체 값 추가

```
키 > 상품
값 > {"name":"노트북","price":1500000}
```

**기대 결과:**
```
추가 완료: 상품 → {"name":"노트북","price":1500000}
```

#### 테스트 1-4: 이미 존재하는 키 추가 시도

이미 `사과` 키가 있는 상태에서:

```
키 > 사과
```

**기대 결과:**
```
'사과' 키가 이미 존재합니다. 수정은 메뉴 4를 이용하세요.
```

데이터에 변경이 없어야 합니다.

---

### TC-02 전체 조회 (Read)

**목적:** 저장된 모든 key-value 쌍이 출력되는지 확인합니다.

메뉴 `1` 선택

**기대 결과 (TC-01 이후 기준):**
```
[전체 목록 - 3건]
  사과            → 3
  색깔            → "노란색"
  상품            → {"name":"노트북","price":1500000}
```

#### DB가 비어있을 때

`data/db.json` 내용을 `{}`로 초기화한 뒤 메뉴 `1` 선택:

```
저장된 데이터가 없습니다.
```

---

### TC-03 키로 조회 (Read)

**목적:** 키를 입력하면 해당 값만 반환되는지 확인합니다.

메뉴 `2` 선택

#### 테스트 3-1: 존재하는 키 조회

```
키 입력 > 사과
```

**기대 결과:**
```
사과 → 3
```

#### 테스트 3-2: 중첩 객체 키 조회

```
키 입력 > 상품
```

**기대 결과:**
```
상품 → {
  "name" : "노트북",
  "price" : 1500000
}
```

#### 테스트 3-3: 존재하지 않는 키 조회

```
키 입력 > 포도
```

**기대 결과:**
```
'포도' 키가 존재하지 않습니다.
```

---

### TC-04 데이터 수정 (Update)

**목적:** 기존 키의 값이 새 값으로 교체되는지 확인합니다.

메뉴 `4` 선택

#### 테스트 4-1: 숫자 값 수정

```
수정할 키 > 사과
현재 값: 3
새 값   > 10
```

**기대 결과:**
```
수정 완료: 사과 → 10
```

**검증:** 메뉴 `2`에서 `사과`를 조회해 `10`이 반환되는지 확인합니다.

#### 테스트 4-2: 값 타입 변경 (숫자 → 객체)

```
수정할 키 > 사과
새 값   > {"count":10,"color":"빨강"}
```

**기대 결과:**
```
수정 완료: 사과 → {"count":10,"color":"빨강"}
```

#### 테스트 4-3: 존재하지 않는 키 수정 시도

```
수정할 키 > 포도
```

**기대 결과:**
```
'포도' 키가 존재하지 않습니다.
```

DB에 변경이 없어야 합니다.

---

### TC-05 데이터 삭제 (Delete)

**목적:** 특정 키가 DB에서 완전히 제거되는지 확인합니다.

메뉴 `5` 선택

#### 테스트 5-1: 정상 삭제

```
삭제할 키 > 색깔
삭제 대상: 색깔 → "노란색"
정말 삭제하시겠습니까? (y/n) > y
```

**기대 결과:**
```
삭제 완료!
```

**검증:** 메뉴 `2`에서 `색깔`을 조회하면 "키가 존재하지 않습니다."가 출력되어야 합니다.

#### 테스트 5-2: 삭제 취소

```
삭제할 키 > 사과
정말 삭제하시겠습니까? (y/n) > n
```

**기대 결과:**
```
삭제가 취소되었습니다.
```

**검증:** `사과` 데이터가 그대로 남아있어야 합니다.

#### 테스트 5-3: 존재하지 않는 키 삭제 시도

```
삭제할 키 > 포도
```

**기대 결과:**
```
'포도' 키가 존재하지 않습니다.
```

---

## 5. 예외 처리 테스트

| 상황 | 입력 | 기대 결과 |
|------|------|-----------|
| 잘못된 메뉴 번호 | `9` | `[오류] 올바른 번호를 입력하세요.` 후 메뉴 재표시 |
| 중복 키 추가 | 이미 있는 키 입력 | `'키' 키가 이미 존재합니다. 수정은 메뉴 4를 이용하세요.` |
| 없는 키 조회 | 존재하지 않는 키 | `'키' 키가 존재하지 않습니다.` |
| 없는 키 수정 | 존재하지 않는 키 | `'키' 키가 존재하지 않습니다.` |
| 없는 키 삭제 | 존재하지 않는 키 | `'키' 키가 존재하지 않습니다.` |

---

## 6. 영속성 테스트

**목적:** 앱을 종료하고 재시작해도 데이터가 유지되는지 확인합니다.

```
Step 1.  앱 실행 → 메뉴 3 → 키: 테스트키, 값: 12345 추가
Step 2.  메뉴 0 → 앱 종료
Step 3.  data/db.json 파일을 직접 열어 "테스트키": 12345 가 있는지 확인
Step 4.  앱 재실행 (.\gradlew.bat run --console=plain)
Step 5.  메뉴 2 → 키: 테스트키 입력
         기대 결과: 테스트키 → 12345
```

---

## 7. 자동화 테스트 실행

코드 변경 후 기능이 깨지지 않았는지 자동으로 검증합니다.

```powershell
.\gradlew.bat test --console=plain
```

**정상 완료 시:**
```
BUILD SUCCESSFUL
81 tests completed, 0 failed
```

테스트 리포트 상세 내용은 아래 경로에서 확인할 수 있습니다.

```
build/reports/tests/test/index.html
```

---

### 단위 테스트 (Unit Test)

개별 클래스의 메서드가 명세대로 동작하는지 검증합니다.

#### `JsonHandlerTest` — 11개

| 테스트 | 검증 내용 |
|--------|-----------|
| `parse_string_object` | JSON 객체 문자열 파싱 후 필드 값 일치 |
| `parse_string_array` | JSON 배열 파싱 후 ArrayNode 반환 |
| `parse_string_invalid_throwsIOException` | 잘못된 JSON → IOException |
| `parse_file_roundTrip` | 저장 후 파일 파싱 시 동일한 값 반환 |
| `parse_file_missing_throwsIOException` | 없는 파일 파싱 → IOException |
| `save_createsFileWithContent` | 파일 생성 및 내용 일치 확인 |
| `save_createsParentDirectories` | 부모 디렉토리 자동 생성 |
| `stringify_roundTrip` | JsonNode → 문자열 → 재파싱 시 값 일치 |
| `isValid_validJson_returnsTrue` | 올바른 JSON(객체·배열·숫자·빈 배열) → true |
| `isValid_invalidJson_returnsFalse` | 잘못된 JSON·빈 문자열 → false |

#### `JsonDatabaseTest` — 25개

| 범주 | 주요 검증 항목 |
|------|----------------|
| 초기화 (2) | 파일 없으면 빈 `{}` 자동 생성, 기존 파일 내용 유지 |
| put (5) | 신규 키 true, 중복 키 false, 문자열·객체 값 저장, 파일 영속성 |
| get (3) | 존재 키 값 반환, 없는 키 empty, 빈 DB에서 empty |
| getAll (2) | 전체 항목 반환, 빈 DB에서 빈 Map |
| exists (2) | 존재 키 true, 없는 키 false |
| update (4) | 값 교체, 없는 키 false, 다른 키 영향 없음, 파일 영속성 |
| delete (5) | 키 제거, 없는 키 false, 다른 키 영향 없음, 파일 영속성, 이후 재조회 empty |
| 파일 형식 (2) | pretty-print, 항상 유효한 JSON |

---

### Regression Test

**목적:** 리팩토링·기능 추가 이후에도 기존에 정상 동작하던 기능이 동일하게 동작하는지 확인합니다. 이 테스트가 전부 통과하면 기존 동작이 보존된 것입니다.

**파일 위치:** `src/test/java/org/example/regression/RegressionTest.java`

#### 24개 테스트 항목

| 범주 | 테스트 | 검증 내용 |
|------|--------|-----------|
| Handler 기본 동작 | `handler_parse_string_preservesValues` | JSON 파싱 후 필드 값 유지 |
| | `handler_saveAndParse_roundTrip` | 저장 → 재파싱 값 동일 |
| | `handler_isValid_correctlyClassifies` | 유효/무효 JSON 분류 정확성 |
| CRUD 반환값 계약 | `db_put_newKey_returnsTrue` | 신규 키 put → true |
| | `db_put_duplicateKey_returnsFalse` | 중복 키 put → false |
| | `db_update_existingKey_returnsTrue` | 존재 키 update → true |
| | `db_update_missingKey_returnsFalse` | 없는 키 update → false |
| | `db_delete_existingKey_returnsTrue` | 존재 키 delete → true |
| | `db_delete_missingKey_returnsFalse` | 없는 키 delete → false |
| 값 보존 계약 | `db_numberValue_preservedOnRoundTrip` | 숫자 값 put → get 시 변하지 않음 |
| | `db_stringValue_preservedOnRoundTrip` | 문자열 값 put → get 시 변하지 않음 |
| | `db_objectValue_preservedOnRoundTrip` | 중첩 객체 put → get 시 변하지 않음 |
| | `db_put_duplicate_originalValueUnchanged` | 중복 put 시도 후 기존 값 유지 |
| 독립성 계약 | `db_update_doesNotAffectOtherKeys` | update는 지정 키만 변경 |
| | `db_delete_doesNotAffectOtherKeys` | delete는 지정 키만 제거 |
| 순서 보존 | `db_getAll_preservesInsertionOrder` | 삽입 순서대로 반환 |
| 영속성 계약 | `db_put_persistsAcrossInstances` | put 후 새 인스턴스 로드 시 유지 |
| | `db_update_persistsAcrossInstances` | update 후 새 인스턴스 로드 시 유지 |
| | `db_delete_persistsAcrossInstances` | delete 후 새 인스턴스 로드 시 키 없음 |
| 파일 형식 계약 | `db_savedFile_isPrettyPrinted` | 들여쓰기·줄바꿈 포함 형식 |
| | `db_savedFile_isAlwaysValidJson` | 연산 후에도 항상 유효한 JSON |
| 전체 흐름 | `db_fullCrudCycle` | put → get → update → delete 순서 정상 동작 |

---

### Safety Test

**목적:** 예상치 못한 입력이나 오용 시나리오에서 데이터 손상 없이 안전하게 반응하는지 확인합니다.

**파일 위치:** `src/test/java/org/example/safety/SafetyTest.java`

#### 21개 테스트 항목

| 범주 | 테스트 | 검증 내용 |
|------|--------|-----------|
| 키 edge 케이스 | `key_emptyString_worksCorrectly` | 빈 문자열 `""` 키 정상 저장·조회 |
| | `key_withSpaces_worksCorrectly` | 공백 포함 키 정상 처리 |
| | `key_withSpecialChars_worksCorrectly` | `!@#$%^&*()` 특수문자 키 정상 처리 |
| | `key_unicode_worksCorrectly` | 한국어·이모지 키 정상 처리 |
| | `key_variousSpecialForms_storedAndRetrievedCorrectly` | `.` `/` `\` `"` `:` `\t` 등 파라미터화 테스트 |
| 값 타입 edge 케이스 | `value_booleanTrue/False_storedCorrectly` | boolean 값 저장·조회 |
| | `value_jsonNull_keyExistsButValueIsNull` | JSON null 값 저장 시 키는 존재, 값은 null |
| | `value_emptyObject/Array_storedCorrectly` | `{}` `[]` 빈 컨테이너 저장·조회 |
| | `value_array_storedCorrectly` | 배열 값 저장·조회 |
| | `value_negativeNumber_storedCorrectly` | 음수 값 저장·조회 |
| | `value_floatNumber_storedWithoutPrecisionLoss` | 소수 정밀도 손실 없이 저장 |
| | `value_deeplyNestedObject_storedCorrectly` | 깊은 중첩 객체 손상 없이 저장 |
| 오용 시나리오 | `misuse_putAfterDelete_keyCanBeReused` | 삭제 후 같은 키로 재등록 가능 |
| | `misuse_deleteCalledTwice_secondReturnsFalse` | 연속 delete 시 두 번째는 false, 예외 없음 |
| | `misuse_updateNonExistentKey_dbUnchanged` | 없는 키 연속 update 시 DB 변경 없음 |
| | `misuse_updateChangesValueType_worksCorrectly` | update로 값 타입 변경 (숫자 → 객체) |
| | `misuse_deleteAllKeys_dbBecomesEmpty` | 전체 삭제 후 빈 DB에서 새 데이터 추가 가능 |
| | `misuse_repeatedOperationsOnSameKey_dbRemainsConsistent` | 동일 키 반복 CRUD 후 DB 일관성 유지 |
| 손상된 파일 | `corruption_invalidJsonFile_throwsIOException` | 잘못된 JSON 파일 접근 시 IOException |
| | `corruption_emptyFile_throwsIOException` | 빈 파일 접근 시 IOException |
| null 안전성 | `handler_isValid_null_returnsFalse` | `isValid(null)` → 예외 없이 false 반환 |

> **버그 수정 포함:** Safety 테스트 작성 과정에서 `isValid(null)` 호출 시 `NullPointerException`이 발생하는 버그가 발견되어 수정되었습니다. null 또는 빈 문자열이 입력되면 예외 없이 `false`를 반환합니다.

---

## 8. 테스트 체크리스트

### JSON POC
- [ ] `data/db.json` 파일이 자동으로 생성되는지
- [ ] 저장한 키-값이 파일에 올바르게 기록되는지
- [ ] 키로 조회 시 정확한 값이 반환되는지
- [ ] 수정 후 변경된 값이 반환되는지
- [ ] 삭제 후 해당 키가 사라지는지
- [ ] 새 인스턴스로 파일을 로드해도 데이터가 유지되는지 (영속성)

### CRUD 앱 수동 테스트
- [ ] 숫자 값이 올바르게 저장되는지
- [ ] 평문 입력이 JSON 문자열로 자동 변환되는지
- [ ] JSON 객체 값이 저장되는지
- [ ] 중복 키 입력 시 기존 값이 유지되는지
- [ ] 전체 조회 시 모든 key-value가 출력되는지
- [ ] 키로 조회 시 해당 값만 반환되는지
- [ ] 중첩 객체가 보기 좋게 출력되는지
- [ ] 수정 전 현재 값이 표시되고 수정 후 새 값으로 교체되는지
- [ ] `y` 입력 시 키가 삭제되고, `n` 입력 시 유지되는지
- [ ] 앱 종료 후 재실행해도 데이터가 유지되는지

### 자동화 테스트
- [ ] `.\gradlew.bat test` 실행 시 **81개 전부 통과**하는지
- [ ] Regression Test 24개 전부 통과하는지 (기존 동작 보존)
- [ ] Safety Test 21개 전부 통과하는지 (edge 케이스 안전 처리)
- [ ] 테스트 리포트(`build/reports/tests/test/index.html`)에서 실패 항목이 없는지
