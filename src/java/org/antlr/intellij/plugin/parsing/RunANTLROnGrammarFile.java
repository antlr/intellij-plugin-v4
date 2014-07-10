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
import com.intellij.openapi.vfs.VirtualFile;
import org.antlr.intellij.plugin.ANTLRv4PluginController;
import org.antlr.intellij.plugin.configdialogs.ConfigANTLRPerGrammar;
import org.antlr.intellij.plugin.preview.PreviewState;
import org.antlr.v4.Tool;
import org.antlr.v4.codegen.CodeGenerator;
import org.antlr.v4.runtime.misc.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.stringtemplate.v4.misc.Misc;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// learned how to do from Grammar-Kit by Gregory Shrago
public class RunANTLROnGrammarFile extends Task.Modal {
	public static final Logger LOG = Logger.getInstance("RunANTLROnGrammarFile");
	public static final String OUTPUT_DIR_NAME = "gen" ;
	public static final String MISSING = "";
	public static final String groupDisplayId = "ANTLR 4 Parser Generation";

	public VirtualFile grammarFile;
	public Project project;
	public boolean forceGeneration;

	public RunANTLROnGrammarFile(VirtualFile grammarFile,
								 @Nullable final Project project,
								 @NotNull final String title,
								 final boolean canBeCancelled,
								 boolean forceGeneration)
	{
		super(project, title, canBeCancelled); //, inBackground ? new BackgroundFromStartOption() : null);
		this.grammarFile = grammarFile;
		this.project = project;
		this.forceGeneration = forceGeneration;
	}

	@Override
	public void run(@NotNull ProgressIndicator indicator) {
		indicator.setIndeterminate(true);
		String qualFileName = grammarFile.getPath();
		boolean autogen = getBooleanProp(project, qualFileName, "auto-gen", false);
		System.out.println("autogen is "+autogen+", force="+forceGeneration);
		if ( forceGeneration || (autogen && isGrammarStale()) ) {
			antlr(grammarFile);
		}
	}

	// TODO: lots of duplication with antlr() function.
	public boolean isGrammarStale() {
		String qualFileName = grammarFile.getPath();
		String sourcePath = getParentDir(grammarFile);
		sourcePath = getProp(project, qualFileName, "lib-dir", sourcePath);
		String fullyQualifiedInputFileName = sourcePath+File.separator+grammarFile.getName();

		ANTLRv4PluginController controller = ANTLRv4PluginController.getInstance(project);
		final PreviewState previewState = controller.getPreviewState(fullyQualifiedInputFileName);
		// Grammar should be updated in the preview state before calling this function
		if ( previewState.g==null ) {
			return false;
		}
		String language = previewState.g.getOptionString("language");
		CodeGenerator generator = new CodeGenerator(null, previewState.g, language);
		String recognizerFileName = generator.getRecognizerFileName();

		VirtualFile contentRoot = getContentRoot(project, grammarFile);
		String package_ = getProp(project, qualFileName, "package", MISSING);
		String outputDirName = getOutputDirName(project, qualFileName, contentRoot, package_);
		String fullyQualifiedOutputFileName = outputDirName+File.separator+recognizerFileName;

		File inF = new File(fullyQualifiedInputFileName);
		File outF = new File(fullyQualifiedOutputFileName);
		boolean stale = inF.lastModified()>outF.lastModified();
		LOG.info((!stale ? "not" : "") + "stale: " + fullyQualifiedInputFileName + " -> " + fullyQualifiedOutputFileName);
		return stale;
	}

	/** Run ANTLR tool on file according to preferences in intellij for this file.
	 *  Returns set of generated files or empty set if error.
 	 */
	public void antlr(VirtualFile vfile) {
		if ( vfile==null ) return;

		LOG.info("antlr(\""+vfile.getPath()+"\")");
		Map<String,String> argMap = getANTLRArgs(project, vfile);

		List<String> args = new ArrayList<String>();
		for (String option : argMap.keySet()) {
			args.add(option);
			String value = argMap.get(option);
			if ( value.length()!=0 ) {
				args.add(value);
			}
		}

		String sourcePath = getParentDir(vfile);
		String fullyQualifiedInputFileName = sourcePath+File.separator+vfile.getName();
		args.add(fullyQualifiedInputFileName); // add grammar file last

		LOG.info("args: " + Utils.join(args.iterator(), " "));

		Tool antlr = new Tool(args.toArray(new String[args.size()]));

		ConsoleView console = ANTLRv4PluginController.getInstance(project).getConsole();
		String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());
		console.print(timeStamp+": antlr4 "+Misc.join(args.iterator(), " ")+"\n", ConsoleViewContentType.SYSTEM_OUTPUT);
		antlr.removeListeners();
		RunANTLRListener listener = new RunANTLRListener(antlr, console);
		antlr.addListener(listener);

		try {
			antlr.processGrammarsOnCommandLine();
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
	}

	public static Map<String,String> getANTLRArgs(Project project, VirtualFile vfile) {
		Map<String,String> args = new HashMap<String, String>();
		String qualFileName = vfile.getPath();
		String sourcePath = getParentDir(vfile);

		String package_ = getProp(project, qualFileName, "package", MISSING);
		if ( package_!=MISSING) {
			args.put("-package", package_);
		}

		String language = getProp(project, qualFileName, "language", MISSING);
		if ( language!=MISSING) {
			args.put("-Dlanguage="+language, "");
		}

		// create gen dir at root of project by default, but add in package if any
		VirtualFile contentRoot = getContentRoot(project, vfile);
		String outputDirName = getOutputDirName(project, qualFileName, contentRoot, package_);
		args.put("-o", outputDirName);

		String libDir = getProp(project, qualFileName, "lib-dir", sourcePath);
		args.put("-lib", libDir);

		String encoding = getProp(project, qualFileName, "encoding", MISSING);
		if ( encoding!=MISSING ) {
			args.put("-encoding", encoding);
		}

		if ( getBooleanProp(project, qualFileName, "gen-listener", true) ) {
			args.put("-listener", "");
		}
		else {
			args.put("-no-listener", "");
		}
		if ( getBooleanProp(project, qualFileName, "gen-visitor", true) ) {
			args.put("-visitor", "");
		}
		else {
			args.put("-no-visitor", "");
		}

		return args;
	}

	public static String getOutputDirName(Project project, String qualFileName, VirtualFile contentRoot, String package_) {
		String outputDirName = contentRoot.getPath()+File.separator+OUTPUT_DIR_NAME;
		outputDirName = getProp(project, qualFileName, "output-dir", outputDirName);
		if ( package_!=MISSING ) {
			outputDirName += File.separator+package_.replace('.',File.separatorChar);
		}
		return outputDirName;
	}

	public static String getProp(Project project, String qualFileName, String name, String defaultValue) {
		PropertiesComponent props = PropertiesComponent.getInstance(project);
		String v = props.getValue(ConfigANTLRPerGrammar.getPropNameForFile(qualFileName, name));
		if ( v==null || v.trim().length()==0 ) return defaultValue;
		return v;
	}

	public static boolean getBooleanProp(Project project, String qualFileName, String name, boolean defaultValue) {
		PropertiesComponent props = PropertiesComponent.getInstance(project);
		return props.getBoolean(ConfigANTLRPerGrammar.getPropNameForFile(qualFileName, name), defaultValue);
	}

	public static String getParentDir(VirtualFile vfile) {
		return vfile.getParent().getPath();
	}

	public static VirtualFile getContentRoot(Project project, VirtualFile vfile) {
		VirtualFile root =
			ProjectRootManager.getInstance(project)
				.getFileIndex().getContentRootForFile(vfile);
		if ( root!=null ) return root;
		return vfile.getParent();
	}

	public String getOutputDirName() {
		VirtualFile contentRoot = getContentRoot(project, grammarFile);
		Map<String,String> argMap = getANTLRArgs(project, grammarFile);
		String package_ = argMap.get("-package");
		if ( package_==null ) {
			package_ = MISSING;
		}
		return getOutputDirName(project, grammarFile.getPath(), contentRoot, package_);
	}
}
