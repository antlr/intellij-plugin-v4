package org.antlr.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import org.antlr.intellij.plugin.parsing.RunANTLROnGrammarFile;

/** Generate parser from ANTLR grammar;
 *  learned how to do from Grammar-Kit by Gregory Shrago.
 */
public class GenerateParserAction extends AnAction implements DumbAware {
	public static final Logger LOG = Logger.getInstance("ANTLR GenerateAction");

	@Override
	public void update(AnActionEvent e) {
		MyActionUtils.selectedFileIsGrammar(e);
	}

	@Override
	public void actionPerformed(final AnActionEvent e) {
		if ( e.getProject()==null ) {
			LOG.error("actionPerformed no project for "+e);
			return; // whoa!
		}
		VirtualFile grammarFile = MyActionUtils.getGrammarFileFromEvent(e);
		LOG.info("actionPerformed "+(grammarFile==null ? "NONE" : grammarFile));
		if ( grammarFile==null ) return;
		String title = "ANTLR Code Generation";
		boolean canBeCancelled = true;

		// commit changes to PSI and file system
		PsiDocumentManager.getInstance(e.getProject()).commitAllDocuments();
		FileDocumentManager.getInstance().saveAllDocuments();

		boolean forceGeneration = true; // from action, they really mean it
		Task.Backgroundable gen =
			new RunANTLROnGrammarFile(grammarFile,
									  e.getProject(),
									  title,
									  canBeCancelled,
									  forceGeneration);
		ProgressManager.getInstance().run(gen);
	}
}
