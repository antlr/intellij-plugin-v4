package org.antlr.intellij.plugin.folding;

import com.intellij.lang.ASTNode;
import com.intellij.lang.folding.CustomFoldingBuilder;
import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.lang.folding.NamedFoldingDescriptor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.FoldingGroup;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.UnfairTextRange;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.SourceTreeToPsiMap;
import com.intellij.psi.impl.source.tree.TreeUtil;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilCore;
import org.antlr.intellij.adaptor.lexer.ElementTypeFactory;
import org.antlr.intellij.plugin.ANTLRv4FileRoot;
import org.antlr.intellij.plugin.ANTLRv4Language;
import org.antlr.intellij.plugin.ANTLRv4TokenTypes;
import org.antlr.intellij.plugin.parser.ANTLRv4Lexer;
import org.antlr.intellij.plugin.parser.ANTLRv4Parser;
import org.antlr.intellij.plugin.psi.GrammarElementRefNode;
import org.antlr.intellij.plugin.psi.LexerRuleRefNode;
import org.antlr.intellij.plugin.psi.RuleSpecNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Created by jason on 1/7/15.
 * mostly copied from JavaFoldingBuilderBase
 *
 * @see com.intellij.codeInsight.folding.impl.JavaFoldingBuilderBase
 */
public class ANTLRv4FoldingBuilder extends CustomFoldingBuilder {
    private static final Logger LOG = Logger.getInstance(ANTLRv4FoldingBuilder.class.getName());

    private static final IElementType DOC_COMMENT = ANTLRv4TokenTypes.TOKEN_ELEMENT_TYPES.get(ANTLRv4Lexer.DOC_COMMENT);
    private static final IElementType BLOCK_COMMENT = ANTLRv4TokenTypes.TOKEN_ELEMENT_TYPES.get(ANTLRv4Lexer.BLOCK_COMMENT);

    private static final IElementType SEMICOLON = ANTLRv4TokenTypes.TOKEN_ELEMENT_TYPES.get(ANTLRv4Lexer.SEMI);

    private static final TokenSet RULE_BLOCKS = ElementTypeFactory.createRuleSet(
            ANTLRv4Language.INSTANCE,
            Arrays.asList(ANTLRv4Lexer.tokenNames),
            ANTLRv4Parser.RULE_lexerBlock, ANTLRv4Parser.RULE_ruleBlock);

    @Override
    protected void buildLanguageFoldRegions(@NotNull List<FoldingDescriptor> descriptors,
                                            @NotNull PsiElement root,
                                            @NotNull Document document,
                                            boolean quick) {
        if (!(root instanceof ANTLRv4FileRoot)) {
            return;
        }
        ANTLRv4FileRoot file = (ANTLRv4FileRoot) root;


        for (RuleSpecNode specNode : PsiTreeUtil.findChildrenOfType(root, RuleSpecNode.class)) {
            GrammarElementRefNode refNode = PsiTreeUtil.findChildOfAnyType(specNode, GrammarElementRefNode.class);
            if (refNode == null) continue;
            PsiElement nextSibling = refNode.getNextSibling();
            if (nextSibling == null) continue;
            int startOffset = nextSibling.getTextOffset();

            ASTNode backward = TreeUtil.findChildBackward(specNode.getNode(), SEMICOLON);
            if (backward == null) continue;
            int endOffset = backward.getTextRange().getEndOffset();
            if (startOffset >= endOffset) continue;

            descriptors.add(new FoldingDescriptor(specNode, new TextRange(startOffset, endOffset)));


        }


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

    @Nullable
    public TextRange getRangeToFold(PsiElement element) {
        return null;
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
        return getPlaceholderText(SourceTreeToPsiMap.treeElementToPsi(node));
    }

    @SuppressWarnings("SimplifiableIfStatement")
    @Override
    protected boolean isRegionCollapsedByDefault(@NotNull ASTNode node) {
        final PsiElement element = SourceTreeToPsiMap.treeElementToPsi(node);
        if (element == null) return false;

        ANTLRv4FoldingSettings settings = ANTLRv4FoldingSettings.getInstance();

        if (RULE_BLOCKS.contains(node.getElementType())) return settings.isCollapseRuleBlocks();


        if (element instanceof ANTLRv4FileRoot) {
            return settings.isCollapseFileHeader();
        }
        if (node.getElementType() == DOC_COMMENT) {

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

    /**
     * We want to allow to fold subsequent single line comments like
     * <pre>
     *     // this is comment line 1
     *     // this is comment line 2
     * </pre>
     *
     * @param comment           comment to check
     * @param processedComments set that contains already processed elements. It is necessary because we process all elements of
     *                          the PSI tree, hence, this method may be called for both comments from the example above. However,
     *                          we want to create fold region during the first comment processing, put second comment to it and
     *                          skip processing when current method is called for the second element
     * @param foldElements      fold descriptors holder to store newly created descriptor (if any)
     */
    private static void addCommentFolds(@NotNull PsiComment comment, @NotNull Set<PsiElement> processedComments,
                                        @NotNull List<FoldingDescriptor> foldElements) {
        if (processedComments.contains(comment) || comment.getTokenType() != JavaTokenType.END_OF_LINE_COMMENT) {
            return;
        }

        PsiElement end = null;
        boolean containsCustomRegionMarker = isCustomRegionElement(comment);
        for (PsiElement current = comment.getNextSibling(); current != null; current = current.getNextSibling()) {
            ASTNode node = current.getNode();
            if (node == null) {
                break;
            }
            IElementType elementType = node.getElementType();
            if (elementType == JavaTokenType.END_OF_LINE_COMMENT) {
                end = current;
                // We don't want to process, say, the second comment in case of three subsequent comments when it's being examined
                // during all elements traversal. I.e. we expect to start from the first comment and grab as many subsequent
                // comments as possible during the single iteration.
                processedComments.add(current);
                containsCustomRegionMarker |= isCustomRegionElement(current);
                continue;
            }
            if (elementType == TokenType.WHITE_SPACE) {
                continue;
            }
            break;
        }

        if (end != null && !containsCustomRegionMarker) {
            foldElements.add(
                    new FoldingDescriptor(comment, new TextRange(comment.getTextRange().getStartOffset(), end.getTextRange().getEndOffset()))
            );
        }
    }

    private boolean addToFold(List<FoldingDescriptor> list, PsiElement elementToFold, Document document, boolean allowOneLiners) {
        PsiUtilCore.ensureValid(elementToFold);
        TextRange range = getRangeToFold(elementToFold);
        if (range == null) return false;
        return addFoldRegion(list, elementToFold, document, allowOneLiners, range);
    }

    private static boolean addFoldRegion(final List<FoldingDescriptor> list, final PsiElement elementToFold, final Document document,
                                         final boolean allowOneLiners,
                                         final TextRange range) {
        final TextRange fileRange = elementToFold.getContainingFile().getTextRange();
        if (range.equals(fileRange)) return false;

        LOG.assertTrue(range.getStartOffset() >= 0 && range.getEndOffset() <= fileRange.getEndOffset());
        // PSI element text ranges may be invalid because of reparse exception (see, for example, IDEA-10617)
        if (range.getStartOffset() < 0 || range.getEndOffset() > fileRange.getEndOffset()) {
            return false;
        }
        if (!allowOneLiners) {
            int startLine = document.getLineNumber(range.getStartOffset());
            int endLine = document.getLineNumber(range.getEndOffset() - 1);
            if (startLine < endLine && range.getLength() > 1) {
                list.add(new FoldingDescriptor(elementToFold, range));
                return true;
            }
            return false;
        } else {
            if (range.getLength() > getPlaceholderText(elementToFold).length()) {
                list.add(new FoldingDescriptor(elementToFold, range));
                return true;
            }
            return false;
        }
    }

    private static String getPlaceholderText(PsiElement element) {
        if (element instanceof PsiComment) {
            return "//...";
        } else if (element instanceof RuleSpecNode) {
            return ":...";
        }
        return "...";
    }
}
