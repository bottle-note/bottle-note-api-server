# Banner mediaType 필드 추가

> Issue: https://github.com/bottle-note/workspace/issues/205
> 관련: [presigned-url-content-type.md](presigned-url-content-type.md)

## 배경

animated WebP는 모바일에서 하드웨어 디코딩 미지원으로 CPU 소프트웨어 디코딩 → 저사양 기기 버벅임.
mp4는 모바일 VPU 활용으로 부하 없이 재생 가능. Banner에 미디어 유형 구분이 필요하다.

## 작업 범위

1. `MediaType` enum 생성 (`IMAGE`, `VIDEO`)
2. `Banner` 엔티티에 `mediaType` 필드 추가
3. 배너 등록/수정 API: `mediaType` 파라미터 수신
4. 배너 조회 API: 응답에 `mediaType` 포함 (FE에서 `<Image>` vs `<video>` 분기)
5. DB 스키마: `banners` 테이블에 `media_type` 컬럼 추가
6. 기존 데이터 마이그레이션: `media_type = 'IMAGE'`

## 현재 구조 분석

### Banner 엔티티 (`Banner.java`)
- 15개 필드, `mediaType` 없음
- enum 필드는 `@Enumerated(EnumType.STRING)` 사용 (converter 불필요)
- 기존 enum 패턴: `BannerType.java`, `TextPosition.java` → `@JsonCreator` + `parsing()` 메서드

### 수정 대상 파일

**신규 (1개)**
- `bottlenote-mono/.../banner/constant/MediaType.java`

**mono 모듈 (6개)**
- `banner/domain/Banner.java` - 필드 + `update()` 파라미터 추가
- `banner/dto/request/AdminBannerCreateRequest.java` - `mediaType` 추가 (기본값 `IMAGE`)
- `banner/dto/request/AdminBannerUpdateRequest.java` - `mediaType` 추가
- `banner/dto/response/AdminBannerDetailResponse.java` - `mediaType` 추가
- `banner/dto/response/BannerResponse.java` - `mediaType` 추가
- `banner/service/AdminBannerService.java` - create/update/getDetail 반영
- `banner/service/BannerQueryService.java` - 응답 변환 반영

**서브모듈 (1개)**
- `git.environment-variables/storage/mysql/init/01-init-core-table.sql` - banners 테이블 (line 587~608)

**테스트**
- `mono/.../banner/fixture/BannerTestFactory.java`
- `mono/.../banner/fixture/InMemoryBannerRepository.java`
- `admin-api/.../helper/banner/BannerHelper.kt`
- RestDocs 테스트 (admin-api, product-api)

## 검증

```bash
./gradlew :bottlenote-mono:compileJava        # 컴파일 + Q타입 재생성
./gradlew :bottlenote-admin-api:compileKotlin  # admin-api 컴파일
./gradlew :bottlenote-mono:test                # mono 테스트
./gradlew :bottlenote-product-api:test         # product-api 테스트
./gradlew :bottlenote-admin-api:admin_integration_test  # admin 통합 테스트
```
