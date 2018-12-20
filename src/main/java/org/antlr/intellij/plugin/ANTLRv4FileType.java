package org.antlr.intellij.plugin;

import com.intellij.openapi.fileTypes.LanguageFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class ANTLRv4FileType extends LanguageFileType {
	public static final ANTLRv4FileType INSTANCE = new ANTLRv4FileType();

	private ANTLRv4FileType() {
		super(ANTLRv4Language.INSTANCE);
	}

	@NotNull
	@Override
	public String getName() {
		return "ANTLR v4 grammar file";
	}

	@NotNull
	@Override
	public String getDescription() {
		return "ANTLR v4 grammar file";
	}

	@NotNull
	@Override
	public String getDefaultExtension() {
		return "g4";
	}

	@Nullable
	@Override
	public Icon getIcon() {
		return Icons.FILE;
	}
}
