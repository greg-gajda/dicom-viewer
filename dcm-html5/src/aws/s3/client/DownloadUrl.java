package aws.s3.client;

import java.net.URL;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;

public interface DownloadUrl extends S3Client {

	default URL generate(String bucket, String key){
		try{
			GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, key);
			return getS3client().generatePresignedUrl(request);
		} catch (AmazonClientException e) {
			throw new S3ClientException(String.format("Error accessing key %s in bucket %s", key, bucket), e);
		}
	}
}
