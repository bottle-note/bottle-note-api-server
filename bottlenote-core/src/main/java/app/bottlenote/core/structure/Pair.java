package app.bottlenote.core.structure;


import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public record Pair<L, R>(L first, R second) {

	/**
	 * 팩토리 메서드: Pair 객체 생성
	 */
	public static <L, R> Pair<L, R> of(L first, R second) {
		return new Pair<>(first, second);
	}

	/**
	 * 첫 번째와 두 번째 값을 교환한 새 Pair 반환
	 */
	public Pair<R, L> swap() {
		return new Pair<>(second, first);
	}

	/**
	 * 첫 번째 값에 함수를 적용한 새 Pair 반환
	 */
	public <T> Pair<T, R> mapFirst(Function<? super L, ? extends T> mapper) {
		return new Pair<>(mapper.apply(first), second);
	}

	/**
	 * 두 번째 값에 함수를 적용한 새 Pair 반환
	 */
	public <T> Pair<L, T> mapSecond(Function<? super R, ? extends T> mapper) {
		return new Pair<>(first, mapper.apply(second));
	}

	/**
	 * 두 값 모두에 함수를 적용한 결과 반환
	 */
	public <T> T map(BiFunction<? super L, ? super R, ? extends T> mapper) {
		return mapper.apply(first, second);
	}

	/**
	 * Pair를 List로 변환
	 */
	public List<Object> toList() {
		return List.of(first, second);
	}

	/**
	 * Pair를 배열로 변환
	 */
	public Object[] toArray() {
		return new Object[]{first, second};
	}
}
