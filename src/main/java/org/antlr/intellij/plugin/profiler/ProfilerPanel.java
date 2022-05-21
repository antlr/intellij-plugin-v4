package org.antlr.intellij.plugin.profiler;

import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.editor.ScrollingModel;
import com.intellij.openapi.editor.event.EditorMouseEvent;
import com.intellij.openapi.editor.markup.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.table.JBTable;
import org.antlr.intellij.plugin.ANTLRv4PluginController;
import org.antlr.intellij.plugin.preview.InputPanel;
import org.antlr.intellij.plugin.preview.PreviewPanel;
import org.antlr.intellij.plugin.preview.PreviewState;
import org.antlr.runtime.CommonToken;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.tool.Grammar;
import org.antlr.v4.tool.Rule;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.text.NumberFormat;

public class ProfilerPanel {
	public static final Color AMBIGUITY_COLOR = new Color(138, 0, 0);
	public static final Color FULLCTX_COLOR = new Color(255, 128, 0);
	public static final Color PREDEVAL_COLOR = new Color(110, 139, 61);
	public static final Color DEEPESTLOOK_COLOR = new Color(0, 128, 128);

	public static final Key<DecisionEventInfo> DECISION_EVENT_INFO_KEY = Key.create("DECISION_EVENT_INFO");
	public static final Key<DecisionInfo> DECISION_INFO_KEY = Key.create("DECISION_INFO_KEY");

	public Project project;
	public PreviewState previewState;
	public PreviewPanel previewPanel;

	protected JPanel outerPanel;
	protected JPanel statsPanel;
	protected JLabel parseTimeField;
	protected JLabel predictionTimeField;
	protected JLabel lookaheadBurdenField;
	protected JLabel cacheMissRateField;
	protected JLabel inputSizeField;
	protected JLabel numTokensField;
	protected JCheckBox expertCheckBox;
	protected JLabel ambiguityColorLabel;
	protected JLabel contextSensitivityColorLabel;
	protected JLabel predEvaluationColorLabel;
	protected JBTable profilerDataTable;
	protected JLabel deepestLookaheadLabel;

	protected class ProfilerJBTable extends JBTable {
		@Override
		public void setEnabled(boolean enabled) {
			super.setEnabled(enabled);
		}

		@Override
		protected JTableHeader createDefaultTableHeader() {
			return new ProfilerTableHeader();
		}

		@Override
		public TableCellRenderer getDefaultRenderer(Class<?> columnClass) {
			return new ProfileTableCellRenderer();
		}

		protected class ProfilerTableHeader extends JBTableHeader {
			@Override
			public void setEnabled(boolean enabled) {
				super.setEnabled(enabled);
			}

			public String getToolTipText(MouseEvent e) {
				Point p = e.getPoint();
				int index = columnModel.getColumnIndexAtX(p.x);
				int realIndex = columnModel.getColumn(index).getModelIndex();
				TableModel model = getModel();
				if ( model instanceof ProfilerTableDataModel) {
					return ((ProfilerTableDataModel) model).getColumnToolTips()[realIndex];
				}
				return model.getColumnName(realIndex);
			}
		}
	}

	public void grammarFileSaved(PreviewState previewState, VirtualFile grammarFile) {
		// leave model and such alone.
	}

	public void switchToGrammar(PreviewState previewState, VirtualFile grammarFile) {
		this.previewState = previewState;
		DefaultTableModel model = new DefaultTableModel();
		profilerDataTable.setModel(model);
		profilerDataTable.setAutoCreateRowSorter(true);
	}

	public void mouseEnteredGrammarEditorEvent(VirtualFile vfile, EditorMouseEvent e) {
		// clear grammar highlighters related to decision info
		InputPanel.removeHighlighters(e.getEditor(), ProfilerPanel.DECISION_INFO_KEY);
	}

	public JPanel getComponent() {
		return outerPanel;
	}

	public JBTable getProfilerDataTable() {
		return profilerDataTable;
	}

	public ProfilerPanel(Project project, PreviewPanel previewPanel) {
		this.project = project;
		this.previewPanel = previewPanel;
	}

	public void setProfilerData(PreviewState previewState, long parseTime_ns) {
		this.previewState = previewState;
		Parser parser = previewState.parsingResult.parser;
		ParseInfo parseInfo = parser.getParseInfo();
		updateTableModelPerExpertCheckBox(parseInfo,parser);
		double parseTimeMS = parseTime_ns/(1000.0*1000.0);
		// microsecond decimal precision
		NumberFormat formatter = new DecimalFormat("#.###");
		parseTimeField.setText(formatter.format(parseTimeMS));
		double predTimeMS = parseInfo.getTotalTimeInPrediction()/(1000.0*1000.0);
		predictionTimeField.setText(
			String.format("%s = %3.2f%%", formatter.format(predTimeMS), 100*(predTimeMS)/parseTimeMS)
		                           );
		TokenStream tokens = parser.getInputStream();
		int numTokens = tokens.size();
		Token lastToken = tokens.get(numTokens-1);
		int numChar = lastToken.getStopIndex();
		int numLines = lastToken.getLine();
		if ( lastToken.getType()==Token.EOF ) {
			if ( numTokens<=1 ) {
				numLines = 0;
			}
			else {
				Token secondToLastToken = tokens.get(numTokens-2);
				numLines = secondToLastToken.getLine();
			}
		}
		inputSizeField.setText(String.format("%d char, %d lines",
		                                     numChar,
		                                     numLines));
		numTokensField.setText(String.valueOf(numTokens));
		double look =
			parseInfo.getTotalSLLLookaheadOps()+
				parseInfo.getTotalLLLookaheadOps();
		lookaheadBurdenField.setText(
			String.format("%d/%d = %3.2f", (long) look, numTokens, look/numTokens)
		                            );
		double atnLook = parseInfo.getTotalATNLookaheadOps();
		cacheMissRateField.setText(
			String.format("%d/%d = %3.2f%%", (long) atnLook, (long) look, atnLook*100.0/look)
		                          );
	}

	public void updateTableModelPerExpertCheckBox(ParseInfo parseInfo,Parser parser) {
		AbstractTableModel model;
		if ( expertCheckBox.isSelected() ) {
			model = new ExpertProfilerTableDataModel(parseInfo,parser);
		}
		else {
			model = new SimpleProfilerTableDataModel(parseInfo,parser);
		}
		profilerDataTable.setModel(model);
		profilerDataTable.setRowSorter(new TableRowSorter<>(model));
	}

	public void selectDecisionInGrammar(PreviewState previewState, int decision) {
		final ANTLRv4PluginController controller = ANTLRv4PluginController.getInstance(previewState.project);
		if ( controller==null ) return;
		final Editor grammarEditor = controller.getEditor(previewState.grammarFile);
		if ( grammarEditor==null ) return;

		DecisionState decisionState = previewState.g.atn.getDecisionState(decision);
		Interval region = previewState.g.getStateToGrammarRegion(decisionState.stateNumber);
		if ( region==null ) {
			System.err.println("decision "+decision+" has state "+decisionState.stateNumber+" but no region");
			return;
		}

		InputPanel.removeHighlighters(grammarEditor, ProfilerPanel.DECISION_INFO_KEY);

		org.antlr.runtime.TokenStream tokens = previewState.g.tokenStream;
		if ( region.a>=tokens.size() || region.b>=tokens.size() ) {
			return;
		}
		CommonToken startToken = (CommonToken) tokens.get(region.a);
		CommonToken stopToken = (CommonToken) tokens.get(region.b);
		JBColor effectColor = JBColor.darkGray;
		DecisionInfo decisionInfo = previewState.parsingResult.parser.getParseInfo().getDecisionInfo()[decision];
		if ( decisionInfo.predicateEvals.size()>0 ) {
			effectColor = new JBColor(PREDEVAL_COLOR, AMBIGUITY_COLOR);
		}
		if ( decisionInfo.contextSensitivities.size()>0 ) {
			effectColor = new JBColor(FULLCTX_COLOR, AMBIGUITY_COLOR);
		}
		if ( decisionInfo.ambiguities.size()>0 ) {
			effectColor = new JBColor(AMBIGUITY_COLOR, AMBIGUITY_COLOR);
		}

		TextAttributes attr =
			new TextAttributes(JBColor.BLACK, JBColor.WHITE, effectColor,
			                   EffectType.ROUNDED_BOX, Font.PLAIN);
		MarkupModel markupModel = grammarEditor.getMarkupModel();
		final RangeHighlighter rangeHighlighter = markupModel.addRangeHighlighter(
			startToken.getStartIndex(),
			stopToken.getStopIndex()+1,
			HighlighterLayer.SELECTION, // layer
			attr,
			HighlighterTargetArea.EXACT_RANGE
		                                                                         );
		rangeHighlighter.putUserData(DECISION_INFO_KEY, decisionInfo);

		ScrollingModel scrollingModel = grammarEditor.getScrollingModel();
		CaretModel caretModel = grammarEditor.getCaretModel();
		caretModel.moveToOffset(startToken.getStartIndex());
		scrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE);
	}

	public void highlightInputPhrases(PreviewState previewState, int decision) {
		if ( previewState==null || previewState.parsingResult==null ) {
			return;
		}

		Editor inputEditor = previewState.getInputEditor();
		ScrollingModel scrollingModel = inputEditor.getScrollingModel();
		CaretModel caretModel = inputEditor.getCaretModel();
		MarkupModel markupModel = inputEditor.getMarkupModel();

		InputPanel.clearDecisionEventHighlighters(inputEditor);

		ParseInfo parseInfo = previewState.parsingResult.parser.getParseInfo();
		DecisionInfo decisionInfo = parseInfo.getDecisionInfo()[decision];

		Token firstToken = null;
		// deepest lookahead
		long maxLook = Math.max(decisionInfo.LL_MaxLook, decisionInfo.SLL_MaxLook);
		if ( maxLook>1 ) // ignore k=1
		{
			LookaheadEventInfo maxLookEvent = decisionInfo.SLL_MaxLookEvent;
			if ( decisionInfo.LL_MaxLook>decisionInfo.SLL_MaxLook ) {
				maxLookEvent = decisionInfo.LL_MaxLookEvent;
			}
			firstToken = addDecisionEventHighlighter(previewState, markupModel,
			                                         maxLookEvent,
			                                         DEEPESTLOOK_COLOR,
			                                         EffectType.BOLD_DOTTED_LINE);
		}

		// pred evals
		for (PredicateEvalInfo predEvalInfo : decisionInfo.predicateEvals) {
			Token t = addDecisionEventHighlighter(previewState, markupModel, predEvalInfo, PREDEVAL_COLOR, EffectType.ROUNDED_BOX);
			if ( firstToken==null ) firstToken = t;
		}

		// context-sensitivities
		for (ContextSensitivityInfo ctxSensitivityInfo : decisionInfo.contextSensitivities) {
			Token t = addDecisionEventHighlighter(previewState, markupModel, ctxSensitivityInfo, FULLCTX_COLOR, EffectType.ROUNDED_BOX);
			if ( firstToken==null ) firstToken = t;
		}

		// ambiguities (might overlay context-sensitivities)
		for (AmbiguityInfo ambiguityInfo : decisionInfo.ambiguities) {
			Token t = addDecisionEventHighlighter(previewState, markupModel, ambiguityInfo, AMBIGUITY_COLOR, EffectType.ROUNDED_BOX);
			if ( firstToken==null ) firstToken = t;
		}

		if ( firstToken!=null ) {
			caretModel.moveToOffset(firstToken.getStartIndex());
			scrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE);
		}
	}

	public Token addDecisionEventHighlighter(PreviewState previewState, MarkupModel markupModel,
	                                         DecisionEventInfo info, Color errorStripeColor,
	                                         EffectType effectType) {
		TokenStream tokens = previewState.parsingResult.parser.getInputStream();
		Token startToken = tokens.get(info.startIndex);
		Token stopToken = tokens.get(info.stopIndex);
		TextAttributes textAttributes =
			new TextAttributes(JBColor.BLACK, JBColor.WHITE, errorStripeColor,
			                   effectType, Font.PLAIN);
		textAttributes.setErrorStripeColor(errorStripeColor);
		final RangeHighlighter rangeHighlighter =
			markupModel.addRangeHighlighter(
				startToken.getStartIndex(), stopToken.getStopIndex()+1,
				HighlighterLayer.ADDITIONAL_SYNTAX, textAttributes,
				HighlighterTargetArea.EXACT_RANGE);
		rangeHighlighter.putUserData(DECISION_EVENT_INFO_KEY, info);
		rangeHighlighter.setErrorStripeMarkColor(errorStripeColor);
		return startToken;
	}

	public static String getSemanticContextDisplayString(PredicateEvalInfo pred,
	                                                     PreviewState previewState,
	                                                     SemanticContext semctx,
	                                                     int alt,
	                                                     boolean result) {
		Grammar g = previewState.g;
		String semanticContextDisplayString = g.getSemanticContextDisplayString(semctx);
		if ( semctx instanceof SemanticContext.PrecedencePredicate ) {
			int ruleIndex = previewState.parsingResult.parser.getATN().decisionToState.get(pred.decision).ruleIndex;
			Rule rule = g.getRule(ruleIndex);
			int precedence = ((SemanticContext.PrecedencePredicate) semctx).precedence;
			// precedence = n - originalAlt + 1, So:
			int originalAlt = rule.getOriginalNumberOfAlts()-precedence+1;
			alt = originalAlt;
		}
		return semanticContextDisplayString+" => alt "+alt+" is "+result;
	}

	class ProfileTableCellRenderer extends DefaultTableCellRenderer {
		public Component getTableCellRendererComponent(JTable table, Object value,
		                                               boolean isSelected, boolean hasFocus,
		                                               int row, int column) {
			Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			if ( previewState==null || previewState.parsingResult==null ) {
				return c;
			}
			ParseInfo parseInfo = previewState.parsingResult.parser.getParseInfo();
			int decision = profilerDataTable.convertRowIndexToModel(row);
			DecisionInfo[] decisions = parseInfo.getDecisionInfo();
			if ( decision>=decisions.length ) {
				return c;
			}
			DecisionInfo decisionInfo = decisions[decision];
			if ( decisionInfo.ambiguities.size()>0 ) {
				setForeground(AMBIGUITY_COLOR);
			}
			else if ( decisionInfo.contextSensitivities.size()>0 ) {
				setForeground(FULLCTX_COLOR);
			}
			else if ( decisionInfo.predicateEvals.size()>0 ) {
				setForeground(PREDEVAL_COLOR);
			}
			return c;
		}
	}

	private void createUIComponents() {
		expertCheckBox = new JBCheckBox();
		expertCheckBox.setSelected(false);
		expertCheckBox.addActionListener(e -> {
			if (previewState.parsingResult == null) {
				// nothing has been parsed yet (no text in the editor)
				return;
			}
			ParseInfo parseInfo = previewState.parsingResult.parser.getParseInfo();
			updateTableModelPerExpertCheckBox(parseInfo,previewState.parsingResult.parser);
		});
		profilerDataTable = new ProfilerJBTable();
		ListSelectionModel selectionModel = profilerDataTable.getSelectionModel();
		selectionModel.addListSelectionListener(
				e -> {
					// previewState, project set later
					if ( e.getValueIsAdjusting() ) {
						return; // this seems to be "mouse down" but not mouse up
					}
					// get state for current grammar editor tab
					if ( project==null ) {
						return;
					}
					if ( previewState!=null && profilerDataTable.getModel().getClass()!=DefaultTableModel.class ) {
						int selectedRow = profilerDataTable.getSelectedRow();
						if ( selectedRow==-1 ) {
							selectedRow = 0;
						}
						int decision = profilerDataTable.convertRowIndexToModel(selectedRow);
						int numberOfDecisions = previewState.g.atn.getNumberOfDecisions();
						if ( decision<=numberOfDecisions ) {
							selectDecisionInGrammar(previewState, decision);
							highlightInputPhrases(previewState, decision);
						}
					}
				}
		);
		selectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		ambiguityColorLabel = new JBLabel("Ambiguity");
		ambiguityColorLabel.setForeground(AMBIGUITY_COLOR);
		contextSensitivityColorLabel = new JBLabel("Context sensitivity");
		contextSensitivityColorLabel.setForeground(FULLCTX_COLOR);
		predEvaluationColorLabel = new JBLabel("Predicate evaluation");
		predEvaluationColorLabel.setForeground(PREDEVAL_COLOR);
		deepestLookaheadLabel = new JBLabel("Deepest lookahead");
		deepestLookaheadLabel.setForeground(DEEPESTLOOK_COLOR);
	}
}
