package org.antlr.intellij.plugin.parsing;

import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.antlr.intellij.adaptor.lexer.RuleIElementType;
import org.antlr.intellij.plugin.ANTLRv4PluginController;
import org.antlr.intellij.plugin.ANTLRv4TokenTypes;
import org.antlr.intellij.plugin.configdialogs.ANTLRv4GrammarProperties;
import org.antlr.intellij.plugin.parser.ANTLRv4Parser;
import org.antlr.intellij.plugin.preview.PreviewState;
import org.antlr.intellij.plugin.psi.AtAction;
import org.antlr.intellij.plugin.psi.GrammarSpecNode;
import org.antlr.v4.Tool;
import org.antlr.v4.codegen.CodeGenerator;
import org.antlr.v4.runtime.misc.Utils;
import org.antlr.v4.tool.Grammar;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.stringtemplate.v4.misc.Misc;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

import static com.intellij.psi.util.PsiTreeUtil.getChildOfType;
import static org.antlr.intellij.plugin.configdialogs.ANTLRv4GrammarPropertiesStore.getGrammarProperties;
import static org.antlr.intellij.plugin.psi.MyPsiUtils.findChildrenOfType;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;

// learned how to do from Grammar-Kit by Gregory Shrago
public class RunANTLROnGrammarFile extends Task.Modal {
	public static final Logger LOG = Logger.getInstance("RunANTLROnGrammarFile");
	public static final String OUTPUT_DIR_NAME = "gen" ;
	public static final String groupDisplayId = "ANTLR 4 Parser Generation";

	private static final Pattern PACKAGE_DEFINITION_REGEX = Pattern.compile("package\\s+[a-z][a-z0-9_]*(\\.[a-z0-9_]+)+[0-9a-z_];");

	private final VirtualFile grammarFile;
	private final Project project;
	private final boolean forceGeneration;

	public RunANTLROnGrammarFile(VirtualFile grammarFile,
								 @Nullable final Project project,
								 @NotNull final String title,
								 final boolean canBeCancelled,
								 boolean forceGeneration)
	{
		super(project, title, canBeCancelled);
		this.grammarFile = grammarFile;
		this.project = project;
		this.forceGeneration = forceGeneration;
	}

	@Override
	public void run(@NotNull ProgressIndicator indicator) {
		indicator.setIndeterminate(true);
		ANTLRv4GrammarProperties grammarProperties = getGrammarProperties(project, grammarFile);
		boolean autogen = grammarProperties.shouldAutoGenerateParser();
		if ( forceGeneration || (autogen && isGrammarStale(grammarProperties)) ) {
			ReadAction.run(() -> antlr(grammarFile));
		}
		else {
			ANTLRv4PluginController controller = ANTLRv4PluginController.getInstance(project);
			final PreviewState previewState = controller.getPreviewState(grammarFile);
			// is lexer file? gen .tokens file no matter what as tokens might have changed;
			// a parser that feeds off of that file will need to see the changes.
			if ( previewState.g==null && previewState.lg!=null) {
				Grammar g = previewState.lg;
				String language = g.getOptionString(ANTLRv4GrammarProperties.PROP_LANGUAGE);
				Tool tool = ParsingUtils.createANTLRToolForLoadingGrammars(getGrammarProperties(project, grammarFile));
				CodeGenerator gen = CodeGenerator.create(tool, g, language);
				gen.writeVocabFile();
			}
		}
	}

	// TODO: lots of duplication with antlr() function.
	private boolean isGrammarStale(ANTLRv4GrammarProperties grammarProperties) {
		String sourcePath = grammarProperties.resolveLibDir(project, getParentDir(grammarFile));
		String fullyQualifiedInputFileName = sourcePath+File.separator+grammarFile.getName();

		ANTLRv4PluginController controller = ANTLRv4PluginController.getInstance(project);
		final PreviewState previewState = controller.getPreviewState(grammarFile);
		Grammar g = previewState.getMainGrammar();
		// Grammar should be updated in the preview state before calling this function
		if ( g==null ) {
			return false;
		}

		String language = g.getOptionString(ANTLRv4GrammarProperties.PROP_LANGUAGE);
		CodeGenerator generator = CodeGenerator.create(null, g, language);
		String recognizerFileName = generator.getRecognizerFileName();

		VirtualFile contentRoot = getContentRoot(project, grammarFile);
		String package_ = grammarProperties.getPackage();
		String outputDirName = grammarProperties.resolveOutputDirName(project, contentRoot, package_);
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
	private void antlr(VirtualFile vfile) {
		if ( vfile==null ) return;

		LOG.info("antlr(\""+vfile.getPath()+"\")");
		List<String> args = getANTLRArgsAsList(project, vfile);

		String sourcePath = getParentDir(vfile);
		String fullyQualifiedInputFileName = sourcePath+File.separator+vfile.getName();
		args.add(fullyQualifiedInputFileName); // add grammar file last

		String lexerGrammarFileName = ParsingUtils.getLexerNameFromParserFileName(fullyQualifiedInputFileName);
		if ( new File(lexerGrammarFileName).exists() ) {
			// build the lexer too as the grammar surely uses it if it exists
			args.add(lexerGrammarFileName);
		}

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
			ANTLRv4PluginController.showConsoleWindow(project);
		}
	}

	public static List<String> getANTLRArgsAsList(Project project, VirtualFile vfile) {
		Map<String,String> argMap = getANTLRArgs(project, vfile);
		List<String> args = new ArrayList<>();
		for (String option : argMap.keySet()) {
			args.add(option);
			String value = argMap.get(option);
			if ( value.length()!=0 ) {
				args.add(value);
			}
		}
		return args;
	}

	private static Map<String,String> getANTLRArgs(Project project, VirtualFile vfile) {
		Map<String,String> args = new HashMap<>();
		ANTLRv4GrammarProperties grammarProperties = getGrammarProperties(project, vfile);
		String sourcePath = getParentDir(vfile);

		String package_ = grammarProperties.getPackage();
		if ( isBlank(package_) && !hasPackageDeclarationInHeader(project, vfile)) {
			package_ = ProjectRootManager.getInstance(project).getFileIndex().getPackageNameByDirectory(vfile.getParent());
		}
		if ( isNotBlank(package_) ) {
			args.put("-package", package_);
		}

		String language = grammarProperties.getLanguage();
		if ( isNotBlank(language) ) {
			args.put("-Dlanguage="+language, "");
		}

		// create gen dir at root of project by default, but add in package if any
		VirtualFile contentRoot = getContentRoot(project, vfile);
		String outputDirName = grammarProperties.resolveOutputDirName(project, contentRoot, package_);
		args.put("-o", outputDirName);

		String libDir = grammarProperties.resolveLibDir(project, sourcePath);
		File f = new File(libDir);
		if ( !f.isAbsolute() ) { // if not absolute file spec, it's relative to project root
			libDir = contentRoot.getPath()+File.separator+libDir;
		}
		args.put("-lib", libDir);

		String encoding = grammarProperties.getEncoding();
		if ( isNotBlank(encoding) ) {
			args.put("-encoding", encoding);
		}

		if ( grammarProperties.shouldGenerateParseTreeListener() ) {
			args.put("-listener", "");
		}
		else {
			args.put("-no-listener", "");
		}
		if ( grammarProperties.shouldGenerateParseTreeVisitor() ) {
			args.put("-visitor", "");
		}
		else {
			args.put("-no-visitor", "");
		}

		return args;
	}

	private static boolean hasPackageDeclarationInHeader(Project project, VirtualFile grammarFile) {
		return ApplicationManager.getApplication().runReadAction((Computable<Boolean>) () -> {
			PsiFile file = PsiManager.getInstance(project).findFile(grammarFile);
			GrammarSpecNode grammarSpecNode = getChildOfType(file, GrammarSpecNode.class);

			if ( grammarSpecNode != null ) {
				RuleIElementType prequelElementType = ANTLRv4TokenTypes.getRuleElementType(ANTLRv4Parser.RULE_prequelConstruct);

				for ( PsiElement prequelConstruct : findChildrenOfType(grammarSpecNode, prequelElementType) ) {
					AtAction atAction = getChildOfType(prequelConstruct, AtAction.class);

					if ( atAction!=null && atAction.getIdText().equals("header") ) {
						return PACKAGE_DEFINITION_REGEX.matcher(atAction.getActionBlockText()).find();
					}
				}
			}

			return false;
		});
	}

	private static String getParentDir(VirtualFile vfile) {
		return vfile.getParent().getPath();
	}

	private static VirtualFile getContentRoot(Project project, VirtualFile vfile) {
		return ReadAction.compute(() -> {
			VirtualFile root = ProjectRootManager.getInstance(project).getFileIndex().getContentRootForFile(vfile);

			if (root != null) {
				return root;
			}
			return vfile.getParent();
		});
	}

	public String getOutputDirName() {
		VirtualFile contentRoot = getContentRoot(project, grammarFile);
		Map<String,String> argMap = getANTLRArgs(project, grammarFile);
		String package_ = argMap.get("-package");

		return getGrammarProperties(project, grammarFile)
				.resolveOutputDirName(project, contentRoot, package_);
	}
}
