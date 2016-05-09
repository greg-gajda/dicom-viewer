package storage.control;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Local;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import aws.s3.client.S3;
import config.ConfigParams;
import storage.boundary.ImageStorage;
import storage.entity.Kind;

@Local(ImageStorage.class)
@Singleton
@Startup
public class S3Storage implements ImageStorage {
	
	@Resource(name = ConfigParams.AWS_BUCKET)
	String bucket;

	@Resource(name = ConfigParams.AWS_REGION)
	String region;

	S3 s3;

	@PostConstruct
	public void init() {
		s3 = new S3(region);
	}

	@Override
	public Kind getKind() {
		return Kind.S3;
	}

	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	@Override
	public byte[] getImage(String image) {
		return s3.get(bucket, "images/".concat(image));		
	}

	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	@Override
	public void store(String image, byte[] content) {
		s3.put(bucket, "images/".concat(image), content);
	}

}
