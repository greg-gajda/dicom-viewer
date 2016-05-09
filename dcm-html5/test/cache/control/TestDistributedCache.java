package cache.control;

import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;

public class TestDistributedCache {

	@Before
	public void setUp() throws Exception {
		org.apache.log4j.BasicConfigurator.configure();
	}

	@Test
	public void testInit() {
		DistributedCache dc = new DistributedCache();
		dc.region = "eu-central-1";
		dc.init();
		Map<String, String> ipAndDns = dc.ec2.getPrivateIpsPublicDns(true);
		assertTrue(ipAndDns.isEmpty() == false);
		//ipAndDns .forEach((k, v) -> System.out.println(k + " - " + v));
	}

}
