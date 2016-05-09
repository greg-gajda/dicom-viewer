package aws.s3.client;

public class S3ClientException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public S3ClientException(Throwable e){
		super(e);
	}
	
	public S3ClientException(String message, Throwable e){
		super(message, e);
	}
	
}
