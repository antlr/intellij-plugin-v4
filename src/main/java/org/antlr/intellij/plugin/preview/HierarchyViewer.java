package org.antlr.intellij.plugin.preview;

import com.intellij.ui.components.JBScrollPane;
import org.antlr.intellij.plugin.Icons;
import org.antlr.v4.gui.TreeTextProvider;
import org.antlr.v4.gui.TreeViewer;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.tree.Tree;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * A Preview sub-tab that displays a hierarchical tree of matched parser rules.
 */
class HierarchyViewer extends JPanel implements TreeSelectionListener {

	private final JTree myTree = new com.intellij.ui.treeStructure.Tree();
	private final List<ParsingResultSelectionListener> selectionListeners = new ArrayList<>();

	private TreeTextProvider treeTextProvider;

	HierarchyViewer(Tree tree) {
		setupComponents();
		setupTree(tree);
	}

	/**
	 * Registers a new rule selection listener.
	 */
	public void addParsingResultSelectionListener(ParsingResultSelectionListener listener) {
		selectionListeners.add(listener);
	}

	private void setupComponents() {
		setLayout(new BorderLayout(0, 0));

		JScrollPane scrollPane = new JBScrollPane(myTree);
		add(scrollPane, BorderLayout.CENTER);
	}

	private void setupTree(Tree tree) {
		setTree(tree);

		DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
		renderer.setOpenIcon(Icons.PARSER_RULE);
		renderer.setClosedIcon(Icons.PARSER_RULE);
		renderer.setLeafIcon(Icons.LEXER_RULE);
		myTree.setCellRenderer(renderer);
		myTree.addTreeSelectionListener(this);
	}

	public void setTree(Tree tree) {
		myTree.setModel(new DefaultTreeModel(wrap(tree), false));
	}

	public void setRuleNames(List<String> ruleNames) {
		treeTextProvider = new TreeViewer.DefaultTreeTextProvider(ruleNames);
	}

	public void setTreeTextProvider(TreeTextProvider treeTextProvider) {
		this.treeTextProvider = treeTextProvider;
	}

	private MutableTreeNode wrap(final Tree tree) {
		if ( tree==null ) {
			return null;
		}
		DefaultMutableTreeNode root = new DefaultMutableTreeNode(tree) {
			@Override
			public String toString() {
				return treeTextProvider.getText((Tree) getUserObject());
			}


		};

		for ( int i = 0; i<tree.getChildCount(); i++ ) {
			root.add(wrap(tree.getChild(i)));
		}
		return root;
	}

	public void selectNodeAtOffset(int offset) {
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) myTree.getModel().getRoot();
		if ( root==null ) {
			return; // probably because the grammar is not valid
		}
		Tree tree = (Tree) root.getUserObject();

		if ( tree instanceof ParseTree ) {
			DefaultMutableTreeNode atOffset = getNodeAtOffset(root, offset);

			if ( atOffset!=null ) {
				TreePath path = new TreePath(atOffset.getPath());
				myTree.getSelectionModel().setSelectionPath(path);
				myTree.scrollPathToVisible(path);
			}
		}
	}

	@Nullable
	private DefaultMutableTreeNode getNodeAtOffset(DefaultMutableTreeNode node, int offset) {
		Tree tree = (Tree) node.getUserObject();

		if ( tree instanceof ParserRuleContext ctx ) {
			if ( inBounds(ctx, offset) ) {
				for ( int i = 0; i<node.getChildCount(); i++ ) {
					DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
					DefaultMutableTreeNode atOffset = getNodeAtOffset(child, offset);

					if ( atOffset!=null ) {
						return atOffset;
					}
				}
				// None of the children match, so it must be this node
				return node;
			}
		}
		else if ( tree instanceof TerminalNode terminal ) {
			if ( terminal.getSymbol().getStartIndex()<=offset && terminal.getSymbol().getStopIndex()>=offset ) {
				return node;
			}
		}

		return null;
	}

	private boolean inBounds(ParserRuleContext ctx, int offset) {
		Token start = ctx.getStart();
		Token stop = ctx.getStop();
		if ( start!=null && stop!=null ) {
			return start.getStartIndex()<=offset && stop.getStopIndex()>=offset;
		}
		return false;
	}

	/**
	 * Fired when a rule is selected in the tree to highlight the corresponding text in the input editor.
	 */
	@Override
	public void valueChanged(TreeSelectionEvent e) {
		AWTEvent currentEvent = EventQueue.getCurrentEvent();

		if ( currentEvent == null || !(currentEvent.getSource() instanceof com.intellij.ui.treeStructure.Tree) )	{
			// Do not try to highlight input unless we got a mouse event in the hierarchy viewer
			// otherwise it selects entire token when you click in the input pane. E.g., an
			// entire string or keyword when you're trying to click-n-edit in input pane.
			return;
		}

		TreePath path = e.getPath();
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
		Tree tree = (Tree) node.getUserObject();

		for ( ParsingResultSelectionListener listener : selectionListeners ) {
			listener.onParserRuleSelected(tree);
		}
	}
}
