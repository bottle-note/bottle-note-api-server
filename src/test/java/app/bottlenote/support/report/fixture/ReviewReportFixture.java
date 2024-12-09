package app.bottlenote.support.report.fixture;

import app.bottlenote.support.report.domain.ReviewReport;
import app.bottlenote.support.report.domain.ReviewReport.ReviewReportBuilder;

public class ReviewReportFixture {

	public static ReviewReportBuilder getReviewReportObject() {
		return ReviewReport.builder();
	}
}
