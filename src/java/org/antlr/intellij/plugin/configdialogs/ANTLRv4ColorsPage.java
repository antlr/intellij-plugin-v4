package org.antlr.intellij.plugin.configdialogs;

import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.options.colors.AttributesDescriptor;
import com.intellij.openapi.options.colors.ColorDescriptor;
import com.intellij.openapi.options.colors.ColorSettingsPage;
import org.antlr.intellij.plugin.ANTLRv4SyntaxHighlighter;
import org.antlr.intellij.plugin.Icons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Map;

public class ANTLRv4ColorsPage implements ColorSettingsPage {
	private static final AttributesDescriptor[] ATTRIBUTES =
		{
			new AttributesDescriptor("Lexer Rule", ANTLRv4SyntaxHighlighter.TOKENNAME),
			new AttributesDescriptor("Parser Rule", ANTLRv4SyntaxHighlighter.RULENAME),
		};

	@Nullable
	@Override
	public Icon getIcon() {
		return Icons.FILE;
	}

	@NotNull
	@Override
	public SyntaxHighlighter getHighlighter() {
		return new ANTLRv4SyntaxHighlighter();
	}

	@NotNull
	@Override
	public String getDemoText() {
		return
			"grammar Foo;\n" +
			"\n" +
			"compilationUnit : STUFF EOF;\n" +
			"\n" +
			"STUFF : 'stuff' -> pushMode(OTHER_MODE);\n" +
			"WS : [ \\t]+ -> channel(HIDDEN);\n" +
			"NEWLINE : [\\r\\n]+ -> type(WS);\n" +
			"BAD_CHAR : . -> skip;\n" +
			"\n" +
			"mode OTHER_MODE;\n" +
			"\n" +
			"KEYWORD : 'keyword' -> popMode;\n";
	}

	@Nullable
	@Override
	public Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
		return null;
	}

	@NotNull
	@Override
	public AttributesDescriptor[] getAttributeDescriptors() {
		return ATTRIBUTES;
	}

	@NotNull
	@Override
	public ColorDescriptor[] getColorDescriptors() {
		return ColorDescriptor.EMPTY_ARRAY;
	}

	@NotNull
	@Override
	public String getDisplayName() {
		return "ANTLR";
	}
}
