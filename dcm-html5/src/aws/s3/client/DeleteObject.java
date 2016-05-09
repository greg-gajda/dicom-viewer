package aws.s3.client;

import com.amazonaws.AmazonClientException;

public interface DeleteObject extends S3Client {
		
	default void delete(String bucket, String key){
		try{ 
			getS3client().deleteObject(bucket, key);
		} catch (AmazonClientException e) {
			throw new S3ClientException(String.format("Error removing object with key %s from bucket %s", key, bucket), e);
		}
	}
}
