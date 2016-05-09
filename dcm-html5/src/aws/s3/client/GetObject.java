package aws.s3.client;

import java.io.File;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;

import utils.FileUtils;

public interface GetObject extends S3Client {
	
	default byte[] get(String bucket, String key){
		try{
			S3Object s3o = getS3client().getObject(bucket, key);
			return FileUtils.readBytes(s3o.getObjectContent());
		} catch (AmazonClientException e) {
			throw new S3ClientException(String.format("Error accessing key %s in bucket %s", key, bucket), e);
		}			
	}
	
	default String getToFile(String bucket, String key, File file){		
		try{ ObjectMetadata metadata = getS3client().getObject(new GetObjectRequest(bucket, key), file);
			return metadata.getETag();
		} catch (AmazonClientException e) {
			throw new S3ClientException(String.format("Error accessing key %s in bucket %s", key, bucket), e);
		}
	}
	
}
