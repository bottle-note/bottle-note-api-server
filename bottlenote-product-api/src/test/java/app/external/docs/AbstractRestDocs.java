package app.external.docs;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import app.bottlenote.global.exception.handler.GlobalExceptionHandler;
import app.docs.config.RestDocsConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.annotation.Import;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.operation.preprocess.Preprocessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.filter.CharacterEncodingFilter;

@Import(RestDocsConfiguration.class)
@ExtendWith(RestDocumentationExtension.class)
public abstract class AbstractRestDocs {

  protected MockMvc mockMvc;
  protected ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp(RestDocumentationContextProvider provider) {
    this.mockMvc =
        MockMvcBuilders.standaloneSetup(initController())
            .apply(
                documentationConfiguration(provider)
                    .operationPreprocessors()
                    .withRequestDefaults(Preprocessors.prettyPrint())
                    .withResponseDefaults(Preprocessors.prettyPrint()))
            .alwaysDo(print())
            .setControllerAdvice(GlobalExceptionHandler.class)
            .addFilters(new CharacterEncodingFilter("UTF-8", true))
            .build();
  }

  protected abstract Object initController();
}
