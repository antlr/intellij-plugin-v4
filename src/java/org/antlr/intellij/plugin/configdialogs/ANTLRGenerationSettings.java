package org.antlr.intellij.plugin.configdialogs;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores settings related to code generation per grammar file.
 */
public class ANTLRGenerationSettings {
	@SuppressWarnings("WeakerAccess")
	public List<PerGrammarGenerationSettings> perGrammarGenerationSettings
			= new ArrayList<PerGrammarGenerationSettings>();

	public PerGrammarGenerationSettings findSettingsForFile(String fileName) {
		for (PerGrammarGenerationSettings settings : perGrammarGenerationSettings) {
			if (settings.fileName.equals(fileName)) {
				return settings;
			}
		}

		PerGrammarGenerationSettings settings = new PerGrammarGenerationSettings();
		settings.fileName = fileName;
		perGrammarGenerationSettings.add(settings);

		return settings;
	}
}
