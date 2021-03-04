# Plugin architecture

## Build

Gradle is used to build the project. [`gradle-intellij-plugin`][1] is used to pull
any given version of IntelliJ to add its JARs to the classpath, build forms,
publish to the marketplace etc.

GitHub Actions are used for continuous integration, see files in `.github/workflows`.

## Lexing/parsing

An ANTLR v4 grammar in `src/main/antlr` is used to generate a lexer/parser. 
`ANTLRv4ParserDefinition` uses the [`antlr4-intellij-adaptor` library][2] to 
delegate parsing/lexing to these generated classes.

Syntax highlighting is also using the generated lexer (by delegation) in
`org.antlr.intellij.plugin.ANTLRv4SyntaxHighlighter.getHighlightingLexer`.

## Error checking

The plugin embeds a complete version of ANTLR v4. To highlight warnings in the
editor, the file is parsed again in `ANTLRv4ExternalAnnotator`, but this time
using the "official" parser. The annotator constructs a `org.antlr.v4.Tool` 
with all the flags configured in the `Configure ANTLR...` dialog. 
`GrammarIssuesCollector` sets up an error listener, then processes all the issues
reported by the `Tool` to show them in the editor.

## Preview window

Contrary to ANTLRWorks, the IntelliJ plugin does not run the actual generated
parser to test a grammar. Instead, it uses [ANTLR interpreters][3]. This is
mainly because the IDE classpath and the project classpath are totally different,
meaning generated parsers and their custom code and dependencies are not
available from the IDE process. 

The interpreter provides a convenient way to "run" grammars without needing 
any generated code. A major drawback of this approach is that custom code will
not be executed: `@members`, actions, predicates...

The entry point for the preview window is `PreviewPanel`, which holds the main
Swing component displayed in the tool window. It uses `ANTLRv4PluginController`
to maintain a cache of parsed grammars that can be previewed. Everytime the
editor switches to another .g4 grammar, the preview is updated accordingly:
the input text and selected rule are restored to their previous state (if any),
and the interpreter results are updated. Interpreter results include profiling
data, a list of tokens and a tree of matched rules and terminal nodes (both
in graphical form and in JTree form).

`PreviewPanel` acts as an orchestrator between inputs (grammar changed event,
input text changed event) and outputs (profiler, token list, parse tree etc.).

Since the preview panel has full control over the interpreter, it can monitor
the parsing phase to detect potential infinite loops caused by bad grammars,
thus preventing IDE freezes or OutOfMemoryExceptions.

[1]: https://github.com/JetBrains/gradle-intellij-plugin
[2]: https://github.com/antlr/antlr4-intellij-adaptor/
[3]: https://github.com/antlr/antlr4/blob/master/doc/interpreters.md
