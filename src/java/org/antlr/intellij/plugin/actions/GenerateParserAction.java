package org.antlr.intellij.plugin.actions;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import org.antlr.intellij.plugin.parsing.RunANTLROnGrammarFile;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

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
		PsiDocumentManager psiMgr = PsiDocumentManager.getInstance(e.getProject());
		FileDocumentManager docMgr = FileDocumentManager.getInstance();
		Document doc = docMgr.getDocument(grammarFile);
		if ( doc==null ) return;

		boolean wasStale = !psiMgr.isCommitted(doc) || docMgr.isDocumentUnsaved(doc);
		if ( wasStale ) {
			// save event triggers ANTLR run if autogen on
			psiMgr.commitDocument(doc);
			docMgr.saveDocument(doc);
		}

		boolean forceGeneration = true; // from action, they really mean it
		RunANTLROnGrammarFile gen =
			new RunANTLROnGrammarFile(grammarFile,
									  e.getProject(),
									  title,
									  canBeCancelled,
									  forceGeneration);
		boolean autogen = gen.getBooleanProp(grammarFile.getPath(), "auto-gen", false);

		if ( !wasStale || (wasStale && !autogen) ) {
			// if everything already saved (!stale) then run ANTLR
			// if had to be save and autogen NOT on, then run ANTLR
			// Otherwise, the save file event will have or will run ANTLR.
			ProgressManager.getInstance().run(gen); //, "Generating", canBeCancelled, e.getProject());

			// refresh from disk to see new files
			Set<File> generatedFiles = new HashSet<File>();
			generatedFiles.add(new File(gen.getOutputDirName()));
			LocalFileSystem.getInstance().refreshIoFiles(generatedFiles, true, true, null);
			// pop up a notification
			Notification notification =
				new Notification(RunANTLROnGrammarFile.groupDisplayId,
								 "parser for " + grammarFile.getName() + " generated",
								 "to " + gen.getOutputDirName(),
								 NotificationType.INFORMATION);
			Notifications.Bus.notify(notification, e.getProject());
		}
	}
}
