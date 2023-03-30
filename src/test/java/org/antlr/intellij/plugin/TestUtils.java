package org.antlr.intellij.plugin;

import com.intellij.openapi.diagnostic.Attachment;
import com.intellij.openapi.diagnostic.ExceptionWithAttachments;
import com.intellij.util.ThrowableRunnable;
import com.intellij.util.lang.CompoundRuntimeException;

import java.io.PrintWriter;
import java.io.StringWriter;

public class TestUtils {
	public static void tearDownIgnoringObjectNotDisposedException(ThrowableRunnable<Exception> delegate) throws Exception {
		try {
			delegate.run();
		} catch (RuntimeException e) {
			// We don't want to release the editor in the Tool Output tool window, so we ignore
			// ObjectNotDisposedExceptions related to this particular editor
			if (exceptionShouldBeIgnored(e)) {
				return;
			}

			throw e;
		}
	}

	private static boolean exceptionShouldBeIgnored(RuntimeException e) {
		if (e instanceof CompoundRuntimeException) {
			for (Throwable exception : ((CompoundRuntimeException) e).getExceptions()) {
				if (exception instanceof RuntimeException && exceptionShouldBeIgnored((RuntimeException) exception)) {
					return true;
				}
			}
		}

		if (e.getClass().getName().equals("com.intellij.openapi.util.TraceableDisposable$ObjectNotDisposedException")) {
			StringWriter stringWriter = new StringWriter();
			e.printStackTrace(new PrintWriter(stringWriter));
			String stack = stringWriter.toString();

			if (stackMatchesOurEditorCreation(stack)) {
				return true;
			}
		}

		if (e.getClass().getName().equals("com.intellij.openapi.util.TraceableDisposable$DisposalException")) {
			for (Attachment attachment : ((ExceptionWithAttachments) e).getAttachments()) {
				if (stackMatchesOurEditorCreation(attachment.getDisplayText())) {
					return true;
				}
			}
		}

		return false;
	}

	private static boolean stackMatchesOurEditorCreation(String stack) {
		return stack.contains("ANTLRv4PluginController.createToolWindows")
			|| stack.contains("Issue559Test")
			|| stack.contains("Issue540Test")
			|| stack.contains("org.antlr.intellij.plugin.preview.InputPanel.createPreviewEditor");
	}
}


