package org.antlr.intellij.plugin.parsing;

import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vcs.changes.BackgroundFromStartOption;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.antlr.intellij.plugin.ANTLRv4PluginController;
import org.antlr.intellij.plugin.configdialogs.ConfigANTLRPerGrammar;
import org.antlr.intellij.plugin.preview.PreviewState;
import org.antlr.v4.Tool;
import org.antlr.v4.codegen.CodeGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.stringtemplate.v4.misc.Misc;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// learned how to do from Grammar-Kit by Gregory Shrago
public class RunANTLROnGrammarFile extends Task.Backgroundable {
	public static final Logger LOG = Logger.getInstance("org.antlr.intellij.plugin.actions.RunANTLROnGrammarFile");
	public static final String OUTPUT_DIR_NAME = "gen" ;
	public static final String MISSING = "";

	VirtualFile grammarFile;
	Project project;
	boolean forceGeneration;

	PropertiesComponent props;

	public RunANTLROnGrammarFile(VirtualFile grammarFile,
								 @Nullable final Project project,
								 @NotNull final String title,
								 final boolean canBeCancelled,
								 boolean forceGeneration)
	{
		super(project, title, canBeCancelled, new BackgroundFromStartOption());
		this.grammarFile = grammarFile;
		this.project = project;
		props = PropertiesComponent.getInstance(project);
		this.forceGeneration = forceGeneration;
	}

	@Override
	public void run(@NotNull ProgressIndicator indicator) {
		indicator.setIndeterminate(true);
		String qualFileName = grammarFile.getPath();
		boolean autogen = getBooleanProp(qualFileName, "auto-gen", true);
		if ( forceGeneration || (autogen && isGrammarStale()) ) {
			antlr(grammarFile);
		}
	}

	// TODO: lots of duplication with antlr() function.
	public boolean isGrammarStale() {
		String qualFileName = grammarFile.getPath();
		String sourcePath = getParentDir(grammarFile);
		sourcePath = getProp(qualFileName, "lib-dir", sourcePath);
		String fullyQualifiedInputFileName = sourcePath+File.separator+grammarFile.getName();

		ANTLRv4PluginController controller = ANTLRv4PluginController.getInstance(project);
		final PreviewState previewState = controller.getPreviewState(fullyQualifiedInputFileName);
		// Grammar should be updated in the preview state before calling this function
		String language = previewState.g.getOptionString("language");
		CodeGenerator generator = new CodeGenerator(null, previewState.g, language);
		String recognizerFileName = generator.getRecognizerFileName();

		VirtualFile contentRoot = getContentRoot(grammarFile);
		String package_ = getProp(qualFileName, "package", MISSING);
		String outputDirName = getOutputDirName(qualFileName, contentRoot, package_);
		String fullyQualifiedOutputFileName = outputDirName+File.separator+recognizerFileName;

		System.out.println(fullyQualifiedInputFileName+" -> "+fullyQualifiedOutputFileName);
		File inF = new File(fullyQualifiedInputFileName);
		File outF = new File(fullyQualifiedOutputFileName);
		boolean stale = inF.lastModified()>outF.lastModified();
		System.out.println("stale="+stale);
		return stale;
	}

	/** Run ANTLR tool on file according to preferences in intellij for this file.
	 *  Returns set of generated files or empty set if error.
 	 */
	public void antlr(VirtualFile vfile) {
		if ( vfile==null ) return;

		List<String> args = new ArrayList<String>();

		String qualFileName = vfile.getPath();
		String sourcePath = getParentDir(vfile);
		VirtualFile contentRoot = getContentRoot(vfile);

		String package_ = getProp(qualFileName, "package", MISSING);
		if ( package_!=MISSING) {
			args.add("-package");
			args.add(package_);
		}

		String language = getProp(qualFileName, "language", MISSING);
		if ( language!=MISSING) {
			args.add("-Dlanguage="+language);
//			args.add(language);
		}

		// create gen dir at root of project by default, but add in package if any
		args.add("-o");
		String outputDirName = getOutputDirName(qualFileName, contentRoot, package_);
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

		String fullyQualifiedInputFileName = sourcePath+File.separator+vfile.getName();
		args.add(fullyQualifiedInputFileName); // add grammar file last

		//System.out.println("args="+args);

		Tool antlr = new Tool(args.toArray(new String[args.size()]));

		ConsoleView console = ANTLRv4PluginController.getInstance(project).getConsole();
		String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());
		console.print(timeStamp+": antlr4 "+Misc.join(args.iterator(), " ")+"\n", ConsoleViewContentType.SYSTEM_OUTPUT);
		antlr.removeListeners();
		RunANTLRListener listener = new RunANTLRListener(antlr, console);
		antlr.addListener(listener);


		boolean showGeneratedMsg;
		String groupDisplayId = "ANTLR 4 Parser Generation";
		try {
			antlr.processGrammarsOnCommandLine();
			showGeneratedMsg = antlr.getNumErrors()==0;
		}
		catch (Throwable e) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			String msg = sw.toString();
			Notification notification =
				new Notification(groupDisplayId,
					"can't generate parser for " + vfile.getName(),
					e.toString(),
					NotificationType.INFORMATION);
			Notifications.Bus.notify(notification, project);
			console.print(timeStamp + ": antlr4 " + msg + "\n", ConsoleViewContentType.SYSTEM_OUTPUT);
			listener.hasOutput = true; // show console below
			showGeneratedMsg = false;
		}

		if ( listener.hasOutput ) {
			ApplicationManager.getApplication().invokeLater(
				new Runnable() {
					@Override
					public void run() {
						ANTLRv4PluginController.getInstance(project).getConsoleWindow().show(null);
					}
				}
			);
		}

		if ( showGeneratedMsg ) {
			// refresh from disk to see new files
			Set<File> generatedFiles = new HashSet<File>();
			generatedFiles.add(new File(outputDirName));
			LocalFileSystem.getInstance().refreshIoFiles(generatedFiles, true, true, null);
			// pop up a notification
			Notification notification =
				new Notification(groupDisplayId,
								 "parser for " + vfile.getName() + " generated",
								 "to " + outputDirName,
								 NotificationType.INFORMATION);
			Notifications.Bus.notify(notification, project);
		}
	}

	public String getOutputDirName(String qualFileName, VirtualFile contentRoot, String package_) {
		String outputDirName = contentRoot.getPath()+File.separator+OUTPUT_DIR_NAME;
		outputDirName = getProp(qualFileName, "output-dir", outputDirName);
		if ( package_!=MISSING ) {
			outputDirName += File.separator+package_.replace('.',File.separatorChar);
		}
		return outputDirName;
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
