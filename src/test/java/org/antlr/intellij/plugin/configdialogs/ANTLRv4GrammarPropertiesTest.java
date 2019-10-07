package org.antlr.intellij.plugin.configdialogs;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ANTLRv4GrammarPropertiesTest {

    private static final String QUAL_FILE_NAME = "file";
    private static final String FILE_VALUE = "fileValue";
    private static final String PROJECT_VALUE = "projectValue";
    private static final String DEFAULT_VALUE = "";

    private ANTLRv4GrammarPropertiesStore propertiesStore;

    @Before
    public void setUp() {
        propertiesStore = new ANTLRv4GrammarPropertiesStore();
    }

    @Test
    public void shouldGetPropertyFromFileSettingsWhenDefined() {
        // given:
        ANTLRv4GrammarProperties fileProps = new ANTLRv4GrammarProperties();
        fileProps.fileName = QUAL_FILE_NAME;
        fileProps.language = FILE_VALUE;
        propertiesStore.add(fileProps);

        ANTLRv4GrammarProperties projectProps = new ANTLRv4GrammarProperties();
        projectProps.fileName = "*";
        projectProps.language = PROJECT_VALUE;
        propertiesStore.add(projectProps);

        // when:
        String propertyValueForFile = propertiesStore.getGrammarProperties(QUAL_FILE_NAME).getLanguage();

        // then:
        Assert.assertEquals(FILE_VALUE, propertyValueForFile);
    }

    @Test
    public void shouldGetPropertyProjectSettingsValueWhenNotSetForFile() {
        // given:
        ANTLRv4GrammarProperties projectProps = new ANTLRv4GrammarProperties();
        projectProps.fileName = "*";
        projectProps.language = PROJECT_VALUE;
        propertiesStore.add(projectProps);

        // when:
        String propertyValueForFile = propertiesStore.getGrammarProperties(QUAL_FILE_NAME).getLanguage();

        // then:
        Assert.assertEquals(PROJECT_VALUE, propertyValueForFile);
    }

    @Test
    public void shouldGetPropertyDefaultValueWhenNotSetForFileNorProject() {
        // when:
        String propertyValueForFile = propertiesStore.getGrammarProperties(QUAL_FILE_NAME).getLanguage();

        // then:
        Assert.assertEquals(DEFAULT_VALUE, propertyValueForFile);
    }

}