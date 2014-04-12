package org.antlr.intellij.plugin.preview;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.vfs.VirtualFile;
import org.antlr.intellij.plugin.Icons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class PreviewFileType extends LanguageFileType {
	public static final FileType INSTANCE = new PreviewFileType();

	protected PreviewFileType() {
		super(PreviewLanguage.INSTANCE);
	}

	@NotNull
	@Override
	public String getName() {
		return "Grammar Preview";
	}

	@NotNull
	@Override
	public String getDescription() {
		return "Grammar Preview";
	}

	@NotNull
	@Override
	public String getDefaultExtension() {
		return "input";
	}

	@Nullable
	@Override
	public Icon getIcon() {
		return Icons.FILE;
	}

	@Override
	public boolean isReadOnly() {
		return false;
	}

	@Nullable
	@Override
	public String getCharset(@NotNull VirtualFile file, byte[] content) {
		return null;
	}
}
