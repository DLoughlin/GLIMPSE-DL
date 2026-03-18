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

	private static void printTokenization(String label, String command) {
		java.util.List<String> tokens = CommandLineTokenizer.tokenize(command);
		System.out.println(label + " command=" + command);
		System.out.println(label + " tokens =" + tokens);
		if (!tokens.isEmpty()) {
			File token0 = new File(tokens.get(0));
			System.out.println(label + " token0 =" + token0.getPath() + " exists=" + token0.exists() + " absolute=" + token0.getAbsolutePath());
		}
	}

	public static void main(String[] args) throws Exception {
		GLIMPSEVariables vars = GLIMPSEVariables.getInstance();
		GLIMPSEFiles files = GLIMPSEFiles.getInstance();

		// Inject headless utils.
		files.init(new HeadlessUtils(), vars, null, null);

		printTokenization("plain-notepad", "c:\\windows\\notepad.exe");
		printTokenization("quoted-notepad", "\"c:\\windows\\notepad.exe\"");
		printTokenization("notepad-plus-plus-style", "\"C:\\Program Files\\Notepad++\\notepad++.exe\" -multiInst");

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