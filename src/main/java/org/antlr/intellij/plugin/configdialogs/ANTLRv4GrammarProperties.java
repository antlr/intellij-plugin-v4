package org.antlr.intellij.plugin.configdialogs;

import com.intellij.ide.util.PropertiesComponent;
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

    public static String getPropNameForFile(String qualFileName, String prop) {
        return qualFileName+"::/"+prop;
    }
}
