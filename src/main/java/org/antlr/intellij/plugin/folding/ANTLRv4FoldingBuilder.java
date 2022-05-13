package org.antlr.intellij.plugin.folding;

import com.intellij.lang.ASTNode;
import com.intellij.lang.folding.CustomFoldingBuilder;
import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.UnfairTextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.impl.source.SourceTreeToPsiMap;
import com.intellij.psi.impl.source.tree.TreeUtil;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.PsiTreeUtil;
import org.antlr.intellij.adaptor.lexer.RuleIElementType;
import org.antlr.intellij.adaptor.lexer.TokenIElementType;
import org.antlr.intellij.plugin.ANTLRv4FileRoot;
import org.antlr.intellij.plugin.ANTLRv4TokenTypes;
import org.antlr.intellij.plugin.parser.ANTLRv4Lexer;
import org.antlr.intellij.plugin.parser.ANTLRv4Parser;
import org.antlr.intellij.plugin.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by jason on 1/7/15.
 * parts copied from JavaFoldingBuilderBase
 *
 * @see com.intellij.codeInsight.folding.impl.JavaFoldingBuilderBase
 */
public class ANTLRv4FoldingBuilder extends CustomFoldingBuilder {

    private static final TokenIElementType DOC_COMMENT_TOKEN = ANTLRv4TokenTypes.getTokenElementType(ANTLRv4Lexer.DOC_COMMENT);
    private static final TokenIElementType BLOCK_COMMENT_TOKEN = ANTLRv4TokenTypes.getTokenElementType(ANTLRv4Lexer.BLOCK_COMMENT);
    private static final TokenIElementType LINE_COMMENT_TOKEN = ANTLRv4TokenTypes.getTokenElementType(ANTLRv4Lexer.LINE_COMMENT);

    private static final RuleIElementType OPTIONSSPEC = ANTLRv4TokenTypes.getRuleElementType(ANTLRv4Parser.RULE_optionsSpec);
    private static final TokenIElementType OPTIONS = ANTLRv4TokenTypes.getTokenElementType(ANTLRv4Lexer.OPTIONS);

    private static final RuleIElementType TOKENSSPEC = ANTLRv4TokenTypes.getRuleElementType(ANTLRv4Parser.RULE_tokensSpec);
    private static final TokenIElementType TOKENS = ANTLRv4TokenTypes.getTokenElementType(ANTLRv4Lexer.TOKENS);

    private static final TokenIElementType RBRACE = ANTLRv4TokenTypes.getTokenElementType(ANTLRv4Lexer.RBRACE);
    private static final TokenIElementType SEMICOLON = ANTLRv4TokenTypes.getTokenElementType(ANTLRv4Lexer.SEMI);

    private static final TokenSet RULE_BLOCKS = TokenSet.create(
            ANTLRv4TokenTypes.getRuleElementType(ANTLRv4Parser.RULE_lexerBlock),
            ANTLRv4TokenTypes.getRuleElementType(ANTLRv4Parser.RULE_ruleBlock)
    );


    @Override
    protected void buildLanguageFoldRegions(@NotNull List<FoldingDescriptor> descriptors,
                                            @NotNull PsiElement root,
                                            @NotNull Document document,
                                            boolean quick) {
        if (!(root instanceof ANTLRv4FileRoot)) return;

        addRuleRefFoldingDescriptors(descriptors, root);

        addActionFoldingDescriptors(descriptors, root);

        addHeaderFoldingDescriptor(descriptors, root, document);

        addCommentDescriptors(descriptors, root);

        addOptionsFoldingDescriptor(descriptors, root);

        addTokensFoldingDescriptor(descriptors, root);

        addModeFoldingDescriptors(descriptors, root);
    }

    private static void addTokensFoldingDescriptor(List<FoldingDescriptor> descriptors, PsiElement root) {
        PsiElement tokensSpec = MyPsiUtils.findFirstChildOfType(root, TOKENSSPEC);
        if (tokensSpec != null) {
            PsiElement tokens = tokensSpec.getFirstChild();
            if ( tokens.getNode().getElementType() == TOKENS ) {
                PsiElement rbrace = tokensSpec.getLastChild();
                if ( rbrace.getNode().getElementType()==RBRACE ) {
                    descriptors.add(new FoldingDescriptor(tokensSpec,
                                                          new TextRange(tokens.getTextRange().getEndOffset(), rbrace.getTextRange().getEndOffset())));
                }
            }
        }
    }

    private static void addOptionsFoldingDescriptor(List<FoldingDescriptor> descriptors, PsiElement root) {
        PsiElement optionsSpec = MyPsiUtils.findFirstChildOfType(root, OPTIONSSPEC);
        if (optionsSpec != null) {
            PsiElement options = optionsSpec.getFirstChild();
            if ( options.getNode().getElementType() == OPTIONS ) {
                PsiElement rbrace = optionsSpec.getLastChild();
                if ( rbrace.getNode().getElementType()==RBRACE ) {
                    descriptors.add(new FoldingDescriptor(optionsSpec,
                                                          new TextRange(options.getTextRange().getEndOffset(), rbrace.getTextRange().getEndOffset())));
                }
            }
        }
    }

    private static void addHeaderFoldingDescriptor(List<FoldingDescriptor> descriptors, PsiElement root, Document document) {
        TextRange range = getFileHeader(root);
        if (range != null && range.getLength() > 1 && document.getLineNumber(range.getEndOffset()) > document.getLineNumber(range.getStartOffset())) {
            descriptors.add(new FoldingDescriptor(root, range));
        }
    }

    private static void addCommentDescriptors(List<FoldingDescriptor> descriptors, PsiElement root) {
        Set<PsiElement> processedComments = new HashSet<>();
        for (PsiElement comment : MyPsiUtils.findChildrenOfType(root, ANTLRv4TokenTypes.COMMENTS)) {
            IElementType type = comment.getNode().getElementType();
            if (processedComments.contains(comment)) continue;
            if (type == DOC_COMMENT_TOKEN || type == BLOCK_COMMENT_TOKEN) {
                descriptors.add(new FoldingDescriptor(comment, comment.getTextRange()));
            }
            //addCommentFolds(comment, processedComments, descriptors);
        }

    }

    private static void addActionFoldingDescriptors(List<FoldingDescriptor> descriptors, PsiElement root) {
        for (AtAction atAction : PsiTreeUtil.findChildrenOfType(root, AtAction.class)) {
            PsiElement action = atAction.getLastChild();
            String actionText = action.getText();
            if ( actionText != null && actionText.contains("\n")) {
                descriptors.add(new FoldingDescriptor(atAction, action.getTextRange()));
            }
        }
    }

    private static void addRuleRefFoldingDescriptors(List<FoldingDescriptor> descriptors, PsiElement root) {
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
    }

    private static void addModeFoldingDescriptors(List<FoldingDescriptor> descriptors, PsiElement root) {
        for (ModeSpecNode specNode : PsiTreeUtil.findChildrenOfType(root, ModeSpecNode.class)) {
            PsiElement semi = MyPsiUtils.findFirstChildOfType(specNode, ANTLRv4TokenTypes.getTokenElementType(ANTLRv4Lexer.SEMI));

            if ( semi != null ) {
                TextRange foldingRange = TextRange.create(semi.getTextOffset(), specNode.getNode().getStartOffset() + specNode.getTextLength());
                descriptors.add(new FoldingDescriptor(specNode, foldingRange));
            }
        }
    }

    private static boolean isComment(PsiElement element) {
        IElementType type = element.getNode().getElementType();
        return ANTLRv4TokenTypes.COMMENTS.contains(type);
    }

    @SuppressWarnings("ConstantConditions")
    @Nullable
    private static TextRange getFileHeader(PsiElement file) {
        PsiElement first = file.getFirstChild();
        if (first instanceof PsiWhiteSpace) first = first.getNextSibling();
        PsiElement element = first;
        while (isComment(element)) {
            element = element.getNextSibling();
            if (element instanceof PsiWhiteSpace) {
                element = element.getNextSibling();
            }
            else {
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
        if (node.getElementType() == TOKENSSPEC) return settings.isCollapseTokens();

        if (element instanceof AtAction) return settings.isCollapseActions();

        if (element instanceof ANTLRv4FileRoot) {
            return settings.isCollapseFileHeader();
        }
        if (node.getElementType() == DOC_COMMENT_TOKEN) {

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
        if (isComment(element)) {
            return settings.isCollapseComments();
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
    /* Comment out for now since isCustomRegionElement() not in 13.x i guess.
    private static void addCommentFolds(@NotNull PsiElement comment, @NotNull Set<PsiElement> processedComments,
                                        @NotNull List<FoldingDescriptor> foldElements) {
        if (processedComments.contains(comment)) {
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
            if (elementType == LINE_COMMENT_TOKEN) {
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
    */

    private static String getPlaceholderText(PsiElement element) {

        if (element.getNode().getElementType() == LINE_COMMENT_TOKEN) {
            return "//...";
        }
        else if ( element instanceof ModeSpecNode ) {
            return ";...";
        }
        else if (element instanceof RuleSpecNode) {
            return ":...;";
        }
        else if (element instanceof AtAction) {
            return "{...}";
        }
        return "...";
    }
}
