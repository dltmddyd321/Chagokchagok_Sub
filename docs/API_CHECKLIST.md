# 차곡차곡 서버 API 체크리스트

> **API 작업을 할 때마다 이 문서를 같이 갱신한다.** (서브앱·Pleos 앱 레포에 같은 내용으로 둔다)
> 기준 문서: 서버 Swagger(OpenAPI) JSON — `https://chagokchagok-api.duckdns.org/docs` (Basic 인증)
> 마지막 갱신: 2026-11-14

## 표시

| 표시 | 뜻 |
|---|---|
| ✅ | 화면에 연결 + 실서버 호출 확인 |
| 🔌 | 화면에 연결 (실서버 호출 미확인) |
| 🟡 | Retrofit에 등록만 (화면 미연결) |
| ⬜ | 미등록 |
| ➖ | 이 앱에서는 쓰지 않을 예정 |

## 공통 설정

| 항목 | 서브앱 | Pleos 앱 |
|---|---|---|
| Base URL (`https://chagokchagok-api.duckdns.org`, 끝 `/` 자동 처리) | ✅ | 🔌 서브앱과 같게 맞춤 (실호출 미확인) |
| Basic 인증 (`local.properties`: `CHAGOK_API_USERNAME` / `CHAGOK_API_PASSWORD`) | ✅ | 🔌 |
| 로그에서 Authorization 가림 | ✅ | ✅ |
| `X-Notification-Key` 헤더 (알림 API 필수, 없으면 401 `NOTIFICATION_UNAUTHORIZED`. `local.properties`: `CHAGOK_NOTIFICATION_KEY`) | ✅ | ✅ |
| `Idempotency-Key` 헤더 (POST/PUT/PATCH) | ✅ 등록된 API만 | ✅ 등록된 API만 |

## API 목록 (Swagger 기준 22개)

### 상태 확인
| API | 서브앱 | Pleos 앱 | 쓰는 곳 / 메모 |
|---|---|---|---|
| `GET /api/v1/health` | ⬜ | ⬜ | |
| `GET /api/v1/health/db` | ⬜ | ⬜ | |

### 차량
| API | 서브앱 | Pleos 앱 | 쓰는 곳 / 메모 |
|---|---|---|---|
| `GET /api/v1/vehicles` | ⬜ | ⬜ | 차량 선택 화면 (지금은 Room 기본 차량) |
| `GET /api/v1/vehicles/{vehicle_id}` | ⬜ | ⬜ | 적재 공간·입구 크기 (2D 배치도) |

### 적재 계획
| API | 서브앱 | Pleos 앱 | 쓰는 곳 / 메모 |
|---|---|---|---|
| `POST /api/v1/plans` | 🔌 | 🟡 | 서브앱 계획 저장 화면 "계획 저장하기": `{source_preview_id, selected_arrangement_id}`로 생성 → plan_id·revision. (짐 직접 보내는 PlanCreate는 등록만) / 2026-11-15 실서버 흐름 확인: 서브앱과 같은 요청으로 저장 201 (선택 배치안이 계산에 1개로 저장, arrangement_id는 새로 발급) |
| `GET /api/v1/plans` | ✅ | ✅ | 서브앱 홈: 최근(`sort=latest`)·즐겨찾기(`favorite=true`). 2026-11-14 실호출 200 (계획 0개) / Pleos 앱 시작 시 `PlanSyncRepository`가 **최신 1개**(`limit=1`)를 기기 DB로 동기화 → 홈 최신 계획 카드·도착 감지 |
| `GET /api/v1/plans/{plan_id}` | ⬜ | ✅ | Pleos 동기화 때 계획마다 상세로 짐 크기까지 받아 기기 DB(plan·plan_item·plan_car_seat)에 저장. 내용 같으면 건너뜀 |
| `PUT /api/v1/plans/{plan_id}` | ⬜ | ✅ | Pleos 내 계획: 짐 수정·동승자 변경은 화면에만 들고 있다가 "계획 진행하기 → 저장하고 진행"에서 한 번 전체 변경 (`expected_revision`, 인원 바뀌면 `passenger_seats` 생략). 409면 최신 다시 조회 / 2026-11-15 실서버 흐름 확인: revision +1, **선택 배치안이 null로 초기화** → 재계산·선택 필요 |
| `PATCH /api/v1/plans/{plan_id}/metadata` | 🔌 | ⬜ | 서브앱 계획 저장 2단계: 이름·카테고리·메모 (`expected_revision` = 생성 응답 revision) / 홈 즐겨찾기 별: `{expected_revision, is_favorite}`만 보내고(이름 등은 생략) 끝나면 서버 목록 다시 조회 / 2026-11-15 실서버 흐름 확인: 즐겨찾기만 보내도 이름·메모 유지, revision 그대로 |
| `PUT /api/v1/plans/{plan_id}/selection` | ⬜ | ✅ | Pleos 계획 진행(변경 후): 재계산의 1순위 배치안을 선택 (`expected_revision`, `calculation_id`, `arrangement_id`). 실패해도 가이드는 계속 / 2026-11-15 실서버 흐름 확인: 재계산은 revision을 올리지 않음 → PUT 응답 revision 그대로 사용 |

### 적재 계산
| API | 서브앱 | Pleos 앱 | 쓰는 곳 / 메모 |
|---|---|---|---|
| `POST /api/v1/plans/{plan_id}/calculations` | 🟡 | ✅ | **이미 저장된 계획**을 다시 계산할 때만. 새 계획은 `calculations/preview` → `POST /plans`. Pleos 계획 진행(변경 후): 저장 직후 `expected_revision` + `REPACK_ALL` + 전 좌석 EMPTY(USER)로 한 번, 응답 = 미리보기와 같은 계산 결과 + `calculation_id`. 서브앱 모델은 옛 형식 |
| `GET /api/v1/plans/{plan_id}/calculations/{calculation_id}` | ⬜ | ✅ | Pleos 내 계획 "배치도 미리보기": 계획의 `selected_calculation_id`(없으면 `latest_calculation_id`)로 조회 → `selected_arrangement_id` 배치를 탑뷰로 |
| `POST /api/v1/calculations/preview` | 🔌 | 🔌 | 서브앱 짐 선택 "배치 추천받기" → 배치 추천 화면. `vehicle_state.seats`에 좌석 선택 결과(OCCUPIED/EMPTY, USER)를 꼭 넣어야 좌석·접은 좌석을 씀(없으면 `STATE_REQUIRED`). 2026-11-14 curl로 응답 구조 확인, 앱 실호출 테스트 `PreviewApiLiveTest` 작성(미실행) / Pleos "적재 가능 여부 확인" → 적재 결과 화면: 인원 수만 보내고 `passenger_seats` 생략(서버가 자리 지정), 전 좌석 EMPTY로 보내야 좌석·접은 좌석 사용 (2026-11-15 curl 확인) |

### 내 짐
| API | 서브앱 | Pleos 앱 | 쓰는 곳 / 메모 |
|---|---|---|---|
| `POST /api/v1/items` | ⬜ | ➖ | 측정값 확인 화면 저장 (지금 Room에만) |
| `GET /api/v1/items` | ⬜ | ➖ | 내 물건 가져오기 (지금 Room) |
| `PATCH /api/v1/items/{saved_item_id}/favorite` | ⬜ | ➖ | |

### 제품 검색
| API | 서브앱 | Pleos 앱 | 쓰는 곳 / 메모 |
|---|---|---|---|
| `POST /api/v1/products/search` | ⬜ | ✅ | Pleos "제품명 말하기" 인식 → 검색 → "찾은 제품 규격" 창(운반 상태별 후보, 빠진 치수는 직접 입력) · 빠른 확인. 외부 웹 검색+AI(DB 아님). 2026-11-15 `ProductSearchApiLiveTest` 실호출 성공(포엥 암체어 후보 8개, 가끔 503 `PRODUCT_SEARCH_UNAVAILABLE`). `candidates[].image_url`로 제품 사진(Coil), 비었거나 안 열리면 기본 짐 이미지 — 적절한 사진이 없으면 서버가 필드를 빼고 보냄 → 앱은 null과 같게 처리(기본 짐 이미지) |

### AI
| API | 서브앱 | Pleos 앱 | 쓰는 곳 / 메모 |
|---|---|---|---|
| `POST /api/v1/ai/messages` | ⬜ | ⬜ | |
| `POST /api/v1/ai/loading-guides` | 🔌 | ✅ | 서브앱 배치 추천: 배치안이 나오면 곧장 배치안마다 `{preview_id, arrangement_id}`로 요청 → "가이드 보기" 화면. 2026-11-14 curl 확인: 지금은 `source=FALLBACK`, `steps` 빈 배열 → 앱이 싣는 순서로 기본 문구 생성 / Pleos: 적재 가능이면 결과가 나오자마자 배치안마다 미리 받아 두고 "이 조건으로 진행" → 적재 가이드 화면 / Pleos 내 계획: 앱 시작 때 선택 배치안을 `{plan_id, calculation_id, arrangement_id}`로 미리 받아 둠 → 변경 없으면 "계획 진행하기"가 바로 가이드, 변경 후엔 재계산 결과로 다시 받음 / 2026-11-15 실서버 흐름 확인: plan 기준 source=AI, steps 옴 (문장이 좌표·WDH 등 기술 표현 위주) |

### 알림
| API | 서브앱 | Pleos 앱 | 쓰는 곳 / 메모 |
|---|---|---|---|
| `PUT /api/v1/notifications/devices/{device_id}` | ✅ | ➖ | 앱 시작·토큰 갱신 시 `PushTokenSync`가 등록. device_id는 앱이 만든 UUID. 2026-11-15 `NotificationApiLiveTest` 등록 200 |
| `DELETE /api/v1/notifications/devices/{device_id}` | 🟡 | ➖ | `PushTokenSync.unregister()` 준비 (2026-11-15 실호출 해제 200). 알림 끄기 화면 생기면 연결 |
| `POST /api/v1/plans/{plan_id}/cargo-reminders` | ➖ | ✅ | `ArrivalReminderService`: 도착+P단이면 `DESTINATION_ARRIVED`, 문 열림이면 `DOOR_OPENED`. 차량 알림과 같이 보냄. 2026-11-15 키 인증 통과 확인(없는 계획이라 404 PLAN_NOT_FOUND) / 2026-11-15 실계획으로 201 ACCEPTED, 등록 기기 1대에 전달 접수 |

## 요약

| | 서브앱 | Pleos 앱 |
|---|---|---|
| ✅ 연결·호출 확인 | 2 (`GET /plans`, `PUT /notifications/devices`) | 9 (`POST /products/search`, `GET /plans`, `GET /plans/{id}`, `PUT /plans/{id}`, `GET /plans/{id}/calculations/{calc_id}`, `POST /plans/{id}/calculations`, `PUT /plans/{id}/selection`, `POST /ai/loading-guides`, `POST /cargo-reminders`) |
| 🔌 연결만 | 4 (`POST /calculations/preview`, `POST /ai/loading-guides`, `POST /plans`, `PATCH /plans/{id}/metadata`) — 같은 요청 모양으로 실서버 확인(앱 실행 X) | 1 (`POST /calculations/preview`) |
| 🟡 등록만 | 2 (`POST /plans/{id}/calculations`, `DELETE /notifications/devices`) | 1 (`POST /plans`) |
| ⬜ 남음 | 13 | 7 |
| ➖ 안 씀 | 1 | 5 |

## 변경 기록
- 2026-11-15: 실서버 계획 흐름 확인 (서브앱 저장 → Pleos 동기화·미리 받기·수정·재계산·선택·안내), `PlanFlowApiLiveTest`.
- 2026-11-15: Pleos 제품 검색 실호출 확인 + `image_url` 제품 사진(없으면 기본 짐 이미지).
- 2026-11-15: Pleos 계획 진행하기: 변경 없으면 미리 받아 둔 선택 배치안·적재 안내로 바로 가이드, 변경했으면 확인 창 → `PUT /plans/{id}` → `POST /plans/{id}/calculations` → `PUT /plans/{id}/selection` → `POST /ai/loading-guides`(plan 기준) → 가이드·적재 완료.
- 2026-11-15: Pleos 배치도 미리보기 (`GET /plans/{id}/calculations/{calc_id}`).
- 2026-11-15: Pleos 내 계획 상세 + `PUT /plans/{id}`로 짐·동승자 수정 저장.
- 2026-11-15: Pleos 빠른 확인(제품 검색 → 기기 DB 실측으로 들어가는지 판정).
- 2026-11-15: X-Notification-Key 받음 → 기기 등록·해제 실호출 200, 짐 챙기기 알림 인증 통과.
- 2026-11-15: Pleos 적재 가이드 `POST /ai/loading-guides` 연결 (적재 가능 시 미리 받기, 이 조건으로 진행 → 가이드 화면).
- 2026-11-15: Pleos 적재 가능 여부 확인에 `POST /calculations/preview` 연결 (적재 결과 화면), 좌석 착석 센서로 동승자 기본값.
- 2026-11-14: 서브앱 홈 즐겨찾기 별을 `PATCH metadata`로 연결.
- 2026-11-14: Pleos 제품명 말하기에 `POST /products/search` 연결 (찾은 제품 규격 창).
- 2026-11-14: Pleos 계획 동기화를 최신 1개로 변경 (주행 중 서비스), 홈 최신 계획 카드.
- 2026-11-14: 서브앱 계획 저장 연결 (`POST /plans` 미리보기 결과로 생성 → `PATCH metadata`), 저장 완료 후 홈 서버 목록 갱신.
- 2026-11-14: 서브앱 적재 안내 `POST /ai/loading-guides` 연결 (가이드 대기·가이드 화면).
- 2026-11-14: 서브앱 배치 추천 화면에 `POST /calculations/preview` 연결 (추천 1~3 탭, load_order 순 페이드인).
- 2026-11-14: Pleos 앱 시작 시 동기화 → 도착 감지 순서로 변경.
- 2026-11-14: Pleos 홈에서 서버 계획·짐 목록을 기기 DB로 동기화 (`GET /plans` + `GET /plans/{id}`).
- 2026-11-14: 모바일 알림 3개 등록. 서브앱 기기 등록(토큰 동기화 연결)·해제, Pleos 짐 챙기기 알림(도착 서비스 연결). Pleos Base URL·Basic 인증 서브앱과 맞춤.
- 2026-11-14: 체크리스트 작성. 서브앱 `GET /plans` 연결·실호출 200, Basic 인증·Base URL 정리.
