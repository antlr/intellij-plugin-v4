package org.antlr.intellij.plugin.preview;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
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
	public VirtualFile grammarFile;
	public Grammar g;
	public LexerGrammar lg;
	public String startRuleName;
	public CharSequence manualInputText = ""; // save input when switching grammars
	public String inputFileName = ""; 	// save input file when switching grammars

	public ParsingResult parsingResult;

	/** The current input editor (inputEditor or fileEditor) for this grammar */
	private Editor editor;

	public PreviewState(VirtualFile grammarFile) {
		this.grammarFile = grammarFile;
	}

	public synchronized Editor getEditor() {
		return editor;
	}

	public synchronized void setEditor(Editor editor) {
		releaseEditor();
		this.editor = editor;
	}

	public Grammar getMainGrammar() {
		return g!=null ? g : lg;
	}

	public synchronized void releaseEditor() {
		// It would appear that the project closed event occurs before these
		// close grammars sometimes. Very strange. check for null editor.
		if (editor != null) {
			final EditorFactory factory = EditorFactory.getInstance();
			factory.releaseEditor(editor);
			editor = null;
		}
	}

}
