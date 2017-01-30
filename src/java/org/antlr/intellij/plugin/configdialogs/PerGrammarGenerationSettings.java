package org.antlr.intellij.plugin.configdialogs;

/**
 * Stores the code generation settings for a given grammar file.
 */
@SuppressWarnings("WeakerAccess")
public class PerGrammarGenerationSettings {
	public String fileName;
	public boolean autoGen;
	public String outputDir;
	public String libDir;
	public String encoding;
	public String pkg;
	public String language;
	public boolean generateListener = true;
	public boolean generateVisitor;
}
