package org.antlr.intellij.plugin.preview;

import com.intellij.lang.Language;

public class PreviewLanguage extends Language {
	public static final PreviewLanguage INSTANCE = new PreviewLanguage();

	private PreviewLanguage() {
		super("PreviewParserInput");
	}
}
