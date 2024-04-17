package app.bottlenote.common.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
public class CommonService {

	public LocalDateTime restdocs() {
		log.info("restdocs");
		return LocalDateTime.now();
	}
}
