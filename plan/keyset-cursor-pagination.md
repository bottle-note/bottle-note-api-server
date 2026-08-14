# Plan: Product API Keyset Cursor Pagination (이슈 #402)

## Overview

Product 목록을 숫자 offset cursor에서 HMAC keyset cursor로 전환하고, 페이지 타입을 하나로 합친다. 기존 라우트 일괄 브레이킹. 프론트는 #394.

- Issue: bottle-note/workspace#402
- 계약 논의: #385
- 대상: Product 목록 + notifications + admin help
- `/api/v1/alcohols/search` 삭제

## Execution Mode

- mode: step-by-step
- scope: implement, test, commit
- 한 Task(PR 단위)마다 커밋하고 정지한다. 최종 Opus 리뷰는 코디네이터가 별도 디스패치한다.

## Tasks

- [x] Task 1: 공통 타입 + HMAC + 오류 계약. 기존 공개 API 미변경
- [ ] Task 2: 단순 목록 (help, business-support, follow, reply, lookup, history, notifications)
- [ ] Task 3: 복잡 정렬 (review, rating, explore RANDOM, curation)
- [ ] Task 4: MyBottle
- [ ] Task 5: admin help, search 삭제, legacy 타입 제거

## Progress Log

- 2026-08-15: #402 확정. Task 1 orchestration 시작.
- 2026-08-15: Task 1 완료. `app.bottlenote.global.pagination`에 PaginationRequest/Pagination/PageResponse와 HMAC-SHA256 커서 코덱, INVALID_CURSOR(400)/CURSOR_CONTEXT_MISMATCH(400)/CURSOR_EXPIRED(410)를 추가했다. 기존 CursorPageable·구 PageResponse·목록 API는 변경하지 않았다. mono pagination 단위 테스트 14개 통과.
