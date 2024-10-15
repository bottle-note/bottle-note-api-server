package app.bottlenote.support.help.repository;

import app.bottlenote.support.help.dto.response.HelpListResponse;
import com.querydsl.core.types.ConstructorExpression;
import com.querydsl.core.types.Projections;
import org.springframework.stereotype.Component;

import static app.bottlenote.support.help.domain.QHelp.help;

@Component
public class HelpQuerySupporter {

	/**
	 * 문의글 목록 조회 API에 사용되는 생성자 Projection 메서드입니다.
	 *
	 * @return
	 */
	public ConstructorExpression<HelpListResponse.HelpInfo> helpResponseConstructor(){
		return Projections.constructor(
			HelpListResponse.HelpInfo.class,
			help.id.as("helpId"),
			help.content.as("content"),
			help.createAt.as("createdAt"),
			help.status.as("helpStatus")
		);
	}
}
