package org.antlr.intellij.plugin.configdialogs;

import com.intellij.ide.util.PropertiesComponent;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.when;

public class ANTLRv4GrammarPropertiesTest {

    private static final String MY_PROP_NAME = "myproperty";
    private static final String QUAL_FILE_NAME = "file";
    private static final String PROJECT_VALUE = "projectValue";
    private static final String DEFAULT_VALUE = "defVal";

    private PropertiesComponent propertiesComponent;

    @Before
    public void setUp() {
        propertiesComponent = Mockito.mock(PropertiesComponent.class);
    }

    @Test
    public void shouldGetPropertyProjectSettingsValueWhenNotSetForFile() {
        // given:
        when(propertiesComponent.getValue(ANTLRv4GrammarProperties.getPropNameForFile(QUAL_FILE_NAME, MY_PROP_NAME))).thenReturn(null);
        when(propertiesComponent.getValue(ANTLRv4GrammarProperties.getPropNameForFile("*", MY_PROP_NAME))).thenReturn(PROJECT_VALUE);

        // when:
        String propertyValueForFile = ANTLRv4GrammarProperties.getPropertyValueForFile(propertiesComponent, QUAL_FILE_NAME, MY_PROP_NAME, DEFAULT_VALUE);

        // then:
        Assert.assertEquals(PROJECT_VALUE, propertyValueForFile);
    }

    @Test
    public void shouldGetPropertyDefaultValueWhenNotSetForFileNorProject() {
        // when:
        String propertyValueForFile = ANTLRv4GrammarProperties.getPropertyValueForFile(propertiesComponent, QUAL_FILE_NAME, MY_PROP_NAME, DEFAULT_VALUE);

        // then:
        Assert.assertEquals(DEFAULT_VALUE, propertyValueForFile);
    }

}