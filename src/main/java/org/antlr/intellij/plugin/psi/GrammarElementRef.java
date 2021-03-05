package org.antlr.intellij.plugin.psi;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.antlr.intellij.plugin.ANTLRv4TokenTypes;
import org.antlr.intellij.plugin.parser.ANTLRv4Lexer;
import org.antlr.intellij.plugin.resolve.ImportResolver;
import org.antlr.intellij.plugin.resolve.TokenVocabResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * A reference to a grammar element (parser rule, lexer rule or lexical mode).
 */
public class GrammarElementRef extends PsiReferenceBase<GrammarElementRefNode> {

	private final String ruleName;

	public GrammarElementRef(GrammarElementRefNode idNode, String ruleName) {
		super(idNode, new TextRange(0, ruleName.length()));
		this.ruleName = ruleName;
	}

	/**
	 * Using for completion. Returns list of rules and tokens; the prefix
	 * of current element is used as filter by IDEA later.
	 */
	@NotNull
	@Override
	public Object[] getVariants() {
		RulesNode rules = PsiTreeUtil.getContextOfType(myElement, RulesNode.class);
		// find all rule defs (token, parser)
		Collection<? extends RuleSpecNode> ruleSpecNodes =
				PsiTreeUtil.findChildrenOfAnyType(rules, ParserRuleSpecNode.class, LexerRuleSpecNode.class);

		return ruleSpecNodes.toArray();
	}

	/**
	 * Called upon jump to def for this rule ref
	 */
	@Nullable
	@Override
	public PsiElement resolve() {
		PsiFile tokenVocabFile = TokenVocabResolver.resolveTokenVocabFile(getElement());

		if ( tokenVocabFile!=null ) {
			return tokenVocabFile;
		}

		PsiFile importedFile = ImportResolver.resolveImportedFile(getElement());
		if ( importedFile!=null ) {
			return importedFile;
		}

		GrammarSpecNode grammar = PsiTreeUtil.getContextOfType(getElement(), GrammarSpecNode.class);
		PsiElement specNode = MyPsiUtils.findSpecNode(grammar, ruleName);

		if ( specNode!=null ) {
			return specNode;
		}

		// Look for a rule defined in an imported grammar
		specNode = ImportResolver.resolveInImportedFiles(getElement().getContainingFile(), ruleName);

		if ( specNode!=null ) {
			return specNode;
		}

		// Look for a lexer rule in the tokenVocab file if it exists
		if ( getElement() instanceof LexerRuleRefNode ) {
			return TokenVocabResolver.resolveInTokenVocab(getElement(), ruleName);
		}

		return null;
	}

	@Override
	public PsiElement handleElementRename(String newElementName) throws IncorrectOperationException {
		Project project = getElement().getProject();
		myElement.replace(MyPsiUtils.createLeafFromText(project,
				myElement.getContext(),
				newElementName,
				ANTLRv4TokenTypes.TOKEN_ELEMENT_TYPES.get(ANTLRv4Lexer.TOKEN_REF)));
		return myElement;
	}
}
