package org.antlr.intellij.plugin.preview;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.editor.ScrollingModel;
import com.intellij.openapi.editor.event.DocumentAdapter;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.EditorMouseEvent;
import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComponentWithBrowseButton;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import org.antlr.intellij.adaptor.parser.SyntaxError;
import org.antlr.intellij.plugin.ANTLRv4PluginController;
import org.antlr.intellij.plugin.parsing.ParsingUtils;
import org.antlr.intellij.plugin.parsing.PreviewParser;
import org.antlr.runtime.CommonToken;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.LexerNoViableAltException;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.misc.Utils;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.tool.Rule;
import org.antlr.v4.tool.ast.GrammarAST;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

// Not a view itself but delegates to one.

public class InputPanel {
    private JRadioButton inputRadioButton;
    private JRadioButton fileRadioButton;
    private JTextArea placeHolder;
    private JTextArea errorConsole;
    private JLabel startRuleLabel;
    private TextFieldWithBrowseButton fileChooser;
    protected JPanel outerMostPanel;

    public static final Logger LOG = Logger.getInstance("ANTLR InputPanel");
    public static final int TOKEN_INFO_LAYER = HighlighterLayer.SELECTION; // Show token info over errors
    public static final int ERROR_LAYER = HighlighterLayer.ERROR;

    /**
     * switchToGrammar() was seeing an empty slot instead of a previous
     * editor or placeHolder. Figured it was an order of operations thing
     * and synchronized add/remove ops. Works now w/o error.
     */
    public final Object swapEditorComponentLock = new Object();

    public static final String missingStartRuleLabelText =
            "Start rule: <select from navigator or grammar>";
    public static final String startRuleLabelText = "Start rule: ";

    public PreviewPanel previewPanel;

    PreviewEditorMouseListener editorMouseListener;

    public InputPanel(PreviewPanel previewPanel) {
        $$$setupUI$$$();

        this.previewPanel = previewPanel;

        FileChooserDescriptor singleFileDescriptor =
                FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor();
        ComponentWithBrowseButton.BrowseFolderActionListener<JTextField> browseActionListener =
                new ComponentWithBrowseButton.BrowseFolderActionListener<JTextField>(
                        "Select input file", null,
                        fileChooser,
                        previewPanel.project,
                        singleFileDescriptor,
                        TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT
                ) {
                    @Override
                    protected void onFileChoosen(VirtualFile chosenFile) {
                        super.onFileChoosen(chosenFile);
                        selectFileEvent();
                    }
                };
        fileChooser.addBrowseFolderListener(previewPanel.project, browseActionListener);
        fileChooser.getButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fileRadioButton.setSelected(true);
            }
        });
        fileChooser.setTextFieldPreferredWidth(40);

        inputRadioButton.addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        selectInputEvent();
                    }
                }
        );
        fileRadioButton.addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        selectFileEvent();
                    }
                }
        );

        startRuleLabel.setText(missingStartRuleLabelText);
        startRuleLabel.setForeground(JBColor.RED);

        editorMouseListener = new PreviewEditorMouseListener(this);
    }

    public JPanel getComponent() {
        return outerMostPanel;
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
    }

    public JTextArea getErrorConsole() {
        return errorConsole;
    }

    public JLabel getStartRuleLabel() {
        return startRuleLabel;
    }

    public void selectInputEvent() {
//		System.out.println("input");

        // get state for grammar in current editor, not editor where user is typing preview input!
        ANTLRv4PluginController controller = ANTLRv4PluginController.getInstance(previewPanel.project);
        PreviewState previewState = controller.getPreviewState();
        if (previewState == null) {
            return;
        }

        inputRadioButton.setSelected(true);
        // wipe old and make new one
        final EditorFactory factory = EditorFactory.getInstance();
        Document doc = factory.createDocument("");

        Editor editor = createEditor(controller.getCurrentGrammarFile(), doc);

        setEditorComponent(editor.getComponent()); // do before setting state
        previewState.setEditor(editor);
        previewPanel.clearParseTree();
        clearErrorConsole();
    }

    public void selectFileEvent() {
        String inputFileName = fileChooser.getText();
        if (inputFileName.trim().length() == 0) {
            return;
        }
        fileRadioButton.setSelected(true);
//		System.out.println("file " + inputFileName);

        // get state for grammar in current editor, not editor where user is typing preview input!
        ANTLRv4PluginController controller = ANTLRv4PluginController.getInstance(previewPanel.project);
        PreviewState previewState = controller.getPreviewState();
        if (previewState == null) {
            return;
        }

        // wipe old and make new one
        try {
            char[] inputText = FileUtil.loadFileText(new File(inputFileName));
            final EditorFactory factory = EditorFactory.getInstance();
            Document doc = factory.createDocument(inputText);
            doc.setReadOnly(true);
            Editor editor = createEditor(controller.getCurrentGrammarFile(), doc);
            setEditorComponent(editor.getComponent()); // do before setting state
            previewState.setEditor(editor);
            clearErrorConsole();
            previewPanel.updateParseTreeFromDoc(controller.getCurrentGrammarFile());
        } catch (IOException ioe) {
            LOG.error("can't load input file " + inputFileName, ioe);
        }
    }

    public Editor createEditor(final VirtualFile grammarFile, Document doc) {
        LOG.info("createEditor: create new editor for " + grammarFile.getPath() + " " + previewPanel.project.getName());
        final EditorFactory factory = EditorFactory.getInstance();
        doc.addDocumentListener(
                new DocumentAdapter() {
                    @Override
                    public void documentChanged(DocumentEvent event) {
                        previewPanel.updateParseTreeFromDoc(grammarFile);
                    }
                }
        );
        final Editor editor = factory.createEditor(doc, previewPanel.project);
        EditorSettings settings = editor.getSettings();
        settings.setWhitespacesShown(true); // hmm...doesn't work.  maybe show when showing token tooltip?

        editor.addEditorMouseMotionListener(editorMouseListener);
        editor.addEditorMouseListener(editorMouseListener);

        return editor;
    }

    public void switchToGrammar(VirtualFile grammarFile) {
        String grammarFileName = grammarFile.getPath();
        LOG.info("switchToGrammar " + grammarFileName + " " + previewPanel.project.getName());
        PreviewState previewState = ANTLRv4PluginController.getInstance(previewPanel.project).getPreviewState(grammarFileName);

        selectInputEvent();

        clearParseErrors(grammarFile);

        if (previewState.startRuleName != null) {
            setStartRuleName(grammarFile, previewState.startRuleName);
        } else {
            resetStartRuleLabel();
        }
    }

    public void setEditorComponent(JComponent editor) {
        BorderLayout layout = (BorderLayout) outerMostPanel.getLayout();
        String EDITOR_SPOT_COMPONENT = BorderLayout.CENTER;
        // atomically remove old
        synchronized (swapEditorComponentLock) {
            Component editorSpotComp = layout.getLayoutComponent(EDITOR_SPOT_COMPONENT);
            if (editorSpotComp != null) {
                editorSpotComp.setVisible(false);
                outerMostPanel.remove(editorSpotComp); // remove old editor if it's there
            }
            outerMostPanel.add(editor, EDITOR_SPOT_COMPONENT);
        }
    }

    public Editor getEditor(VirtualFile grammarFile) {
        ANTLRv4PluginController controller = ANTLRv4PluginController.getInstance(previewPanel.project);
        PreviewState previewState = controller.getPreviewState(grammarFile.getPath());
        return previewState.getEditor();
    }

    public String getText(VirtualFile grammarFile) {
        return getEditor(grammarFile).getDocument().getText();
    }

    public void releaseEditor(PreviewState previewState) {
        // release the editor
        previewState.releaseEditor();

        // restore the GUI
        setEditorComponent(placeHolder);
    }

    public void setStartRuleName(VirtualFile grammarFile, String startRuleName) {
        startRuleLabel.setText(startRuleLabelText + startRuleName);
        startRuleLabel.setForeground(JBColor.BLACK);
    }

    public void resetStartRuleLabel() {
        startRuleLabel.setText(missingStartRuleLabelText); // reset
        startRuleLabel.setForeground(JBColor.RED);
    }

    public void clearErrorConsole() {
        errorConsole.setText("");
    }

    public void displayErrorInParseErrorConsole(SyntaxError e) {
        String msg = getErrorDisplayString(e);
        errorConsole.insert(msg + '\n', errorConsole.getText().length());
    }

    public void clearParseErrors(VirtualFile grammarFile) {
        Editor editor = getEditor(grammarFile);
        if (editor == null) return;

        MarkupModel markupModel = editor.getMarkupModel();
        markupModel.removeAllHighlighters();

        HintManager.getInstance().hideAllHints();

        clearErrorConsole();
    }

    /**
     * Display error messages to the console and also add annotations
     * to the preview input window.
     */
    public void showParseErrors(final VirtualFile grammarFile, final List<SyntaxError> errors) {
        MarkupModel markupModel = getEditor(grammarFile).getMarkupModel();
        if (errors.size() == 0) {
            markupModel.removeAllHighlighters();
            return;
        }
        for (SyntaxError e : errors) {
            annotateErrorsInPreviewInputEditor(grammarFile, e);
            displayErrorInParseErrorConsole(e);
        }
    }

    /**
     * Show token information if the meta-key is down and mouse movement occurs
     */
    public void showTokenInfoUponMeta(Editor editor, PreviewState previewState, int offset) {
        Token tokenUnderCursor = ParsingUtils.getTokenUnderCursor(previewState, offset);
        if (tokenUnderCursor == null) {
            PreviewParser parser = (PreviewParser) previewState.parsingResult.parser;
            CommonTokenStream tokenStream = (CommonTokenStream) parser.getInputStream();
            tokenUnderCursor = ParsingUtils.getSkippedTokenUnderCursor(tokenStream, offset);
        }

        if (tokenUnderCursor == null) {
            return;
        }

//		System.out.println("token = "+tokenUnderCursor);
        String channelInfo = "";
        int channel = tokenUnderCursor.getChannel();
        if (channel != Token.DEFAULT_CHANNEL) {
            String chNum = channel == Token.HIDDEN_CHANNEL ? "hidden" : String.valueOf(channel);
            channelInfo = ", Channel " + chNum;
        }
        JBColor color = JBColor.BLUE;
        String tokenInfo =
                String.format("#%d Type %s, Line %d:%d%s",
                        tokenUnderCursor.getTokenIndex(),
                        previewState.g.getTokenDisplayName(tokenUnderCursor.getType()),
                        tokenUnderCursor.getLine(),
                        tokenUnderCursor.getCharPositionInLine(),
                        channelInfo
                );
        if (channel == -1) {
            tokenInfo = "Skipped";
            color = JBColor.gray;
        }

        Interval sourceInterval = Interval.of(tokenUnderCursor.getStartIndex(),
                tokenUnderCursor.getStopIndex() + 1);
        highlightAndOfferHint(editor, offset, sourceInterval,
                color, EffectType.LINE_UNDERSCORE, tokenInfo);
    }

    /**
     * Show tokens/region associated with parse tree parent of this token
     * if the alt-key is down and mouse movement occurs.
     */
    public void showParseRegion(EditorMouseEvent event, Editor editor,
                                PreviewState previewState, int offset) {
        Token tokenUnderCursor = ParsingUtils.getTokenUnderCursor(previewState, offset);
        if (tokenUnderCursor == null) {
            return;
        }

        ParseTree tree = previewState.parsingResult.tree;
        TerminalNode nodeWithToken =
                (TerminalNode) ParsingUtils.getParseTreeNodeWithToken(tree, tokenUnderCursor);
        if (nodeWithToken == null) {
            // hidden token
            return;
        }

        PreviewParser parser = (PreviewParser) previewState.parsingResult.parser;
        CommonTokenStream tokenStream = (CommonTokenStream) parser.getInputStream();
        ParserRuleContext parent = (ParserRuleContext) nodeWithToken.getParent();
        Interval tokenInterval = parent.getSourceInterval();
        Token startToken = tokenStream.get(tokenInterval.a);
        Token stopToken = tokenStream.get(tokenInterval.b);
        Interval sourceInterval =
                Interval.of(startToken.getStartIndex(), stopToken.getStopIndex() + 1);
        int ruleIndex = parent.getRuleIndex();
        String ruleName = parser.getRuleNames()[ruleIndex];
//        System.out.println("parent " + ruleName + " region " + sourceInterval);

        List<String> stack = parser.getRuleInvocationStack(parent);
        Collections.reverse(stack);

        highlightAndOfferHint(editor, offset, sourceInterval,
                JBColor.BLUE, EffectType.ROUNDED_BOX, Utils.join(stack.toArray(), "\n"));


        // Code for a pop up list with selectable elements

//		final JList list = new JBList(stack);
////		PopupChooserBuilder builder = new PopupChooserBuilder(list);
//		JBPopupFactory factory = JBPopupFactory.getInstance();
//		PopupChooserBuilder builder = factory.createListPopupBuilder(list);
//		JBPopup popup = builder.createPopup();
//
//		MouseEvent mouseEvent = event.getMouseEvent();
//		Point point = mouseEvent.getPoint();
//		Dimension dimension = popup.getContent().getLayout().preferredLayoutSize(builder.getScrollPane());
//		System.out.println(dimension);
//		int height = dimension.height;
//		point.translate(10, -height);
//		RelativePoint where = new RelativePoint(mouseEvent.getComponent(), point);
//		popup.show(where);

        // Code for a balloon.

//		JBPopupFactory popupFactory = JBPopupFactory.getInstance();
//		BalloonBuilder builder =
//		    popupFactory.createHtmlTextBalloonBuilder(Utils.join(stack.toArray(), "<br>"),
//												  MessageType.INFO, null);
//		builder.setHideOnClickOutside(true);
//		Balloon balloon = builder.createBalloon();
//		MouseEvent mouseEvent = event.getMouseEvent();
//		Point point = mouseEvent.getPoint();
//		point.translate(10, -15);
//		RelativePoint where = new RelativePoint(mouseEvent.getComponent(), point);
//		balloon.show(where, Balloon.Position.above);
    }

    public void highlightAndOfferHint(Editor editor, int offset,
                                      Interval sourceInterval,
                                      JBColor color,
                                      EffectType effectType, String hintText) {
        CaretModel caretModel = editor.getCaretModel();
        final TextAttributes attr = new TextAttributes();
        attr.setForegroundColor(color);
        attr.setEffectColor(color);
        attr.setEffectType(effectType);
        MarkupModel markupModel = editor.getMarkupModel();
        markupModel.addRangeHighlighter(
                sourceInterval.a,
                sourceInterval.b,
                InputPanel.TOKEN_INFO_LAYER, // layer
                attr,
                HighlighterTargetArea.EXACT_RANGE
        );

        // HINT
        caretModel.moveToOffset(offset); // info tooltip only shows at cursor :(
        HintManager.getInstance().showInformationHint(editor, hintText);
    }

    public void setCursorToGrammarElement(Project project, PreviewState previewState, int offset) {
        Token tokenUnderCursor = ParsingUtils.getTokenUnderCursor(previewState, offset);
        if (tokenUnderCursor == null) {
            return;
        }

        PreviewParser parser = (PreviewParser) previewState.parsingResult.parser;
        Integer atnState = parser.inputTokenToStateMap.get(tokenUnderCursor);
        if (atnState == null) {
            LOG.error("no ATN state for input token " + tokenUnderCursor);
            return;
        }

        Interval region = previewState.g.getStateToGrammarRegion(atnState);
        jumpToGrammarPosition(project, region.a);
    }

    public void setCursorToGrammarRule(Project project, PreviewState previewState, int offset) {
        Token tokenUnderCursor = ParsingUtils.getTokenUnderCursor(previewState, offset);
        if (tokenUnderCursor == null) {
            return;
        }

        ParseTree tree = previewState.parsingResult.tree;
        TerminalNode nodeWithToken =
                (TerminalNode) ParsingUtils.getParseTreeNodeWithToken(tree, tokenUnderCursor);
        if (nodeWithToken == null) {
            // hidden token
            return;
        }

        ParserRuleContext parent = (ParserRuleContext) nodeWithToken.getParent();
        int ruleIndex = parent.getRuleIndex();
        Rule rule = previewState.g.getRule(ruleIndex);
        GrammarAST ruleNameNode = (GrammarAST) rule.ast.getChild(0);
        int start = ((CommonToken) ruleNameNode.getToken()).getStartIndex();

        jumpToGrammarPosition(project, start);
    }

    public void jumpToGrammarPosition(Project project, int start) {
        ANTLRv4PluginController controller = ANTLRv4PluginController.getInstance(project);
        Editor grammarEditor = controller.getCurrentGrammarEditor();
        CaretModel caretModel = grammarEditor.getCaretModel();
        caretModel.moveToOffset(start);
        ScrollingModel scrollingModel = grammarEditor.getScrollingModel();
        scrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE);
        grammarEditor.getContentComponent().requestFocus();
    }

    /**
     * Display syntax errors in tooltips if under the cursor
     */
    public void showTooltipsForErrors(Editor editor, @NotNull PreviewState previewState, int offset) {
        if (previewState.parsingResult == null) return; // no results?

        MarkupModel markupModel = editor.getMarkupModel();
        SyntaxError errorUnderCursor =
                ParsingUtils.getErrorUnderCursor(previewState.parsingResult.syntaxErrorListener.getSyntaxErrors(), offset);
        if (errorUnderCursor == null) {
            // Turn off any tooltips if none under the cursor
            HintManager.getInstance().hideAllHints();
            return;
        }

        // find the highlighter associated with this error by finding error at this offset
        int i = 1;
        for (RangeHighlighter r : markupModel.getAllHighlighters()) {
            int a = r.getStartOffset();
            int b = r.getEndOffset();
//			System.out.printf("#%d: %d..%d %s\n", i, a, b, r.toString());
            i++;
            if (offset >= a && offset < b) { // cursor is over some kind of highlighting
                TextAttributes attr = r.getTextAttributes();
                if (attr != null && attr.getEffectType() == EffectType.WAVE_UNDERSCORE) {
                    // error tool tips
                    String errorDisplayString =
                            InputPanel.getErrorDisplayString(errorUnderCursor);
                    int flags =
                            HintManager.HIDE_BY_ANY_KEY |
                                    HintManager.HIDE_BY_TEXT_CHANGE |
                                    HintManager.HIDE_BY_SCROLLING;
                    int timeout = 0; // default?
                    HintManager.getInstance().showErrorHint(editor, errorDisplayString,
                            offset, offset + 1,
                            HintManager.ABOVE, flags, timeout);
                    return;
                }
            }
        }
    }

    public void annotateErrorsInPreviewInputEditor(VirtualFile grammarFile, SyntaxError e) {
        Editor editor = getEditor(grammarFile);
        MarkupModel markupModel = editor.getMarkupModel();

        int a, b; // Start and stop index
        RecognitionException cause = e.getException();
        if (cause instanceof LexerNoViableAltException) {
            a = ((LexerNoViableAltException) cause).getStartIndex();
            b = ((LexerNoViableAltException) cause).getStartIndex() + 1;
        } else {
            Token offendingToken = (Token) e.getOffendingSymbol();
            a = offendingToken.getStartIndex();
            b = offendingToken.getStopIndex() + 1;
        }
        final TextAttributes attr = new TextAttributes();
        attr.setForegroundColor(JBColor.RED);
        attr.setEffectColor(JBColor.RED);
        attr.setEffectType(EffectType.WAVE_UNDERSCORE);
        markupModel.addRangeHighlighter(a,
                b,
                ERROR_LAYER, // layer
                attr,
                HighlighterTargetArea.EXACT_RANGE);
    }

    public static void removeTokenInfoHighlighters(Editor editor) {
        // Remove any previous underlining or boxing, but not anything else like errors
        MarkupModel markupModel = editor.getMarkupModel();
        for (RangeHighlighter r : markupModel.getAllHighlighters()) {
            TextAttributes attr = r.getTextAttributes();
            if (attr != null &&
                    (attr.getEffectType() == EffectType.LINE_UNDERSCORE ||
                            attr.getEffectType() == EffectType.ROUNDED_BOX)) {
                markupModel.removeHighlighter(r);
            }
        }
    }

    public static String getErrorDisplayString(SyntaxError e) {
        return "line " + e.getLine() + ":" + e.getCharPositionInLine() + " " + e.getMessage();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        outerMostPanel = new JPanel();
        outerMostPanel.setLayout(new BorderLayout(0, 0));
        outerMostPanel.setMinimumSize(new Dimension(100, 70));
        outerMostPanel.setPreferredSize(new Dimension(200, 100));
        errorConsole = new JTextArea();
        errorConsole.setEditable(false);
        errorConsole.setLineWrap(true);
        errorConsole.setRows(3);
        outerMostPanel.add(errorConsole, BorderLayout.SOUTH);
        placeHolder = new JTextArea();
        placeHolder.setBackground(Color.lightGray);
        placeHolder.setEditable(false);
        placeHolder.setEnabled(true);
        placeHolder.setText("");
        outerMostPanel.add(placeHolder, BorderLayout.CENTER);
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 4, new Insets(0, 0, 0, 0), -1, -1));
        outerMostPanel.add(panel1, BorderLayout.NORTH);
        startRuleLabel = new JLabel();
        startRuleLabel.setText("Label");
        panel1.add(startRuleLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        inputRadioButton = new JRadioButton();
        inputRadioButton.setSelected(true);
        inputRadioButton.setText("Input");
        panel1.add(inputRadioButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, 1, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        fileRadioButton = new JRadioButton();
        fileRadioButton.setText("File");
        panel1.add(fileRadioButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, 1, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        fileChooser = new TextFieldWithBrowseButton();
        panel1.add(fileChooser, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(195, 24), null, 0, false));
        ButtonGroup buttonGroup;
        buttonGroup = new ButtonGroup();
        buttonGroup.add(fileRadioButton);
        buttonGroup.add(inputRadioButton);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return outerMostPanel;
    }
}
