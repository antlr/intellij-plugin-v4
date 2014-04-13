package org.antlr.intellij.plugin.preview;

import com.intellij.openapi.editor.Editor;
import org.antlr.v4.tool.Grammar;

/** Track everything associated with the state of the preview window.
 *  For each grammar, we need to track a separate editor object
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
 *
 *  See comment on PreviewParserDefinition.
 */
public class PreviewState {
	public String grammarFileName;
	public Grammar g;
	public Grammar lg;
	public String startRuleName;

	public Editor editor;
}
