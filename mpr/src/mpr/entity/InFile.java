package mpr.entity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import utils.FileUtils;

public class InFile {
	
	private FileInputStream is;
	private File file;

    public InFile(File file) {
        if (file == null) {
            throw new IllegalArgumentException("File cannot be null"); 
        }
        this.file = file;
    }

    public File getFile() {
        return file;
    }

	public FileInputStream getInputStream() {
		if(is == null){
			try {
				is = new FileInputStream(getFile());
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			}
		}
		return is;
	}
    
    public void disposeStream(){
    	FileUtils.safeClose(is);
    }

}
