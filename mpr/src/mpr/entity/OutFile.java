package mpr.entity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;

import utils.FileUtils;

public class OutFile {

	private File file;
	private FileOutputStream os;
	private Map<Integer, Object> tags;

	public OutFile(File file) {
		if (file == null) {
			throw new IllegalArgumentException("File cannot be null");
		}
		this.file = file;
		tags = new HashMap<>();
	}

	public FileOutputStream getOutputStream() {
		if (os == null) {
			try {
				os = new FileOutputStream(getFile());
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			}
		}
		return os;
	}

	public File getFile() {
		return file;
	}

	public void disposeStream() {
		FileUtils.safeClose(os);
	}

	public Map<Integer, Object> getTags() {
		return tags;
	}

}
