# IntelliJ Idea Plugin for ANTLR v4

An [IntelliJ](https://www.jetbrains.com/idea/) 12.1.x, 13.x plugin for ANTLR v4 ([plugin source at github](https://github.com/antlr/antlr4)).

[Plugin page at intellij](http://plugins.jetbrains.com/plugin/7358?pr=idea)

This plugin is for ANTLR v4 grammars. 

Features: 

- syntax highlighting;
- syntax error checking;
- semantic error checking;
- navigation window;
- goto-declaration;
- find usages;
- rename tokens;
- rename rules;
- generates code; 
- shortcut (ctrl-shift-G / meta-shift-G) but it's in Tools menu and popups.
- code completion for tokens, rule names;
- finds tokenVocab option for code gen if there is a tokenVocab option, don't warn about implicit tokens.
- shortcut conflicted with grammar-kit plugin; 
- has live grammar interpreter for grammar preview. Right click on rule and say "Test ANTLR Rule".
- changes to grammar seen in parse tree upon save of grammar. Works with Intellij 13.x and requires 12.1.x.

You can configure the ANTLR tool options per grammar file; right-click
in a grammar or on a grammar element within the structured view.
When you change and save a grammar, it automatically builds with ANTLR
in the background according to the preferences you have set.  ANTLR
tool errors appear in a console you can opened by clicking on a button
in the bottom tab.

You can use the meta-key while moving the mouse and it will show you
token information in the preview editor box via tooltips.

Errors within the preview editor are now highlighted with tooltips
and underlining just like a regular editor window. The difference
is that this window's grammar is specified in your grammar file.

meta-j pops up a list of live templates, just like it does for Java programming.
Currently, there are a number of lexical rules for common tokens such as comments
and identifiers that you can automatically inject.
There are shortcuts like rid that lets you jump directly to the
lexical rule you would like to generate. If you type the shortcut and wait a
second, intellij should pop up an action you can select for that shortcut.

ctrl-return, or whatever you have configured for the generate pop-up,
will bring up a list of things you can generate. The only one so far is
a generator to create lexical rules for any literals, referenced in the parser
 grammar, that have not been defined.

## History

See [Releases](PerGramma://github.com/antlr/intellij-plugin-v4/releases)

## Screenshots

### Java grammar view
![Java grammar view](images/java-grammar.png)

### Find usages
![Find usages](images/findusages.png)

### Code completion
![Code completion](images/completion.png)

### Live templates

You can inject predefined lexer rules. Use meta-j or type the abbreviation like rid and wait a second. It should pop up that choice.

![predefined lexer rules](images/lexer-templates.png)

### Refactoring: generate rules for literals

It guesses rule names or just uses T__&lt;n>. Respects literals already defined. Use Code::Generate menu item or key equivalent.

![def-literals.png](images/def-literals.png)

### Live parse tree preview

You can test any rule in the (parser) grammar.  Right click on rule in grammar
or navigator to "Test ANTLR Rule".  Changing grammar and saving, updates
parse tree. It works with combined grammars and separated but separated
must be in same directory and named XParser.g4 and XLexer.g4.
No raw Java actions are executed obviously during interpretation in
live preview.

[![Live parse preview](http://img.youtube.com/vi/h60VapD1rOo/0.jpg)](//www.youtube.com/embed/h60VapD1rOo)

![Live preview](images/live-preview.png)
![Live preview](images/live-preview-error.png)

You can also use the meta key while moving the mouse in preview window to get token info.

![Live preview](images/token-tooltips.png)

When there are errors, you will see the output in the small console under the input editor in case you need to cut and paste. But, for general viewing you can however the cursor over an underlined error and it will show you the message in a pop-up. Basically the input window behaves like a regular editor window except that it is subject to the grammar in your other editor.

![error-popup.png](images/error-popup.png)

With alt-mouse movement, you'll see parse region for rule matching token under cursor. Click and it jumps to grammar definition.

![parse-region.png](images/parse-region.png)

### Grammar Profiler

The profiler helps you understand which decisions in your grammar are complicated or expensive.  Profiling data is always available just like the parse tree during grammar interpretation.  The profiler cannot track code execution because it is running the grammar interpreter not executing compiled code.  It provides both a simplified set of columns and an expert set the provides a great deal more information. Clicking on a row in the profiler highlights the decision in the grammar and also highlights relevant pieces of the input, such as ambiguities or the deepest lookahead. You can sort the columns by clicking on the header row. Hover over the header row to get tooltips describing the column.

If you see ambiguities highlighted, those you should definitely take a look
 at in your grammar. If you see decisions requiring full context sensitivity,
 when viewing the expert columns, those are very expensive and could be causing
 speed problems. Note that the profiler always tries to keep up-to-date with
 the input. The profiler uses the parser interpreter but is fairly
 consistent with the speed of a generated and compiled parser but it does use
 single-stage full LL parsing which can be slower.  It needs to do that so
 that it gets full and complete profiling information. For those in the know,
 it uses PredictionMode.LL_EXACT_AMBIG_DETECTION. For really big files and
 slow grammars, there is an appreciable delay when displaying the parse tree or profiling information.

![parse-region.png](images/profiler.png)

### Unicode chars are no problem

![unicode.png](images/unicode.png)

### Per file ANTLR configuration

![Configuration](images/per-file-config.png)

### ANTLR output console

![Output console](images/tool-console.png)


### Color preferences

![Live preview](images/color-prefs.png)

