package aws.s3.client;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.model.ObjectMetadata;

public interface PutObject extends S3Client{

	default String put(String bucket, String key, File file){
		try{
			return getS3client().putObject(bucket, key, file).getETag();
		} catch (AmazonClientException e) {
			throw new S3ClientException(String.format("Error putting object with key %s into bucket %s", key, bucket), e);
		}
	}
	
	default String put(String bucket, String key, InputStream is){
		try{
			return getS3client().putObject(bucket, key, is, new ObjectMetadata()).getETag();
		} catch (AmazonClientException e) {
			throw new S3ClientException(String.format("Error putting object with key %s into bucket %s", key, bucket), e);
		}
	}
	
	default String put(String bucket, String key, byte[] content){
		try{
			ObjectMetadata metadata = new ObjectMetadata();
		    metadata.setContentLength(content.length);			
			ByteArrayInputStream bais = new ByteArrayInputStream(content);			
			return getS3client().putObject(bucket, key, bais, metadata).getETag();
		} catch (AmazonClientException e) {
			throw new S3ClientException(String.format("Error putting object with key %s into bucket %s", key, bucket), e);
		}
	}

}
