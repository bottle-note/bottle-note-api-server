package app.rule;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.BeforeEach;

@SuppressWarnings({"JUnitTestClassNamingConvention"})
public abstract class AbstractRules {
	protected JavaClasses importedClasses;

	@BeforeEach
	public void setup() {
		importedClasses = new ClassFileImporter().importPackages("app.bottlenote");
	}
}
