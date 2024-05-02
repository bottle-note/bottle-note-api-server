## 요약(Summary)

> Region API는 카테고리등의 상황에서 사용자가 선택가능한 선택지를 제공해주는 API입니다.

---------

## 배경(Background)

> 검색 조건에서 지역이 필요한 경우가 있어
> 이를 위한 API가 필요하다고 판단되어 개발하게 되었습니다.
> 이 지역은 DB의 Region 테이블에 저장되어 있으며
> Alcohols에 등록되어있는 Region만 가져옵니다.

---------

## 목표(Goals)

- 사용자가 선택가능한 지역을 제공합니다.
- Region 테이블에 있는 지역만 가져옵니다.

---------

## 계획(Plan)

- API : GET /api/v1/regions
- Request : None
- Response

```json
{
  "success": true,
  "code": 200,
  "data": [
    {
      region_id: 1,
      kor_name: '한국',
      eng_name: 'korea'
    },
    {
      region_id: 2,
      kor_name: '몽골',
      eng_name: 'Mongol'
    }
  ]
}
```

- ERROR
    - 500 : 서버 에러 (Backend 에게 문의)
    - 별도의 에러 포인트는 아직 없습니다.
- 서버 시작 시 데이터를 캐싱하는 방법을 고려합니다.
    - 다만 이때 Redis와 같은 캐시 서버를 사용하지 않고 서버 내부에 저장하는 방법을 사용합니다.
    - Redis 자원을 사용할 정도가 아니기 때문입니다.

---------
