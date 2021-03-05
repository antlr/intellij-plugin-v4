package org.antlr.intellij.plugin.generators;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.CheckedTreeNode;
import com.intellij.ui.DoubleClickListener;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.tree.TreeUtil;
import org.antlr.intellij.plugin.Icons;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashSet;

public class LiteralChooser extends DialogWrapper {
	Tree tree;
	LinkedHashSet<String> selectedElements =
			new LinkedHashSet<>();

	public LiteralChooser(@Nullable Project project, java.util.List<String> literals) {
		super(project, true);
		tree = createTree(literals);
		setTitle("Choose Literal Rules to Generate");
		init();
	}

	public Tree createTree(java.util.List<String> literals) {
		final CheckedTreeNode rootNode = new CheckedTreeNode("all literals not defined");
		for (String literal : literals) {
			CheckedTreeNode child = new CheckedTreeNode(new LiteralChooserObject(literal, Icons.LEXER_RULE));
			child.setChecked(true);
			rootNode.add(child);
		}
		DefaultTreeModel treeModel = new DefaultTreeModel(rootNode);

		selectedElements.addAll(literals); // all are "on" by default

		Tree tree = new Tree(treeModel);
		tree.setRootVisible(false);
		tree.setCellRenderer(new LiteralChooserRenderer());
		tree.addTreeSelectionListener(new MyTreeSelectionListener());

		return tree;
	}

	@Nullable
	@Override
	protected JComponent createCenterPanel() {
		JPanel panel = new JPanel(new BorderLayout());
		new DoubleClickListener() {
			@Override
			protected boolean onDoubleClick(MouseEvent e) {
				if (tree.getPathForLocation(e.getX(), e.getY()) != null) {
					doOKAction();
					return true;
				}
				return false;
			}
		}.installOn(tree);

		TreeUtil.installActions(tree);
		JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(tree);
//		scrollPane.setPreferredSize(new Dimension(350, 450));
		panel.add(scrollPane, BorderLayout.CENTER);

		return panel;
	}

	private class MyTreeSelectionListener implements TreeSelectionListener {
		public void valueChanged(TreeSelectionEvent e) {
			TreePath[] paths = e.getPaths();
			if (paths == null) return;
			for (int i = 0; i < paths.length; i++) {
				Object node = paths[i].getLastPathComponent();
				if (node instanceof CheckedTreeNode) {
					Object userObject = ((DefaultMutableTreeNode) node).getUserObject();
					if (userObject instanceof LiteralChooserObject) {
						LiteralChooserObject literalObject = (LiteralChooserObject) userObject;
						String text = literalObject.getText();
						if ( e.isAddedPath(paths[i]) ) {
							if ( selectedElements.contains(text) ) {
								selectedElements.remove(text);
							}
							else {
								selectedElements.add(text);
							}
							CheckedTreeNode checkedNode = (CheckedTreeNode) node;

							checkedNode.setChecked(!checkedNode.isChecked()); // toggle
						}
					}
				}
			}
		}
	}

	@Nullable
	private LinkedHashSet<String> getSelectedElementsList() {
		return getExitCode() == OK_EXIT_CODE ? selectedElements : null;
	}

	@Nullable
	public java.util.List<String> getSelectedElements() {
		final LinkedHashSet<String> list = getSelectedElementsList();
		return list == null ? null : new ArrayList<>(list);
	}

}
