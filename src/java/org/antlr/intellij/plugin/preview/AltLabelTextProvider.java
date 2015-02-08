package org.antlr.intellij.plugin.preview;

import org.antlr.v4.analysis.LeftRecursiveRuleAltInfo;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.tree.Tree;
import org.antlr.v4.runtime.tree.gui.TreeTextProvider;
import org.antlr.v4.tool.Alternative;
import org.antlr.v4.tool.Grammar;
import org.antlr.v4.tool.LeftRecursiveRule;
import org.antlr.v4.tool.Rule;
import org.antlr.v4.tool.ast.AltAST;
import org.antlr.v4.tool.ast.GrammarAST;

import java.util.*;

/**
 * Created by jason on 2/7/15.
 */
public class AltLabelTextProvider implements TreeTextProvider {
    final Map<Token, Integer> tokenStates;
    final BitSet rulesWithAlts;
    final Grammar grammar;
    final Map<Integer, String> state2Name = new HashMap<Integer, String>();

    public AltLabelTextProvider(Map<Token, Integer> tokenStates, Grammar grammar) {
        this.tokenStates = tokenStates;
        this.grammar = grammar;

        List<Rule> rules = grammar.indexToRule;

        rulesWithAlts = new BitSet(rules.size());

        for (int i = 0; i < rules.size(); i++) {
            Rule rule = rules.get(i);

            boolean hasAlts = handleRule(rule);
            if (rule instanceof LeftRecursiveRule) {
                hasAlts |= handleLRRule((LeftRecursiveRule) rule);
            }
            rulesWithAlts.set(i, hasAlts);

        }

    }

    private boolean handleRule(Rule rule) {
        for (int i = 1; i <= rule.numberOfAlts; i++) {
            Alternative alt = rule.alt[i];
            AltAST altAST = alt.ast;

            GrammarAST altLabel = altAST.altLabel;
            if (altLabel == null) return false;

            state2Name.put(getState(altAST), altLabel.getText());

        }
        return true;
    }

    private boolean handleLRRule(LeftRecursiveRule lrRule) {

        List<LeftRecursiveRuleAltInfo> primaryAlts = lrRule.recPrimaryAlts == null ? Collections.<LeftRecursiveRuleAltInfo>emptyList() : lrRule.recPrimaryAlts;

        for (LeftRecursiveRuleAltInfo altInfo : primaryAlts) {
            if (altInfo.altLabel == null) {
                return false;
            } else {
                String labelName = altInfo.altLabel;
                int state = getState(altInfo.originalAltAST);
                state2Name.put(state, labelName);
            }
        }

        Map<Integer, LeftRecursiveRuleAltInfo> opAlts = lrRule.recOpAlts == null ? Collections.<Integer, LeftRecursiveRuleAltInfo>emptyMap() : lrRule.recOpAlts;

        for (Map.Entry<Integer, LeftRecursiveRuleAltInfo> entry : opAlts.entrySet()) {
            LeftRecursiveRuleAltInfo altInfo = entry.getValue();
            if (altInfo.altLabel == null) {
                return false;
            } else {
                String labelName = altInfo.altLabel;
                int state = getState(altInfo.originalAltAST);
                state2Name.put(state, labelName);
            }

        }
        return true;
    }

    private static int getState(AltAST alt) {
        if (alt.leftRecursiveAltInfo != null) {
            alt = alt.leftRecursiveAltInfo.altAST;
        }
        GrammarAST lastChild = (GrammarAST) alt.getChild(alt.getChildCount() - 1);
        return lastChild.atnState.stateNumber;
    }


    public boolean hasLabeledAlts(int ruleIndex) {
        return rulesWithAlts.get(ruleIndex);
    }


    public String getAltName(RuleContext ruleNode) {
        ParseTree lastChild = ruleNode.getChild(ruleNode.getChildCount() - 1);
        Integer state = null;

        if (ruleNode instanceof ParserRuleContext && ((ParserRuleContext) ruleNode).exception != null) {
            //EXCLAMATION QUESTION MARK
            // Unicode: U+2049 U+FE0F, UTF-8: E2 81 89 EF B8 8F
            return "⁉️";

            /*
           NO ENTRY SIGN
           Unicode: U+1F6AB (U+D83D U+DEAB), UTF-8: F0 9F 9A AB

            ‽
            INTERROBANG
            Unicode: U+203D, UTF-8: E2 80 BD

            ☕︎
            HOT BEVERAGE
            Unicode: U+2615 U+FE0E, UTF-8: E2 98 95 EF B8 8E

            ☭
            HAMMER AND SICKLE
            Unicode: U+262D, UTF-8: E2 98 AD
             */
        }
        if (lastChild instanceof RuleContext) {
            state = ((RuleContext) lastChild).invokingState;
        } else if (lastChild instanceof TerminalNode) {
            state = tokenStates.get(((TerminalNode) lastChild).getSymbol());
        }
        assert state != null;
        String name = state2Name.get(state);
        assert name != null;
        return name;


    }

    public String getDisplayName(RuleContext node) {
        if (!hasLabeledAlts(node.getRuleIndex())) {
            return grammar.getRuleNames()[node.getRuleIndex()];
        } else {

            return getAltName(node);
        }
    }

    /**
     *
     *@see org.antlr.v4.runtime.tree.Trees#getNodeText(org.antlr.v4.runtime.tree.Tree, java.util.List)
     */

    @Override
    public String getText(Tree node) {

        if (node instanceof RuleContext) {
            return getDisplayName((RuleContext) node);
        }
        if (node instanceof ErrorNode) {
            return node.toString();
        }
        if (node instanceof TerminalNode) {
            Token symbol = ((TerminalNode) node).getSymbol();
            if (symbol != null) {
                return symbol.getText();
            }
        }


        Object payload = node.getPayload();
        if (payload instanceof Token) {
            return ((Token) payload).getText();
        }
        return node.getPayload().toString();

    }


}
