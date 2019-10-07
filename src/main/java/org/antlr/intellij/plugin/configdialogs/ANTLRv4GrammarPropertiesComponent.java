package org.antlr.intellij.plugin.configdialogs;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * Stores code generation preferences in <code>.idea/misc.xml</code>.
 */
@State(name = "ANTLRGenerationPreferences")
public class ANTLRv4GrammarPropertiesComponent implements PersistentStateComponent<ANTLRv4GrammarPropertiesStore> {

	private ANTLRv4GrammarPropertiesStore mySettings = new ANTLRv4GrammarPropertiesStore();

	public static ANTLRv4GrammarPropertiesComponent getInstance(Project project) {
		return ServiceManager.getService(project, ANTLRv4GrammarPropertiesComponent.class);
	}

	@NotNull
	@Override
	public ANTLRv4GrammarPropertiesStore getState() {
		return mySettings;
	}

	@Override
	public void loadState(ANTLRv4GrammarPropertiesStore state) {
		mySettings = state;
	}
}
