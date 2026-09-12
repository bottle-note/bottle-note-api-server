# OpenAPI 메뉴 트리

Scalar 사이드바에 나오는 순서 그대로다. 태그 이름과 순서는 각 모듈 `OpenApiConfig`의 메뉴 선언에서 오며, 로컬에서 기동한 애플리케이션이 실제로 내려준 스펙으로 만들었다.

## product-api

태그 12개, 엔드포인트 86개

```
product-api
├── 인증  (9)  — 소셜 로그인과 토큰 발급·검증, 약관 동의를 처리한다
│   ├── POST   /api/v2/agreements  —  동의 의사표시를 제출한다
│   ├── GET    /api/v2/agreements/status  —  동의 상태를 조회한다
│   ├── GET    /api/v2/auth/admin/permissions  —  관리자 권한이 있는지 확인한다
│   ├── POST   /api/v2/auth/agent  —  에이전트 키로 로그인한다
│   ├── POST   /api/v2/auth/apple  —  애플 계정으로 로그인한다
│   ├── GET    /api/v2/auth/apple/nonce  —  애플 로그인용 일회성 값을 발급한다
│   ├── POST   /api/v2/auth/kakao  —  카카오 계정으로 로그인한다
│   ├── POST   /api/v2/auth/reissue  —  액세스 토큰을 다시 발급한다
│   └── PUT    /api/v2/auth/token/verify  —  토큰이 유효한지 검증한다
├── 회원  (7)  — 닉네임과 프로필을 관리하고 다른 사용자를 팔로우한다
│   ├── POST   /api/v1/follow  —  팔로우 상태를 변경한다
│   ├── GET    /api/v1/follow/{targetUserId}/follower-list  —  특정 사용자를 팔로우하는 사람 목록을 조회한다
│   ├── GET    /api/v1/follow/{targetUserId}/following-list  —  특정 사용자가 팔로우하는 사람 목록을 조회한다
│   ├── DELETE /api/v1/users  —  회원을 탈퇴한다
│   ├── GET    /api/v1/users/current  —  로그인한 사용자의 프로필을 조회한다
│   ├── PATCH  /api/v1/users/nickname  —  닉네임을 변경한다
│   └── PATCH  /api/v1/users/profile-image  —  프로필 이미지를 변경한다
├── 차단  (8)  — 다른 사용자를 차단하고 차단 관계를 조회한다
│   ├── GET    /api/v1/blocks  —  내가 차단한 사용자 목록을 조회한다
│   ├── POST   /api/v1/blocks  —  사용자를 차단한다
│   ├── GET    /api/v1/blocks/check/{targetUserId}  —  특정 사용자를 내가 차단했는지 확인한다
│   ├── GET    /api/v1/blocks/ids  —  내가 차단한 사용자의 식별자만 조회한다
│   ├── GET    /api/v1/blocks/mutual-check/{targetUserId}  —  특정 사용자와 서로 차단 관계인지 확인한다
│   ├── GET    /api/v1/blocks/stats/blocked-by-count  —  나를 차단한 사용자 수를 조회한다
│   ├── GET    /api/v1/blocks/stats/blocking-count  —  내가 차단한 사용자 수를 조회한다
│   └── DELETE /api/v1/blocks/{blockedUserId}  —  차단을 해제한다
├── 위스키  (9)  — 위스키를 검색·조회하고 인기 순위, 기준 정보, 테이스팅 태그를 제공한다
│   ├── GET    /api/v1/alcohols/categories  —  위스키 종류 목록을 조회한다
│   ├── GET    /api/v1/alcohols/lookup  —  위스키 이름을 자동 완성용으로 조회한다
│   ├── GET    /api/v1/alcohols/{alcoholId}  —  위스키 상세 정보를 조회한다
│   ├── GET    /api/v1/popular/spring  —  봄 추천 위스키를 조회한다
│   ├── GET    /api/v1/popular/view/monthly  —  이번 달 관심도 기준 인기 위스키를 조회한다
│   ├── GET    /api/v1/popular/view/week  —  이번 주 관심도 기준 인기 위스키를 조회한다
│   ├── GET    /api/v1/popular/week  —  이번 주 인기 위스키를 조회한다
│   ├── GET    /api/v1/regions  —  위스키 생산 지역 목록을 조회한다
│   └── GET    /api/v1/tasting-tags/extract  —  문장에서 테이스팅 태그를 추출한다
├── 둘러보기  (2)  — 원하는 기준으로 콘텐츠를 둘러본다
│   ├── GET    /api/v1/alcohols/explore/standard  —  정렬 기준에 따라 위스키를 탐색한다
│   └── GET    /api/v1/reviews/explore/standard  —  키워드로 리뷰를 탐색한다
├── 수입 정보  (5)  — 식약처 수입 주류와 수입사를 조회한다
│   ├── GET    /api/v1/mfds/alcohols  —  수입 주류 목록을 조회한다
│   ├── GET    /api/v1/mfds/alcohols/{id}  —  수입 주류 상세를 조회한다
│   ├── GET    /api/v1/mfds/countries  —  수입 주류 검색용 국가 목록을 조회한다
│   ├── GET    /api/v1/mfds/importers  —  수입사 목록을 조회한다
│   └── GET    /api/v1/mfds/importers/{importerId}  —  수입사 상세를 조회한다
├── 큐레이션  (5)  — 기획으로 엮은 위스키 모음과 그 구성 명세를 조회한다
│   ├── GET    /api/v2/curation-specs  —  사용 중인 큐레이션 명세 목록을 조회한다
│   ├── GET    /api/v2/curation-specs/{specId}  —  큐레이션 명세 상세를 조회한다
│   ├── GET    /api/v2/curations  —  노출 중인 큐레이션 목록을 조회한다
│   ├── GET    /api/v2/curations/feed  —  큐레이션 피드를 조회한다
│   └── GET    /api/v2/curations/{curationId}  —  큐레이션 상세를 조회한다
├── 리뷰  (12)  — 리뷰와 댓글을 작성·조회하고 좋아요를 누른다
│   ├── PUT    /api/v1/likes  —  리뷰 좋아요 상태를 변경한다
│   ├── POST   /api/v1/review/reply/register/{reviewId}  —  리뷰에 댓글을 등록한다
│   ├── GET    /api/v1/review/reply/{reviewId}  —  리뷰의 댓글 목록을 조회한다
│   ├── GET    /api/v1/review/reply/{reviewId}/sub/{rootReplyId}  —  댓글에 달린 대댓글 목록을 조회한다
│   ├── DELETE /api/v1/review/reply/{reviewId}/{replyId}  —  댓글을 삭제한다
│   ├── POST   /api/v1/reviews  —  리뷰를 작성한다
│   ├── GET    /api/v1/reviews/detail/{reviewId}  —  리뷰 하나를 상세히 조회한다
│   ├── GET    /api/v1/reviews/me/{alcoholId}  —  내가 쓴 리뷰 목록을 조회한다
│   ├── GET    /api/v1/reviews/{alcoholId}  —  위스키의 리뷰 목록을 조회한다
│   ├── PATCH  /api/v1/reviews/{reviewId}  —  리뷰를 수정한다
│   ├── DELETE /api/v1/reviews/{reviewId}  —  리뷰를 삭제한다
│   └── PATCH  /api/v1/reviews/{reviewId}/display  —  리뷰의 공개 여부를 변경한다
├── 마이페이지  (10)  — 내가 남긴 리뷰·별점·찜과 활동 기록을 모아 본다
│   ├── GET    /api/v1/history/view/alcohols  —  최근 본 위스키를 조회한다
│   ├── GET    /api/v1/history/{targetUserId}  —  사용자의 활동 기록을 조회한다
│   ├── GET    /api/v1/my-page/{userId}  —  마이페이지 정보를 조회한다
│   ├── GET    /api/v1/my-page/{userId}/my-bottle/picks  —  찜한 위스키 목록을 조회한다
│   ├── GET    /api/v1/my-page/{userId}/my-bottle/ratings  —  별점을 남긴 위스키 목록을 조회한다
│   ├── GET    /api/v1/my-page/{userId}/my-bottle/reviews  —  리뷰를 남긴 위스키 목록을 조회한다
│   ├── PUT    /api/v1/picks  —  위스키 찜 상태를 변경한다
│   ├── GET    /api/v1/rating  —  별점을 매길 위스키 목록을 조회한다
│   ├── POST   /api/v1/rating/register  —  위스키에 별점을 준다
│   └── GET    /api/v1/rating/{alcoholId}  —  특정 위스키에 내가 준 별점을 조회한다
├── 알림함  (4)  — 인증 사용자의 알림 목록을 조회하고 읽음 처리한다
│   ├── GET    /api/v1/notifications  —  내 알림 목록을 조회한다
│   ├── PATCH  /api/v1/notifications/read-all  —  알림을 모두 읽음 처리한다
│   ├── GET    /api/v1/notifications/unread-count  —  미읽음 알림 개수를 조회한다
│   └── PATCH  /api/v1/notifications/{notificationId}/read  —  알림 하나를 읽음 처리한다
├── 고객 지원  (12)  — 문의와 신고, 제휴 문의를 등록하고 답변을 확인한다
│   ├── GET    /api/v1/business-support  —  내가 등록한 비즈니스 문의 목록을 조회한다
│   ├── POST   /api/v1/business-support  —  비즈니스 문의를 등록한다
│   ├── GET    /api/v1/business-support/{id}  —  비즈니스 문의 상세를 조회한다
│   ├── PATCH  /api/v1/business-support/{id}  —  비즈니스 문의를 수정한다
│   ├── DELETE /api/v1/business-support/{id}  —  비즈니스 문의를 삭제한다
│   ├── GET    /api/v1/help  —  내가 등록한 문의 목록을 조회한다
│   ├── POST   /api/v1/help  —  문의를 등록한다
│   ├── GET    /api/v1/help/{helpId}  —  문의 상세를 조회한다
│   ├── PATCH  /api/v1/help/{helpId}  —  문의를 수정한다
│   ├── DELETE /api/v1/help/{helpId}  —  문의를 삭제한다
│   ├── POST   /api/v1/reports/review  —  리뷰를 신고한다
│   └── POST   /api/v1/reports/user  —  사용자를 신고한다
└── 공통  (3)  — 이미지 업로드 주소, 배너, 서버 정보를 제공한다
    ├── GET    /api/v1/app-info  —  서버 배포 정보를 조회한다
    ├── GET    /api/v1/banners  —  노출 중인 배너를 조회한다
    └── GET    /api/v1/s3/presign-url  —  이미지 업로드용 임시 주소를 발급한다
```

## admin-api

태그 12개, 엔드포인트 98개

```
admin-api
├── 인증  (5)  — 관리자 로그인, 토큰 재발급, 관리자 계정 등록·탈퇴를 처리한다
│   ├── POST   /v1/auth/agent  —  에이전트 키로 로그인한다
│   ├── POST   /v1/auth/login  —  이메일과 비밀번호로 로그인한다
│   ├── POST   /v1/auth/refresh  —  리프레시 토큰으로 액세스 토큰을 재발급한다
│   ├── POST   /v1/auth/signup  —  관리자 계정을 등록한다
│   └── DELETE /v1/auth/withdraw  —  관리자 계정을 탈퇴한다
├── 회원과 리뷰  (2)  — 가입한 회원과 작성된 리뷰 목록을 검색하고 조회한다
│   ├── GET    /v1/reviews  —  리뷰 목록을 조회한다
│   └── GET    /v1/users  —  회원 목록을 조회한다
├── 문의  (3)  — 사용자가 남긴 문의를 조회하고 답변을 등록한다
│   ├── GET    /v1/helps  —  문의 목록을 조회한다
│   ├── GET    /v1/helps/{helpId}  —  문의 상세 정보를 조회한다
│   └── POST   /v1/helps/{helpId}/answer  —  문의에 답변을 등록한다
├── IP 접근 제어  (8)  — IP 차단 상태, 감사 이력, 보안 signal 판정을 관리한다
│   ├── GET    /v1/access-control/ip-bans  —  IP 차단 상태 또는 목록을 조회한다
│   ├── POST   /v1/access-control/ip-bans  —  IP를 차단한다
│   ├── DELETE /v1/access-control/ip-bans  —  IP 차단을 해제한다
│   ├── GET    /v1/access-control/ip-bans/signals  —  IP별 보안 signal을 조회한다
│   ├── POST   /v1/access-control/ip-bans/signals  —  IP 보안 signal을 등록한다
│   ├── GET    /v1/access-control/ip-bans/signals/{signalId}  —  IP 보안 signal 상세를 조회한다
│   ├── POST   /v1/access-control/ip-bans/signals/{signalId}/verdict  —  IP 보안 signal을 확정 판정한다
│   └── GET    /v1/access-control/ip-bans/{ipBanId}  —  IP 차단 감사 이력을 조회한다
├── 알코올  (11)  — 위스키를 조회·등록·수정·삭제하고 엑셀과 JSON으로 일괄 검증·등록한다
│   ├── GET    /v1/alcohols  —  알코올 목록을 검색한다
│   ├── POST   /v1/alcohols  —  알코올을 생성한다
│   ├── POST   /v1/alcohols/bulk  —  알코올 목록을 일괄 등록한다
│   ├── POST   /v1/alcohols/bulk/validate  —  알코올 JSON 목록을 검증한다
│   ├── GET    /v1/alcohols/categories/reference  —  카테고리 참조 맵을 조회한다
│   ├── GET    /v1/alcohols/excel/template  —  알코올 엑셀 템플릿을 다운로드한다
│   ├── POST   /v1/alcohols/excel/validate  —  알코올 엑셀을 업로드해 검증한다
│   ├── GET    /v1/alcohols/lookup  —  알코올 lookup 목록을 조회한다
│   ├── GET    /v1/alcohols/{alcoholId}  —  알코올 상세를 조회한다
│   ├── PUT    /v1/alcohols/{alcoholId}  —  알코올을 수정한다
│   └── DELETE /v1/alcohols/{alcoholId}  —  알코올을 삭제한다
├── 생산지  (15)  — 위스키를 만드는 증류소와 생산 지역을 등록·수정·삭제하고 정렬 순서를 관리한다
│   ├── GET    /v1/distilleries  —  증류소 목록을 조회한다
│   ├── POST   /v1/distilleries  —  증류소를 등록한다
│   ├── PATCH  /v1/distilleries/bulk/reorder  —  증류소 목록을 일괄 재정렬한다
│   ├── GET    /v1/distilleries/{distilleryId}  —  증류소 상세 정보를 조회한다
│   ├── PUT    /v1/distilleries/{distilleryId}  —  증류소 정보를 수정한다
│   ├── DELETE /v1/distilleries/{distilleryId}  —  증류소를 삭제한다
│   ├── PATCH  /v1/distilleries/{distilleryId}/sort-order  —  증류소 정렬 순서를 변경한다
│   ├── GET    /v1/regions  —  지역 목록을 조회한다
│   ├── POST   /v1/regions  —  지역을 등록한다
│   ├── PATCH  /v1/regions/bulk/reorder  —  최상위 지역 목록을 일괄 재정렬한다
│   ├── PATCH  /v1/regions/{parentId}/children/bulk/reorder  —  특정 지역의 하위 지역 목록을 일괄 재정렬한다
│   ├── GET    /v1/regions/{regionId}  —  지역 상세 정보를 조회한다
│   ├── PUT    /v1/regions/{regionId}  —  지역 정보를 수정한다
│   ├── DELETE /v1/regions/{regionId}  —  지역을 삭제한다
│   └── PATCH  /v1/regions/{regionId}/sort-order  —  지역 정렬 순서를 변경한다
├── 테이스팅 태그  (7)  — 위스키 맛·향 테이스팅 태그를 등록·수정·삭제하고 위스키와의 연결을 관리한다
│   ├── GET    /v1/tasting-tags  —  테이스팅 태그 목록을 조회한다
│   ├── POST   /v1/tasting-tags  —  테이스팅 태그를 등록한다
│   ├── GET    /v1/tasting-tags/{tagId}  —  테이스팅 태그 상세 정보를 조회한다
│   ├── PUT    /v1/tasting-tags/{tagId}  —  테이스팅 태그 정보를 수정한다
│   ├── DELETE /v1/tasting-tags/{tagId}  —  테이스팅 태그를 삭제한다
│   ├── POST   /v1/tasting-tags/{tagId}/alcohols  —  테이스팅 태그에 위스키를 연결한다
│   └── DELETE /v1/tasting-tags/{tagId}/alcohols  —  테이스팅 태그에서 위스키 연결을 해제한다
├── 수입 정보  (17)  — 식약처 수입 원장에서 수집한 수입사와 수입 신고 데이터를 조회하고, 수입사 연결 근거와 BottleNote 위스키 매칭을 관리한다
│   ├── GET    /v1/mfds/declarations  —  수입 신고 목록을 조회한다
│   ├── GET    /v1/mfds/declarations/{declarationId}  —  수입 신고 상세 정보를 조회한다
│   ├── POST   /v1/mfds/declarations/{declarationId}/importer  —  수입 신고에 수입사를 수동 연결한다
│   ├── DELETE /v1/mfds/declarations/{declarationId}/importer  —  수입 신고의 수입사 연결을 해제한다
│   ├── GET    /v1/mfds/declarations/{declarationId}/matching/candidates  —  저장된 매칭 후보를 조회한다
│   ├── POST   /v1/mfds/declarations/{declarationId}/matching/confirm  —  매칭을 확정한다
│   ├── POST   /v1/mfds/declarations/{declarationId}/matching/release  —  매칭 확정을 해제한다
│   ├── POST   /v1/mfds/declarations/{declarationId}/matching/run  —  매칭을 실행한다
│   ├── PATCH  /v1/mfds/declarations/{declarationId}/normalization-status  —  수입 신고의 정규화 상태를 변경한다
│   ├── GET    /v1/mfds/importers  —  수입사 목록을 조회한다
│   ├── POST   /v1/mfds/importers  —  수입사를 수동 등록한다
│   ├── GET    /v1/mfds/importers/{importerId}  —  수입사 상세 정보를 조회한다
│   ├── PUT    /v1/mfds/importers/{importerId}  —  수입사 관리 항목을 수정한다
│   ├── DELETE /v1/mfds/importers/{importerId}  —  수입사를 삭제한다
│   ├── GET    /v1/mfds/rcno-links  —  RCNO 연결 근거 목록을 조회한다
│   ├── POST   /v1/mfds/rcno-links  —  RCNO 연결 근거를 등록한다
│   └── DELETE /v1/mfds/rcno-links/{rcno}  —  RCNO 연결 근거를 삭제한다
├── 큐레이션  (17)  — 큐레이션과 큐레이션 스펙을 등록·수정·삭제하고 목록·피드·상세를 조회한다
│   ├── GET    /v1/curations  —  큐레이션 목록을 조회한다
│   ├── POST   /v1/curations  —  큐레이션을 등록한다
│   ├── PATCH  /v1/curations/bulk/reorder  —  큐레이션 목록을 일괄 재정렬한다
│   ├── GET    /v1/curations/{curationId}  —  큐레이션 상세 정보를 조회한다
│   ├── PUT    /v1/curations/{curationId}  —  큐레이션 정보를 수정한다
│   ├── DELETE /v1/curations/{curationId}  —  큐레이션을 삭제한다
│   ├── POST   /v1/curations/{curationId}/alcohols  —  큐레이션에 위스키를 추가한다
│   ├── DELETE /v1/curations/{curationId}/alcohols/{alcoholId}  —  큐레이션에서 위스키를 제거한다
│   ├── PATCH  /v1/curations/{curationId}/display-order  —  큐레이션 노출 순서를 변경한다
│   ├── PATCH  /v1/curations/{curationId}/status  —  큐레이션 활성화 상태를 변경한다
│   ├── GET    /v2/curation-specs  —  활성화된 큐레이션 스펙 목록을 조회한다
│   ├── GET    /v2/curation-specs/{specId}  —  큐레이션 스펙 상세 정보를 조회한다
│   ├── GET    /v2/curations  —  스펙 기반 큐레이션 목록을 조회한다
│   ├── POST   /v2/curations  —  스펙 기반 큐레이션을 등록한다
│   ├── GET    /v2/curations/feed  —  스펙 기반 큐레이션 피드를 조회한다
│   ├── GET    /v2/curations/{curationId}  —  스펙 기반 큐레이션 상세 정보를 조회한다
│   └── PUT    /v2/curations/{curationId}  —  스펙 기반 큐레이션 정보를 수정한다
├── 배너  (8)  — 앱에 노출되는 배너를 등록·수정·삭제하고 노출 상태와 정렬 순서를 관리한다
│   ├── GET    /v1/banners  —  배너 목록을 조회한다
│   ├── POST   /v1/banners  —  배너를 등록한다
│   ├── PATCH  /v1/banners/bulk/reorder  —  배너 목록을 일괄 재정렬한다
│   ├── GET    /v1/banners/{bannerId}  —  배너 상세 정보를 조회한다
│   ├── PUT    /v1/banners/{bannerId}  —  배너 정보를 수정한다
│   ├── DELETE /v1/banners/{bannerId}  —  배너를 삭제한다
│   ├── PATCH  /v1/banners/{bannerId}/sort-order  —  배너 정렬 순서를 변경한다
│   └── PATCH  /v1/banners/{bannerId}/status  —  배너 활성 상태를 변경한다
├── 통계  (4)  — 방문자 활동과 위스키 지표 시계열을 조회한다
│   ├── GET    /v1/statistics/alcohols/{alcoholId}/observations/{axis}  —  주류 축별 관측 시계열을 조회한다
│   ├── GET    /v1/statistics/alcohols/{alcoholId}/popularity  —  주류 인기도 시계열을 조회한다
│   ├── GET    /v1/statistics/visitors/active  —  방문자 활동 시계열을 조회한다
│   └── GET    /v1/statistics/visitors/retention  —  방문자 재방문 시계열을 조회한다
└── 이미지 업로드  (1)  — S3에 직접 업로드할 수 있는 presigned URL을 발급한다
    └── GET    /v1/s3/presign-url  —  이미지 업로드용 presigned URL을 발급한다
```
