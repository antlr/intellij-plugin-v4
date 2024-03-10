package org.antlr.intellij.plugin;

import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.util.IconLoader;

import javax.swing.*;

public class Icons {
	public static final Icon FILE = IconLoader.getIcon("/icons/org/antlr/intellij/plugin/antlr.png", Icons.class);
	public static final Icon LEXER_RULE = IconLoader.getIcon("/icons/org/antlr/intellij/plugin/lexer-rule.png", Icons.class);
	public static final Icon PARSER_RULE = IconLoader.getIcon("/icons/org/antlr/intellij/plugin/parser-rule.png", Icons.class);
	public static final Icon MODE = IconLoader.getIcon("/icons/org/antlr/intellij/plugin/mode.png", Icons.class);

	public static Icon getToolWindow() {
		// IntelliJ 2018.2+ has monochrome icons for tool windows so let's use one too
		if (ApplicationInfo.getInstance().getBuild().getBaselineVersion() >= 182) {
			return IconLoader.getIcon("/icons/org/antlr/intellij/plugin/toolWindowAntlr.svg", Icons.class);
		}

		return IconLoader.getIcon("/icons/org/antlr/intellij/plugin/antlr.png", Icons.class);
	}
}
