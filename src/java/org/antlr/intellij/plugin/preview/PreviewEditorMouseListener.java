package org.antlr.intellij.plugin.preview;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.EditorMouseEvent;
import com.intellij.openapi.editor.event.EditorMouseEventArea;
import com.intellij.openapi.editor.event.EditorMouseListener;
import com.intellij.openapi.editor.event.EditorMouseMotionListener;
import org.antlr.intellij.plugin.ANTLRv4PluginController;
import org.antlr.intellij.plugin.actions.MyActionUtils;

import java.awt.event.MouseEvent;

class PreviewEditorMouseListener implements EditorMouseListener, EditorMouseMotionListener {
	private InputPanel inputPanel;

	public PreviewEditorMouseListener(InputPanel inputPanel) {
		this.inputPanel = inputPanel;
	}

	@Override
	public void mouseExited(EditorMouseEvent e) {
		InputPanel.removeTokenInfoHighlighters(e.getEditor());
	}

	@Override
	public void mouseClicked(EditorMouseEvent e) {
		int offset = getEditorCharOffset(e);
		if ( offset<0 ) return;

		Editor editor=e.getEditor();
		ANTLRv4PluginController controller = ANTLRv4PluginController.getInstance(editor.getProject());
		PreviewState previewState =	controller.getPreviewState();
		if ( previewState==null ) {
			return;
		}

		MouseEvent mouseEvent=e.getMouseEvent();
		if ( mouseEvent.isMetaDown() ) {
			inputPanel.setCursorToGrammarElement(e.getEditor().getProject(), previewState, offset);
		}
        else if ( mouseEvent.isAltDown() ) {
            inputPanel.setCursorToGrammarRule(e.getEditor().getProject(), previewState, offset);
        }
	}

	@Override
	public void mouseMoved(EditorMouseEvent e){
		int offset = getEditorCharOffset(e);
		if ( offset<0 ) return;

		Editor editor=e.getEditor();
		ANTLRv4PluginController controller = ANTLRv4PluginController.getInstance(editor.getProject());
		PreviewState previewState =	controller.getPreviewState();
		if ( previewState==null ) {
			return;
		}

		MouseEvent mouseEvent=e.getMouseEvent();
		InputPanel.removeTokenInfoHighlighters(editor);
		if ( mouseEvent.isMetaDown() ) {
			inputPanel.showTokenInfoUponMeta(editor, previewState, offset);
		}
        else if ( mouseEvent.isAltDown() ) {
            inputPanel.showParseRegion(editor, previewState, offset);
        }
		else { // just moving around, show any errors
			inputPanel.showTooltipsForErrors(editor, previewState, offset);
		}
	}

	public int getEditorCharOffset(EditorMouseEvent e) {
		if ( e.getArea()!=EditorMouseEventArea.EDITING_AREA ) {
			return -1;
		}

		MouseEvent mouseEvent=e.getMouseEvent();
		Editor editor=e.getEditor();
		int offset = MyActionUtils.getMouseOffset(mouseEvent, editor);
//		System.out.println("offset="+offset);

		if ( offset >= editor.getDocument().getTextLength() ) {
			return -1;
		}

		// Mouse has moved so make sure we don't show any token information tooltips
		InputPanel.removeTokenInfoHighlighters(editor);
		return offset;
	}

	// ------------------------

	@Override
	public void mousePressed(EditorMouseEvent e) {
	}

	@Override
	public void mouseReleased(EditorMouseEvent e) {
	}

	@Override
	public void mouseEntered(EditorMouseEvent e) {
	}

	@Override
	public void mouseDragged(EditorMouseEvent e) {
	}
}
