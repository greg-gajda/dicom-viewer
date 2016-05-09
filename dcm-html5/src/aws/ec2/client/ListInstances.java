package aws.ec2.client;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public interface ListInstances extends EC2Client {

	String IS_RUNNING = "running";

	default Map<String, String> getPrivateIpsPublicDns(boolean onlyRunning) {
		Map<String, String> map = getEc2client().describeInstances().getReservations().stream().
				flatMap(r -> r.getInstances().stream()).
				filter(i -> onlyRunning ? IS_RUNNING.equals(i.getState().getName()) : i.getState() != null).
				collect(Collectors.toMap(i -> i.getPrivateIpAddress(), i -> i.getPublicDnsName()));
		return map;
	}
	
	default List<String> getPrivateIps(boolean onlyRunning) {
		List<String> list = getEc2client().describeInstances().getReservations().stream().
				flatMap(r -> r.getInstances().stream()).
				filter(i -> onlyRunning ? IS_RUNNING.equals(i.getState().getName()) : i.getState() != null).
				map(i -> i.getPrivateIpAddress()).
				collect(Collectors.toList());
		return list;
	}

	default List<String> getPrivateDns(boolean onlyRunning) {
		List<String> list = getEc2client().describeInstances().getReservations().stream().
				flatMap(r -> r.getInstances().stream()).
				filter(i -> onlyRunning ? IS_RUNNING.equals(i.getState().getName()) : i.getState() != null).
				map(i -> i.getPrivateDnsName()).
				collect(Collectors.toList());
		return list;
	}

	default List<String> getPublicIps(boolean onlyRunning) {
		List<String> list = getEc2client().describeInstances().getReservations().stream().
				flatMap(r -> r.getInstances().stream()).
				filter(i -> onlyRunning ? IS_RUNNING.equals(i.getState().getName()) : i.getState() != null).
				map(i -> i.getPublicIpAddress()).collect(Collectors.toList());
		return list;
	}

	default List<String> getPublicDns(boolean onlyRunning) {
		List<String> list = getEc2client().describeInstances().getReservations().stream().
				flatMap(r -> r.getInstances().stream()).
				filter(i -> onlyRunning ? IS_RUNNING.equals(i.getState().getName()) : i.getState() != null).
				map(i -> i.getPublicDnsName()).collect(Collectors.toList());
		return list;
	}

}
