package org.antlr.intellij.plugin.configdialogs;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

public class ANTLRv4GrammarProperties {
    public static final String PROP_AUTO_GEN = "auto-gen";
    public static final String PROP_OUTPUT_DIR = "output-dir";
    public static final String PROP_LIB_DIR = "lib-dir";
    public static final String PROP_ENCODING = "encoding";
    public static final String PROP_PACKAGE = "package";
    public static final String PROP_LANGUAGE = "language";
    public static final String PROP_GEN_LISTENER = "gen-listener";
    public static final String PROP_GEN_VISITOR = "gen-visitor";
    static final String PROJECT_SETTINGS_PREFIX = "*";

    static boolean shouldGenerateParseTreeVisitor(String qualFileName, PropertiesComponent props) {
        return props.getBoolean(getPropNameForFile(qualFileName, PROP_GEN_VISITOR), false);
    }

    static boolean shouldGenerateParseTreeListener(String qualFileName, PropertiesComponent props) {
        return props.getBoolean(getPropNameForFile(qualFileName, PROP_GEN_LISTENER), true);
    }

    @NotNull
    static String getLanguage(String qualFileName, PropertiesComponent props) {
        return props.getValue(getPropNameForFile(qualFileName, PROP_LANGUAGE), "");
    }

    @NotNull
    static String getPackage(String qualFileName, PropertiesComponent props) {
        return props.getValue(getPropNameForFile(qualFileName, PROP_PACKAGE), "");
    }

    @NotNull
    static String getEncoding(String qualFileName, PropertiesComponent props) {
        return props.getValue(getPropNameForFile(qualFileName, PROP_ENCODING), "");
    }

    @NotNull
    static String getLibDir(String qualFileName, PropertiesComponent props) {
        return props.getValue(getPropNameForFile(qualFileName, PROP_LIB_DIR), "");
    }

    @NotNull
    static String getOutputDir(String qualFileName, PropertiesComponent props) {
        return props.getValue(getPropNameForFile(qualFileName, PROP_OUTPUT_DIR), "");
    }

    static boolean shouldAutoGenerateParser(String qualFileName, PropertiesComponent props) {
        return props.getBoolean(getPropNameForFile(qualFileName, PROP_AUTO_GEN), false);
    }

    static void setGenerateParseTreeVisitor(PropertiesComponent props, String qualFileName, boolean generateParseTreeVisitor) {
        props.setValue(getPropNameForFile(qualFileName, PROP_GEN_VISITOR),
                String.valueOf(generateParseTreeVisitor));
    }

    static void setGenerateParseTreeListener(PropertiesComponent props, String qualFileName, boolean generateParseTreeListener) {
        props.setValue(getPropNameForFile(qualFileName, PROP_GEN_LISTENER),
                String.valueOf(generateParseTreeListener));
    }

    static void setLanguage(PropertiesComponent props, String language, String qualFileName) {
        if (language.trim().length() > 0) {
            props.setValue(getPropNameForFile(qualFileName, PROP_LANGUAGE), language);
        } else {
            props.unsetValue(getPropNameForFile(qualFileName, PROP_LANGUAGE));
        }
    }

    static void setPackageName(PropertiesComponent props, String packageName, String qualFileName) {
        if (packageName.trim().length() > 0) {
            props.setValue(getPropNameForFile(qualFileName, PROP_PACKAGE), packageName);
        } else {
            props.unsetValue(getPropNameForFile(qualFileName, PROP_PACKAGE));
        }
    }

    static void setAutoGen(PropertiesComponent props, String qualFileName, boolean generateParseTreeVisitor) {
        props.setValue(getPropNameForFile(qualFileName, PROP_AUTO_GEN), String.valueOf(generateParseTreeVisitor));
    }

    static void setFileEncoding(PropertiesComponent props, String fileEncoding, String qualFileName) {
        if (fileEncoding.trim().length() > 0) {
            props.setValue(getPropNameForFile(qualFileName, PROP_ENCODING), fileEncoding);
        } else {
            props.unsetValue(getPropNameForFile(qualFileName, PROP_ENCODING));
        }
    }

    static void setLibDir(PropertiesComponent props, String libDir, String qualFileName) {
        if (libDir.trim().length() > 0) {
            props.setValue(getPropNameForFile(qualFileName, PROP_LIB_DIR), libDir);
        } else {
            props.unsetValue(getPropNameForFile(qualFileName, PROP_LIB_DIR));
        }
    }

    static void setOutputDir(PropertiesComponent props, String outputDir, String qualFileName) {
        if (outputDir.trim().length() > 0) {
            props.setValue(getPropNameForFile(qualFileName, PROP_OUTPUT_DIR), outputDir);
        } else {
            props.unsetValue(getPropNameForFile(qualFileName, PROP_OUTPUT_DIR));
        }
    }

    public static String getPropNameForFile(String qualFileName, String prop) {
        return qualFileName + "::/" + prop;
    }

    public static String getProp(Project project, String qualFileName, String name, String defaultValue) {
        PropertiesComponent props = PropertiesComponent.getInstance(project);
        return getPropertyValueForFile(props, qualFileName, name, defaultValue);
    }

    static String getPropertyValueForFile(PropertiesComponent props, String qualFileName, String name, String defaultValue) {
        String propertyValue = props.getValue(getPropNameForFile(qualFileName, name));
        if (!StringUtils.isEmpty(propertyValue)) {
            return propertyValue;
        }

        String projectPropertyValue = props.getValue(getPropNameForFile(PROJECT_SETTINGS_PREFIX, name));
        if (!StringUtils.isEmpty(projectPropertyValue)) {
            return projectPropertyValue;
        }

        return defaultValue;
    }
}
