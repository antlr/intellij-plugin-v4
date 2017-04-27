package org.antlr.intellij.plugin;

import com.intellij.lang.BracePair;
import com.intellij.lang.PairedBraceMatcher;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import org.antlr.intellij.adaptor.lexer.TokenIElementType;
import org.antlr.intellij.plugin.parser.ANTLRv4Lexer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ANTLRv4PairedBraceMatcher implements PairedBraceMatcher {

    private static final TokenIElementType LPAREN = ANTLRv4TokenTypes.getTokenElementType(ANTLRv4Lexer.LPAREN);
    private static final TokenIElementType RPAREN = ANTLRv4TokenTypes.getTokenElementType(ANTLRv4Lexer.RPAREN);

    @Override
    public BracePair[] getPairs() {
        return new BracePair[] {
                new BracePair(LPAREN, RPAREN, true)
        };
    }

    @Override
    public boolean isPairedBracesAllowedBeforeType(@NotNull IElementType iElementType, @Nullable IElementType iElementType1) {
        return true;
    }

    @Override
    public int getCodeConstructStart(PsiFile psiFile, int openingBraceOffset) {
        return openingBraceOffset;
    }
}
