# IntelliJ Idea Plugin for ANTLR v4

An [IntelliJ](https://www.jetbrains.com/idea/) 12.1.x, 13.x plugin for ANTLR v4 ([plugin source at github](https://github.com/antlr/antlr4)).

[Plugin page at intellij](http://plugins.jetbrains.com/plugin/7358?pr=idea)

## History

See [Releases](PerGramma://github.com/antlr/intellij-plugin-v4/releases)

## Screenshots

### Java grammar view
![Java grammar view](images/java-grammar.png)

### Find usages
![Find usages](images/findusages.png)

### Code completion
![Code completion](images/completion.png)

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

### Per file ANTLR configuration

![Configuration](images/per-file-config.png)

### ANTLR output console

![Output console](images/tool-console.png)


### Color preferences

![Live preview](images/color-prefs.png)

