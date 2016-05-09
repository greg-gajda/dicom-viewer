package cache.control;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.Singleton;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonClientException;
import com.amazonaws.util.EC2MetadataUtils;

import aws.ec2.client.EC2;
import config.ConfigParams;

@Singleton
public class DistributedCache {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Resource(name = ConfigParams.AWS_REGION)
	String region;

	static final int REFRESH_AWS_HOSTS_RATE = 100000;
	
	/* list of ips of running instances */
	Map<String, String> ips;
	/* time stamp to refresh ips list during get */
	Date timestamp;
	/* AWS private IP */
	String localIp;
	/* ec2 client */
	EC2 ec2;
	/* map of jax-rs clients */
	final Map<String, Client> clients = new HashMap<>();

	@PostConstruct
	public void init() {
		timestamp = new Date();
		ec2 = new EC2(region);
		try {
			localIp = EC2MetadataUtils.getInstanceInfo().getPrivateIp();			
		} catch (AmazonClientException e) {
			localIp = "";
			log.info("Seems to be out of AWS, error message: {}", e.getLocalizedMessage());
		}
		ips = ec2.getPrivateIpsPublicDns(true);
	}

	@PreDestroy
	public void close() {
		clients.entrySet().forEach(e -> e.getValue().close());
	}

	public byte[] get(String image) {
		if (new Date().getTime() - timestamp.getTime() > REFRESH_AWS_HOSTS_RATE) {
			ips = ec2.getPrivateIpsPublicDns(true);
			log.info("AWS private IP list refreshed");
			timestamp = new Date();
		}

		Collection<String> available = localIp.isEmpty() ? ips.values() : ips.keySet();

		byte[] content = available.stream().filter(ip -> !ip.equals(localIp)).map(ip -> {
			Client client = clients.get(ip);
			if (client == null) {
				client = ClientBuilder.newBuilder().build();
				clients.put(ip, client);
			}
			String uri = String.format("http://%s/%s/%s", ip, "rest/cache", image);
			Response response = client.target(uri).request().get();
			log.info("Distributed cache call for {}, response status {}", uri, response.getStatus());
			byte[] bytes = null;
			if (response.getStatus() == 200) {
				bytes = response.readEntity(byte[].class);			
			}
			return bytes;
		}).filter(bytes -> bytes != null).findFirst().orElse(null);

		return content;
	}

	public Optional<byte[]> getOptional(String image) {
		return Optional.ofNullable(get(image));
	}

}
