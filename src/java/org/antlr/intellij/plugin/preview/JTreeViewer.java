package org.antlr.intellij.plugin.preview;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.editor.Editor;
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
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import static com.intellij.icons.AllIcons.Actions.Find;
import static com.intellij.icons.AllIcons.General.AutoscrollFromSource;
import static org.antlr.intellij.plugin.ANTLRv4PluginController.PREVIEW_WINDOW_ID;

public class JTreeViewer extends JPanel {

	public boolean scrollFromSource = false;
	public boolean highlightSource = false;
	private PreviewPanel previewPanel;

	private JTree myTree = new com.intellij.ui.treeStructure.Tree();
	private TreeTextProvider treeTextProvider;

	public JTreeViewer(Tree tree, PreviewPanel previewPanel) {
		this.previewPanel = previewPanel;

		setupComponents();
		setupTree(tree);

		myTree.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON1) {
					onClick(e);
				}
			}
		});
	}

	private void setupComponents() {
		setLayout(new BorderLayout(0, 0));

		JScrollPane scrollPane = new JBScrollPane(myTree);
		ToggleAction scrollFromSourceBtn = new ToggleAction("Scroll from source", null, AutoscrollFromSource) {
			@Override
			public boolean isSelected(AnActionEvent e) {
				return scrollFromSource;
			}

			@Override
			public void setSelected(AnActionEvent e, boolean state) {
				scrollFromSource = state;
			}
		};
		ToggleAction scrollToSourceBtn = new ToggleAction("Highlight source", null, Find) {
			@Override
			public boolean isSelected(AnActionEvent e) {
				return highlightSource;
			}

			@Override
			public void setSelected(AnActionEvent e, boolean state) {
				highlightSource = state;
			}
		};

		DefaultActionGroup actionGroup = new DefaultActionGroup(
			scrollFromSourceBtn,
			scrollToSourceBtn
		);
		ActionToolbar bar = ActionManager.getInstance().createActionToolbar(PREVIEW_WINDOW_ID, actionGroup, true);

		add(bar.getComponent(), BorderLayout.NORTH);
		add(scrollPane, BorderLayout.CENTER);
	}

	private void setupTree(Tree tree) {
		setTree(tree);

		DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
		renderer.setOpenIcon(Icons.PARSER_RULE);
		renderer.setClosedIcon(Icons.PARSER_RULE);
		renderer.setLeafIcon(Icons.LEXER_RULE);
		myTree.setCellRenderer(renderer);
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
		if (tree == null) {
			return null;
		}
		DefaultMutableTreeNode root = new DefaultMutableTreeNode(tree) {
			@Override
			public String toString() {
				String name = treeTextProvider.getText((Tree) getUserObject());

				if (tree instanceof TerminalNode) {
					return name.equals("<EOF>") ? name : "\"" + name + "\"";
				}

				return name;
			}


		};

		for (int i = 0; i < tree.getChildCount(); i++) {
			root.add(wrap(tree.getChild(i)));
		}
		return root;
	}

	public void selectNodeAtOffset(int offset) {
		if (!scrollFromSource) {
			return;
		}
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) myTree.getModel().getRoot();
		Tree tree = (Tree) root.getUserObject();

		if (tree instanceof ParseTree) {
			DefaultMutableTreeNode atOffset = getNodeAtOffset(root, offset);

			if (atOffset != null) {
				TreePath path = new TreePath(atOffset.getPath());
				myTree.getSelectionModel().setSelectionPath(path);
				myTree.scrollPathToVisible(path);
			}
		}
	}

	@Nullable
	private DefaultMutableTreeNode getNodeAtOffset(DefaultMutableTreeNode node, int offset) {
		Tree tree = (Tree) node.getUserObject();

		if (tree instanceof ParserRuleContext) {
			ParserRuleContext ctx = (ParserRuleContext)tree;
			if ( inBounds(ctx, offset) ) {
				for (int i = 0; i < node.getChildCount(); i++) {
					DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
					DefaultMutableTreeNode atOffset = getNodeAtOffset(child, offset);

					if (atOffset != null) {
						return atOffset;
					}
				}
				// None of the children match, so it must be this node
				return node;
			}
		}
		else if (tree instanceof TerminalNode) {
			TerminalNode terminal = (TerminalNode) tree;

			if (terminal.getSymbol().getStartIndex() <= offset && terminal.getSymbol().getStopIndex() >= offset) {
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

	private void onClick(MouseEvent e) {
		if (highlightSource) {
			TreePath path = myTree.getClosestPathForLocation(e.getX(), e.getY());
			if (path == null) {
				return;
			}
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
			Tree tree = (Tree) node.getUserObject();

			int startIndex;
			int stopIndex;

			if (tree instanceof ParserRuleContext) {
				startIndex = ((ParserRuleContext) tree).getStart().getStartIndex();
				stopIndex = ((ParserRuleContext) tree).getStop().getStopIndex();
			}
			else if (tree instanceof TerminalNode) {
				startIndex = ((TerminalNode) tree).getSymbol().getStartIndex();
				stopIndex = ((TerminalNode) tree).getSymbol().getStopIndex();
			}
			else {
				return;
			}

			Editor editor = previewPanel.inputPanel.getEditor();
			editor.getSelectionModel().removeSelection();
			editor.getSelectionModel().setSelection(startIndex, stopIndex + 1);
		}
	}
}
