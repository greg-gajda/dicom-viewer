package aws.s3.client;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.model.CannedAccessControlList;

public interface SetObjectACL extends S3Client{

	default void setPrivate(String bucket, String key){
		try{
			getS3client().setObjectAcl(bucket, key, CannedAccessControlList.Private);
		} catch (AmazonClientException e) {
			throw new S3ClientException(String.format("Error setting ACL of object with key %s in bucket %s", key, bucket), e);
		}
	}
	
	default void setPublicRead(String bucket, String key){
		try{
			getS3client().setObjectAcl(bucket, key, CannedAccessControlList.PublicRead);
		} catch (AmazonClientException e) {
			throw new S3ClientException(String.format("Error setting ACL of object with key %s in bucket %s", key, bucket), e);
		}
	}

	default void setPublicReadWrite(String bucket, String key){
		try{
			getS3client().setObjectAcl(bucket, key, CannedAccessControlList.PublicReadWrite);
		} catch (AmazonClientException e) {
			throw new S3ClientException(String.format("Error setting ACL of object with key %s in bucket %s", key, bucket), e);
		}
	}

}
