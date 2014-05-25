package org.antlr.intellij.plugin.profiler;

import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.editor.ScrollingModel;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Splitter;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.table.JBTable;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import org.antlr.intellij.plugin.ANTLRv4PluginController;
import org.antlr.intellij.plugin.preview.PreviewState;
import org.antlr.runtime.CommonToken;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.atn.AmbiguityInfo;
import org.antlr.v4.runtime.atn.ContextSensitivityInfo;
import org.antlr.v4.runtime.atn.DecisionEventInfo;
import org.antlr.v4.runtime.atn.DecisionInfo;
import org.antlr.v4.runtime.atn.DecisionState;
import org.antlr.v4.runtime.atn.ParseInfo;
import org.antlr.v4.runtime.atn.PredicateContextEvalInfo;
import org.antlr.v4.runtime.misc.Interval;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

public class ProfilerPanel {
	/* TODO:
	 * TER: context-sens is when SLL fails but LL has no conflict in my book
	 * SLL fail is heuristic though. hmm...
	 *
	 * SAM: At the point of LL fallback, mine makes a note of the conflicting
	 * ATNConfigSet produced by SLL prediction, and then compares that set
	 * to the result provided by LL prediction. If SLL conflict resolution
	 * would not have produced the same result as LL prediction, I report
	 * the decision as context-sensitive.
	 */


	public static final Color AMBIGUITY_COLOR = new Color(138, 0, 0);
	public static final Color FULLCTX_COLOR = new Color(255, 128, 0);
	public static final Color PREDEVAL_COLOR = new Color(110, 139, 61);

	public static final Key<DecisionEventInfo> DECISION_EVENT_INFO_KEY = Key.create("DECISION_EVENT_INFO");

	public Project project;
	public PreviewState previewState;

	protected JPanel outerPanel;
	protected JTextArea inputDisplayPane;
	protected JBTable profilerDataTable;
	protected JPanel statsPanel;
	protected JLabel parseTimeField;
	protected JLabel predictionTimeField;
	protected JLabel lookaheadBurdenField;
	protected JLabel cacheMissRateField;
	protected JLabel inputSizeField;
	protected Splitter splitter;
	protected JLabel numTokensField;
	protected JCheckBox expertCheckBox;
	protected JLabel ambiguityColorLabel;
	protected JLabel contextSensitivityColorLabel;
	protected JLabel predEvaluationColorLabel;

	public JPanel getComponent() {
		return outerPanel;
	}

	public JTextArea getInputDisplayPane() {
		return inputDisplayPane;
	}

	public JBTable getProfilerDataTable() {
		return profilerDataTable;
	}

	public ProfilerPanel(Project project) {
		this.project = project;
	}

	public void setProfilerData(PreviewState previewState,
								long parseTime_ns) {
		this.previewState = previewState;
		Parser parser = previewState.parsingResult.parser;
		ParseInfo parseInfo = parser.getParseInfo();
		updateTableModelPerExpertCheckBox(parseInfo);
		long parseTimeMS = (long) (parseTime_ns / (1000.0 * 1000.0));
		parseTimeField.setText(String.valueOf(parseTimeMS));
		int predTimeMS = (int) (parseInfo.getTotalTimeInPrediction() / (1000.0 * 1000.0));
		predictionTimeField.setText(
			String.format("%d = %3.2f%%", predTimeMS, 100 * ((double) predTimeMS) / parseTimeMS)
		);
		TokenStream tokens = parser.getInputStream();
		int numTokens = tokens.size();
		Token lastToken = tokens.get(numTokens - 1);
		int numChar = lastToken.getStopIndex();
		int numLines = lastToken.getLine();
		if (lastToken.getType() == Token.EOF) {
			if (numTokens <= 1) {
				numLines = 0;
			}
			else {
				Token secondToLastToken = tokens.get(numTokens - 2);
				numLines = secondToLastToken.getLine();
			}
		}
		inputSizeField.setText(String.format("%d char, %d lines",
											 numChar,
											 numLines));
		numTokensField.setText(String.valueOf(numTokens));
		double look = parseInfo.getTotalLookaheadOps();
		lookaheadBurdenField.setText(
			String.format("%d/%d = %3.2f", (long) look, numTokens, look / numTokens)
		);
		double atnLook = parseInfo.getTotalATNLookaheadOps();
		cacheMissRateField.setText(
			String.format("%d/%d = %3.2f%%", (long) atnLook, (long) look, atnLook * 100.0 / look)
		);
	}

	public void updateTableModelPerExpertCheckBox(ParseInfo parseInfo) {
		AbstractTableModel model;
		if (expertCheckBox.isSelected()) {
			model = new ExpertProfilerTableDataModel(parseInfo);
		}
		else {
			model = new SimpleProfilerTableDataModel(parseInfo);
		}
		profilerDataTable.setModel(model);
		profilerDataTable.setRowSorter(new TableRowSorter<AbstractTableModel>(model));
	}

	private void createUIComponents() {
		expertCheckBox = new JBCheckBox();
		expertCheckBox.setSelected(false);
		expertCheckBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ParseInfo parseInfo = previewState.parsingResult.parser.getParseInfo();
				updateTableModelPerExpertCheckBox(parseInfo);
			}
		});
		profilerDataTable = new JBTable() {
			@Override
			protected JTableHeader createDefaultTableHeader() {
				return new JTableHeader(columnModel) {
					public String getToolTipText(MouseEvent e) {
						Point p = e.getPoint();
						int index = columnModel.getColumnIndexAtX(p.x);
						int realIndex = columnModel.getColumn(index).getModelIndex();
						return getModel().getColumnName(realIndex);
					}
				};
			}

			@Override
			public TableCellRenderer getDefaultRenderer(Class<?> columnClass) {
				return new ProfileTableCellRenderer();
			}
		};
		ListSelectionModel selectionModel = profilerDataTable.getSelectionModel();
		selectionModel.addListSelectionListener(
			new ListSelectionListener() {
				@Override
				public void valueChanged(ListSelectionEvent e) {
					// previewState, project set later
					if (e.getValueIsAdjusting()) {
						return; // this seems to be "mouse down" but not mouse up
					}
					// get state for current grammar editor tab
					PreviewState previewState = ANTLRv4PluginController.getInstance(project).getPreviewState();
					if (previewState != null && project != null) {
						int selectedRow = profilerDataTable.getSelectedRow();
						if (selectedRow == -1) {
							selectedRow = 0;
						}
						int decision = profilerDataTable.convertRowIndexToModel(selectedRow);
						int numberOfDecisions = previewState.g.atn.getNumberOfDecisions();
						if (decision <= numberOfDecisions) {
							selectDecisionInGrammar(previewState, decision);
							highlightPhrases(previewState, decision);
						}
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
	}

	public void switchToGrammar(PreviewState previewState, VirtualFile grammarFile) {
		DefaultTableModel model = new DefaultTableModel();
		profilerDataTable.setModel(model);
		profilerDataTable.setRowSorter(new TableRowSorter<AbstractTableModel>(model));
	}

	public void selectDecisionInGrammar(PreviewState previewState, int decision) {
		ANTLRv4PluginController controller = ANTLRv4PluginController.getInstance(project);
		Editor grammarEditor = controller.getCurrentGrammarEditor();

		DecisionState decisionState = previewState.g.atn.getDecisionState(decision);
		Interval region = previewState.g.getStateToGrammarRegion(decisionState.stateNumber);
		if (region == null) {
			System.err.println("decision " + decision + " has state " + decisionState.stateNumber + " but no region");
			return;
		}
		SelectionModel selectionModel = grammarEditor.getSelectionModel();
		org.antlr.runtime.TokenStream tokens = previewState.g.tokenStream;
		if (region.a >= tokens.size() || region.b >= tokens.size()) {
//			System.out.println("out of range: " + region + " tokens.size()=" + tokens.size());
			return;
		}
		CommonToken startToken = (CommonToken) tokens.get(region.a);
		CommonToken stopToken = (CommonToken) tokens.get(region.b);
		selectionModel.setSelection(startToken.getStartIndex(), stopToken.getStopIndex() + 1);

//		System.out.println("dec " + decision + " from " + startToken + " to " + stopToken);

		ScrollingModel scrollingModel = grammarEditor.getScrollingModel();
		CaretModel caretModel = grammarEditor.getCaretModel();
		caretModel.moveToOffset(startToken.getStartIndex());
		scrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE);

		Editor editor = previewState.getEditor();
		MarkupModel markupModel = editor.getMarkupModel();
		markupModel.removeAllHighlighters();
	}

	public void highlightPhrases(PreviewState previewState, int decision) {
		if (previewState.parsingResult == null) {
			return;
		}
		ParseInfo parseInfo = previewState.parsingResult.parser.getParseInfo();
		DecisionInfo decisionInfo = parseInfo.getDecisionInfo()[decision];
		if (decisionInfo.ambiguities.size() == 0 &&
			decisionInfo.contextSensitivities.size() == 0 &&
			decisionInfo.predicateEvals.size() == 0) {
			return;
		}

		Editor editor = previewState.getEditor();
		SelectionModel selectionModel = editor.getSelectionModel();
		ScrollingModel scrollingModel = editor.getScrollingModel();
		CaretModel caretModel = editor.getCaretModel();
		MarkupModel markupModel = editor.getMarkupModel();

		// context-sensitivities
		Token firstToken = null, lastToken = null;
		for (ContextSensitivityInfo ctxSensitivityInfo : decisionInfo.contextSensitivities) {
			TokenStream tokens = previewState.parsingResult.parser.getInputStream();
			Token startToken = tokens.get(ctxSensitivityInfo.startIndex);
			if (firstToken == null) {
				firstToken = startToken;
			}
			Token stopToken = tokens.get(ctxSensitivityInfo.stopIndex);
			lastToken = stopToken;
//            selectionModel.setSelection(startToken.getStartIndex(), stopToken.getStopIndex() + 1);

			// ambiguities
			TextAttributes textAttributes =
				new TextAttributes(JBColor.BLACK, JBColor.WHITE, FULLCTX_COLOR, EffectType.WAVE_UNDERSCORE, 1);
			textAttributes.setErrorStripeColor(FULLCTX_COLOR);
			final RangeHighlighter rangeHighlighter =
				markupModel.addRangeHighlighter(
					startToken.getStartIndex(), stopToken.getStopIndex() + 1,
					HighlighterLayer.WARNING, textAttributes,
					HighlighterTargetArea.EXACT_RANGE);
			rangeHighlighter.putUserData(DECISION_EVENT_INFO_KEY, ctxSensitivityInfo);
			rangeHighlighter.setErrorStripeMarkColor(FULLCTX_COLOR);
		}

		// pred evals
		for (PredicateContextEvalInfo predEvalInfo : decisionInfo.predicateEvals) {
			TokenStream tokens = previewState.parsingResult.parser.getInputStream();
			Token startToken = tokens.get(predEvalInfo.startIndex);
			if (firstToken == null) {
				firstToken = startToken;
			}
			Token stopToken = tokens.get(predEvalInfo.stopIndex);
			lastToken = stopToken;

			// ambiguities
			TextAttributes textAttributes =
				new TextAttributes(JBColor.BLACK, JBColor.WHITE, PREDEVAL_COLOR, EffectType.WAVE_UNDERSCORE, 1);
			textAttributes.setErrorStripeColor(PREDEVAL_COLOR);
			final RangeHighlighter rangeHighlighter =
				markupModel.addRangeHighlighter(
					startToken.getStartIndex(), stopToken.getStopIndex() + 1,
					HighlighterLayer.ADDITIONAL_SYNTAX, textAttributes,
					HighlighterTargetArea.EXACT_RANGE);
			rangeHighlighter.putUserData(DECISION_EVENT_INFO_KEY, predEvalInfo);
			rangeHighlighter.setErrorStripeMarkColor(PREDEVAL_COLOR);
		}

		// ambiguities (might overlay context-sensitivities)
		for (AmbiguityInfo ambiguityInfo : decisionInfo.ambiguities) {
			TokenStream tokens = previewState.parsingResult.parser.getInputStream();
			Token startToken = tokens.get(ambiguityInfo.startIndex);
			if (firstToken == null) {
				firstToken = startToken;
			}
			Token stopToken = tokens.get(ambiguityInfo.stopIndex);
			lastToken = stopToken;
//            selectionModel.setSelection(startToken.getStartIndex(), stopToken.getStopIndex() + 1);

			TextAttributes textAttributes =
				new TextAttributes(JBColor.BLACK, JBColor.WHITE, AMBIGUITY_COLOR, EffectType.WAVE_UNDERSCORE, 1);
			textAttributes.setErrorStripeColor(AMBIGUITY_COLOR);
			final RangeHighlighter rangeHighlighter =
				markupModel.addRangeHighlighter(
					startToken.getStartIndex(), stopToken.getStopIndex() + 1,
					HighlighterLayer.ERROR, textAttributes,
					HighlighterTargetArea.EXACT_RANGE);
			rangeHighlighter.putUserData(DECISION_EVENT_INFO_KEY, ambiguityInfo);
			rangeHighlighter.setErrorStripeMarkColor(AMBIGUITY_COLOR);
		}

		if (firstToken != null && lastToken != null) {
//			rangeHighlighter.setLineMarkerRenderer(
//				new LineMarkerRenderer() {
//					@Override
//					public void paint(Editor editor, Graphics g, Rectangle r) {
//						// draws left gutter range like for brace highlighting
//						final EditorGutterComponentEx gutter = ((EditorEx) editor).getGutterComponentEx();
//						g.setColor(AMBIGUITY_COLOR);
//
//						final int endX = gutter.getWhitespaceSeparatorOffset();
//						final int x = r.x + r.width - 5;
//						final int width = endX - x;
//						if (r.height > 0) {
//							g.fillRect(x, r.y, width, r.height);
//							g.setColor(gutter.getOutlineColor(false));
//							UIUtil.drawLine(g, x, r.y, x + width, r.y);
//							UIUtil.drawLine(g, x, r.y, x, r.y + r.height - 1);
//							UIUtil.drawLine(g, x, r.y + r.height - 1, x + width, r.y + r.height - 1);
//						}
//						else {
//							final int[] xPoints = new int[]{x,
//								x,
//								x + width - 1};
//							final int[] yPoints = new int[]{r.y - 4,
//								r.y + 4,
//								r.y};
//							g.fillPolygon(xPoints, yPoints, 3);
//
//							g.setColor(gutter.getOutlineColor(false));
//							g.drawPolygon(xPoints, yPoints, 3);
//						}
//					}
//				}
//			);

//			HighlightManager.getInstance(project).addRangeHighlight(
//				editor,
//				firstToken.getStartIndex(),
//				firstToken.getStopIndex() + 1,
//				textAttributes,
//				false,
//				false,
//				null);

//            HighlightManager.getInstance(project).addOccurrenceHighlight(
//                    editor,
//                    firstToken.getStartIndex(),
//                    firstToken.getStopIndex() + 1,
//                    textAttributes,
//                    HighlightManager.HIDE_BY_TEXT_CHANGE,
//                    new ArrayList<RangeHighlighter>() {{
//                        add(rangeHighlighter);
//                    }},
//                    AMBIGUITY_COLOR
//            );

			caretModel.moveToOffset(firstToken.getStartIndex());
			scrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE);
		}

	}

	{
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
		$$$setupUI$$$();
	}

	/**
	 * Method generated by IntelliJ IDEA GUI Designer
	 * >>> IMPORTANT!! <<<
	 * DO NOT edit this method OR call it in your code!
	 *
	 * @noinspection ALL
	 */
	private void $$$setupUI$$$() {
		createUIComponents();
		outerPanel = new JPanel();
		outerPanel.setLayout(new BorderLayout(0, 0));
		splitter = new Splitter();
		splitter.setLayout(new GridBagLayout());
		splitter.setProportion(0.7f);
		outerPanel.add(splitter, BorderLayout.CENTER);
		final JScrollPane scrollPane1 = new JScrollPane();
		GridBagConstraints gbc;
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		splitter.add(scrollPane1, gbc);
		profilerDataTable.setPreferredScrollableViewportSize(new Dimension(800, 400));
		scrollPane1.setViewportView(profilerDataTable);
		statsPanel = new JPanel();
		statsPanel.setLayout(new GridLayoutManager(12, 3, new Insets(0, 5, 0, 0), -1, -1));
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		splitter.add(statsPanel, gbc);
		final JLabel label1 = new JLabel();
		label1.setText("Parse time (ms):");
		statsPanel.add(label1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(130, 16), null, 0, false));
		final JLabel label2 = new JLabel();
		label2.setText("Prediction time (ms):");
		statsPanel.add(label2, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(130, 16), null, 0, false));
		final JLabel label3 = new JLabel();
		label3.setText("Lookahead burden:");
		statsPanel.add(label3, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(130, 16), null, 0, false));
		final JLabel label4 = new JLabel();
		label4.setText("DFA cache miss rate:");
		statsPanel.add(label4, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(130, 16), null, 0, false));
		final Spacer spacer1 = new Spacer();
		statsPanel.add(spacer1, new GridConstraints(11, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(-1, 14), null, 0, false));
		final Spacer spacer2 = new Spacer();
		statsPanel.add(spacer2, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
		parseTimeField = new JLabel();
		parseTimeField.setText("0");
		statsPanel.add(parseTimeField, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		predictionTimeField = new JLabel();
		predictionTimeField.setText("0");
		statsPanel.add(predictionTimeField, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		lookaheadBurdenField = new JLabel();
		lookaheadBurdenField.setText("0");
		statsPanel.add(lookaheadBurdenField, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		cacheMissRateField = new JLabel();
		cacheMissRateField.setText("0");
		statsPanel.add(cacheMissRateField, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		final JLabel label5 = new JLabel();
		label5.setText("Input size:");
		statsPanel.add(label5, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(130, 16), null, 0, false));
		inputSizeField = new JLabel();
		inputSizeField.setText("0");
		statsPanel.add(inputSizeField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		final JLabel label6 = new JLabel();
		label6.setText("Number of tokens:");
		statsPanel.add(label6, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		numTokensField = new JLabel();
		numTokensField.setText("0");
		statsPanel.add(numTokensField, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		expertCheckBox.setText("Show expert columns");
		statsPanel.add(expertCheckBox, new GridConstraints(7, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		ambiguityColorLabel.setText("Ambiguity");
		statsPanel.add(ambiguityColorLabel, new GridConstraints(8, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		contextSensitivityColorLabel.setText("Context-sensitivity");
		statsPanel.add(contextSensitivityColorLabel, new GridConstraints(9, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		predEvaluationColorLabel.setText("Predicate evaluation");
		statsPanel.add(predEvaluationColorLabel, new GridConstraints(10, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		splitter.setFirstComponent(scrollPane1);
		splitter.setSecondComponent(statsPanel);
	}

	/**
	 * @noinspection ALL
	 */
	public JComponent $$$getRootComponent$$$() {
		return outerPanel;
	}

	class ProfileTableCellRenderer extends DefaultTableCellRenderer {
		public Component getTableCellRendererComponent(JTable table, Object value,
													   boolean isSelected, boolean hasFocus,
													   int row, int column) {
			Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			if (previewState == null || previewState.parsingResult == null) {
				return c;
			}
			ParseInfo parseInfo = previewState.parsingResult.parser.getParseInfo();
			int decision = profilerDataTable.convertRowIndexToModel(row);
			DecisionInfo[] decisions = parseInfo.getDecisionInfo();
			if (decision >= decisions.length) {
				return c;
			}
			DecisionInfo decisionInfo = decisions[decision];
			if (decisionInfo.ambiguities.size() > 0) {
				setForeground(AMBIGUITY_COLOR);
			}
			else if (decisionInfo.contextSensitivities.size() > 0) {
				setForeground(FULLCTX_COLOR);
			}
			else if (decisionInfo.predicateEvals.size() > 0) {
				setForeground(PREDEVAL_COLOR);
			}
			return c;
		}
	}
}
