package org.antlr.intellij.plugin.actions;

import com.intellij.execution.ui.ConsoleView;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.PerformInBackgroundOption;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.ExceptionUtil;
import org.antlr.intellij.plugin.ANTLRv4FileRoot;
import org.antlr.intellij.plugin.ANTLRv4ProjectComponent;
import org.antlr.intellij.plugin.psi.MyPsiUtils;
import org.antlr.intellij.plugin.tooloutput.ToolOutputWindowFactory;
import org.antlr.v4.Tool;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

// learned how to do from Grammar-Kit by Gregory Shrago
public class RunANTLROnGrammarFile extends Task.Backgroundable implements Runnable {
	public static final Logger LOG = Logger.getInstance("org.antlr.intellij.plugin.actions.RunANTLROnGrammarFile");

	Set<File> generatedFiles = new HashSet<File>();
	VirtualFile[] files;
	Project project;
	/** We use invokeLater to save file changes; must wait til done before invoking antlr */
	boolean docSaved;

	public RunANTLROnGrammarFile(VirtualFile[] files,
								 @Nullable final Project project,
								 @NotNull final String title,
								 final boolean canBeCancelled,
								 @Nullable final PerformInBackgroundOption backgroundOption)
	{
		super(project, title, canBeCancelled, backgroundOption);
		this.files = files;
		this.project = project;
	}

	@Override
	public void run(@NotNull ProgressIndicator indicator) {
		indicator.setIndeterminate(true);
		ApplicationManager.getApplication().runReadAction(this);
	}

	@Override
	public void run() {
		PsiManager psiManager = PsiManager.getInstance(project);
		for (VirtualFile file : files) {
			PsiFile f = psiManager.findFile(file);
			if ( !(f instanceof ANTLRv4FileRoot) ) continue; // not grammar file

			ANTLRv4FileRoot gfile = (ANTLRv4FileRoot)f;
			VirtualFile virtualFile = f.getVirtualFile();
			String sourcePath =
				virtualFile == null?
					"" :
					VfsUtil.virtualToIoFile(virtualFile).getParentFile().getAbsolutePath();

			VirtualFile content = ProjectRootManager.getInstance(project).getFileIndex().getContentRootForFile(file);
			VirtualFile parentDir = content == null ? file.getParent() : content;
			// create gen dir at root of project
			String outputDirName = "gen";

			// find package
			String pack = MyPsiUtils.findPackageIfAny(gfile);
			if ( pack!=null ) {
				outputDirName += File.separator+pack.replace('.',File.separatorChar);
			}

			File genDir = new File(VfsUtil.virtualToIoFile(parentDir), outputDirName);
			String outputPath = genDir.getAbsolutePath();
			generatedFiles.add(new File(outputPath));
			String groupDisplayId = "ANTLR 4 Code Generation";
			try {
				boolean success = generate(gfile, sourcePath, outputPath);
				if ( success ) {
					LocalFileSystem.getInstance().refreshIoFiles(generatedFiles, true, true, null);
					Notification notification =
						new Notification(groupDisplayId,
										 "parser for " + file.getName() + " generated",
										 "to " + outputPath,
										 NotificationType.INFORMATION);
					Notifications.Bus.notify(notification, project);
				}
			}
			catch (Exception ex) {
				Notification notification =
					new Notification(groupDisplayId,
									 "generation of parser for "+file.getName()+" failed",
									 ExceptionUtil.getUserStackTrace(ex, RunANTLROnGrammarFile.LOG),
									 NotificationType.ERROR);
				Notifications.Bus.notify(notification, project);
				RunANTLROnGrammarFile.LOG.warn(ex);
			}
		}
	}

	// return success/failure
	public boolean generate(ANTLRv4FileRoot file, String sourcePath, String outputPath) {
//		// wait until we've saved the grammar file.
//		while ( !docSaved ) {
//			System.out.println("waiting for save");
//			try { Thread.sleep(500); }
//			catch (InterruptedException ie) {
//				LOG.error(ie);
//			}
//		}
		Tool antlr = new Tool(new String[] {
			"-o", outputPath,
			"-lib", sourcePath, // lets us see tokenVocab stuff
			sourcePath+File.separator+file.getName()}
		);
		ConsoleView console = ANTLRv4ProjectComponent.getInstance(project).getConsole();
		console.clear();
		antlr.removeListeners();
		RunANTLRListener listener = new RunANTLRListener(antlr, console);
		antlr.addListener(listener);
		antlr.processGrammarsOnCommandLine();
		if ( listener.hasOutput ) {
			ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
			final ToolWindow toolWindow = toolWindowManager.getToolWindow(ToolOutputWindowFactory.ID);
			ApplicationManager.getApplication().invokeLater(
				new Runnable() {
					@Override
					public void run() {
						toolWindow.show(null);
					}
				}
			);
		}
		return !listener.hasOutput;
	}
}
