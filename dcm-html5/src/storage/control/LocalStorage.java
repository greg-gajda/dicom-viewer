package storage.control;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.annotation.Resource;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import config.ConfigParams;
import storage.boundary.ImageStorage;
import storage.entity.Kind;
import utils.FileUtils;

@Local(ImageStorage.class)
@Stateless
public class LocalStorage implements ImageStorage {

	@Resource(name = ConfigParams.LOCAL_STORAGE_PATH)
	String path;

	@Override
	public Kind getKind() {
		return Kind.LOCAL;
	}

	private byte[] getFileContent(File file) throws IOException {
		return FileUtils.readBytes(new BufferedInputStream(new FileInputStream(file)));
	}

	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	@Override
	public byte[] getImage(String image) {
		try {
			return getFileContent(new File(path.concat(image)));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	@Override
	public void store(String image, byte[] content) {
		File file = new File(path, image);
		FileUtils.saveFile(file, content);		
	}

}
