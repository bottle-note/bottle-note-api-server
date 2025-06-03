package app.bottlenote.alcohols.fixture;

import app.bottlenote.alcohols.constant.AlcoholCategoryGroup;
import app.bottlenote.alcohols.constant.AlcoholType;
import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.domain.Distillery;
import app.bottlenote.alcohols.domain.Region;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Random;

@RequiredArgsConstructor
@Component
public class AlcoholTestFactory {

	private final Random random = new SecureRandom();

	@Autowired
	private EntityManager em;

	/**
	 * 기본 Region 생성 (스코틀랜드)
	 */
	@Transactional
	public Region persistRegion() {
		Region region = Region.builder()
				.korName("스코틀랜드-" + generateRandomSuffix())
				.engName("Scotland-" + generateRandomSuffix())
				.continent("Europe")
				.build();
		em.persist(region);
		return region;
	}

	/**
	 * 커스텀 Region 생성
	 */
	@Transactional
	public Region persistRegion(String korName, String engName) {
		Region region = Region.builder()
				.korName(korName + "-" + generateRandomSuffix())
				.engName(engName + "-" + generateRandomSuffix())
				.build();
		em.persist(region);
		return region;
	}

	/**
	 * 빌더를 통한 Region 생성
	 */
	@Transactional
	public Region persistRegion(Region.RegionBuilder builder) {
		Region region = builder.build();
		em.persist(region);
		return region;
	}

	/**
	 * 기본 Distillery 생성 (맥캘란)
	 */
	@Transactional
	public Distillery persistDistillery() {
		Distillery distillery = Distillery.builder()
				.korName("맥캘란-" + generateRandomSuffix())
				.engName("Macallan-" + generateRandomSuffix())
				.logoImgPath("https://example.com/macallan-logo.jpg")
				.build();
		em.persist(distillery);
		return distillery;
	}

	/**
	 * 커스텀 Distillery 생성
	 */
	@Transactional
	public Distillery persistDistillery(String korName, String engName) {
		Distillery distillery = Distillery.builder()
				.korName(korName + "-" + generateRandomSuffix())
				.engName(engName + "-" + generateRandomSuffix())
				.build();
		em.persist(distillery);
		return distillery;
	}

	/**
	 * 빌더를 통한 Distillery 생성
	 */
	@Transactional
	public Distillery persistDistillery(Distillery.DistilleryBuilder builder) {
		Distillery distillery = builder.build();
		em.persist(distillery);
		return distillery;
	}

	/**
	 * 기본 Alcohol 생성 (위스키) - 연관 엔티티 자동 생성
	 */
	@Transactional
	public Alcohol persistAlcohol() {
		// 연관 엔티티 자동 생성
		Region region = persistRegionInternal();
		Distillery distillery = persistDistilleryInternal();

		Alcohol alcohol = Alcohol.builder()
				.korName("명작 위스키-" + generateRandomSuffix())
				.engName("Masterpiece Whisky-" + generateRandomSuffix())
				.abv("40%")
				.type(AlcoholType.WHISKY)
				.korCategory("위스키")
				.engCategory("Whiskey")
				.categoryGroup(AlcoholCategoryGroup.SINGLE_MALT)
				.region(region)
				.distillery(distillery)
				.cask("American Oak")
				.imageUrl("https://example.com/image.jpg")
				.build();
		em.persist(alcohol);
		return alcohol;
	}


	/**
	 * 타입별 Alcohol 생성 - 연관 엔티티 자동 생성
	 */
	@Transactional
	public Alcohol persistAlcohol(AlcoholType type) {
		// 연관 엔티티 자동 생성
		Region region = persistRegionInternal();
		Distillery distillery = persistDistilleryInternal();

		Alcohol alcohol = Alcohol.builder()
				.korName(type.getDefaultKorName() + "-" + generateRandomSuffix())
				.engName(type.getDefaultEngName() + "-" + generateRandomSuffix())
				.abv("40%")
				.type(type)
				.korCategory(type.getKorCategory())
				.engCategory(type.getEngCategory())
				.categoryGroup(type.getDefaultCategoryGroup())
				.region(region)
				.distillery(distillery)
				.cask("Oak")
				.imageUrl("https://example.com/" + type.name().toLowerCase() + ".jpg")
				.build();
		em.persist(alcohol);
		return alcohol;
	}


	/**
	 * 이름과 타입으로 Alcohol 생성 - 연관 엔티티 자동 생성
	 */
	@Transactional
	public Alcohol persistAlcohol(String korName, String engName, AlcoholType type) {
		// 연관 엔티티 자동 생성
		Region region = persistRegionInternal();
		Distillery distillery = persistDistilleryInternal();

		Alcohol alcohol = Alcohol.builder()
				.korName(korName + "-" + generateRandomSuffix())
				.engName(engName + "-" + generateRandomSuffix())
				.abv("40%")
				.type(type)
				.korCategory(type.getKorCategory())
				.engCategory(type.getEngCategory())
				.categoryGroup(type.getDefaultCategoryGroup())
				.region(region)
				.distillery(distillery)
				.cask("Oak")
				.imageUrl("https://example.com/custom.jpg")
				.build();
		em.persist(alcohol);
		return alcohol;
	}


	/**
	 * 연관 엔티티와 함께 Alcohol 생성
	 */
	@Transactional
	public Alcohol persistAlcohol(AlcoholType type, Region region, Distillery distillery) {
		Alcohol alcohol = Alcohol.builder()
				.korName(type.getDefaultKorName() + "-" + generateRandomSuffix())
				.engName(type.getDefaultEngName() + "-" + generateRandomSuffix())
				.abv("40%")
				.type(type)
				.korCategory(type.getKorCategory())
				.engCategory(type.getEngCategory())
				.categoryGroup(type.getDefaultCategoryGroup())
				.region(region)
				.distillery(distillery)
				.cask("Oak")
				.imageUrl("https://example.com/" + type.name().toLowerCase() + ".jpg")
				.build();
		em.persist(alcohol);
		return alcohol;
	}


	/**
	 * 빌더를 통한 Alcohol 생성 - 누락 필드 자동 채우기
	 */
	@Transactional
	public Alcohol persistAlcohol(Alcohol.AlcoholBuilder builder) {
		// 누락 필드 채우기
		Alcohol.AlcoholBuilder filledBuilder = fillMissingAlcoholFields(builder);
		Alcohol alcohol = filledBuilder.build();
		em.persist(alcohol);
		return alcohol;
	}

	/**
	 * 빌더를 통한 Alcohol 생성 후 flush (즉시 ID 필요한 경우)
	 */
	@Transactional
	public Alcohol persistAndFlushAlcohol(Alcohol.AlcoholBuilder builder) {
		// 누락 필드 채우기
		Alcohol.AlcoholBuilder filledBuilder = fillMissingAlcoholFields(builder);
		Alcohol alcohol = filledBuilder.build();
		em.persist(alcohol);
		em.flush(); // 즈시 ID 필요한 경우에만 사용
		return alcohol;
	}

	/**
	 * 랜덤 접미사 생성 헬퍼 메서드
	 */
	private String generateRandomSuffix() {
		return String.valueOf(random.nextInt(10000));
	}

	/**
	 * 내부용 Region 생성 (트랜잭션 전파 없음)
	 */
	private Region persistRegionInternal() {
		Region region = Region.builder()
				.korName("스코틀랜드-" + generateRandomSuffix())
				.engName("Scotland-" + generateRandomSuffix())
				.continent("Europe")
				.build();
		em.persist(region);
		return region;
	}

	/**
	 * 내부용 Distillery 생성 (트랜잭션 전파 없음)
	 */
	private Distillery persistDistilleryInternal() {
		Distillery distillery = Distillery.builder()
				.korName("맥캘란-" + generateRandomSuffix())
				.engName("Macallan-" + generateRandomSuffix())
				.logoImgPath("https://example.com/macallan-logo.jpg")
				.build();
		em.persist(distillery);
		return distillery;
	}

	/**
	 * Alcohol 빌더의 누락 필드 채우기
	 */
	private Alcohol.AlcoholBuilder fillMissingAlcoholFields(Alcohol.AlcoholBuilder builder) {
		// 빌더를 임시로 빌드해서 필드 체크
		Alcohol tempAlcohol;
		try {
			tempAlcohol = builder.build();
		} catch (Exception e) {
			// 필수 필드 누락 시 기본값으로 채우기
			AlcoholType defaultType = AlcoholType.WHISKY;
			return builder
					.korName(defaultType.getDefaultKorName() + "-" + generateRandomSuffix())
					.engName(defaultType.getDefaultEngName() + "-" + generateRandomSuffix())
					.abv("40%")
					.type(defaultType)
					.korCategory(defaultType.getKorCategory())
					.engCategory(defaultType.getEngCategory())
					.categoryGroup(defaultType.getDefaultCategoryGroup())
					.region(persistRegionInternal())
					.distillery(persistDistilleryInternal())
					.cask("Oak")
					.imageUrl("https://example.com/default.jpg");
		}

		// 개별 필드 체크 및 채우기
		if (tempAlcohol.getKorName() == null) {
			AlcoholType type = tempAlcohol.getType() != null ? tempAlcohol.getType() : AlcoholType.WHISKY;
			builder.korName(type.getDefaultKorName() + "-" + generateRandomSuffix());
		}
		if (tempAlcohol.getEngName() == null) {
			AlcoholType type = tempAlcohol.getType() != null ? tempAlcohol.getType() : AlcoholType.WHISKY;
			builder.engName(type.getDefaultEngName() + "-" + generateRandomSuffix());
		}
		if (tempAlcohol.getType() == null) {
			builder.type(AlcoholType.WHISKY);
		}
		if (tempAlcohol.getAbv() == null) {
			builder.abv("40%");
		}
		if (tempAlcohol.getKorCategory() == null) {
			AlcoholType type = tempAlcohol.getType() != null ? tempAlcohol.getType() : AlcoholType.WHISKY;
			builder.korCategory(type.getKorCategory());
		}
		if (tempAlcohol.getEngCategory() == null) {
			AlcoholType type = tempAlcohol.getType() != null ? tempAlcohol.getType() : AlcoholType.WHISKY;
			builder.engCategory(type.getEngCategory());
		}
		if (tempAlcohol.getCategoryGroup() == null) {
			AlcoholType type = tempAlcohol.getType() != null ? tempAlcohol.getType() : AlcoholType.WHISKY;
			builder.categoryGroup(type.getDefaultCategoryGroup());
		}
		if (tempAlcohol.getRegion() == null) {
			builder.region(persistRegionInternal());
		}
		if (tempAlcohol.getDistillery() == null) {
			builder.distillery(persistDistilleryInternal());
		}
		if (tempAlcohol.getCask() == null) {
			builder.cask("Oak");
		}
		if (tempAlcohol.getImageUrl() == null) {
			builder.imageUrl("https://example.com/default.jpg");
		}

		return builder;
	}

}
