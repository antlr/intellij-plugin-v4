package org.antlr.intellij.plugin;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Created by miguel on 04/04/2014.
 */
public class ANTLRv4SettingsConfigurable implements Configurable {
    private JComponent _component;
    private JCheckBox _listenerCb, _visitorCb;
    private JPanel _panel;
    private Project _project;

    public ANTLRv4SettingsConfigurable(Project proj) {
        _project = proj;
    }

    @Nls
    @Override
    public String getDisplayName() {
        return "ANTLR Settings";
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return "ANTLR Settings";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        boolean generate_listener = PropertiesComponent.getInstance(_project).getBoolean("antlr4-generate-listener", false);
        boolean generate_visitor = PropertiesComponent.getInstance(_project).getBoolean("antlr4-generate-visitor", false);

        _listenerCb.setSelected(generate_listener);
        _visitorCb.setSelected(generate_visitor);

        _component = (JComponent) _panel;
        return _component;
    }

    @Override
    public boolean isModified() {
        return true;
    }

    @Override
    public void apply() throws ConfigurationException {
        boolean generate_listener = _listenerCb.isSelected();
        boolean generate_visitor = _visitorCb.isSelected();

        PropertiesComponent.getInstance(_project).setValue("antlr4-generate-listener", Boolean.toString(generate_listener));
        PropertiesComponent.getInstance(_project).setValue("antlr4-generate-visitor", Boolean.toString(generate_visitor));
    }

    @Override
    public void reset() {

    }

    @Override
    public void disposeUIResources() {

    }
}
