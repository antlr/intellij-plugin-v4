package org.antlr.intellij.plugin.preview;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.ui.Splitter;
import com.intellij.ui.components.JBScrollPane;
import org.antlr.intellij.plugin.ANTLRv4ProjectComponent;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.gui.TreeViewer;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.io.IOException;
import java.util.Arrays;

public class ParseTreePanel extends JPanel {
	protected TreeViewer viewer;  // the antlr tree viewer component itself
	protected JLabel startRuleLabel;
	protected String inputText = "";
	protected String combinedGrammarFileName;
	protected String startRule;

	public ParseTreePanel() {
		buildGUI();
	}

	public void buildGUI() {
		this.setLayout(new BorderLayout());

		// wrap tree and slider in panel
		JPanel treePanel = new JPanel(new BorderLayout(0,0));
		treePanel.setBackground(Color.white);
		// Wrap tree viewer component in scroll pane
		viewer = new TreeViewer(null, null);
		JScrollPane scrollPane = new JBScrollPane(viewer); // use Intellij's scroller
		treePanel.add(scrollPane, BorderLayout.CENTER);

		// Add scale slider to bottom, under tree view scroll panel
		int sliderValue = (int) ((viewer.getScale()-1.0) * 1000);
		final JSlider scaleSlider = new JSlider(JSlider.HORIZONTAL,
										  -999,1000,sliderValue);
		scaleSlider.addChangeListener(
			new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					int v = scaleSlider.getValue();
					viewer.setScale(v / 1000.0 + 1.0);
				}
			}
									 );
		treePanel.add(scaleSlider, BorderLayout.SOUTH);

		final EditorFactory factory = EditorFactory.getInstance();
		Document doc = factory.createDocument(inputText);
		Editor ed = factory.createEditor(doc);

		doc.addDocumentListener(new DocumentListener() {
			@Override
			public void beforeDocumentChange(DocumentEvent event) {}
			@Override
			public void documentChanged(DocumentEvent event) {
				Document doc = event.getDocument();
				String newText = doc.getText();
//				System.out.println("CHANGED: " + newText);
				setInput(newText);
			}
		});

		JPanel editorPanel = new JPanel(new BorderLayout(0,0));
		startRuleLabel = new JLabel("Start rule: <undefined>");
		editorPanel.add(startRuleLabel, BorderLayout.NORTH);
		editorPanel.add(ed.getComponent(), BorderLayout.CENTER);

		Splitter splitPane = new Splitter();
		splitPane.setFirstComponent(editorPanel);
		splitPane.setSecondComponent(treePanel);

		this.add(splitPane, BorderLayout.CENTER);
	}

	public void setStartRule(String startRule) {
		this.startRule = startRule;
		if ( startRule==null ) {
			startRule = "<undefined>";
		}
		startRuleLabel.setText("Start rule: "+startRule);
	}

	public void setInput(String inputText) {
		this.inputText = inputText;
		setInputAndGrammar(inputText, combinedGrammarFileName, startRule);
	}

	public void setInputAndGrammar(String inputText,
								   String combinedGrammarFileName,
								   String startRule)
	{
		this.combinedGrammarFileName = combinedGrammarFileName;
		setStartRule(startRule);

		if ( combinedGrammarFileName==null || startRule==null ) {
			return;
		}

		ParseTree root = null;
		Parser parser = null;
		try {
			Object[] results =
				ANTLRv4ProjectComponent.parseText(inputText,
												  combinedGrammarFileName,
												  startRule);
			if ( results!=null ) {
				parser = (Parser)results[0];
				root = (ParseTree)results[1];
				viewer.setRuleNames(Arrays.asList(parser.getRuleNames()));
				viewer.setTree(root);
			}
			else {
				viewer.setTree(null);
			}
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	public TreeViewer getViewer() {
		return viewer;
	}

	public String getCombinedGrammarFileName() {
		return combinedGrammarFileName;
	}

	public String getInputText() {
		return inputText;
	}

	public String getStartRule() {
		return startRule;
	}
}
