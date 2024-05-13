## 요약(Summary)

> 이 문서는 위스키 상세 정보를 조회하는 API에 대한 기술적인 명세를 기술합니다.
>

----

## 목표(Goals)

- 술 상세 정보를 조회하는 API를 제공합니다.
- 토큰에 따른 찜하기 여부를 제공합니다.
- 술 아이디를 통해 상세 정보를 조회합니다.
- 리뷰 정보를 조회합니다.

---------

### API 설계

- `#77` 이슈를 기반으로 진행됩니다.
- API endpoint  :  `GET /api/v1/alcohols/{alcoholId}`
    - alcoholId : 술 아이디
- 별점이 없을 경우 0.0으로 전달합니다.

- 응답 값
    - 술 기본 정보
        - 술 아이디
        - 한글 카테고리
        - 술 썸네일 이미지
        - 술 한글명
        - 술 영문명
        - 별점(평균 0.0 이여도 전달 미표기는 프론트에서 처리 )
        - 별점 총 참여자  (숫자만 전달)
        - 내가 준 별점 (없을 경우 0 전달)
        - 찜 여부 (찜한 경우 true, 아닌 경우 false)
        - 한글 카테고리
        - 영문 카테고리
        - 한글 리전명
        - 영문 리전명
        - 영문 캐스트
        - 도수 (숫자만 전달)
        - 한글 증류소명
        - 영문 증류소명
        - 테이스팅 태그들
            - 태그 1
            - 태그 2
    - 마셔본 친구들 정보
        - 마셔본 총 친구 수
        - 친구들 정보
            - 썸네일
            - 유저 아이디
            - 유저 닉네임
            - 별점
    - 리뷰 정보
        - 총 리뷰 수
        - 베스트 리뷰 (1개)
            - 유저 아이디
            - 유저 프로필(썸네일)
            - 유저 닉네임
            - 리뷰 아이디
            - 리뷰 내용
            - 리뷰가 준 별점
            - 보틀 / 잔 여부
            - 가격 (정수로만 , 기호 없이)

            - 리뷰 조회 수
            - 리뷰 좋아요 수
            - 리뷰 댓글 수
            - 나의 코멘트 여부
            - 공개 여부
            - 좋아요 여부
            - 댓글 여부

            - 리뷰 썸네일
            - 리뷰 작성일
        - 최신순 리뷰 (4개)

- 응답의 값이 많아 내부적으로 4가지 쿼리를 통한 데이터를 가져옵니다.
    - 술 정보
    - 마셔본 친구들 정보
    - 베스트 리뷰 정보
    - 최신순 리뷰 정보

주의 할점

- 별점이 없을 경우 0.0으로 전달합니다.
- 별점 총 참여자  (숫자만 전달)
- 내가 준 별점 (없을 경우 0 전달)
- 찜 여부 (찜한 경우 true, 아닌 경우 false)
- 리뷰 정보가 없을 경우 빈 배열로 전달합니다.
- 리뷰 정보가 있을 경우 최신순 4개를 전달합니다.

```
response  # 응답
│
├── alcohols  # 위스키 정보
│   ├── alcoholId  # 술 아이디
│   ├── koreanCategory  # 한글 카테고리
│   ├── thumbnailImage  # 술 썸네일 이미지
│   ├── koreanName  # 술 한글명
│   ├── englishName  # 술 영문명
│   ├── averageRating  # 별점 (평균)
│   ├── totalRatings  # 별점 총 참여자 수
│   ├── myRating  # 내가 준 별점
│   ├── isFavorited  # 찜 여부
│   ├── koreanCategory  # 한글 카테고리
│   ├── englishCategory  # 영문 카테고리
│   ├── koreanRegion  # 한글 리전명
│   ├── englishRegion  # 영문 리전명
│   ├── englishCask  # 영문 캐스크
│   ├── alcoholContent  # 도수
│   ├── koreanDistillery  # 한글 증류소명
│   ├── englishDistillery  # 영문 증류소명
│   └── tastingTags  # 테이스팅 태그들
│       ├── tag1  # 태그 1
│       └── tag2  # 태그 2
│
├── friendsInfo  # 마셔본 친구들 정보
│   ├── totalFriends  # 마셔본 총 친구 수
│   └── friends  # 친구들 정보
│       ├── thumbnail  # 썸네일
│       ├── userId  # 유저 아이디
│       ├── userNickname  # 유저 닉네임
│       └── rating  # 별점
│
└── reviewInfos  # 리뷰 정보
    ├── totalReviews  # 총 리뷰 수
    ├── bestReview  # 베스트 리뷰 정보
    │   ├── userId  # 유저 아이디
    │   ├── userThumbnail  # 유저 프로필(썸네일)
    │   ├── userNickname  # 유저 닉네임
    │   ├── reviewId  # 리뷰 아이디
    │   ├── reviewContent  # 리뷰 내용
    │   ├── reviewRating  # 리뷰가 준 별점
    │   ├── bottleOrGlass  # 보틀 / 잔 여부
    │   ├── price  # 가격
    │   ├── viewCount  # 리뷰 조회 수
    │   ├── likeCount  # 리뷰 좋아요 수
    │   ├── commentCount  # 리뷰 댓글 수
    │   ├── isMyComment  # 나의 코멘트 여부
    │   ├── isPublic  # 공개 여부
    │   ├── isLiked  # 좋아요 여부
    │   ├── hasComment  # 댓글 여부
    │   ├── reviewThumbnail  # 리뷰 썸네일
    │   └── reviewDate  # 리뷰 작성일
    └── recentReviewInfos  # 최신순 리뷰 정보 (4개)
        ├── review1  # 리뷰 1
        ├── review2  # 리뷰 2
        ├── review3  # 리뷰 3
        └── review4  # 리뷰 4
```
