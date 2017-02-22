package org.kneelawk.simplecursemodpackdownloader;

import java.io.IOException;

import org.kneelawk.cpcontrol.CPControl3;

public class SimpleCurseModpackDownloaderLauncher {

	public static void main(String[] args) {
		CPControl3 cp = new CPControl3("org.kneelawk.simplecursemodpackdownloader.SimpleCurseModpackDownloader");

		cp.addExtractingFromFileLibrary(CPControl3.ME)
				.addLibrary("application", new CPControl3.DirectoryEntryFilter("app"), CPControl3.ALWAYS_DELETE)
				.addLibrary("libraries", new CPControl3.DirectoryEntryFilter("libs"), CPControl3.ALWAYS_DELETE);

		try {
			cp.launch(args);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
