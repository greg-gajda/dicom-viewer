package cache.control;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

import javax.annotation.Resource;
import javax.ejb.Asynchronous;
import javax.ejb.Local;
import javax.ejb.Stateless;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cache.boundary.CacheResource;
import cache.boundary.ImageCache;
import config.ConfigParams;
import utils.FileUtils;

@Local({ImageCache.class, CacheResource.class})
@Stateless
public class ImageCacheBean implements ImageCache, CacheResource {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Resource(name = ConfigParams.CACHE_PATH)
	String cacheDir;

	@Override
	public String getCacheDir() {
		return cacheDir;
	}
	
	@Override
	public boolean contains(String image) {
		Path path = Paths.get(cacheDir, image);
		return Files.exists(path);
	}

	@Override
	public byte[] get(String image) {
		try {
			Path path = Paths.get(cacheDir, image);
			if (Files.exists(path)) {
				byte[] bytes = FileUtils.getBytesFromFile(new File(cacheDir, image)).array(); //Files.readAllBytes(Paths.get(cacheDir, image));
				log.debug(String.format("Object %s get from cache", image));
				return bytes;
			}
		} catch (/*IO*/Exception e) {
			log.warn(String.format("Error getting object from cache %s", image));
		}
		return null;
	}
	
	@Override
	public Optional<byte[]> getOptional(String image) {
		try {
			Path path = Paths.get(cacheDir, image);
			if (Files.exists(path)) {
				byte[] bytes = Files.readAllBytes(Paths.get(cacheDir, image));
				log.debug(String.format("Object %s get from cache", image));
				return Optional.of(bytes);
			}
		} catch (IOException e) {
			log.warn(String.format("Error getting object from cache %s", image));
		}
		return Optional.empty();
	}
	
	@Override
	public void put(String image, byte[] content) {
		try {
			Files.write(Paths.get(cacheDir, image), content, StandardOpenOption.CREATE);
			log.debug(String.format("Object %s put into cache", image));
		} catch (IOException e) {
			log.warn(String.format("Error putting object into cache %s", image));
		}
	}

	@Override
	public String tryGet(String image) {
		File[] files = new File(cacheDir).listFiles(new FileFilter() {
			@Override
			public boolean accept(File file) {
				return file.getName().startsWith(image.concat("="));
			}
		});		
		if (files.length > 0) {
			log.debug(String.format("Object %s ready to be taken from cache", image));
			return files[0].getAbsolutePath();
		}
		return null;
	}

	@Asynchronous
	@Override
	public void putAsync(String image, byte[] content) {
		try {
			Files.write(Paths.get(cacheDir, image), content, StandardOpenOption.CREATE);
			log.debug(String.format("Object %s put into cache", image));
		} catch (IOException e) {
			log.warn(String.format("Error putting object into cache %s", image));
		}
	}

}
