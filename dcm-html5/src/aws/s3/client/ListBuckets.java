package aws.s3.client;

import java.util.List;
import java.util.stream.Collectors;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.model.Bucket;

public interface ListBuckets extends S3Client {
	
	default List<String> getBucketNames(){		
		try{
			List<Bucket> buckets = getS3client().listBuckets();
			return buckets.stream().map(b -> b.getName()).collect(Collectors.toList());
		} catch (AmazonClientException e) {
			throw new S3ClientException(e);
		}			
	}
}
