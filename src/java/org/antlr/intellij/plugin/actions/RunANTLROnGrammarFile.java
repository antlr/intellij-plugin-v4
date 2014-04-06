package org.antlr.intellij.plugin.actions;

import com.intellij.execution.ui.ConsoleView;
import com.intellij.ide.util.PropertiesComponent;
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
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.antlr.intellij.plugin.ANTLRv4FileRoot;
import org.antlr.intellij.plugin.ANTLRv4ProjectComponent;
import org.antlr.intellij.plugin.dialogs.ConfigANTLRPerGrammar;
import org.antlr.intellij.plugin.psi.MyPsiUtils;
import org.antlr.intellij.plugin.tooloutput.ToolOutputWindowFactory;
import org.antlr.v4.Tool;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// learned how to do from Grammar-Kit by Gregory Shrago
public class RunANTLROnGrammarFile extends Task.Backgroundable implements Runnable {
	public static final String OUTPUT_DIR_NAME = "gen" ;
	public static final String MISSING = "";

	public static final Logger LOG = Logger.getInstance("org.antlr.intellij.plugin.actions.RunANTLROnGrammarFile");

	VirtualFile[] files;
	Project project;

	PropertiesComponent props;

	public RunANTLROnGrammarFile(VirtualFile[] files,
								 @Nullable final Project project,
								 @NotNull final String title,
								 final boolean canBeCancelled,
								 @Nullable final PerformInBackgroundOption backgroundOption)
	{
		super(project, title, canBeCancelled, backgroundOption);
		this.files = files;
		this.project = project;
		props = PropertiesComponent.getInstance(project);
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

			antlr((ANTLRv4FileRoot)f);
		}
	}

	/** Run ANTLR tool on file according to preferences in intellij for this file.
	 *  Returns set of generated files or empty set if error.
 	 */
	public void antlr(ANTLRv4FileRoot file) {
		VirtualFile vfile = file.getVirtualFile();
		if ( vfile==null ) return;

		List<String> args = new ArrayList<String>();

		String qualFileName = vfile.getPresentableName();
		String sourcePath = getParentDir(vfile);
		VirtualFile contentRoot = getContentRoot(vfile);

		// create gen dir at root of project by default
		String outputDirName = contentRoot.getPath()+File.separator+OUTPUT_DIR_NAME;
		// find package if none in prefs
		String pack = MyPsiUtils.findPackageIfAny(file);
		if ( pack!=null ) {
			outputDirName += File.separator+pack.replace('.',File.separatorChar);
		}
		args.add("-o");
		outputDirName = getProp(qualFileName, "output-dir", outputDirName);
		args.add(outputDirName);

		args.add("-lib");
		sourcePath = getProp(qualFileName, "lib-dir", sourcePath);
		args.add(sourcePath);

		String encoding = getProp(qualFileName, "encoding", MISSING);
		if ( encoding!=MISSING ) {
			args.add("-encoding");
			args.add(encoding);
		}

		if ( getBooleanProp(qualFileName, "gen-listener", true) ) {
			args.add("-listener");
		}
		else {
			args.add("-no-listener");
		}
		if ( getBooleanProp(qualFileName, "gen-visitor", true) ) {
			args.add("-visitor");
		}
		else {
			args.add("-no-visitor");
		}

		args.add(sourcePath+File.separator+vfile.getName()); // add grammar file last

		System.out.println("args="+args);

		Tool antlr = new Tool(args.toArray(new String[args.size()]));

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

		String groupDisplayId = "ANTLR 4 Parser Generation";
		if ( antlr.getNumErrors()==0 ) {
			// refresh from disk to see new files
			Set<File> generatedFiles = new HashSet<File>();
			generatedFiles.add(new File(outputDirName));
			LocalFileSystem.getInstance().refreshIoFiles(generatedFiles, true, true, null);
			// pop up a notification
			Notification notification =
				new Notification(groupDisplayId,
								 "parser for " + file.getName() + " generated",
								 "to " + outputDirName,
								 NotificationType.INFORMATION);
			Notifications.Bus.notify(notification, project);
		}
	}

	public String getProp(String qualFileName, String name, String defaultValue) {
		String v = props.getValue(ConfigANTLRPerGrammar.getPropNameForFile(qualFileName, name));
		if ( v==null || v.trim().length()==0 ) return defaultValue;
		return v;
	}

	public boolean getBooleanProp(String qualFileName, String name, boolean defaultValue) {
		return props.getBoolean(ConfigANTLRPerGrammar.getPropNameForFile(qualFileName, name), defaultValue);
	}

	public String getParentDir(VirtualFile vfile) {
		return vfile.getParent().getPath();
	}

	public VirtualFile getContentRoot(VirtualFile vfile) {
		VirtualFile root =
			ProjectRootManager.getInstance(project)
				.getFileIndex().getContentRootForFile(vfile);
		if ( root!=null ) return root;
		return vfile.getParent();
	}
}
