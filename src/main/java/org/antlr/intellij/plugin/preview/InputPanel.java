package org.antlr.intellij.plugin.preview;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInsight.hint.HintManagerImpl;
import com.intellij.codeInsight.hint.HintUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.event.CaretListener;
import com.intellij.openapi.editor.event.DocumentAdapter;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.ex.EditorMarkupModel;
import com.intellij.openapi.editor.markup.*;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComponentWithBrowseButton;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.ui.JBColor;
import com.intellij.ui.LightweightHint;
import org.antlr.intellij.adaptor.parser.SyntaxError;
import org.antlr.intellij.plugin.ANTLRv4PluginController;
import org.antlr.intellij.plugin.Icons;
import org.antlr.intellij.plugin.actions.MyActionUtils;
import org.antlr.intellij.plugin.parsing.ParsingUtils;
import org.antlr.intellij.plugin.parsing.PreviewParser;
import org.antlr.intellij.plugin.profiler.ProfilerPanel;
import org.antlr.runtime.CommonToken;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.misc.Pair;
import org.antlr.v4.runtime.misc.Utils;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.tool.Rule;
import org.antlr.v4.tool.ast.GrammarAST;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Not a view itself but delegates to one.

public class InputPanel {
	private static final Key<SyntaxError> SYNTAX_ERROR = Key.create("SYNTAX_ERROR");
	private static final int MAX_STACK_DISPLAY = 30;
	private static final int MAX_HINT_WIDTH = 110;
	private static final Logger LOG = Logger.getInstance("ANTLR InputPanel");
	private static final int TOKEN_INFO_LAYER = HighlighterLayer.SELECTION; // Show token info over errors
	private static final int ERROR_LAYER = HighlighterLayer.ERROR;
	private static final String missingStartRuleLabelText = "%s start rule: <select from navigator or grammar>";
	private static final String startRuleLabelText = "%s start rule: %s";

	private JRadioButton inputRadioButton;
	private JRadioButton fileRadioButton;
	private JTextArea placeHolder;
	private JTextArea errorConsole;
	private JLabel startRuleLabel;
	private JPanel radioButtonPanel;
	private JPanel startRuleAndInputPanel;
	private TextFieldWithBrowseButton fileChooser;
	private JPanel outerMostPanel;

	/**
	 * switchToGrammar() was seeing an empty slot instead of a previous
	 * editor or placeHolder. Figured it was an order of operations thing
	 * and synchronized add/remove ops. Works now w/o error.
	 */
	private final Object swapEditorComponentLock = new Object();

	private final PreviewPanel previewPanel;

	/**
	 * state for grammar in current editor, not editor where user is typing preview input!
	 */
	public PreviewState previewState;

	private final PreviewEditorMouseListener editorMouseListener;

	private final List<CaretListener> caretListeners = new ArrayList<>();

	public InputPanel(final PreviewPanel previewPanel) {
		WrappedFlowLayout layout = new WrappedFlowLayout(5, 0);
		layout.setAlignment(FlowLayout.CENTER);
		this.startRuleAndInputPanel.setLayout(layout);
		this.previewPanel = previewPanel;

		FileChooserDescriptor singleFileDescriptor =
			FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor();
		ComponentWithBrowseButton.BrowseFolderActionListener<JTextField> browseActionListener =
			new ComponentWithBrowseButton.BrowseFolderActionListener<JTextField>(
				"Select Input File", null,
				fileChooser,
				previewPanel.project,
				singleFileDescriptor,
				TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT
			) {
				protected void onFileChosen(@NotNull VirtualFile chosenFile) {
					// this next line is the code taken from super; pasted in
					// to avoid compile error on super.onFileCho[o]sen
					TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT.setText(fileChooser.getChildComponent(),
					                                                    chosenFileToResultingText(chosenFile));
					InputPanel.this.onFileChosen(chosenFile);
				}
			};
		fileChooser.getTextField().addActionListener(e -> {
            VirtualFile chosenFile = VirtualFileManager.getInstance()
                    .getFileSystem("file")
			        .findFileByPath(fileChooser.getText());
			onFileChosen(chosenFile);
		});
		fileChooser.addActionListener(browseActionListener);
		fileChooser.getButton().addActionListener(e -> fileRadioButton.setSelected(true));
		fileChooser.setTextFieldPreferredWidth(40);

		inputRadioButton.addActionListener(e -> selectInputEvent());
		fileRadioButton.addActionListener(e -> selectFileEvent());

		resetStartRuleLabel();

		editorMouseListener = new PreviewEditorMouseListener(this);
	}

	private void onFileChosen(VirtualFile chosenFile) {
		if ( previewState!=null ) {
			previewState.inputFile = chosenFile;
		}
		selectFileEvent();
	}

	public JPanel getComponent() {
		return outerMostPanel;
	}

	public void selectInputEvent() {
		inputRadioButton.setSelected(true);
		previewPanel.clearParseTree();
		clearErrorConsole();

		// wipe old and make new one
		if ( previewState!=null ) {
			releaseEditor(previewState);
			createManualInputPreviewEditor(previewState);
		}
	}

	public void createManualInputPreviewEditor(final PreviewState previewState) {
		final EditorFactory factory = EditorFactory.getInstance();
		Document doc = factory.createDocument("");
		doc.addDocumentListener(
			new DocumentAdapter() {
				@Override
				public void documentChanged(DocumentEvent e) {
					previewState.manualInputText = e.getDocument().getCharsSequence();
				}
			}
		                       );

		Editor editor = createPreviewEditor(previewState.grammarFile, doc, false);
		setEditorComponent(editor.getComponent()); // do before setting state
		previewState.setInputEditor(editor);

		// Set text last to trigger change events
		ApplicationManager.getApplication().runWriteAction(() -> doc.setText(previewState.manualInputText));
	}

	public void selectFileEvent() {
		fileRadioButton.setSelected(true);

		if ( previewState==null ) {
			return;
		}

		VirtualFile inputFile = previewState.inputFile;
		if (inputFile == null) {
			errorConsole.setText("Invalid input file");
			return;
		}

		Document inputDocument = FileDocumentManager.getInstance().getDocument(inputFile);

		if (inputDocument == null) {
			errorConsole.setText("Input file does not exist or cannot be loaded: " + inputFile.getPath());
			return;
		}

		// get state for grammar in current editor, not editor where user is typing preview input!
		ANTLRv4PluginController controller = ANTLRv4PluginController.getInstance(previewPanel.project);

		// wipe old and make new one
		releaseEditor(previewState);

		VirtualFile currentGrammarFile = controller != null ? controller.getCurrentGrammarFile() : null;
		if (currentGrammarFile == null) {
			return;
		}

		Editor editor = createPreviewEditor(currentGrammarFile, inputDocument, true);
		setEditorComponent(editor.getComponent()); // do before setting state
		previewState.setInputEditor(editor);
		clearErrorConsole();

		previewPanel.updateParseTreeFromDoc(currentGrammarFile);
	}

	public Editor createPreviewEditor(final VirtualFile grammarFile, Document doc, boolean readOnly) {
		LOG.info("createEditor: create new editor for "+grammarFile.getPath()+" "+previewPanel.project.getName());
		final EditorFactory factory = EditorFactory.getInstance();
		doc.addDocumentListener(
			new DocumentAdapter() {
				@Override
				public void documentChanged(DocumentEvent event) {
					previewPanel.updateParseTreeFromDoc(grammarFile);
				}
			}
		);
		final Editor editor = readOnly
				? factory.createViewer(doc, previewPanel.project)
				: factory.createEditor(doc, previewPanel.project);
		// force right margin
		((EditorMarkupModel) editor.getMarkupModel()).setErrorStripeVisible(true);
		EditorSettings settings = editor.getSettings();
		settings.setWhitespacesShown(true);
		settings.setLineNumbersShown(true);
		settings.setLineMarkerAreaShown(true);
		installListeners(editor);

		return editor;
	}

	public void grammarFileSaved() {
		clearParseErrors();
		if ( previewState!=null && previewState.startRuleName!=null ) {
			setStartRuleName(previewState.grammarFile, previewState.startRuleName);
		}
		else {
			resetStartRuleLabel();
		}
	}

	public void switchToGrammar(PreviewState previewState, VirtualFile grammarFile) {
		String grammarFileName = grammarFile.getPath();
		LOG.info("switchToGrammar "+grammarFileName+" "+previewPanel.project.getName());
		this.previewState = previewState;

		if ( previewState.inputFile !=null ) {
			fileChooser.setText(previewState.inputFile.getPath());
			selectFileEvent();
		}
		else {
			selectInputEvent();
		}

		clearParseErrors();

		if ( previewState.startRuleName!=null ) {
			setStartRuleName(grammarFile, previewState.startRuleName);
		}
		else {
			resetStartRuleLabel();
		}
	}

	public void setEditorComponent(JComponent editor) {
		BorderLayout layout = (BorderLayout) outerMostPanel.getLayout();
		String EDITOR_SPOT_COMPONENT = BorderLayout.CENTER;
		// atomically remove old
		synchronized (swapEditorComponentLock) {
			Component editorSpotComp = layout.getLayoutComponent(EDITOR_SPOT_COMPONENT);
			if ( editorSpotComp!=null ) {
				editorSpotComp.setVisible(false);
				outerMostPanel.remove(editorSpotComp); // remove old editor if it's there
			}
			outerMostPanel.add(editor, EDITOR_SPOT_COMPONENT);
		}
	}

	public Editor getInputEditor() {
		if ( previewState==null ) {
			// seems there are some out of sequence issues with InputPanels
			// being created but before we get a switchToGrammar event, which
			// creates the previewState.
			return null;
		}
		Editor editor = previewState.getInputEditor();
		if ( editor==null ) {
			createManualInputPreviewEditor(previewState); // ensure we always have an input window
			editor = previewState.getInputEditor();
		}

		return editor;
	}

	public void releaseEditor(PreviewState previewState) {
		uninstallListeners(previewState.getInputEditor());

		// release the editor
		previewState.releaseEditor();

		// restore the GUI
		setEditorComponent(placeHolder);
	}

	public void installListeners(Editor editor) {
		if (editor instanceof EditorEx) {
			// Avoid showing the default context menu
			((EditorEx) editor).setContextMenuGroupId("AntlrContextMenu");
		}
		editor.addEditorMouseMotionListener(editorMouseListener);
		editor.addEditorMouseListener(editorMouseListener);
		for ( CaretListener listener : caretListeners ) {
			editor.getCaretModel().addCaretListener(listener);
		}
	}

	public void uninstallListeners(Editor editor) {
		if ( editor==null ) return;
		editor.removeEditorMouseListener(editorMouseListener);
		editor.removeEditorMouseMotionListener(editorMouseListener);
		for ( CaretListener listener : caretListeners ) {
			editor.getCaretModel().removeCaretListener(listener);
		}
	}

	public void setStartRuleName(VirtualFile grammarFile, String startRuleName) {
		startRuleLabel.setText(String.format(startRuleLabelText, grammarFile.getName(), startRuleName));
		startRuleLabel.setForeground(JBColor.BLACK);
		final Font oldFont = startRuleLabel.getFont();
		startRuleLabel.setFont(oldFont.deriveFont(Font.BOLD));
		startRuleLabel.setIcon(Icons.FILE);
	}

	public void resetStartRuleLabel() {
		String grammarName = "?.g4";
		if ( previewState!=null ) {
			grammarName = previewState.grammarFile.getName();
		}
		startRuleLabel.setText(String.format(missingStartRuleLabelText, grammarName));
		startRuleLabel.setForeground(JBColor.RED);
		startRuleLabel.setIcon(Icons.FILE);
	}

	public void clearErrorConsole() {
		errorConsole.setText("");
	}

	public void displayErrorInParseErrorConsole(SyntaxError e) {
		String msg = getErrorDisplayString(e);
		errorConsole.insert(msg+'\n', errorConsole.getText().length());
	}

	public void clearParseErrors() {
		Editor editor = getInputEditor();
		if ( editor==null ) return;

		clearInputEditorHighlighters();

		HintManager.getInstance().hideAllHints();

		clearErrorConsole();
	}

	/**
	 * Clear all input highlighters
	 */
	public void clearInputEditorHighlighters() {
		Editor editor = getInputEditor();
		if ( editor==null ) return;

		MarkupModel markupModel = editor.getMarkupModel();
		markupModel.removeAllHighlighters();
	}

	/**
	 * Clear decision stuff but leave syntax errors
	 */
	public static void clearDecisionEventHighlighters(Editor editor) {
		removeHighlighters(editor, ProfilerPanel.DECISION_EVENT_INFO_KEY);
	}

	/**
	 * Remove any previous underlining or boxing, but not errors or decision event info
	 */
	public static void clearTokenInfoHighlighters(Editor editor) {
		MarkupModel markupModel = editor.getMarkupModel();
		for (RangeHighlighter r : markupModel.getAllHighlighters()) {
			if ( r.getUserData(ProfilerPanel.DECISION_EVENT_INFO_KEY)==null &&
				r.getUserData(SYNTAX_ERROR)==null ) {
				markupModel.removeHighlighter(r);
			}
		}
	}

	/**
	 * Display error messages to the console and also add annotations
	 * to the preview input window.
	 */
	public void showParseErrors(final List<SyntaxError> errors) {
		if ( errors.size()==0 ) {
			clearInputEditorHighlighters();
			return;
		}
		for (SyntaxError e : errors) {
			annotateErrorsInPreviewInputEditor(e);
			displayErrorInParseErrorConsole(e);
		}
	}

	/**
	 * Show token information if the ctrl-key is down and mouse movement occurs
	 */
	public void showTokenInfoUponCtrlKey(Editor editor, PreviewState previewState, int offset) {
		Token tokenUnderCursor = ParsingUtils.getTokenUnderCursor(previewState, offset);
		if ( tokenUnderCursor==null ) {
			PreviewParser parser = (PreviewParser) previewState.parsingResult.parser;
			CommonTokenStream tokenStream = (CommonTokenStream) parser.getInputStream();
			tokenUnderCursor = ParsingUtils.getSkippedTokenUnderCursor(tokenStream, offset);
		}

		if ( tokenUnderCursor==null ) {
			return;
		}

		String channelInfo = "";
		int channel = tokenUnderCursor.getChannel();
		if ( channel!=Token.DEFAULT_CHANNEL ) {
			String chNum = channel==Token.HIDDEN_CHANNEL ? "hidden" : String.valueOf(channel);
			channelInfo = ", Channel "+chNum;
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
		if ( channel==-1 ) {
			tokenInfo = "Skipped";
			color = JBColor.gray;
		}

		Interval sourceInterval = Interval.of(tokenUnderCursor.getStartIndex(),
		                                      tokenUnderCursor.getStopIndex()+1);
		highlightAndOfferHint(editor, offset, sourceInterval,
		                      color, EffectType.LINE_UNDERSCORE, tokenInfo);
	}

	/**
	 * Show tokens/region associated with parse tree parent of this token
	 * if the alt-key is down and mouse movement occurs.
	 */
	public void showParseRegion(Editor editor, PreviewState previewState, int offset) {
		Token tokenUnderCursor = ParsingUtils.getTokenUnderCursor(previewState, offset);
		if ( tokenUnderCursor==null ) {
			return;
		}

		ParseTree tree = previewState.parsingResult.tree;
		TerminalNode nodeWithToken =
			(TerminalNode) ParsingUtils.getParseTreeNodeWithToken(tree, tokenUnderCursor);
		if ( nodeWithToken==null ) {
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
			Interval.of(startToken.getStartIndex(), stopToken.getStopIndex()+1);

		List<String> stack = parser.getRuleInvocationStack(parent);
		Collections.reverse(stack);

		if ( stack.size()>MAX_STACK_DISPLAY ) {
			// collapse contiguous dups to handle left-recursive stacks
			List<Pair<String, Integer>> smaller = new ArrayList<>();
			int last = 0;
			smaller.add(new Pair<>(stack.get(0), 1)); // init to having first element, count of 1
			for (int i = 1; i<stack.size(); i++) {
				String s = stack.get(i);
				if ( smaller.get(last).a.equals(s) ) {
					smaller.set(last, new Pair<>(s, smaller.get(last).b + 1));
				}
				else {
					smaller.add(new Pair<>(s, 1));
					last++;
				}
			}
			stack = new ArrayList<>();
			for ( Pair<String, Integer> pair : smaller ) {
				if ( pair.b>1 ) {
					stack.add(pair.a + "^" + pair.b);
				}
				else {
					stack.add(pair.a);
				}
			}
		}
		String stackS = Utils.join(stack.toArray(), "\n");
		highlightAndOfferHint(editor, offset, sourceInterval,
		                      JBColor.BLUE, EffectType.ROUNDED_BOX, stackS);
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

		if ( hintText.contains("<") ) {
			hintText = hintText.replaceAll("<", "&lt;");
		}

		// HINT
		caretModel.moveToOffset(offset); // info tooltip only shows at cursor :(
		HintManager.getInstance().showInformationHint(editor, hintText);
	}

	public void setCursorToGrammarElement(Project project, PreviewState previewState, int offset) {
		Token tokenUnderCursor = ParsingUtils.getTokenUnderCursor(previewState, offset);
		if ( tokenUnderCursor==null ) {
			return;
		}

		PreviewParser parser = (PreviewParser) previewState.parsingResult.parser;
		Integer atnState = parser.inputTokenToStateMap.get(tokenUnderCursor);
		if ( atnState==null ) { // likely an error token
			//LOG.error("no ATN state for input token " + tokenUnderCursor);
			return;
		}

		Interval region = previewState.g.getStateToGrammarRegion(atnState);
		CommonToken token =
			(CommonToken) previewState.g.tokenStream.get(region.a);
		jumpToGrammarPosition(project, token.getStartIndex());
	}

	public void setCursorToGrammarRule(Project project, PreviewState previewState, int offset) {
		Token tokenUnderCursor = ParsingUtils.getTokenUnderCursor(previewState, offset);
		if ( tokenUnderCursor==null ) {
			return;
		}

		ParseTree tree = previewState.parsingResult.tree;
		TerminalNode nodeWithToken =
			(TerminalNode) ParsingUtils.getParseTreeNodeWithToken(tree, tokenUnderCursor);
		if ( nodeWithToken==null ) {
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
		final ANTLRv4PluginController controller = ANTLRv4PluginController.getInstance(project);
		if ( controller==null ) return;
		final Editor grammarEditor = controller.getEditor(previewState.grammarFile);
		if ( grammarEditor==null ) return;

		CaretModel caretModel = grammarEditor.getCaretModel();
		caretModel.moveToOffset(start);
		ScrollingModel scrollingModel = grammarEditor.getScrollingModel();
		scrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE);
		grammarEditor.getContentComponent().requestFocus();
	}

	public void setCursorToHierarchyViewElement(int offset) {
		previewPanel.hierarchyViewer.selectNodeAtOffset(offset);
	}

	/**
	 * Display syntax errors, hints in tooltips if under the cursor
	 */
	public static void showTooltips(Editor editor, @NotNull PreviewState previewState, int offset) {
		if ( previewState.parsingResult==null ) return; // no results?

		// Turn off any tooltips if none under the cursor
		// find the highlighter associated with this offset
		List<RangeHighlighter> highlightersAtOffset = MyActionUtils.getRangeHighlightersAtOffset(editor, offset);
		if ( highlightersAtOffset.size()==0 ) {
			return;
		}

		List<String> msgList = new ArrayList<>();
		boolean foundDecisionEvent = false;
		for ( RangeHighlighter r : highlightersAtOffset ) {
			DecisionEventInfo eventInfo = r.getUserData(ProfilerPanel.DECISION_EVENT_INFO_KEY);
			String msg;
			if ( eventInfo!=null ) {
				// TODO: move decision event stuff to profiler?
				if ( eventInfo instanceof AmbiguityInfo ) {
					msg = "Ambiguous upon alts " + eventInfo.configs.getAlts().toString();
				}
				else if ( eventInfo instanceof ContextSensitivityInfo ) {
					msg = "Context-sensitive";
				}
				else if ( eventInfo instanceof LookaheadEventInfo ) {
					int k = eventInfo.stopIndex - eventInfo.startIndex + 1;
					msg = "Deepest lookahead k=" + k;
				}
				else if ( eventInfo instanceof PredicateEvalInfo ) {
					PredicateEvalInfo evalInfo = (PredicateEvalInfo) eventInfo;
					msg = ProfilerPanel.getSemanticContextDisplayString(evalInfo,
							previewState,
							evalInfo.semctx, evalInfo.predictedAlt,
							evalInfo.evalResult);
					msg = msg + (!evalInfo.fullCtx ? " (DFA)" : "");
				}
				else {
					msg = "Unknown decision event: " + eventInfo;
				}
				foundDecisionEvent = true;
			}
			else {
				// error tool tips
				SyntaxError errorUnderCursor = r.getUserData(SYNTAX_ERROR);
				msg = getErrorDisplayString(errorUnderCursor);
				if ( msg.length()>MAX_HINT_WIDTH ) {
					msg = msg.substring(0, MAX_HINT_WIDTH) + "...";
				}
				if ( msg.indexOf('<') >= 0 ) {
					msg = msg.replaceAll("<", "&lt;");
				}
			}
			msgList.add(msg);
		}
		String combinedMsg = Utils.join(msgList.iterator(), "\n");
		HintManagerImpl hintMgr = (HintManagerImpl) HintManager.getInstance();
		if ( foundDecisionEvent ) {
			showDecisionEventToolTip(editor, offset, hintMgr, combinedMsg);
		}
		else {
			showPreviewEditorErrorToolTip(editor, offset, hintMgr, combinedMsg);
		}
	}

	public static void showPreviewEditorErrorToolTip(Editor editor, int offset, HintManagerImpl hintMgr, String msg) {
		int flags =
			HintManager.HIDE_BY_ANY_KEY|
				HintManager.HIDE_BY_TEXT_CHANGE|
				HintManager.HIDE_BY_SCROLLING;
		int timeout = 0; // default?
		hintMgr.showErrorHint(editor, msg,
		                      offset, offset+1,
		                      HintManager.ABOVE, flags, timeout);
	}

	public static void showDecisionEventToolTip(Editor editor, int offset, HintManagerImpl hintMgr, String msg) {
		int flags =
			HintManager.HIDE_BY_ANY_KEY|
				HintManager.HIDE_BY_TEXT_CHANGE|
				HintManager.HIDE_BY_SCROLLING;
		int timeout = 0; // default?
		JComponent infoLabel = HintUtil.createInformationLabel(msg);
		LightweightHint hint = new LightweightHint(infoLabel);
		final LogicalPosition pos = editor.offsetToLogicalPosition(offset);
		final Point p = HintManagerImpl.getHintPosition(hint, editor, pos, HintManager.ABOVE);
		hintMgr.showEditorHint(hint, editor, p, flags, timeout, false);
	}

	public void annotateErrorsInPreviewInputEditor(SyntaxError e) {
		Editor editor = getInputEditor();
		if ( editor==null ) return;
		MarkupModel markupModel = editor.getMarkupModel();

		int a, b; // Start and stop index
		RecognitionException cause = e.getException();
		if ( cause instanceof LexerNoViableAltException ) {
			a = ((LexerNoViableAltException) cause).getStartIndex();
			b = ((LexerNoViableAltException) cause).getStartIndex()+1;
		}
		else {
			Token offendingToken = e.getOffendingSymbol();
			a = offendingToken.getStartIndex();
			b = offendingToken.getStopIndex()+1;
		}

		// ANTLRv4PluginController.parseText() can be slow and is done lazily. That means it
		// is possible to parse and get error messages that are no longer appropriate because
		// the user has started altering the input after the parse was kicked off; another parse
		// will follow up and change the error messages and location of annotations in this input panel.
		// Avoid trying to select text outside of doc[0..stopindex] as a general rule too.
		if (a >= 0 && b + 1 <= editor.getDocument().getTextLength()) {
			final TextAttributes attr = new TextAttributes();
			attr.setForegroundColor(JBColor.RED);
			attr.setEffectColor(JBColor.RED);
			attr.setEffectType(EffectType.WAVE_UNDERSCORE);
			RangeHighlighter highlighter =
					markupModel.addRangeHighlighter(a,
							b,
							ERROR_LAYER, // layer
							attr,
							HighlighterTargetArea.EXACT_RANGE);
			highlighter.putUserData(SYNTAX_ERROR, e);
		}
	}

	public static void removeHighlighters(Editor editor, Key<?> key) {
		// Remove anything with user data accessible via key
		MarkupModel markupModel = editor.getMarkupModel();
		for (RangeHighlighter r : markupModel.getAllHighlighters()) {
			if ( r.getUserData(key)!=null ) {
				markupModel.removeHighlighter(r);
			}
		}
	}

	public static String getErrorDisplayString(SyntaxError e) {
		return "line "+e.getLine()+":"+e.getCharPositionInLine()+" "+e.getMessage();
	}

	public void addCaretListener(CaretListener caretListener) {
		this.caretListeners.add(caretListener);
	}
}
