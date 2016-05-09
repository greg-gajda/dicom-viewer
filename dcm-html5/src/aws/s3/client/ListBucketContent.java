package aws.s3.client;

import java.util.List;
import java.util.stream.Collectors;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.model.ObjectListing;

public interface ListBucketContent extends S3Client {
	
	default List<String> getObjectKeys(String bucket){
		try{
			ObjectListing objects = getS3client().listObjects(bucket);
			return objects.getObjectSummaries().stream().map(s3 -> s3.getKey()).collect(Collectors.toList());
		} catch (AmazonClientException e) {
			throw new S3ClientException(String.format("Error accessing bucket %s", bucket), e);
		}			
	}
}
