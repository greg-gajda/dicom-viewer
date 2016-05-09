package aws.ec2.client;

public class EC2ClientException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public EC2ClientException(Throwable e) {
		super(e);
	}

	public EC2ClientException(String message, Throwable e) {
		super(message, e);
	}

}
