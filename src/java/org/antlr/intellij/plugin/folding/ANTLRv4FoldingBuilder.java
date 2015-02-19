package org.antlr.intellij.plugin.folding;

import com.google.common.collect.PeekingIterator;
import com.intellij.lang.ASTNode;
import com.intellij.lang.folding.CustomFoldingBuilder;
import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.lang.folding.NamedFoldingDescriptor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.UnfairTextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.TokenType;
import com.intellij.psi.impl.source.SourceTreeToPsiMap;
import com.intellij.psi.impl.source.tree.TreeUtil;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.antlr.intellij.adaptor.lexer.RuleElementType;
import org.antlr.intellij.adaptor.lexer.TokenElementType;
import org.antlr.intellij.plugin.ANTLRv4FileRoot;
import org.antlr.intellij.plugin.parser.ANTLRv4Lexer;
import org.antlr.intellij.plugin.parser.ANTLRv4Parser;
import org.antlr.intellij.plugin.psi.AtAction;
import org.antlr.intellij.plugin.psi.GrammarElementRefNode;
import org.antlr.intellij.plugin.psi.GrammarSpecNode;
import org.antlr.intellij.plugin.psi.RuleSpecNode;
import org.antlr.intellij.plugin.psi.iter.ASTIterable;
import org.antlr.intellij.plugin.psi.iter.Tokens;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static org.antlr.intellij.plugin.ANTLRv4TokenTypes.getRuleElementType;
import static org.antlr.intellij.plugin.ANTLRv4TokenTypes.getTokenElementType;

/**
 * Created by jason on 1/7/15.
 * parts copied from JavaFoldingBuilderBase
 *
 * @see com.intellij.codeInsight.folding.impl.JavaFoldingBuilderBase
 */
public class ANTLRv4FoldingBuilder extends CustomFoldingBuilder {

    private static final TokenElementType DOC_COMMENT_TOKEN = getTokenElementType(ANTLRv4Lexer.DOC_COMMENT);
    private static final TokenElementType BLOCK_COMMENT_TOKEN = getTokenElementType(ANTLRv4Lexer.BLOCK_COMMENT);
    private static final TokenElementType LINE_COMMENT_TOKEN = getTokenElementType(ANTLRv4Lexer.LINE_COMMENT);

    private static final RuleElementType OPTIONSSPEC = getRuleElementType(ANTLRv4Parser.RULE_optionsSpec);
    private static final TokenElementType OPTIONS = getTokenElementType(ANTLRv4Lexer.OPTIONS);

    private static final RuleElementType TOKENSSPEC = getRuleElementType(ANTLRv4Parser.RULE_tokensSpec);
    private static final TokenElementType TOKENS = getTokenElementType(ANTLRv4Lexer.TOKENS);

    private static final TokenElementType RBRACE = getTokenElementType(ANTLRv4Lexer.RBRACE);
    private static final TokenElementType SEMICOLON = getTokenElementType(ANTLRv4Lexer.SEMI);
    private static final RuleElementType ACTION = getRuleElementType(ANTLRv4Parser.RULE_action);
    private static final TokenElementType ACTION_TOKEN = getTokenElementType(ANTLRv4Lexer.ACTION);


    private static final Tokens RULE_BLOCKS = Tokens.of(
            getRuleElementType(ANTLRv4Parser.RULE_lexerBlock),
            getRuleElementType(ANTLRv4Parser.RULE_ruleBlock)
    );

    static final TokenSet REFS = TokenSet.create(
            getTokenElementType(ANTLRv4Lexer.TOKEN_REF),
            getTokenElementType(ANTLRv4Lexer.RULE_REF)
    );
    static final   TokenSet SPECS = TokenSet.create(
            getRuleElementType(ANTLRv4Parser.RULE_parserRuleSpec),
            getRuleElementType(ANTLRv4Parser.RULE_lexerRule)
    );

    static final Tokens COMMENTS = Tokens.of(
            getTokenElementType(ANTLRv4Lexer.DOC_COMMENT),
            getTokenElementType(ANTLRv4Lexer.BLOCK_COMMENT),
            getTokenElementType(ANTLRv4Lexer.LINE_COMMENT)
    );

    @Override
    protected void buildLanguageFoldRegions(@NotNull List<FoldingDescriptor> descriptors,
                                            @NotNull PsiElement root,
                                            @NotNull Document document,
                                            boolean quick) {
        if (!(root instanceof ANTLRv4FileRoot)) return;
        ANTLRv4FileRoot file = (ANTLRv4FileRoot) root;
        GrammarSpecNode grammarSpec = file.getGrammarSpec();
        ASTNode grammarSpecNode = grammarSpec.getNode();
        ASTNode rootNode = root.getNode();
        final Set<ASTNode> processedComments = new HashSet<ASTNode>();


        for (ASTNode node : ASTIterable.directChildrenOf(grammarSpecNode).excludingWhitespace()) {
            IElementType nodeType = node.getElementType();
            if (getRuleElementType(ANTLRv4Parser.RULE_prequelConstruct) == nodeType) {
                ASTNode prequelChild = node.getFirstChildNode();
                IElementType prequelChildType = prequelChild.getElementType();
                if (prequelChildType == OPTIONSSPEC) {
                    addOptionsFoldingDescriptor(descriptors, prequelChild);
                } else if (prequelChildType == TOKENSSPEC) {
                    addTokensFoldingDescriptor(descriptors, prequelChild);
                } else if (prequelChildType == ACTION) {

                    ASTNode code = TreeUtil.findChildBackward(prequelChild, ACTION_TOKEN);
                    if (code == null) continue;
                    descriptors.add(new FoldingDescriptor(prequelChild, code.getTextRange()));

                }
            } else if (getRuleElementType(ANTLRv4Parser.RULE_rules) == nodeType) {
                handleRules(descriptors, document, node);
            } else if (getRuleElementType(ANTLRv4Parser.RULE_modeSpec) == nodeType) {
                handleModeSpec(descriptors, node);
            }
        }


        addHeaderFoldingDescriptor(descriptors, rootNode, document, processedComments);
        addCommentDescriptors(descriptors, rootNode, processedComments);

    }

    /*
    modeSpec
        :	MODE id SEMI lexerRule*
        ;
     */
    private static void handleModeSpec(List<FoldingDescriptor> descriptors, ASTNode modeSpec) {
        assert modeSpec.getElementType() == getRuleElementType(ANTLRv4Parser.RULE_modeSpec);

        for (ASTNode lexerRule : ASTIterable.directChildrenOf(modeSpec).filter(getRuleElementType(ANTLRv4Parser.RULE_lexerRule))) {
            ASTNode lexerRef = lexerRule.findChildByType(getTokenElementType(ANTLRv4Lexer.TOKEN_REF));
            if (lexerRef == null) continue;
            descriptors.add(new FoldingDescriptor(lexerRule, foldingRangeForRef(lexerRef)));

        }

    }

    /*
    rules
        :	ruleSpec*
        ;

    ruleSpec
        :	parserRuleSpec
        |	lexerRule
        ;

        parserRuleSpec
        :	DOC_COMMENT?
            ruleModifiers? RULE_REF ARG_ACTION?
            ruleReturns? throwsSpec? localsSpec?
            rulePrequel*
            COLON
                ruleBlock
            SEMI
            exceptionGroup
        ;

        lexerRule
        :	DOC_COMMENT? FRAGMENT?
            TOKEN_REF COLON lexerRuleBlock SEMI
        ;
     */
    private static void handleRules(List<FoldingDescriptor> descriptors, Document document, ASTNode rulesNode) {
        assert rulesNode.getElementType() == getRuleElementType(ANTLRv4Parser.RULE_rules);

        PeekingIterator<ASTNode> ruleSpecIterator = ASTIterable.directChildrenOf(rulesNode)
                .excludingWhitespaceAndComments()
                .peekingIterator();



        while (ruleSpecIterator.hasNext()) {

            ASTNode ruleSpec = ruleSpecIterator.next();


            ASTNode rule = ruleSpec.findChildByType(SPECS);
            assert rule != null;
            ASTNode ruleRef = rule.findChildByType(REFS);
            assert ruleRef != null;

            LinkedList<ASTNode> ruleRefsInGroup = new LinkedList<ASTNode>();
            ruleRefsInGroup.add(ruleRef);


            TextRange range = foldingRangeForRef(ruleRef);

            int endOfs = range.getEndOffset();
            int lastLine = document.getLineNumber(endOfs);

            while (ruleSpecIterator.hasNext()) {
                ASTNode nextSpec = ruleSpecIterator.peek();
                ASTNode nextRule = nextSpec.findChildByType(SPECS);
                if (nextRule == null) {
                    System.out.println("rule was null!");
                    break;
                }
                ASTNode nextRef = nextRule.findChildByType(REFS);
                assert nextRef != null;

                TextRange nextRange = foldingRangeForRef(nextRef);

                int nextSpecStartLine = document.getLineNumber(nextSpec.getStartOffset());
                if (lastLine + 1 == nextSpecStartLine) {
                    ruleSpecIterator.next();// consume nextSpec from iterator;
                    ruleRefsInGroup.add(nextRef);
                    endOfs = nextRange.getEndOffset();
                    lastLine = document.getLineNumber(endOfs);

                } else break;
            }


            if (ruleRefsInGroup.size() > 1) {
                TextRange groupRange = new TextRange(ruleSpec.getStartOffset(), endOfs);
                FoldingDescriptor descriptor = new NamedFoldingDescriptor(rule, groupRange, null, myMakeRuleGroupPlaceholderText(ruleRefsInGroup));
                descriptors.add(descriptor);
            } else {
                descriptors.add(new FoldingDescriptor(rule, range));

            }
        }

    }


    static TextRange foldingRangeForRef(ASTNode ref) {
        assert ref.getPsi() instanceof GrammarElementRefNode;
        ASTNode afterRef = ref.getTreeNext();
        assert afterRef != null;
        ASTNode semiColon = TreeUtil.findSibling(afterRef, SEMICOLON);
        assert semiColon != null;

        int startOfs = afterRef.getStartOffset();
        int endOffset = semiColon.getTextRange().getEndOffset();
        assert startOfs < endOffset;

        return new TextRange(startOfs, endOffset);
    }

    private static void addTokensFoldingDescriptor(List<FoldingDescriptor> descriptors, ASTNode tokensSpec) {
        if (tokensSpec != null) {
            assert tokensSpec.getElementType() == getRuleElementType(ANTLRv4Parser.RULE_tokensSpec);
            ASTNode tokens = tokensSpec.getFirstChildNode();
            assert tokens.getElementType() == TOKENS;
            ASTNode rbrace = tokensSpec.getLastChildNode();
            assert rbrace.getElementType() == RBRACE;
            //the last char of tokens should be the left brace, so subtract 1 from the end offset.
            descriptors.add(new FoldingDescriptor(tokensSpec,
                    new TextRange(tokens.getTextRange().getEndOffset() - 1, rbrace.getTextRange().getEndOffset())));
        }
    }

    private static void addOptionsFoldingDescriptor(List<FoldingDescriptor> descriptors, ASTNode optionsSpec) {
        if (optionsSpec != null) {
            assert optionsSpec.getElementType() == getRuleElementType(ANTLRv4Parser.RULE_optionsSpec);
            ASTNode options = optionsSpec.getFirstChildNode();
            assert options.getElementType() == OPTIONS;
            ASTNode rbrace = optionsSpec.getLastChildNode();
            assert rbrace.getElementType() == RBRACE;
            descriptors.add(new FoldingDescriptor(optionsSpec,
                    new TextRange(options.getTextRange().getEndOffset(), rbrace.getTextRange().getEndOffset())));
        }
    }

    private static void addHeaderFoldingDescriptor(List<FoldingDescriptor> descriptors, ASTNode root, Document document, Set<ASTNode> processedComments) {
        TextRange range = getFileHeader(root, processedComments);
        if (range != null && range.getLength() > 1 && document.getLineNumber(range.getEndOffset()) > document.getLineNumber(range.getStartOffset())) {
            descriptors.add(new FoldingDescriptor(root, range));

        }
    }


    private static String myMakeRuleGroupPlaceholderText(Iterable<ASTNode> refs) {
        StringBuilder sb = new StringBuilder();
        for (Iterator<ASTNode> iterator = refs.iterator(); iterator.hasNext(); ) {
            sb.append(iterator.next().getChars());
            if (iterator.hasNext()) sb.append(", ");
        }
        return sb.toString();
    }

    private static boolean isCommentButNotDocComment(ASTNode e) {
        IElementType type = e.getElementType();
        return type == LINE_COMMENT_TOKEN || type == BLOCK_COMMENT_TOKEN;

    }

    @SuppressWarnings("ConstantConditions")
    @Nullable
    private static TextRange getFileHeader(ASTNode file, Set<ASTNode> processedComments) {
        ASTNode first = file.getFirstChildNode();
        if (first instanceof PsiWhiteSpace) first = first.getTreeNext();
        ASTNode element = first;
        while (isCommentButNotDocComment(element)) {
            processedComments.add(element);
            element = element.getTreeNext();
            if (element instanceof PsiWhiteSpace) {
                element = element.getTreeNext();
            } else {
                break;
            }
        }
        if (element == null) return null;
        if (element.getTreePrev() instanceof PsiWhiteSpace) element = element.getTreePrev();
        if (element == null || element.equals(first)) return null;
        return new UnfairTextRange(first.getStartOffset(), element.getStartOffset());
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
        if (COMMENTS.contains(node.getElementType())) {
            return settings.isCollapseComments();
        }

        return false;

    }


    private static void addCommentDescriptors(List<FoldingDescriptor> descriptors, ASTNode root, Set<ASTNode> processedComments) {
        for (ASTNode comment : ASTIterable.depthFirst(root).filter(COMMENTS)) {
            if (processedComments.contains(comment)) continue;
            IElementType type = comment.getElementType();
            if (type == DOC_COMMENT_TOKEN || type == BLOCK_COMMENT_TOKEN) {
                processedComments.add(comment);
                descriptors.add(new FoldingDescriptor(comment, comment.getTextRange()));
            } else {
                addCommentFolds(comment, processedComments, descriptors);

            }
        }


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
    private static void addCommentFolds(@NotNull ASTNode comment, @NotNull Set<ASTNode> processedComments,
                                        @NotNull List<FoldingDescriptor> foldElements) {

        if (processedComments.contains(comment)) {
            return;
        }


        ASTNode end = null;
        boolean containsCustomRegionMarker = isCustomRegionElement(comment.getPsi());

        for (ASTNode current = comment.getTreeNext(); current != null; current = current.getTreeNext()) {

            IElementType elementType = current.getElementType();
            if (elementType == LINE_COMMENT_TOKEN) {
                end = current;
                // We don't want to process, say, the second comment in case of three subsequent comments when it's being examined
                // during all elements traversal. I.e. we expect to start from the first comment and grab as many subsequent
                // comments as possible during the single iteration.
                processedComments.add(current);
                containsCustomRegionMarker |= isCustomRegionElement(current.getPsi());
                continue;
            }
            if (elementType == TokenType.WHITE_SPACE) {
                continue;
            }
            break;
        }

        if (end != null && !containsCustomRegionMarker) {
            TextRange range = new TextRange(comment.getStartOffset(), end.getTextRange().getEndOffset());
            foldElements.add(new FoldingDescriptor(comment, range));
        } else {
            foldElements.add(new FoldingDescriptor(comment, comment.getTextRange()));
        }
    }


    private static String getPlaceholderText(PsiElement element) {

        if (element.getNode().getElementType() == LINE_COMMENT_TOKEN) {
            return "//...";
        } else if (element instanceof RuleSpecNode) {
            return ":...;";
        } else if (element instanceof AtAction) {
            return "{...}";
        }
        return "...";
    }
}
