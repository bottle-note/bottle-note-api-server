## 요약(Summary)

> 리뷰 등록기능을 구현합니다. 리뷰는 이미지와, 별점,리뷰 제목, 리뷰 내용으로 구성됩니다. 그리고 리뷰에 대해 댓글을 남길 수 있습니다.   



---------

## 배경(Background)

> 보틀노트 프로젝트는 유저가 경험한 주류에 대해 리뷰를 남김으로써 기록 및 공유를 할 수 있습니다.
> 커뮤니티성의 리뷰 보다는 본인의 히스토리를 관리하는 듯한 느낌의 리뷰를 제공합니다.


---------

## 목표(Goals)

> - 리뷰를 등록할 수 있다.
> - 리뷰에 별점을 함께 등록할 수 있다.
> - 리뷰에 이미지를 등록할 수 있다.
> - 리뷰에 댓글을 달 수 있다.

  
---------

## 목표가 아닌 것(Non-Goals)

> - 리뷰 신고 기능
> - 어뷰징 대처
>   -  특정 시간 내에 등록할 수 있는 리뷰의 개수 제한


---------

## 계획(Plan)

- API  :  POST `/api/v1/review`

### Request
```json
{
  "user_id" : 1,
  "alcohol_id" : 1,
  "review_content" : "맛있어요",
  "size_type" : "BOTTLE",
  "price" : 50000,
  "zip_code" : 15242,
  "address" : "서울시 영등포구 영등포동 7가",
  "detail_address" : "기계회관 5층",
  "image_url" : "tmp_image_url"
}
```

### Response
프론트와 협의 후 작성예정

### 비즈니스 로직
1. 로그인 된 사용자인지 검증
2. RequestBody 파싱해서, User엔티티와 Alcohol엔티티 DB에서 GET
3. ReviewCreateRequest 객체 생성
4. ReviewCreateRequest를 Review 엔티티로 변환 후 save
5. save 후 반환 된 엔티티를 ReviewCreateResponse로 변환 후 반환

 
---------

## 이외 고려 사항들(Other Considerations)
미작성
> 고려했었으나 하지 않기로 결정된 사항들을 적습니다.
> 이렇게 함으로써 이전에 논의되었던 주제가 다시 나오지 않도록 할 수 있고,
> 이미 논의되었던 내용이더라도 리뷰어들이 다시 살펴볼 수 있습니다.



---------
