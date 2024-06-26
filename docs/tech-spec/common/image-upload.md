## 이미지 업로드

### 개요

- 서비스 내에서 전반적으로 사용되는 이미지 업로드에 대한 기능을 설계/정의한다.

### 요구사항 정의

- 이미지는 백엔드 서버를 거치치 않고 클라이언트에서 직접 업로드한다.
    - 프론트 엔드에서는 `presigned URL`을 통해 이미지를 업로드한다.
    - 백엔드에서는 `presigned URL`을 생성하여 프론트 엔드에 전달한다.
- `presigned URL`은
    - `PUT` 메소드로 이미지를 업로드할 수 있는 URL이다.
    - **하나의 링크당 하나의 이미지만** 업로드 가능하다.
    - 업로드 되는 이미지는 모두 **JPG 포맷**으로 변경되서 저장된다.
    - 이미지 사이즈 조절
    - <img src="https://i.imgur.com/aFE6gK2.png" width="400" alt="">
- 이미지의 `업로드`는 `S3`에 `업로드` 되지만 `S3`에 직접 접근하는 것은 불가능하다.
    - <img src="https://i.imgur.com/oHOmiXo.png" width="400" alt="">
- 이미지 업로드는 `S3`에 저장되며, `S3`에 저장된 이미지는 `CloudFront`를 통해 서비스된다.

### 용어 사전

| 한글명          | 영문명           | 설명                                             |
|--------------|---------------|------------------------------------------------|
| 이미지 업로드      | Image Upload  | 이미지를 업로드하는 기능                                  |
| 식별자          | key           | 이미지를 식별하는 값  , 경로+UUID+확장자                     |
| 루트 경로        | root path     | 이미지를 저장하는 루트 경로    ex. review , user/profile등등 |
| 인증된 업로드용 URL | presigned URL | 이미지를 업로드할 수 있는 URL                             |
| 조회용 URL      | View URL      | 이미지를 조회할 수 있는 URL                              |
| 이미지 순서       | order         | 이미지의 순서를 나타내는 값                                |
| 버킷           | Bucket        | 이미지를 저장하는 공간                                   |
| 클라우드 프론트     | CloudFront    | 이미지를 보여주기 위한 서비스하는 CDN                         |
| 오브젝트 스토리지    | S3            | 이미지를 실제 저장하는 스토리지                              |
| 이미지 포맷       | Image Format  | 이미지의 형식을 나타내는 포맷                               |
| 이미지 사이즈      | Image Size    | 이미지의 크기를 나타내는 값                                |

### 요청시에는 뭐가 있어야 할까.

- 업로드할 이미지 파일 경로
    - ex `review/20240501` : `review/20240501`라는 디렉토리 `747a61ad-49f8-4e9f-b099-fe5f3d82fb6e.jpg` 같은 이름으로 저장된다.
    - `review` ,`user/profile`와 같은 prefix는 프론트에서 저장된다.
    - `20240501`는 년월일의 형식으로 저장된다.

### 응답 시에는 뭐가 있어야 할까.

- 업로드될 버킷명
- 제공된 URL의 수
- 업로드 정보
    - 이미지 순서
        - 서버로 저장시 이 URL을 제공하면된다.
    - 조회 시 사용될 URL 정보
        - 서버로 저장시 이 URL을 제공하면된다.
    - 생성된 업로드 이미지 업로드 URL
        - 순서 + UUID + .jpeg

### 어떤 걸 검증해야할까.

- root path가 존재하는지
    - 하나도 없을 경우에는 저장 할 수 없다.
- root path가 적절한지.
    - 검증 절차가 필요할 수도
- root path의 시작과 끝에 `/`가 있을 경우 제거한다.
- 로그인 된 유저인지 비 로그인 유저는 업로드 할 수 없다.

### 추가적인 참고 요소는?

- 업로드라는 걔념은 포괄적이여서 S3를 활용하는 인터페이스를 추출하는것이 긍정적일것.
- 이미지 업로드는 S3에 저장되며, S3에 저장된 이미지는 CloudFront를 통해 서비스된다.
    - 이미지 업로드 경로 제공 시 view URL을 함께 제공한다.
