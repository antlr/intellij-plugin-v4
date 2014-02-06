package org.antlr.intellij.plugin.actions;

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
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.ExceptionUtil;
import org.antlr.intellij.plugin.ANTLRv4FileRoot;
import org.antlr.intellij.plugin.psi.MyPsiUtils;
import org.antlr.v4.Tool;
import org.antlr.v4.tool.ANTLRMessage;
import org.antlr.v4.tool.ANTLRToolListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

// learned how to do from Grammar-Kit by Gregory Shrago
public class RunANTLROnGrammarFile extends Task.Backgroundable {
	class ExecANTLR implements Runnable {
		@Override
		public void run() {
			PsiManager psiManager = PsiManager.getInstance(project);
			for (VirtualFile file : files) {
				PsiFile f = psiManager.findFile(file);
				if ( !(f instanceof ANTLRv4FileRoot) ) continue;
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
					generate(gfile, sourcePath, outputPath);
					Notification notification =
						new Notification(groupDisplayId,
										 "parser for "+file.getName()+" generated",
										 "to " + outputPath,
										 NotificationType.INFORMATION);
					Notifications.Bus.notify(notification, project);
				}
				catch (Exception ex) {
					Notification notification =
						new Notification(groupDisplayId,
										 "generation of parser for "+file.getName()+" failed",
										 ExceptionUtil.getUserStackTrace(ex, LOG),
										 NotificationType.ERROR);
					Notifications.Bus.notify(notification, project);
					LOG.warn(ex);
				}
			}
		}
	}

	public static final Logger LOG = Logger.getInstance("org.antlr.intellij.plugin.actions.RunANTLROnGrammarFile");

	Set<File> generatedFiles = new HashSet<File>();
	VirtualFile[] files;
	Project project;

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
	public void onSuccess() {
		LocalFileSystem.getInstance().refreshIoFiles(generatedFiles, true, true, null);
	}

	@Override
	public void onCancel() {
		LocalFileSystem.getInstance().refreshIoFiles(generatedFiles, true, true, null);
	}

	@Override
	public void run(@NotNull ProgressIndicator indicator) {
		indicator.setIndeterminate(true);
		ApplicationManager.getApplication().runReadAction(new ExecANTLR());
	}

	public void generate(ANTLRv4FileRoot file, String sourcePath, String outputPath) {
		Tool antlr = new Tool(new String[] {
			"-o", outputPath,
			"-lib", sourcePath, // lets us see tokenVocab stuff
			sourcePath+File.separator+file.getName()}
		);
		antlr.addListener(new ANTLRToolListener() {
			@Override
			public void info(String msg) {
			}
			@Override
			public void error(ANTLRMessage msg) {
			}
			@Override
			public void warning(ANTLRMessage msg) {
			}
		});
		antlr.processGrammarsOnCommandLine();
	}
}
