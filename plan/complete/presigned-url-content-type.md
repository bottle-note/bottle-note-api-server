```
================================================================================
                          PROJECT COMPLETION STAMP
================================================================================
Status: **COMPLETED**
Completion Date: 2026-03-30

** Core Achievements **
- PreSignUrlProvider의 하드코딩 확장자(.jpg) 제거, 7개 MIME 타입 동적 지원
- ImageUploadService에 withContentType 적용으로 S3 Content-Type 제어 가능
- ImageUploadRequest에 contentType 필드 추가 (기본값 image/jpeg)

** Key Components **
- PreSignUrlProvider.java: ALLOWED_CONTENT_TYPES 맵 (jpg, png, webp, gif, svg, mp4, pdf)
- ImageUploadService.java: generatePreSignUrl(imageKey, contentType) 시그니처 변경
- ImageUploadRequest.java: contentType 필드 추가
- FileExceptionCode.java: UNSUPPORTED_CONTENT_TYPE 예외 추가
================================================================================
```

# PreSigned URL Content-Type 지원

> Issue: https://github.com/bottle-note/workspace/issues/205
> 관련: [banner-media-type.md](banner-media-type.md)

## 배경

현재 PreSigned URL 발급 시 확장자가 `.jpg`로 하드코딩되어 있고, Content-Type 제어가 없다.
배너에 mp4 비디오를 업로드하려면 URL 발급 요청 시 `contentType`을 입력받아 확장자와 Content-Type을 동적으로 처리해야 한다.

## 현재 구조

```
GET /api/v1/s3/presign-url?rootPath={path}&uploadSize={n}
  → ImageUploadRequest(rootPath, uploadSize)
  → PreSignUrlProvider.getImageKey() → 확장자 .jpg 하드코딩
  → amazonS3.generatePresignedUrl() → Content-Type 미지정
  → 클라이언트가 S3로 직접 PUT 업로드
```

### 문제점
- `PreSignUrlProvider.java:14` → `EXTENSION = "jpg"` 하드코딩
- `PreSignUrlProvider.java:49` → `UUID + "." + EXTENSION` 으로 S3 키 생성
- `ImageUploadService.java:102-104` → Content-Type 없이 presigned URL 생성

## 작업 내용

### 1. `ImageUploadRequest` 수정
- `contentType` 필드 추가 (MIME 문자열, 기본값 `"image/jpeg"`)

### 2. `PreSignUrlProvider` 수정
- `EXTENSION = "jpg"` 상수 제거
- MIME → 확장자 허용 목록 추가:
  | MIME | 확장자 |
  |------|--------|
  | image/jpeg | jpg |
  | image/png | png |
  | image/webp | webp |
  | video/mp4 | mp4 |
- `getImageKey(rootPath, index)` → `getImageKey(rootPath, index, contentType)` 시그니처 변경
- 허용 목록에 없는 contentType이면 예외

### 3. `FileExceptionCode` 수정
- `UNSUPPORTED_CONTENT_TYPE(HttpStatus.BAD_REQUEST, "지원하지 않는 Content-Type입니다.")` 추가

### 4. `ImageUploadService` 수정
- `generatePreSignUrls()` → contentType 전달
- `generatePreSignUrl(imageKey)` → `generatePreSignUrl(imageKey, contentType)` 변경
- `GeneratePresignedUrlRequest`에 `.withContentType(contentType)` 적용

### 컨트롤러 변경 없음
- `ImageUploadController.java` (product-api): `@ModelAttribute` 자동 바인딩
- `AdminImageUploadController.kt` (admin-api): 동일

## 수정 대상 파일

**mono 모듈 (4개)**
- `common/file/dto/request/ImageUploadRequest.java`
- `common/file/PreSignUrlProvider.java`
- `common/file/service/ImageUploadService.java`
- `common/file/exception/FileExceptionCode.java`

## 검증

```bash
./gradlew :bottlenote-mono:compileJava
./gradlew :bottlenote-product-api:test
./gradlew :bottlenote-admin-api:compileKotlin
```
