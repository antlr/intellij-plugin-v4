intellij-plugin-v4
==================

An Intellij 12.x plugin for ANTLR v4

1.0a4:
	finds tokenVocab option for code gen
	if there is a tokenVocab option, don't warn about implicit tokens.
	shortcut conflicted with grammar-kit plugin.

1.0a3:
	generates code. Uses package dir if @header { package x.y.z; } action present.
	generates in <root>/gen/package/YourGrammarRecognizer.java
	Shortcut (ctrl-shift-G / meta-shift-G) but it's in Tools menu, popups.
	Code completion for tokens, rule names.

1.0a2:
	goto-declaration
	ANTLR itself processes grammar and semantic errors get highlighted.
	find usages
	rename tokens, rules

1.0a1:
 	syntax-aware editing, highlighting, structure view

Screenshots

![Java grammar view](images/java-grammar.png)

![Find usages](images/findusages.png)

![Code completion](images/completion.png)