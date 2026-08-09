# IP ban concurrency k6 scenario

이 스크립트는 **500 VU가 각각 1회씩 요청**해 총 500개의 동시 요청으로 IP ban 요청 경로의 영향을 확인한다. 실행 전 대상 환경과 부하 실행 승인을 확인하며, 로컬 검증 외에는 실행하지 않는다.

```bash
# 고정 health 확인: 모든 응답이 200이어야 함
BASE_URL="$TARGET_BASE_URL" MODE=fixed k6 run scripts/k6/ip-ban-concurrency.js

# 공개 GET 경로를 무작위로 호출
BASE_URL="$TARGET_BASE_URL" MODE=random k6 run scripts/k6/ip-ban-concurrency.js
```

`BASE_URL`은 필수이며 저장소에 URL이나 비밀값을 기록하지 않는다. `MODE`는 `fixed` 또는 `random`이고, `AUTHORIZATION`은 필요할 때만 환경 변수로 전달한다. `READ_PATHS`에 쉼표 구분 상대 경로를 넣으면 random 경로 집합을 교체한다.

두 모드 모두 네트워크 실패(`status=0`)와 5xx가 0이고 p95가 1,000ms 미만, p99가 2,000ms 미만이어야 한다. fixed 모드는 모든 응답이 200이어야 하며, random 모드는 4xx를 별도 `status_4xx` 카운터로 보고한다. 상태 분포는 `status_network`, `status_2xx`, `status_3xx`, `status_4xx`, `status_5xx`, `status_other`의 6개 제한된 카운터로 출력된다.

안전한 구문 검증은 실제 요청 없이 다음처럼 수행한다.

```bash
k6 inspect scripts/k6/ip-ban-concurrency.js
k6 inspect -e MODE=random scripts/k6/ip-ban-concurrency.js
```
