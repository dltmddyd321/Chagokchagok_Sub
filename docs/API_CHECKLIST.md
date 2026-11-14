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
| `POST /api/v1/plans` | 🟡 | 🟡 | 계획 저장 화면 "계획 저장하기" (미연결) |
| `GET /api/v1/plans` | ✅ | ⬜ | 서브앱 홈: 최근(`sort=latest`)·즐겨찾기(`favorite=true`). 2026-11-14 실호출 200 (계획 0개) / Pleos 홈 "저장한 배치 계획" 예정 |
| `GET /api/v1/plans/{plan_id}` | ⬜ | ⬜ | 계획 상세, Pleos에서 plan_id로 불러오기 |
| `PUT /api/v1/plans/{plan_id}` | ⬜ | ⬜ | 계획 전체 변경 (`expected_revision`) |
| `PATCH /api/v1/plans/{plan_id}/metadata` | ⬜ | ⬜ | 서브앱 홈 즐겨찾기 별 (지금 서버 목록일 땐 동작 안 함) |
| `PUT /api/v1/plans/{plan_id}/selection` | ⬜ | ⬜ | 사용할 배치안 선택 |

### 적재 계산
| API | 서브앱 | Pleos 앱 | 쓰는 곳 / 메모 |
|---|---|---|---|
| `POST /api/v1/plans/{plan_id}/calculations` | 🟡 | 🟡 | 모델이 옛 형식 (`vehicle_state`·`packing_mode` 등 없음) |
| `GET /api/v1/plans/{plan_id}/calculations/{calculation_id}` | ⬜ | ⬜ | |
| `POST /api/v1/calculations/preview` | ⬜ | ⬜ | 서브앱 "짐 배치 추천받기" / Pleos "적재 가능 여부 확인" 후보 |

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
| `POST /api/v1/ai/loading-guides` | ⬜ | ⬜ | 선택한 배치의 적재 순서 안내 문구 |

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
| 🔌 연결만 | 1 (`PUT /notifications/devices`) | 1 (`POST /cargo-reminders`) |
| 🟡 등록만 | 3 (`POST /plans`, `POST /plans/{id}/calculations`, `DELETE /notifications/devices`) | 2 (`POST /plans`, `POST /plans/{id}/calculations`) |
| ⬜ 남음 | 16 | 14 |
| ➖ 안 씀 | 1 | 5 |

## 변경 기록
- 2026-11-14: 모바일 알림 3개 등록. 서브앱 기기 등록(토큰 동기화 연결)·해제, Pleos 짐 챙기기 알림(도착 서비스 연결). Pleos Base URL·Basic 인증 서브앱과 맞춤.
- 2026-11-14: 체크리스트 작성. 서브앱 `GET /plans` 연결·실호출 200, Basic 인증·Base URL 정리.
