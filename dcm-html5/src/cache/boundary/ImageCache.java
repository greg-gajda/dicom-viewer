package cache.boundary;

import java.util.Optional;

public interface ImageCache {
	boolean contains(String image);
	Optional<byte[]> getOptional(String image);
	byte[] get(String image);
	void put(String image, byte[] content);
	void putAsync(String image, byte[] content);
	String getCacheDir();
	String tryGet(String image);
}
