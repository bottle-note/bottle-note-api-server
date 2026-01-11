package app.bottlenote.alcohols.service;

import app.bottlenote.alcohols.domain.TastingTagRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TastingTagService {

	private final TastingTagRepository tastingTagRepository;

	/**
	 * 문장에서 태그 이름 목록을 추출한다.
	 *
	 * @param text 분석할 문장
	 * @return 매칭된 태그 이름 목록
	 */
	public List<String> extractTagNames(String text) {
		// TODO: Aho-Corasick 알고리즘 적용 예정
		return List.of();
	}
}
