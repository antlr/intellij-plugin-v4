package org.antlr.intellij.plugin.folding;

import com.intellij.lang.ASTNode;
import com.intellij.lang.folding.CustomFoldingBuilder;
import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.UnfairTextRange;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.impl.source.SourceTreeToPsiMap;
import com.intellij.psi.tree.IElementType;
import org.antlr.intellij.plugin.ANTLRv4FileRoot;
import org.antlr.intellij.plugin.ANTLRv4TokenTypes;
import org.antlr.intellij.plugin.parser.ANTLRv4Lexer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Created by jason on 1/7/15.
 * mostly copied from JavaFoldingBuilderBase
 *
 * @see com.intellij.codeInsight.folding.impl.JavaFoldingBuilderBase
 */
public class ANTLRv4FoldingBuilder extends CustomFoldingBuilder {

    private static final IElementType DOC_COMMENT = ANTLRv4TokenTypes.TOKEN_ELEMENT_TYPES.get(ANTLRv4Lexer.DOC_COMMENT);
    private static final IElementType BLOCK_COMMENT = ANTLRv4TokenTypes.TOKEN_ELEMENT_TYPES.get(ANTLRv4Lexer.BLOCK_COMMENT);


    @Override
    protected void buildLanguageFoldRegions(@NotNull List<FoldingDescriptor> descriptors,
                                            @NotNull PsiElement root,
                                            @NotNull Document document,
                                            boolean quick) {
        if (!(root instanceof ANTLRv4FileRoot)) {
            return;
        }

        ANTLRv4FileRoot file = (ANTLRv4FileRoot) root;

        TextRange range = getFileHeader(file);

        //TODO: is this relevant to antlr?
        if (range != null && range.getLength() > 1 && document.getLineNumber(range.getEndOffset()) > document.getLineNumber(range.getStartOffset())) {
            PsiElement anchorElementToUse = file;
            PsiElement candidate = file.getFirstChild();

            // We experienced the following problem situation:
            //     1. There is a collapsed class-level javadoc;
            //     2. User starts typing at class definition line (e.g. we had definition like 'public class Test' and user starts
            //        typing 'abstract' between 'public' and 'class');
            //     3. Collapsed class-level javadoc automatically expanded. That happened because PSI structure became invalid (because
            //        class definition line at start looks like 'public class Test');
            // So, our point is to preserve fold descriptor referencing javadoc PSI element.
            if (candidate != null && candidate.getTextRange().equals(range)) {
                ASTNode node = candidate.getNode();
                if (node != null && node.getElementType() == DOC_COMMENT) {
                    anchorElementToUse = candidate;
                }
            }
            descriptors.add(new FoldingDescriptor(anchorElementToUse, range));
        }


    }

    @SuppressWarnings("RedundantIfStatement")
    private static boolean headerCandidate(PsiElement element) {
        if (element instanceof PsiComment) return true;
        IElementType type = element.getNode().getElementType();
        if (type == BLOCK_COMMENT) return true;
        if (type == DOC_COMMENT) return true;
        return false;

    }

    @SuppressWarnings("ConstantConditions")
    @Nullable
    private static TextRange getFileHeader(ANTLRv4FileRoot file) {
        PsiElement first = file.getFirstChild();
        if (first instanceof PsiWhiteSpace) first = first.getNextSibling();
        PsiElement element = first;
        while (headerCandidate(element)) {
            element = element.getNextSibling();
            if (element instanceof PsiWhiteSpace) {
                element = element.getNextSibling();
            } else {
                break;
            }
        }
        if (element == null) return null;
        if (element.getPrevSibling() instanceof PsiWhiteSpace) element = element.getPrevSibling();
        if (element == null || element.equals(first)) return null;
        return new UnfairTextRange(first.getTextOffset(), element.getTextOffset());
    }

    @Override
    protected String getLanguagePlaceholderText(@NotNull ASTNode node, @NotNull TextRange range) {
        PsiElement element = SourceTreeToPsiMap.treeElementToPsi(node);

        if (element instanceof PsiComment) {
            return "//...";
        }
        return "...";
    }

    @SuppressWarnings("SimplifiableIfStatement")
    @Override
    protected boolean isRegionCollapsedByDefault(@NotNull ASTNode node) {
        final PsiElement element = SourceTreeToPsiMap.treeElementToPsi(node);

        ANTLRv4FoldingSettings settings = ANTLRv4FoldingSettings.getInstance();

        if (element instanceof ANTLRv4FileRoot) {
            return settings.isCollapseFileHeader();
        }
        if (node.getElementType() == DOC_COMMENT) {
            if (element == null) return false;
            PsiElement parent = element.getParent();

            if (parent instanceof ANTLRv4FileRoot) {
                PsiElement firstChild = parent.getFirstChild();
                if (firstChild instanceof PsiWhiteSpace) {
                    firstChild = firstChild.getNextSibling();
                }
                if (element.equals(firstChild)) {
                    return settings.isCollapseFileHeader();
                }
            }
            return settings.isCollapseDocComments();
        }
        if (element instanceof PsiComment) {
            return settings.isCollapseEndOfLineComments();
        }
        return false;

    }
}
