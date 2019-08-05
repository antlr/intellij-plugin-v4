package org.antlr.intellij.plugin.configdialogs;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

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
        return configurationForm.isModified(PropertiesComponent.getInstance(project), ANTLRv4GrammarProperties.PROJECT_SETTINGS_PREFIX);
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
