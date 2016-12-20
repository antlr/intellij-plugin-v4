package org.antlr.intellij.plugin.preview;

import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.PopupChooserBuilder;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBPanel;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.intellij.util.containers.Predicate;
import org.antlr.intellij.plugin.Utils;
import org.antlr.intellij.plugin.parsing.ParsingUtils;
import org.antlr.intellij.plugin.parsing.PreviewInterpreterRuleContext;
import org.antlr.v4.gui.TreeViewer;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserInterpreter;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.atn.AmbiguityInfo;
import org.antlr.v4.runtime.atn.LookaheadEventInfo;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.tree.Tree;
import org.antlr.v4.tool.GrammarParserInterpreter;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

public class ShowAmbigTreesDialog extends JDialog {
	public static final int MAX_PHRASE_WIDTH = 25;
	private JPanel contentPane;
	private JButton buttonOK;
	protected JScrollPane treeScrollPane;
	protected JSlider treeSizeSlider;
	protected JLabel ambigPhraseLabel;
	public List<? extends RuleContext> ambiguousParseTrees;
	public TreeViewer[] treeViewers;
	public PreviewState previewState;

	public ShowAmbigTreesDialog() {
		$$$setupUI$$$();
		setContentPane(contentPane);
		setModal(false);
		getRootPane().setDefaultButton(buttonOK);

		buttonOK.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onOK();
			}
		});

		treeSizeSlider.addChangeListener(
			new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					int v = ((JSlider) e.getSource()).getValue();
					setScale(v/1000.0+1.0);
				}
			});
	}

	public static JBPopup createAmbigTreesPopup(final PreviewState previewState,
	                                            final AmbiguityInfo ambigInfo) {
		final JBList list = new JBList("Show all phrase interpretations");
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JBPopupFactory factory = JBPopupFactory.getInstance();
		PopupChooserBuilder builder = factory.createListPopupBuilder(list);
		builder.setItemChoosenCallback(
			new Runnable() {
				@Override
				public void run() {
					popupAmbigTreesDialog(previewState, ambigInfo);
				}
			}
		                              );
		JBPopup popup = builder.createPopup();
		return popup;
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
			dialog.setTrees(previewState, ambiguousParseTrees, title, 0,
			                ambigInfo.startIndex, ambigInfo.stopIndex, true);
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
		builder.setItemChoosenCallback(
			new Runnable() {
				@Override
				public void run() {
					popupLookaheadTreesDialog(previewState, lookaheadInfo);
				}
			}
		                              );

		JBPopup popup = builder.createPopup();
		return popup;
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
			dialog.setTrees(previewState, lookaheadParseTrees, title, lookaheadInfo.predictedAlt-1,
			                lookaheadInfo.startIndex, lookaheadInfo.stopIndex, false);
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
	                     List<? extends RuleContext> trees,
	                     String title,
	                     int highlightTreeIndex,
	                     int startIndex,
	                     int stopIndex,
	                     boolean highlightDiffs) {
		this.previewState = previewState;
		this.ambiguousParseTrees = trees;
		if ( ambiguousParseTrees!=null ) {
			int numTrees = ambiguousParseTrees.size();
			setTitle(title);
			treeViewers = new TreeViewer[ambiguousParseTrees.size()];
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
				// highlight root so people can see it across trees; might not be top node
				final Tree root = ParsingUtils.findOverriddenDecisionRoot(ctx);
				if ( root==null ) {
					// I saw this when a (...)+ exit branch was taken here
//					declarationSpecifiers
//					    :   declarationSpecifier+
//					    ;
					// TODO: display a message?
				}
				treeViewers[i].addHighlightedNodes(new ArrayList<Tree>() {{
					add(root);
				}});
				if ( ctx!=chosenTree ) {
					mark(chosenTree, ctx, startIndex, stopIndex);
				}
				JBPanel wrapper = new JBPanel(new BorderLayout());
				if ( i==highlightTreeIndex ) {
					wrapper.setBackground(JBColor.white);
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
	                        final PreviewInterpreterRuleContext u,
	                        final int startIndex,
	                        final int stopIndex) {
		// First mark from roots down
		markFromRoots(t, u);

		// Get leaves so we can do a difference between the trees starting at the bottom and top
		List<Tree> tleaves = ParsingUtils.getAllLeaves(t);
		List<Tree> uleaves = ParsingUtils.getAllLeaves(u);

		TerminalNode first_tleaf = (TerminalNode) tleaves.get(0);
		TerminalNode first_uleaf = (TerminalNode) uleaves.get(0);
		final int first = Math.max(first_tleaf.getSymbol().getTokenIndex(),
		                           first_uleaf.getSymbol().getTokenIndex());

		// filter so we start in same place
		tleaves = Utils.filter(tleaves,
		                       new Predicate<Tree>() {
			                       @Override
			                       public boolean apply(Tree t) {
				                       return ((Token) t.getPayload()).getTokenIndex()>=first;
			                       }
		                       });
		uleaves = Utils.filter(uleaves,
		                       new Predicate<Tree>() {
			                       @Override
			                       public boolean apply(Tree t) {
				                       return ((Token) t.getPayload()).getTokenIndex()>=first;
			                       }
		                       });
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
//				if (tancestor == t || uancestor == u)
//					break; // stop if we hit incoming root nodes
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

	private void onOK() {
// add your code here
		dispose();
	}

	/**
	 * Method generated by IntelliJ IDEA GUI Designer
	 * >>> IMPORTANT!! <<<
	 * DO NOT edit this method OR call it in your code!
	 *
	 * @noinspection ALL
	 */
	private void $$$setupUI$$$() {
		contentPane = new JPanel();
		contentPane.setLayout(new GridLayoutManager(3, 1, new Insets(10, 10, 10, 10), -1, -1));
		final JPanel panel1 = new JPanel();
		panel1.setLayout(new GridLayoutManager(1, 4, new Insets(0, 0, 0, 0), -1, -1));
		contentPane.add(panel1, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK|GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
		final Spacer spacer1 = new Spacer();
		panel1.add(spacer1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
		final JPanel panel2 = new JPanel();
		panel2.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
		panel1.add(panel2, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK|GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK|GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
		buttonOK = new JButton();
		buttonOK.setText("OK");
		panel2.add(buttonOK, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK|GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		ambigPhraseLabel = new JLabel();
		ambigPhraseLabel.setFont(new Font(ambigPhraseLabel.getFont().getName(), Font.BOLD, ambigPhraseLabel.getFont().getSize()));
		ambigPhraseLabel.setText("ambiguity");
		panel1.add(ambigPhraseLabel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		final Spacer spacer2 = new Spacer();
		panel1.add(spacer2, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
		treeScrollPane = new JScrollPane();
		contentPane.add(treeScrollPane, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK|GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK|GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
		treeSizeSlider = new JSlider();
		treeSizeSlider.setMaximum(1000);
		treeSizeSlider.setMinimum(-400);
		treeSizeSlider.setValue(0);
		contentPane.add(treeSizeSlider, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
	}

	/**
	 * @noinspection ALL
	 */
	public JComponent $$$getRootComponent$$$() {
		return contentPane;
	}
}
