package org.antlr.intellij.plugin.configdialogs;

import com.intellij.ide.util.PropertiesComponent;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

public class ConfigANTLRPerGrammarTest {

    private static final String DEFAULT_OUTPUT_DIR = "DefaultOutputDir";
    private static final String DEFAULT_ENCODING = "DefaultEncoding";
    private static final String DEFAULT_LIBRARY = "DefaultLibrary";
    private static final String DEFAULT_PACKAGE = "DefaultPackage";
    private static final String DEFAULT_LANGUAGE = "DefaultLanguage";

    private PropertiesComponent propertiesComponent;
    private ConfigANTLRPerGrammar form;

    @Before
    public void setUp() {
        propertiesComponent = mockPropertiesComponent();
        this.form = mockForm();
    }

    private ConfigANTLRPerGrammar mockForm() {
        ConfigANTLRPerGrammar form = Mockito.mock(ConfigANTLRPerGrammar.class, Mockito.CALLS_REAL_METHODS);
        doReturn(DEFAULT_OUTPUT_DIR).when(form).getOutputDirText();
        doReturn(DEFAULT_ENCODING).when(form).getFileEncodingText();
        doReturn(DEFAULT_LIBRARY).when(form).getLibDirText();
        doReturn(DEFAULT_PACKAGE).when(form).getPackageFieldText();
        doReturn(DEFAULT_LANGUAGE).when(form).getLanguageText();
        return form;
    }

    private PropertiesComponent mockPropertiesComponent() {
        PropertiesComponent propertiesComponent = Mockito.mock(PropertiesComponent.class);
        when(propertiesComponent.getValue(ANTLRv4GrammarProperties.getPropNameForFile(ANTLRv4GrammarProperties.PROJECT_SETTINGS_PREFIX, ANTLRv4GrammarProperties.PROP_OUTPUT_DIR), ""))
                .thenReturn(DEFAULT_OUTPUT_DIR);
        when(propertiesComponent.getValue(ANTLRv4GrammarProperties.getPropNameForFile(ANTLRv4GrammarProperties.PROJECT_SETTINGS_PREFIX, ANTLRv4GrammarProperties.PROP_ENCODING), ""))
                .thenReturn(DEFAULT_ENCODING);
        when(propertiesComponent.getValue(ANTLRv4GrammarProperties.getPropNameForFile(ANTLRv4GrammarProperties.PROJECT_SETTINGS_PREFIX, ANTLRv4GrammarProperties.PROP_LIB_DIR), ""))
                .thenReturn(DEFAULT_LIBRARY);
        when(propertiesComponent.getValue(ANTLRv4GrammarProperties.getPropNameForFile(ANTLRv4GrammarProperties.PROJECT_SETTINGS_PREFIX, ANTLRv4GrammarProperties.PROP_PACKAGE), ""))
                .thenReturn(DEFAULT_PACKAGE);
        when(propertiesComponent.getValue(ANTLRv4GrammarProperties.getPropNameForFile(ANTLRv4GrammarProperties.PROJECT_SETTINGS_PREFIX, ANTLRv4GrammarProperties.PROP_LANGUAGE), ""))
                .thenReturn(DEFAULT_LANGUAGE);
        return propertiesComponent;
    }

    @Test
    public void shouldDetectModifiedOutputDirectory() {
        // given:
        when(form.getOutputDirText()).thenReturn("ModifiedOutputDir");

        // then:
        Assert.assertTrue(form.isModified(propertiesComponent, ANTLRv4GrammarProperties.PROJECT_SETTINGS_PREFIX));
    }

    @Test
    public void shouldDetectModifiedLibDirectory() {
        // given:
        when(form.getLibDirText()).thenReturn("ModifiedLibDir");

        // then:
        Assert.assertTrue(form.isModified(propertiesComponent, ANTLRv4GrammarProperties.PROJECT_SETTINGS_PREFIX));
    }

    @Test
    public void shouldDetectModifiedEncoding() {
        // given:
        when(form.getFileEncodingText()).thenReturn("ModifiedEncoding");

        // then:
        Assert.assertTrue(form.isModified(propertiesComponent, ANTLRv4GrammarProperties.PROJECT_SETTINGS_PREFIX));
    }

    @Test
    public void shouldDetectModifiedPackage() {
        // given:
        when(form.getFileEncodingText()).thenReturn("ModifiedPackage");

        // then:
        Assert.assertTrue(form.isModified(propertiesComponent, ANTLRv4GrammarProperties.PROJECT_SETTINGS_PREFIX));
    }

    @Test
    public void shouldDetectModifiedLanguage() {
        // given:
        when(form.getLanguageText()).thenReturn("ModifiedLanguage");

        // then:
        Assert.assertTrue(form.isModified(propertiesComponent, ANTLRv4GrammarProperties.PROJECT_SETTINGS_PREFIX));
    }
}