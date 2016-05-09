package aws.s3.client;

import com.amazonaws.services.s3.AmazonS3;

public interface S3Client {
	AmazonS3 getS3client();
}
