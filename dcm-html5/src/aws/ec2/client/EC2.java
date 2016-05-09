package aws.ec2.client;

import static com.amazonaws.regions.RegionUtils.getRegion;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.ec2.AmazonEC2Client;

public class EC2 implements ListInstances {

	final AmazonEC2Client client;
	
	public EC2(String region){
		client = new AmazonEC2Client(new DefaultAWSCredentialsProviderChain());
		client.setRegion(getRegion(region));
	}
	
	public EC2(String accessKey, String secretKey, String region){
		client = new AmazonEC2Client(new BasicAWSCredentials(accessKey, secretKey));		
		client.setRegion(getRegion(region));//"eu-central-1"
	}

	@Override
	public AmazonEC2Client getEc2client() {
		return client;
	}

}
