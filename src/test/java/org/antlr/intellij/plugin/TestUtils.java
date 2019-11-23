package org.antlr.intellij.plugin;

import com.intellij.util.ThrowableRunnable;

import java.io.PrintWriter;
import java.io.StringWriter;

public class TestUtils {
	public static void tearDownIgnoringObjectNotDisposedException(ThrowableRunnable<Exception> delegate) throws Exception {
		try {
			delegate.run();
		} catch (RuntimeException e) {
			// We don't want to release the editor in the Tool Output tool window, so we ignore
			// ObjectNotDisposedExceptions related to this particular editor
			if (e.getClass().getName().equals("com.intellij.openapi.util.TraceableDisposable$ObjectNotDisposedException")) {
				StringWriter stringWriter = new StringWriter();
				e.printStackTrace(new PrintWriter(stringWriter));
				String stack = stringWriter.toString();
				if ( stack.contains("ANTLRv4PluginController.createToolWindows")
						|| stack.contains("org.antlr.intellij.plugin.preview.InputPanel.createPreviewEditor") ) {
					return;
				}
			}

			throw e;
		}
	}
}


