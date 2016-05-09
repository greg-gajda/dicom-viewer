package converter.boundary;

public interface ImageConverter {

	byte[] convert(byte[] content, String format, Integer size, Float wc, Float ww);
}
