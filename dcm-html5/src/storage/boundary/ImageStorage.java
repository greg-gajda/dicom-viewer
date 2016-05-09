package storage.boundary;

import storage.entity.Kind;

public interface ImageStorage {
	Kind getKind();
	byte[] getImage(String image);
	void store(String image, byte[] content);
}
