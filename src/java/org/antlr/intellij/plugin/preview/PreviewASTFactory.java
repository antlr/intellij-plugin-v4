package org.antlr.intellij.plugin.preview;

import com.intellij.lang.ASTFactory;
import com.intellij.psi.impl.source.tree.LeafElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.Nullable;

public class PreviewASTFactory extends ASTFactory {
	@Nullable
	@Override
	public LeafElement createLeaf(IElementType type, CharSequence text) {
//		// adjust endOffset to ignore dummy id from intellij
//		String s = text.toString();
//		int endOffset = text.length();
//		if ( s.endsWith(CompletionInitializationContext.DUMMY_IDENTIFIER) ) {
//			endOffset = endOffset - CompletionInitializationContext.DUMMY_IDENTIFIER.length();
//		}
//		else if ( s.endsWith(CompletionInitializationContext.DUMMY_IDENTIFIER_TRIMMED) ) {
//			endOffset = endOffset - CompletionInitializationContext.DUMMY_IDENTIFIER_TRIMMED.length();
//		}
//		text = text.subSequence(0, endOffset);
		LeafElement t;
		t = new PreviewTokenNode(type, text);
		System.out.println("create PreviewPsiLeaf "+t+" from "+type+" "+text);
		return t;
	}
}
