package app.bottlenote.core.structure;


import java.util.List;
import java.util.function.Function;

public record Triple<F, S, T>(F first, S second, T third) {
	/**
	 * 팩토리 메서드: Triple 객체 생성
	 */
	public static <F, S, T> Triple<F, S, T> of(F first, S second, T third) {
		return new Triple<>(first, second, third);
	}

	/**
	 * 세 값의 순서를 first -> second -> third -> first 순으로 회전
	 */
	public Triple<S, T, F> rotate() {
		return new Triple<>(second, third, first);
	}

	/**
	 * 세 값의 순서를 first -> third -> second -> first 순으로 회전
	 */
	public Triple<T, F, S> rotateBackward() {
		return new Triple<>(third, first, second);
	}

	/**
	 * 첫 번째 값을 변환한 새 Triple 반환
	 */
	public <R> Triple<R, S, T> mapFirst(Function<? super F, ? extends R> mapper) {
		return new Triple<>(mapper.apply(first), second, third);
	}

	/**
	 * 두 번째 값을 변환한 새 Triple 반환
	 */
	public <R> Triple<F, R, T> mapSecond(Function<? super S, ? extends R> mapper) {
		return new Triple<>(first, mapper.apply(second), third);
	}

	/**
	 * 세 번째 값을 변환한 새 Triple 반환
	 */
	public <R> Triple<F, S, R> mapThird(Function<? super T, ? extends R> mapper) {
		return new Triple<>(first, second, mapper.apply(third));
	}

	/**
	 * 첫 번째와 두 번째 값으로 구성된 Pair 반환
	 */
	public Pair<F, S> toFirstSecondPair() {
		return new Pair<>(first, second);
	}

	/**
	 * 첫 번째와 세 번째 값으로 구성된 Pair 반환
	 */
	public Pair<F, T> toFirstThirdPair() {
		return new Pair<>(first, third);
	}

	/**
	 * 두 번째와 세 번째 값으로 구성된 Pair 반환
	 */
	public Pair<S, T> toSecondThirdPair() {
		return new Pair<>(second, third);
	}

	/**
	 * Triple을 List로 변환
	 */
	public List<Object> toList() {
		return List.of(first, second, third);
	}

	/**
	 * Triple을 배열로 변환
	 */
	public Object[] toArray() {
		return new Object[]{first, second, third};
	}
}
