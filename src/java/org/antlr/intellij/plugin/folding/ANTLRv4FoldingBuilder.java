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
import org.antlr.intellij.adaptor.lexer.RuleElementType;
import org.antlr.intellij.adaptor.lexer.TokenElementType;
import org.antlr.intellij.plugin.ANTLRv4FileRoot;
import org.antlr.intellij.plugin.ANTLRv4TokenTypes;
import org.antlr.intellij.plugin.parser.ANTLRv4Lexer;
import org.antlr.intellij.plugin.parser.ANTLRv4Parser;
import org.antlr.intellij.plugin.psi.AtAction;
import org.antlr.intellij.plugin.psi.GrammarElementRefNode;
import org.antlr.intellij.plugin.psi.GrammarSpecNode;
import org.antlr.intellij.plugin.psi.RuleSpecNode;
import org.antlr.intellij.plugin.psi.iter.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static org.antlr.intellij.plugin.psi.iter.CommonFilters.acceptingNodesWithElementType;

/**
 * Created by jason on 1/7/15.
 * parts copied from JavaFoldingBuilderBase
 *
 * @see com.intellij.codeInsight.folding.impl.JavaFoldingBuilderBase
 */
public class ANTLRv4FoldingBuilder extends CustomFoldingBuilder {

    private static final TokenElementType DOC_COMMENT_TOKEN = ANTLRv4TokenTypes.getTokenElementType(ANTLRv4Lexer.DOC_COMMENT);
    private static final TokenElementType BLOCK_COMMENT_TOKEN = ANTLRv4TokenTypes.getTokenElementType(ANTLRv4Lexer.BLOCK_COMMENT);
    private static final TokenElementType LINE_COMMENT_TOKEN = ANTLRv4TokenTypes.getTokenElementType(ANTLRv4Lexer.LINE_COMMENT);

    private static final RuleElementType OPTIONSSPEC = ANTLRv4TokenTypes.getRuleElementType(ANTLRv4Parser.RULE_optionsSpec);
    private static final TokenElementType OPTIONS = ANTLRv4TokenTypes.getTokenElementType(ANTLRv4Lexer.OPTIONS);

    private static final RuleElementType TOKENSSPEC = ANTLRv4TokenTypes.getRuleElementType(ANTLRv4Parser.RULE_tokensSpec);
    private static final TokenElementType TOKENS = ANTLRv4TokenTypes.getTokenElementType(ANTLRv4Lexer.TOKENS);

    private static final TokenElementType RBRACE = ANTLRv4TokenTypes.getTokenElementType(ANTLRv4Lexer.RBRACE);
    private static final TokenElementType SEMICOLON = ANTLRv4TokenTypes.getTokenElementType(ANTLRv4Lexer.SEMI);
    private static final TokenElementType COLON = ANTLRv4TokenTypes.getTokenElementType(ANTLRv4Lexer.COLON);
    private static final RuleElementType ACTION = ANTLRv4TokenTypes.getRuleElementType(ANTLRv4Parser.RULE_action);
    private static final TokenElementType ACTION_TOKEN = ANTLRv4TokenTypes.getTokenElementType(ANTLRv4Lexer.ACTION);
    private static final Tokens RULE_BLOCKS = Tokens.of(
            ANTLRv4TokenTypes.getRuleElementType(ANTLRv4Parser.RULE_lexerBlock),
            ANTLRv4TokenTypes.getRuleElementType(ANTLRv4Parser.RULE_ruleBlock)
    );
    private static final Tokens RULE_REFS = Tokens.of(
            ANTLRv4TokenTypes.getTokenElementType(ANTLRv4Lexer.TOKEN_REF),
            ANTLRv4TokenTypes.getTokenElementType(ANTLRv4Lexer.RULE_REF)
    );

    private static final Tokens RULESPECS = Tokens.of(
            ANTLRv4TokenTypes.getRuleElementType(ANTLRv4Parser.RULE_parserRuleSpec),
            ANTLRv4TokenTypes.getRuleElementType(ANTLRv4Parser.RULE_lexerRule)
    );


    @Override
    protected void buildLanguageFoldRegions(@NotNull List<FoldingDescriptor> descriptors,
                                            @NotNull PsiElement root,
                                            @NotNull Document document,
                                            boolean quick) {
        if (!(root instanceof ANTLRv4FileRoot)) return;
        ANTLRv4FileRoot file = (ANTLRv4FileRoot) root;
        GrammarSpecNode grammarSpec = file.getGrammarSpec();
        ASTNode rootNode = root.getNode();
        final Set<ASTNode> processedComments = new HashSet<ASTNode>();


        addHeaderFoldingDescriptor(descriptors, rootNode, document, processedComments);
        addRuleSpecFoldingDescriptors(descriptors, grammarSpec, document);

        singlePass(rootNode, descriptors, processedComments);

        // addCommentDescriptors(descriptors, rootNode, processedComments);

        //  if (grammarSpec == null) return;


        //  addActionFoldingDescriptors(descriptors, grammarSpec);

        // addOptionsFoldingDescriptor(descriptors, grammarSpecNode);

        // addTokensFoldingDescriptor(descriptors, grammarSpecNode);


    }

    static void singlePass(ASTNode root, List<FoldingDescriptor> descriptors, Set<ASTNode> processedComments) {
        final FoldCtx ctx = new FoldCtx(descriptors, processedComments);
        for (ASTNode node : ASTIterable.depthFirst(root).excludingWhitespace()) {
            Iterator<Folder> folderIterator = ctx.folderIterator();
            while (folderIterator.hasNext()) {
                Folder folder = folderIterator.next();
                if (folder.predicate.acceptNode(node)) folder.fold(node, ctx);
            }

        }

    }


    static class FoldCtx {
        final List<FoldingDescriptor> descriptors;
        final Set<ASTNode> processedComments;
        Iterator<Folder> folderIterator;
        private final Set<Folder> folders = EnumSet.allOf(Folder.class);

        Iterator<Folder> folderIterator() {
            return this.folderIterator = folders.iterator();
        }

        void removeThisFoler() {
            folderIterator.remove();
        }

        FoldCtx(List<FoldingDescriptor> descriptors, Set<ASTNode> processedComments) {
            this.descriptors = descriptors;
            this.processedComments = processedComments;
        }
    }

    enum Folder {

        Options(acceptingNodesWithElementType(OPTIONSSPEC)) {
            @Override
            void fold(ASTNode node, FoldCtx ctx) {


                ASTNode options = node.getFirstChildNode();
                assert options.getElementType() == ANTLRv4FoldingBuilder.OPTIONS;
                ASTNode rbrace = node.getLastChildNode();
                assert rbrace.getElementType() == ANTLRv4FoldingBuilder.RBRACE;

                ctx.descriptors.add(new FoldingDescriptor(options,
                        new TextRange(options.getTextRange().getEndOffset(), rbrace.getTextRange().getEndOffset())));
                ctx.removeThisFoler();
            }
        },
        Tokens(acceptingNodesWithElementType(TOKENSSPEC)) {
            @Override
            void fold(ASTNode node, FoldCtx ctx) {


                ASTNode tokens = node.getFirstChildNode();
                assert tokens.getElementType() == TOKENS;
                ASTNode rbrace = node.getLastChildNode();
                assert rbrace.getElementType() == RBRACE;
                //the last char of tokens should be the left brace, so subtract 1 from the end offset.
                ctx.descriptors.add(new FoldingDescriptor(node,
                        new TextRange(tokens.getTextRange().getEndOffset() - 1, rbrace.getTextRange().getEndOffset())));
                ctx.removeThisFoler();

            }
        },
        Comments(COMMENTS) {
            @Override
            void fold(ASTNode comment, FoldCtx ctx) {
                if (ctx.processedComments.contains(comment)) return;
                IElementType type = comment.getElementType();
                if (type == DOC_COMMENT_TOKEN || type == BLOCK_COMMENT_TOKEN) {
                    ctx.processedComments.add(comment);
                    ctx.descriptors.add(new FoldingDescriptor(comment, comment.getTextRange()));
                } else {
                    addCommentFolds(comment, ctx.processedComments, ctx.descriptors);

                }
            }

        },
        Actions(acceptingNodesWithElementType(ACTION)) {
            @Override
            void fold(ASTNode node, FoldCtx ctx) {
                ASTNode code = TreeUtil.findChildBackward(node, ACTION_TOKEN);
                ctx.descriptors.add(new FoldingDescriptor(node, code.getTextRange()));

            }
        };
        //        Rules(RULESPECS) {
//            @Override
//            void fold(ASTNode node, FoldCtx ctx) {
//
//                List<RuleSpecNode> rulesInGroup = new LinkedList<RuleSpecNode>();
//
//                TextRange ruleRange = rangeForRule(node);
//
//                for (ASTNode next = node.getTreeNext(); node != null; next = next.getTreeNext()) {
//
//                }
//
//
//            }
//        };
        final ASTFilter predicate;

        Folder(ASTFilter predicate) {
            this.predicate = predicate;
        }

        abstract void fold(ASTNode node, FoldCtx ctx);

    }

    private static void addTokensFoldingDescriptor(List<FoldingDescriptor> descriptors, ASTNode root) {
        ASTNode tokensSpec = ASTIterable.depthFirst(root).filter(TOKENSSPEC).first();
        if (tokensSpec != null) {
            ASTNode tokens = tokensSpec.getFirstChildNode();
            assert tokens.getElementType() == TOKENS;
            ASTNode rbrace = tokensSpec.getLastChildNode();
            assert rbrace.getElementType() == RBRACE;
            //the last char of tokens should be the left brace, so subtract 1 from the end offset.
            descriptors.add(new FoldingDescriptor(tokensSpec,
                    new TextRange(tokens.getTextRange().getEndOffset() - 1, rbrace.getTextRange().getEndOffset())));
        }
    }

    private static void addOptionsFoldingDescriptor(List<FoldingDescriptor> descriptors, ASTNode root) {
        ASTNode optionsSpec = ASTIterable.depthFirst(root).filter(OPTIONSSPEC).first();
        if (optionsSpec != null) {
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

    private static void addActionFoldingDescriptors(List<FoldingDescriptor> descriptors, PsiElement root) {
        for (AtAction atAction : PsiIterable.depthFirst(root).filter(AtAction.class)) {
            PsiElement action = atAction.getLastChild();
            descriptors.add(new FoldingDescriptor(atAction, action.getTextRange()));
        }
    }


    private static void addRuleSpecFoldingDescriptors(List<FoldingDescriptor> descriptors, GrammarSpecNode grammarSpec, Document document) {
        PeekingIterator<RuleSpecNode> iterator = PsiIterable.depthFirst(grammarSpec).filter(RuleSpecNode.class).peekingIterator();

        List<RuleSpecNode> rulesInGroup = new LinkedList<RuleSpecNode>();

        while (iterator.hasNext()) {
            rulesInGroup.clear();
            RuleSpecNode ruleSpec = iterator.next();

            TextRange ruleBodyRange = rangeForRuleSpec(ruleSpec.getNode());
            if (ruleBodyRange == TextRange.EMPTY_RANGE) continue;
            int endOfs = ruleBodyRange.getEndOffset();

            int lastLine = document.getLineNumber(endOfs);
            int count = 1;


            while (iterator.hasNext()) {
                RuleSpecNode candidate = iterator.peek();
                TextRange nextRange = rangeForRuleSpec(candidate.getNode());
                if (nextRange == TextRange.EMPTY_RANGE) break;

                if (lastLine + 1 == document.getLineNumber(candidate.getTextRange().getStartOffset())) {
                    rulesInGroup.add(iterator.next());//consume next

                    endOfs = nextRange.getEndOffset();
                    lastLine = document.getLineNumber(endOfs);
                    count++;
                } else break;
            }

            if (count > 1) {
                rulesInGroup.add(0, ruleSpec);
                TextRange range = new TextRange(ruleSpec.getTextRange().getStartOffset(), endOfs);
                FoldingDescriptor descriptor = new NamedFoldingDescriptor(ruleSpec.getNode(), range, null, makeRuleGroupPlaceholderText(rulesInGroup));
                descriptors.add(descriptor);
            } else addRule(descriptors, ruleSpec.getNode());

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

    private static String makeRuleGroupPlaceholderText(List<RuleSpecNode> rest) {
        StringBuilder sb = new StringBuilder();
        for (Iterator<RuleSpecNode> iterator = rest.iterator(); iterator.hasNext(); ) {
            sb.append(iterator.next().getName());
            if (iterator.hasNext()) sb.append(", ");
        }
        return sb.toString();
    }

    private static void addRule(List<FoldingDescriptor> descriptors, ASTNode ruleSpec) {
        TextRange range = rangeForRuleSpec(ruleSpec);
        if (range != TextRange.EMPTY_RANGE) {
            descriptors.add(new FoldingDescriptor(ruleSpec, range));
        }
    }

    /*
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
     */
    @SuppressWarnings("unchecked")
    private static TextRange rangeForRuleSpec(ASTNode specNode) {

        ASTNode myRef = MyTreeUtil.findChild(specNode, RULE_REFS);
        assert myRef != null;
        assert myRef.getPsi() instanceof GrammarElementRefNode;

        if (myRef == null) return TextRange.EMPTY_RANGE;
        ASTNode nextSiblingNode = myRef.getTreeNext();
        assert nextSiblingNode != null;
        if (nextSiblingNode == null) return TextRange.EMPTY_RANGE;
        int startOffset = nextSiblingNode.getStartOffset();

        ASTNode semiColon = TreeUtil.findChildBackward(specNode, SEMICOLON);
        assert semiColon != null;
        if (semiColon == null) return TextRange.EMPTY_RANGE;
        int endOffset = semiColon.getTextRange().getEndOffset();
        if (startOffset >= endOffset) {
            assert false;
            return TextRange.EMPTY_RANGE;
        }
        return new TextRange(startOffset, endOffset);
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

    static final Tokens COMMENTS = Tokens.of(
            ANTLRv4TokenTypes.getTokenElementType(ANTLRv4Lexer.DOC_COMMENT),
            ANTLRv4TokenTypes.getTokenElementType(ANTLRv4Lexer.BLOCK_COMMENT),
            ANTLRv4TokenTypes.getTokenElementType(ANTLRv4Lexer.LINE_COMMENT)
    );

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
