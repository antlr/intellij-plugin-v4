package org.antlr.intellij.plugin.configdialogs;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

import static org.antlr.intellij.plugin.configdialogs.ANTLRv4GrammarPropertiesStore.getGrammarProperties;

/**
 * The UI that allows viewing/modifying default grammar settings for an entire project.
 *
 * @see ConfigANTLRPerGrammar
 */
public class ANTLRv4ProjectSettings implements SearchableConfigurable, Disposable {

    private ConfigANTLRPerGrammar configurationForm;

    private final Project project;

    public ANTLRv4ProjectSettings(@NotNull Project project) {
        this.project = project;
    }

    @NotNull
    @Override
    public String getId() {
        return "ANTLR4ProjectSettings";
    }

    @Nullable
    @Override
    public Runnable enableSearch(String option) {
        return null;
    }

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "ANTLR4 Project Settings";
    }

    @Nullable
    public String getHelpTopic() {
        return "ANTLR4 Project Settings";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        configurationForm = ConfigANTLRPerGrammar.getProjectSettingsForm(project, ANTLRv4GrammarProperties.PROJECT_SETTINGS_PREFIX);
        return configurationForm.createCenterPanel();
    }

    @Override
    public boolean isModified() {
        ANTLRv4GrammarProperties grammarProperties = getGrammarProperties(project, ANTLRv4GrammarProperties.PROJECT_SETTINGS_PREFIX);
        return configurationForm.isModified(grammarProperties);
    }

    @Override
    public void apply() {
        configurationForm.saveValues(project, ANTLRv4GrammarProperties.PROJECT_SETTINGS_PREFIX);
    }

    public void reset() {
        configurationForm.loadValues(project, ANTLRv4GrammarProperties.PROJECT_SETTINGS_PREFIX);
    }

    @Override
    public void disposeUIResources() {
        Disposer.dispose(configurationForm.getDisposable());
    }

    @Override
    public void dispose() {
        configurationForm = null;
    }
}
