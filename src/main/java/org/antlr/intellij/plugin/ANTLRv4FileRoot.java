package org.antlr.intellij.plugin;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import org.jetbrains.annotations.NotNull;

public class ANTLRv4FileRoot extends PsiFileBase {
    public ANTLRv4FileRoot(@NotNull FileViewProvider viewProvider) {
        super(viewProvider, ANTLRv4Language.INSTANCE);
    }

    @NotNull
    @Override
    public FileType getFileType() {
        return ANTLRv4FileType.INSTANCE;
    }

    @Override
    public String toString() {
        return "ANTLR v4 grammar file";
    }
}
