package glimpseUtil;

import java.io.File;

/**
 * Headless smoke test for GLIMPSEFiles.showFileInTextEditor.
 *
 * This test is intentionally minimal and should run without JavaFX.
 * It verifies that the method doesn't throw if the configured text editor is invalid
 * and that it properly falls back to the Desktop handler (which may still fail on
 * some headless systems, so we only assert "no crash").
 */
public final class GLIMPSEFilesEditorSmokeTest {
	public static void main(String[] args) throws Exception {
		// Ensure vars and utils exist.
		GLIMPSEVariables vars = GLIMPSEVariables.getInstance();
		GLIMPSEFiles files = GLIMPSEFiles.getInstance();
		files.init(GLIMPSEUtils.getInstance(), vars, null, null);

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
