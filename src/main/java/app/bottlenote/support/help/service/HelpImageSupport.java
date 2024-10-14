package app.bottlenote.support.help.service;

import app.bottlenote.support.help.domain.Help;
import app.bottlenote.support.help.domain.HelpImage;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HelpImageSupport {

	public void saveHelpImages(Help help, List<HelpImage> helpImages) {
		help.saveImages(helpImages);
	}

	public void updateHelpImages(Help help, List<HelpImage> helpImages){
		help.updateImages(helpImages);
	}
}
