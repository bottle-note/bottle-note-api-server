# Plan: Product API Keyset Cursor Pagination (이슈 #402)

## Overview

Product 목록을 숫자 offset cursor에서 HMAC keyset cursor로 전환하고, 페이지 타입을 하나로 합친다. 기존 라우트 일괄 브레이킹. 프론트는 #394.

- Issue: bottle-note/workspace#402
- 계약 논의: #385
- 후속 분리: bottle-note/workspace#404 (`[ms7-3] Admin/Product 페이지네이션 계약 분리`)
- 대상: Product 목록 + notifications
- 제외: Admin 목록 API는 page/size를 유지한다. Admin help는 Product help와 서비스·요청이 달라 원복해도 Product 커서는 유지된다.
- 예외(#402 한정): Admin 위스키 룩업은 공유 `AlcoholLookupService` 때문에 지금은 커서를 유지한다. yml cursor secret도 기동에 필요하다. 이 결합 해소는 #404에서 한다.
- `/api/v1/alcohols/search` 삭제

## Execution Mode

- mode: step-by-step
- scope: implement, test, commit
- 한 Task(PR 단위)마다 커밋하고 정지한다. 최종 Opus 리뷰는 코디네이터가 별도 디스패치한다.

## Tasks

- [x] Task 1: 공통 타입 + HMAC + 오류 계약. 기존 공개 API 미변경
- [x] Task 2: 단순 목록 (help, business-support, follow, reply, lookup, history, notifications)
- [x] Task 3: 복잡 정렬 (review, rating, explore RANDOM, curation)
- [x] Task 4: MyBottle
- [ ] Task 5: admin help를 page/size로 원복. lookup·yml 시크릿은 유지. search 삭제·legacy 잔여 정리

## Progress Log

- 2026-08-15: #402 확정. Task 1 orchestration 시작.
- 2026-08-15: Task 1 커밋 `23414a6ad`. Grok 구현, Opus 리뷰 Critical 0 / Important 7. Task 2 전에 size 팩토리·decode 가시성·fromOverflow 가드를 고치는 것을 권고.
- 2026-08-15: Task 1 완료. `app.bottlenote.global.pagination`에 PaginationRequest/Pagination/PageResponse와 HMAC-SHA256 커서 코덱, INVALID_CURSOR(400)/CURSOR_CONTEXT_MISMATCH(400)/CURSOR_EXPIRED(410)를 추가했다. 기존 CursorPageable·구 PageResponse·목록 API는 변경하지 않았다. mono pagination 단위 테스트 14개 통과.
- 2026-08-15: Task 3 완료. 알코올 탐색은 CRC32(seed,id) keyset, 시드는 커서 extra로만 이어진다. 리뷰 탐색은 createAt+id, 큐레이션 피드는 displayOrder+id. 응답은 items + meta.pagination. 관련 단위 테스트 통과.
- 2026-08-15: Task 4 완료. MyBottle 3탭을 HMAC keyset으로 전환하고 totalCount를 제거했다. 요청 키는 cursor+size, 응답 페이지 정보는 meta.pagination. UserQuerySupporterTest 통과.
- 2026-08-15: Task 5는 완료가 아니다. Admin help·search 삭제는 들어갔지만 FQCN PageResponse, CollectionResponse, 구 cursor 타입, 차단·조회 히스토리 표준 전환, 문서가 남아 있었다. 완료 표시는 최종 검증 전까지 유지하지 않는다.
- 2026-08-15: Task 5 잔여 정리 진행. FQCN 제거, 죽은 검색 DTO·cursor 타입·CollectionResponse 삭제, 차단 목록과 조회 히스토리를 items + meta.pagination HMAC 계약으로 전환, API 문서를 meta.pagination으로 수정. 최종 검증 전이라 Task 5는 미완료.
- 2026-08-15: 리뷰 좋아요 COUNT DISTINCT·별점순 `review.reviewRating`·히스토리 기간 HMAC context 수정. 커밋 `6d39eef11`.
- 2026-08-15: Admin 범위 재확정. Admin 목록은 대상이 아니다. Admin help는 Product와 독립이라 page/size 원복이 가능하다. Admin lookup은 공유 `AlcoholLookupService`라 커서 유지가 맞고, yml cursor secret은 lookup + `CursorProperties` 기동 검증 때문에 빼면 어드민이 뜨지 않는다. Task 5를 admin help 원복 + lookup/yml 유지로 수정.
- 2026-08-15: 후속 이슈 등록. bottle-note/workspace#404 (`[ms7-3] Admin/Product 페이지네이션 계약 분리`). 룩업 서비스 분리·admin help 원복·yml 시크릿 해제는 #402가 아니라 #404에서 한다.
