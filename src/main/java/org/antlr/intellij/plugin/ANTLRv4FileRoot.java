package org.antlr.intellij.plugin;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.util.IncorrectOperationException;
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
    public @NotNull String getName() {
        return super.getName().replace(".g4", "");
    }

    @Override
    public PsiElement setName(@NotNull String name) throws IncorrectOperationException {
        return super.setName(name.replace(".g4", "") + ".g4");
    }

    @Override
    public String toString() {
        return "ANTLR v4 grammar file";
    }
}
