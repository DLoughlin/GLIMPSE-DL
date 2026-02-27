package glimpseUtil;

import java.io.File;

/**
 * Headless smoke test for GLIMPSEFiles.showFileInTextEditor.
 *
 * This avoids pulling in JavaFX-heavy GLIMPSEUtils by substituting a minimal
 * implementation that only supports warningMessage.
 */
public final class GLIMPSEFilesEditorSmokeTestHeadless {
	/** Minimal stub to avoid JavaFX dependencies. */
	private static final class HeadlessUtils extends GLIMPSEUtils {
		@Override
		public void warningMessage(String s) {
			System.out.println("warningMessage: " + s);
		}
	}

	public static void main(String[] args) throws Exception {
		GLIMPSEVariables vars = GLIMPSEVariables.getInstance();
		GLIMPSEFiles files = GLIMPSEFiles.getInstance();

		// Inject headless utils.
		files.init(new HeadlessUtils(), vars, null, null);

		// Force a bogus editor so we exercise the ProcessRunner-start failure path.
		vars.setTextEditor("this_editor_does_not_exist_hopefully");

		File tmp = File.createTempFile("glimpse_editor_smoke", ".txt");
		tmp.deleteOnExit();
		files.saveFile("smoke", tmp);

		try {
			files.showFileInTextEditor(tmp.getAbsolutePath());
			System.out.println("OK: showFileInTextEditor returned without throwing");
		} catch (Throwable t) {
			System.err.println("FAIL: showFileInTextEditor threw: " + t);
			t.printStackTrace();
			System.exit(1);
		}
	}
}
