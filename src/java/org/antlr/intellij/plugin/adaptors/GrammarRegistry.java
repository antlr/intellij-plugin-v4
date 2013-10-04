package org.antlr.intellij.plugin.adaptors;

import com.intellij.lang.Language;
import com.intellij.psi.tree.IElementType;
import org.antlr.v4.runtime.Lexer;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class GrammarRegistry {
	public static GrammarRegistry INSTANCE = new GrammarRegistry();

	protected Map<Language, GrammarDescriptor> registry =
		Collections.synchronizedMap(new HashMap<Language, GrammarDescriptor>());

	public void register(Language language,
						 Class<? extends Lexer> lexerClass,
						 Class<? extends IElementType> tokenClass)
	{
		GrammarDescriptor desc = new GrammarDescriptor(language, lexerClass, tokenClass);
		registry.put(language, desc);
	}

	private GrammarRegistry() { }

//	public static GrammarRegistry getInstance() {
//		if(instance == null) {
//			instance = new GrammarRegistry();
//		}
//		return instance;
//	}
}
