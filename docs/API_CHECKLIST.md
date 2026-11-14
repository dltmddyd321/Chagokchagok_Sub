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
| `X-Notification-Key` 헤더 (알림 API, `local.properties`: `CHAGOK_NOTIFICATION_KEY`, 비면 생략) | 🔌 | 🔌 |
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
| `POST /api/v1/plans` | 🔌 | 🟡 | 서브앱 계획 저장 화면 "계획 저장하기": `{source_preview_id, selected_arrangement_id}`로 생성 → plan_id·revision. (짐 직접 보내는 PlanCreate는 등록만) |
| `GET /api/v1/plans` | ✅ | 🔌 | 서브앱 홈: 최근(`sort=latest`)·즐겨찾기(`favorite=true`). 2026-11-14 실호출 200 (계획 0개) / Pleos 앱 시작 시 `PlanSyncRepository`가 **최신 1개**(`limit=1`)를 기기 DB로 동기화 → 홈 최신 계획 카드·도착 감지 |
| `GET /api/v1/plans/{plan_id}` | ⬜ | 🔌 | Pleos 동기화 때 계획마다 상세로 짐 크기까지 받아 기기 DB(plan·plan_item·plan_car_seat)에 저장. 내용 같으면 건너뜀 |
| `PUT /api/v1/plans/{plan_id}` | ⬜ | ⬜ | 계획 전체 변경 (`expected_revision`) |
| `PATCH /api/v1/plans/{plan_id}/metadata` | 🔌 | ⬜ | 서브앱 계획 저장 2단계: 이름·카테고리·메모 (`expected_revision` = 생성 응답 revision). 홈 즐겨찾기 별은 아직 미연결 |
| `PUT /api/v1/plans/{plan_id}/selection` | ⬜ | ⬜ | 사용할 배치안 선택 |

### 적재 계산
| API | 서브앱 | Pleos 앱 | 쓰는 곳 / 메모 |
|---|---|---|---|
| `POST /api/v1/plans/{plan_id}/calculations` | 🟡 | 🟡 | **이미 저장된 계획**을 다시 계산할 때만. 새 계획은 `calculations/preview` → `POST /plans`. 모델이 옛 형식 |
| `GET /api/v1/plans/{plan_id}/calculations/{calculation_id}` | ⬜ | ⬜ | |
| `POST /api/v1/calculations/preview` | 🔌 | ⬜ | 서브앱 짐 선택 "배치 추천받기" → 배치 추천 화면. `vehicle_state.seats`에 좌석 선택 결과(OCCUPIED/EMPTY, USER)를 꼭 넣어야 좌석·접은 좌석을 씀(없으면 `STATE_REQUIRED`). 2026-11-14 curl로 응답 구조 확인, 앱 실호출 테스트 `PreviewApiLiveTest` 작성(미실행) / Pleos "적재 가능 여부 확인" 후보 |

### 내 짐
| API | 서브앱 | Pleos 앱 | 쓰는 곳 / 메모 |
|---|---|---|---|
| `POST /api/v1/items` | ⬜ | ➖ | 측정값 확인 화면 저장 (지금 Room에만) |
| `GET /api/v1/items` | ⬜ | ➖ | 내 물건 가져오기 (지금 Room) |
| `PATCH /api/v1/items/{saved_item_id}/favorite` | ⬜ | ➖ | |

### 제품 검색
| API | 서브앱 | Pleos 앱 | 쓰는 곳 / 메모 |
|---|---|---|---|
| `POST /api/v1/products/search` | ⬜ | ⬜ | Pleos "제품명 말하기" 인식 후 호출 자리 |

### AI
| API | 서브앱 | Pleos 앱 | 쓰는 곳 / 메모 |
|---|---|---|---|
| `POST /api/v1/ai/messages` | ⬜ | ⬜ | |
| `POST /api/v1/ai/loading-guides` | 🔌 | ⬜ | 서브앱 배치 추천: 배치안이 나오면 곧장 배치안마다 `{preview_id, arrangement_id}`로 요청 → "가이드 보기" 화면. 2026-11-14 curl 확인: 지금은 `source=FALLBACK`, `steps` 빈 배열 → 앱이 싣는 순서로 기본 문구 생성 |

### 알림
| API | 서브앱 | Pleos 앱 | 쓰는 곳 / 메모 |
|---|---|---|---|
| `PUT /api/v1/notifications/devices/{device_id}` | 🔌 | ➖ | 앱 시작·토큰 갱신 시 `PushTokenSync`가 등록. device_id는 앱이 만든 UUID. 실호출 테스트 `NotificationApiLiveTest` 작성(미실행) |
| `DELETE /api/v1/notifications/devices/{device_id}` | 🟡 | ➖ | `PushTokenSync.unregister()` 준비. 알림 끄기 화면 생기면 연결 |
| `POST /api/v1/plans/{plan_id}/cargo-reminders` | ➖ | 🔌 | `ArrivalReminderService`: 도착+P단이면 `DESTINATION_ARRIVED`, 문 열림이면 `DOOR_OPENED`. 차량 알림과 같이 보냄. 실호출 테스트 `CargoReminderApiLiveTest` 작성(미실행) |

## 요약

| | 서브앱 | Pleos 앱 |
|---|---|---|
| ✅ 연결·호출 확인 | 1 (`GET /plans`) | 0 |
| 🔌 연결만 | 5 (`PUT /notifications/devices`, `POST /calculations/preview`, `POST /ai/loading-guides`, `POST /plans`, `PATCH /plans/{id}/metadata`) | 3 (`GET /plans`, `GET /plans/{id}`, `POST /cargo-reminders`) |
| 🟡 등록만 | 2 (`POST /plans/{id}/calculations`, `DELETE /notifications/devices`) | 2 (`POST /plans`, `POST /plans/{id}/calculations`) |
| ⬜ 남음 | 13 | 12 |
| ➖ 안 씀 | 1 | 5 |

## 변경 기록
- 2026-11-14: Pleos 계획 동기화를 최신 1개로 변경 (주행 중 서비스), 홈 최신 계획 카드.
- 2026-11-14: 서브앱 계획 저장 연결 (`POST /plans` 미리보기 결과로 생성 → `PATCH metadata`), 저장 완료 후 홈 서버 목록 갱신.
- 2026-11-14: 서브앱 적재 안내 `POST /ai/loading-guides` 연결 (가이드 대기·가이드 화면).
- 2026-11-14: 서브앱 배치 추천 화면에 `POST /calculations/preview` 연결 (추천 1~3 탭, load_order 순 페이드인).
- 2026-11-14: Pleos 앱 시작 시 동기화 → 도착 감지 순서로 변경.
- 2026-11-14: Pleos 홈에서 서버 계획·짐 목록을 기기 DB로 동기화 (`GET /plans` + `GET /plans/{id}`).
- 2026-11-14: 모바일 알림 3개 등록. 서브앱 기기 등록(토큰 동기화 연결)·해제, Pleos 짐 챙기기 알림(도착 서비스 연결). Pleos Base URL·Basic 인증 서브앱과 맞춤.
- 2026-11-14: 체크리스트 작성. 서브앱 `GET /plans` 연결·실호출 200, Basic 인증·Base URL 정리.
