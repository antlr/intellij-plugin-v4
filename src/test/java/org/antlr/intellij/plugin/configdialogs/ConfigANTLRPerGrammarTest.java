package org.antlr.intellij.plugin.configdialogs;

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

    private ANTLRv4GrammarProperties originalProperties;
    private ConfigANTLRPerGrammar form;

    @Before
    public void setUp() {
        originalProperties = buildOriginalProperties();
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

    private ANTLRv4GrammarProperties buildOriginalProperties() {
        ANTLRv4GrammarProperties properties = new ANTLRv4GrammarProperties();

        properties.outputDir = DEFAULT_OUTPUT_DIR;
        properties.encoding = DEFAULT_ENCODING;
        properties.libDir = DEFAULT_LIBRARY;
        properties.pkg = DEFAULT_PACKAGE;
        properties.language = DEFAULT_LANGUAGE;

        return properties;
    }

    @Test
    public void shouldDetectModifiedOutputDirectory() {
        // given:
        when(form.getOutputDirText()).thenReturn("ModifiedOutputDir");

        // then:
        Assert.assertTrue(form.isModified(originalProperties));
    }

    @Test
    public void shouldDetectModifiedLibDirectory() {
        // given:
        when(form.getLibDirText()).thenReturn("ModifiedLibDir");

        // then:
        Assert.assertTrue(form.isModified(originalProperties));
    }

    @Test
    public void shouldDetectModifiedEncoding() {
        // given:
        when(form.getFileEncodingText()).thenReturn("ModifiedEncoding");

        // then:
        Assert.assertTrue(form.isModified(originalProperties));
    }

    @Test
    public void shouldDetectModifiedPackage() {
        // given:
        when(form.getFileEncodingText()).thenReturn("ModifiedPackage");

        // then:
        Assert.assertTrue(form.isModified(originalProperties));
    }

    @Test
    public void shouldDetectModifiedLanguage() {
        // given:
        when(form.getLanguageText()).thenReturn("ModifiedLanguage");

        // then:
        Assert.assertTrue(form.isModified(originalProperties));
    }
}