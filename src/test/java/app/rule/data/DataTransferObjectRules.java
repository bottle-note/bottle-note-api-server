package app.rule.data;

import app.rule.AbstractRules;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;


@Tag("rule")
@DisplayName("데이터 전송 객체(DTO) 아키텍처 규칙")
public class DataTransferObjectRules extends AbstractRules {
/*
- queryDsl DTO가 Criteria로 끝나는지 검증
- response 객체가 Response로 끝나는지 검증
- request 객체가 Request로 끝나는지 검증
- DTO 패키지 구조 검증 (request, response, dsl 패키지 사용)
- DTO 클래스 위치 규칙 검증 (모든 DTO는 적절한 패키지에 위치)
- 액션 용어 표준화 규칙 검증 (Create, Update, Delete, Search, List, Detail)
- 접미사 표준화 규칙 검증 (Request, Response, Criteria)

---

- 퍼사드 인터페이스 명명 규칙 검증 (`[도메인]Facade`)
- 퍼사드 구현체 명명 규칙 검증 (`Default[도메인]Facade`)
- 퍼사드 인터페이스의 메서드 명명 규칙 검증
- 퍼사드 패키지 구조 검증
- 퍼사드와 서비스 계층의 책임 분리 규칙 검증
* */

}
