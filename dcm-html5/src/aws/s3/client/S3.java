package aws.s3.client;

import static com.amazonaws.regions.RegionUtils.getRegion;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;

public class S3 implements DeleteObject, GetObject, ListBuckets, ListBucketContent, PutObject, SetObjectACL, DownloadUrl{

	final AmazonS3 s3client;
		
	public S3(String region){
		s3client = new AmazonS3Client(new DefaultAWSCredentialsProviderChain());
		s3client.setRegion(getRegion(region));
	}

	public S3(String accessKey, String secretKey, String region){
		s3client = new AmazonS3Client(new BasicAWSCredentials(accessKey, secretKey));
		s3client.setRegion(getRegion(region));
	}
	
	public AmazonS3 getS3client(){
		return s3client;
	}

}
