package org.antlr.intellij.plugin.preview;

import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.PopupChooserBuilder;
import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.UIUtil;
import org.antlr.intellij.plugin.Utils;
import org.antlr.intellij.plugin.parsing.ParsingUtils;
import org.antlr.intellij.plugin.parsing.PreviewInterpreterRuleContext;
import org.antlr.v4.gui.TreeViewer;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.AmbiguityInfo;
import org.antlr.v4.runtime.atn.LookaheadEventInfo;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.tree.Tree;
import org.antlr.v4.tool.GrammarParserInterpreter;

import javax.swing.*;
import java.awt.*;
import java.util.List;

import static java.util.Collections.singletonList;

public class ShowAmbigTreesDialog extends JDialog {
	private static final int MAX_PHRASE_WIDTH = 25;

	private JPanel contentPane;
	private JButton buttonOK;
	private JScrollPane treeScrollPane;
	private JSlider treeSizeSlider;
	private JLabel ambigPhraseLabel;
	private TreeViewer[] treeViewers;

	public ShowAmbigTreesDialog() {
		setContentPane(contentPane);
		setModal(false);
		getRootPane().setDefaultButton(buttonOK);

		buttonOK.addActionListener(e -> dispose());

		treeSizeSlider.addChangeListener(e -> {
			int v = ((JSlider) e.getSource()).getValue();
			setScale(v / 1000.0 + 1.0);
		});
	}

	public static JBPopup createAmbigTreesPopup(final PreviewState previewState,
	                                            final AmbiguityInfo ambigInfo) {
		final JBList list = new JBList("Show all phrase interpretations");
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JBPopupFactory factory = JBPopupFactory.getInstance();
		PopupChooserBuilder builder = factory.createListPopupBuilder(list);
		builder.setItemChoosenCallback(() -> popupAmbigTreesDialog(previewState, ambigInfo));
		return builder.createPopup();
	}

	public static void popupAmbigTreesDialog(PreviewState previewState, AmbiguityInfo ambigInfo) {
		// pop up subtrees for ambig intrepretation
		ShowAmbigTreesDialog dialog = new ShowAmbigTreesDialog();
		Parser parser = previewState.parsingResult.parser;
		int startRuleIndex = parser.getRuleIndex(previewState.startRuleName);
		List<ParserRuleContext> ambiguousParseTrees = null;
		try {
			ambiguousParseTrees =
				GrammarParserInterpreter.getAllPossibleParseTrees(previewState.g,
				                                                  parser,
				                                                  parser.getTokenStream(),
				                                                  ambigInfo.decision,
				                                                  ambigInfo.ambigAlts,
				                                                  ambigInfo.startIndex,
				                                                  ambigInfo.stopIndex,
				                                                  startRuleIndex);
		} catch (ParseCancellationException pce) {
			// should be no errors for ambiguities, unless original
			// input itself has errors. Just display error in this case.
			JBPanel errPanel = new JBPanel(new BorderLayout());
			errPanel.add(new JBLabel("Cannot display ambiguous trees while there are syntax errors in your input."));
			dialog.treeScrollPane.setViewportView(errPanel);
		}

		if ( ambiguousParseTrees!=null ) {
			TokenStream tokens = previewState.parsingResult.parser.getInputStream();
			String phrase = tokens.getText(Interval.of(ambigInfo.startIndex, ambigInfo.stopIndex));
			if ( phrase.length()>MAX_PHRASE_WIDTH ) {
				phrase = phrase.substring(0, MAX_PHRASE_WIDTH)+"...";
			}
			String title = ambiguousParseTrees.size()+
				" Interpretations of Ambiguous Input Phrase: "+
				phrase;
			dialog.ambigPhraseLabel.setText(title);
			dialog.setTrees(previewState, ambiguousParseTrees, title, 0, true);
		}

		dialog.pack();
		dialog.setVisible(true);
	}

	public static JBPopup createLookaheadTreesPopup(final PreviewState previewState,
	                                                final LookaheadEventInfo lookaheadInfo) {
		final JBList list = new JBList("Show all lookahead interpretations");
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JBPopupFactory factory = JBPopupFactory.getInstance();
		PopupChooserBuilder builder = factory.createListPopupBuilder(list);
		builder.setItemChoosenCallback(() -> popupLookaheadTreesDialog(previewState, lookaheadInfo));

		return builder.createPopup();
	}

	public static void popupLookaheadTreesDialog(PreviewState previewState, LookaheadEventInfo lookaheadInfo) {
		// pop up subtrees for lookahead
		ShowAmbigTreesDialog dialog = new ShowAmbigTreesDialog();
		ParserInterpreter parser = (ParserInterpreter) previewState.parsingResult.parser;
		int startRuleIndex = parser.getRuleIndex(previewState.startRuleName);
		List<ParserRuleContext> lookaheadParseTrees =
			GrammarParserInterpreter.getLookaheadParseTrees(previewState.g,
			                                                parser,
			                                                parser.getTokenStream(),
			                                                startRuleIndex,
			                                                lookaheadInfo.decision,
			                                                lookaheadInfo.startIndex,
			                                                lookaheadInfo.stopIndex);
		if ( parser.getNumberOfSyntaxErrors()>0 ) {
			// should be no errors for ambiguities, unless original
			// input itself has errors. Just display error in this case.
			JBPanel errPanel = new JBPanel(new BorderLayout());
			errPanel.add(new JBLabel("Cannot display lookahead trees while there are syntax errors in your input."));
			dialog.treeScrollPane.setViewportView(errPanel);
			lookaheadParseTrees = null;
		}
		if ( lookaheadParseTrees!=null ) {
			Interval range = Interval.of(lookaheadInfo.startIndex, lookaheadInfo.stopIndex);
			String phrase = parser.getTokenStream().getText(range);
			if ( phrase.length()>MAX_PHRASE_WIDTH ) {
				phrase = phrase.substring(0, MAX_PHRASE_WIDTH)+"...";
			}
			String title = lookaheadParseTrees.size()+
				" Interpretations of Lookahead Phrase: "+
				phrase;
			dialog.ambigPhraseLabel.setText(title);
			dialog.setTrees(previewState, lookaheadParseTrees, title, lookaheadInfo.predictedAlt-1, false);
		}
		dialog.pack();
		dialog.setVisible(true);
	}

	public void setScale(double scale) {
		if ( treeViewers==null ) return;
		for (TreeViewer viewer : treeViewers) {
			viewer.setScale(scale);
		}
		treeScrollPane.revalidate();
	}

	public void setTrees(PreviewState previewState,
						 List<? extends RuleContext> ambiguousParseTrees,
						 String title,
						 int highlightTreeIndex,
						 boolean highlightDiffs) {
		if ( ambiguousParseTrees!=null ) {
			int numTrees = ambiguousParseTrees.size();
			setTitle(title);
			treeViewers = new TreeViewer[numTrees];
			JBPanel panelOfTrees = new JBPanel();
			PreviewInterpreterRuleContext chosenTree =
				(PreviewInterpreterRuleContext) ambiguousParseTrees.get(highlightTreeIndex);
			panelOfTrees.setLayout(new BoxLayout(panelOfTrees, BoxLayout.X_AXIS));
			for (int i = 0; i<numTrees; i++) {
				if ( i>0 ) {
					panelOfTrees.add(new JSeparator(JSeparator.VERTICAL));
				}
				PreviewInterpreterRuleContext ctx = (PreviewInterpreterRuleContext) ambiguousParseTrees.get(i);
				treeViewers[i] = new TrackpadZoomingTreeView(null, null, highlightDiffs); // && ctx != chosenTree);
				AltLabelTextProvider treeText =
					new AltLabelTextProvider(previewState.parsingResult.parser, previewState.g);
				treeViewers[i].setTreeTextProvider(treeText);
				treeViewers[i].setTree(ctx);
				treeViewers[i].setHighlightedBoxColor(new JBColor(JBColor.lightGray, JBColor.GREEN));

				// highlight root so people can see it across trees; might not be top node
				treeViewers[i].addHighlightedNodes(singletonList(ParsingUtils.findOverriddenDecisionRoot(ctx)));
				if ( ctx!=chosenTree ) {
					mark(chosenTree, ctx);
				}
				JBPanel wrapper = new JBPanel(new BorderLayout());
				if ( i==highlightTreeIndex ) {
					wrapper.setBackground(JBColor.white);
				}
				else if ( UIUtil.isUnderDarcula() ) {
					wrapper.setBackground(Gray._43);
				}
				wrapper.add(treeViewers[i], BorderLayout.CENTER);
				panelOfTrees.add(wrapper);
			}

			// Wrap tree viewer components in scroll pane
			treeScrollPane.setViewportView(panelOfTrees);
		}
	}

	/**
	 * Given two trees, t and u, compare them starting from the leaves and the decision root
	 * Tree t is considered the "truth" and we are comparing u to it. That
	 * means u might contain fewer in-range leaves. t's leaves should be
	 * start..stop indexes.
	 */
	public static void mark(final PreviewInterpreterRuleContext t,
							final PreviewInterpreterRuleContext u) {
		// First mark from roots down
		markFromRoots(t, u);

		// Get leaves so we can do a difference between the trees starting at the bottom and top
		List<TerminalNode> tleaves = ParsingUtils.getAllLeaves(t);
		List<TerminalNode> uleaves = ParsingUtils.getAllLeaves(u);

		int firstTleafTokenIndex = tleaves.isEmpty() ? -1 : tleaves.get(0).getSymbol().getTokenIndex();
		int firstUleafTokenIndex = uleaves.isEmpty() ? -1 : uleaves.get(0).getSymbol().getTokenIndex();
		final int first = Math.max(firstTleafTokenIndex, firstUleafTokenIndex);

		// filter so we start in same place
		tleaves = Utils.filter(tleaves, tree -> ((Token) tree.getPayload()).getTokenIndex()>=first);
		uleaves = Utils.filter(uleaves, tree -> ((Token) tree.getPayload()).getTokenIndex()>=first);
		int n = Math.min(tleaves.size(), uleaves.size());
		for (int i = 0; i<n; i++) { // for each leaf in t and u
			Tree tleaf = tleaves.get(i);
			Tree uleaf = uleaves.get(i);
			List<? extends Tree> tancestors = ParsingUtils.getAncestors(tleaf);
			List<? extends Tree> uancestors = ParsingUtils.getAncestors(uleaf);
			int a = 0;
			int nta = tancestors.size();
			int nua = uancestors.size();
			PreviewInterpreterRuleContext tancestor;
			PreviewInterpreterRuleContext uancestor;
			while ( a<nta && a<nua ) { // walk ancestor chain for each leaf until not equal
				tancestor = (PreviewInterpreterRuleContext) tancestors.get(a);
				uancestor = (PreviewInterpreterRuleContext) uancestors.get(a);
				if ( !tancestor.equals(uancestor) ) break;
				tancestor.reached = true;
				uancestor.reached = true;
				a++;
			}
		}
	}

	public static void markFromRoots(final PreviewInterpreterRuleContext t, final PreviewInterpreterRuleContext u) {
		if ( t==null || u==null ) return;
		if ( !t.equals(u) ) return;
		t.reached = true;
		u.reached = true;
		int n = Math.min(t.getChildCount(), u.getChildCount());
		for (int i = 0; i<n; i++) { // for each leaf of t and u
			Tree tchild = t.getChild(i);
			Tree uchild = u.getChild(i);
			if ( !tchild.equals(uchild) ) {
				return; // don't consider other kids if ith doesn't match
			}
			if ( tchild instanceof PreviewInterpreterRuleContext &&
				uchild instanceof PreviewInterpreterRuleContext ) {
				markFromRoots((PreviewInterpreterRuleContext) tchild,
				              (PreviewInterpreterRuleContext) uchild);
			}
			else {
				return; // mismatched kids. should be caught above but...
			}
		}
	}

}
