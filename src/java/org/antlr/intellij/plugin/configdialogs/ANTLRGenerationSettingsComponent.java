package org.antlr.intellij.plugin.configdialogs;

import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;

/**
 * Stores code generation preferences in <code>.idea/misc.xml</code>.
 */
@State(name = "ANTLRGenerationPreferences")
public class ANTLRGenerationSettingsComponent implements PersistentStateComponent<ANTLRGenerationSettings> {

	private ANTLRGenerationSettings mySettings = new ANTLRGenerationSettings();

	public static ANTLRGenerationSettingsComponent getInstance(Project project) {
		return ServiceManager.getService(project, ANTLRGenerationSettingsComponent.class);
	}

	public ANTLRGenerationSettings getSettings() {
		return mySettings;
	}

	@Nullable
	@Override
	public ANTLRGenerationSettings getState() {
		return mySettings;
	}

	@Override
	public void loadState(ANTLRGenerationSettings state) {
		mySettings = state;
	}
}
