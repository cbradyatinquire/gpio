package gpio;

import java.io.IOException;
import java.nio.file.*;

public class PortWatcher extends Thread {

	public int[] changeCounts = new int[14];
	WatchService watchService;
	WatchKey watchKey;
	
	public PortWatcher ( String pinDir ) {
		Path pinDirectory = FileSystems.getDefault().getPath(pinDir);
		for (int p = 0; p<14; p++) {
			changeCounts[p]=0;
		}
		try {
			watchService = FileSystems.getDefault().newWatchService();
			watchKey = pinDirectory.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void run() {
		while (true) {
			try {
				final WatchKey wk = watchService.take();
				for (WatchEvent<?> event : wk.pollEvents()) {
					//we're only registered for "ENTRY_MODIFY" so the context is always a Path.
					final Path changed = (Path) event.context();
					String changedString = changed.toString();
					int numBegin = changedString.indexOf("gpio");
					if (numBegin > 0) {
						numBegin = numBegin + "gpio".length();
						String id = changedString.substring(numBegin);
						int port = Integer.valueOf(id);
						changeCounts[port]++;
					}
				}
				// reset the key
				boolean valid = wk.reset();
				if (!valid) {
					System.out.println("Key has been unregistered");
				}
			}
			catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}

}
