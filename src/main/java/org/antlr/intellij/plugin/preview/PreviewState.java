package org.antlr.intellij.plugin.preview;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.antlr.intellij.plugin.parsing.ParsingResult;
import org.antlr.v4.tool.Grammar;
import org.antlr.v4.tool.LexerGrammar;

/** Track everything associated with the state of the preview window.
 *  For each grammar, we need to track an InputPanel (with <= 2 editor objects)
 *  that we will flip to every time we come back to a specific grammar,
 *  uniquely identified by the fully-qualified grammar name.
 *
 *  Before parsing can begin, we need to know the start rule. That means that
 *  we should not show an editor until this field is filled in.
 *
 *  The plug-in controller should update all of these elements atomically so
 *  they are self consistent.  We must be careful then to send these fields
 *  around together as a unit instead of asking the controller for the
 *  elements piecemeal. That could get g and lg for different grammar files,
 *  for example.
 */
public class PreviewState {
	public Project project;
	public VirtualFile grammarFile;
	public Grammar g;
	public LexerGrammar lg;
	public String startRuleName;
	public CharSequence manualInputText = ""; // save input when switching grammars
	public VirtualFile inputFile; 	// save input file when switching grammars

	public ParsingResult parsingResult;

	/** The current input editor (inputEditor or fileEditor) for this grammar
	 *  in InputPanel. This can be null when a PreviewState and InputPanel
	 *  are created out of sync. Depends on order IDE opens files vs
	 *  creates preview pane.
	 */
	private Editor inputEditor;

	public PreviewState(Project project, VirtualFile grammarFile) {
		this.project = project;
		this.grammarFile = grammarFile;
	}

	public synchronized Editor getInputEditor() {
		return inputEditor;
	}

	public synchronized void setInputEditor(Editor inputEditor) {
		releaseEditor();
		this.inputEditor = inputEditor;
	}

	public Grammar getMainGrammar() {
		return g!=null ? g : lg;
	}

	public synchronized void releaseEditor() {

		// Editor can't be release during unit tests, because it is used by multiple tests
		if (ApplicationManager.getApplication().isUnitTestMode()) return;

		// It would appear that the project closed event occurs before these
		// close grammars sometimes. Very strange. check for null editor.
		if (inputEditor != null) {
			final EditorFactory factory = EditorFactory.getInstance();
			factory.releaseEditor(inputEditor);
			inputEditor = null;
		}
	}

}
