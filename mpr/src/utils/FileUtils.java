package utils;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class FileUtils {
	public static void saveFile(File file, byte[] content) {
		try {
			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
			bos.write(content);
			bos.flush();
			bos.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static byte[] readBytes(InputStream is) {
		if (is == null) {
			throw new IllegalStateException("inputStream is null");
		}
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		transfer(is, os);
		byte[] bytes = os.toByteArray();
		return bytes;
	}

	public static int transfer(InputStream in, OutputStream out) {
		int BUFFERSIZE = 4096;
		int total = 0;
		try {
			byte[] buffer = new byte[BUFFERSIZE];

			int bytesRead = in.read(buffer);

			while (bytesRead != -1) {
				out.write(buffer, 0, bytesRead);
				total += bytesRead;
				bytesRead = in.read(buffer);
			}

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return total;
	}

	public static String getExtension(String filename) {
		String extension = "";
		int i = filename.lastIndexOf('.');
		if (i > 0) {
			extension = filename.substring(i + 1);
		}
		return extension;
	}

	public static void safeClose(final AutoCloseable object) {
		if (object != null) {
			try {
				object.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static ByteBuffer getBytesFromFile(File file) {
		FileInputStream is = null;
		try {
			ByteBuffer byteBuffer = ByteBuffer.allocate(Long.valueOf(file.length()).intValue());
			byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
			is = new FileInputStream(file);
			FileChannel in = is.getChannel();
			in.read(byteBuffer);
			byteBuffer.flip();
			return byteBuffer;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			FileUtils.safeClose(is);
		}
		return null;
	}

	public static void writToFile(File file, ByteBuffer byteBuffer) {
		FileOutputStream os = null;
		try {
			byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
			os = new FileOutputStream(file);
			FileChannel out = os.getChannel();
			out.write(byteBuffer);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			FileUtils.safeClose(os);
		}
	}

	public static void removeDirectory(File... dirs) {
		for (File dir : dirs) {
			try {
				Files.walkFileTree(dir.toPath(), new SimpleFileVisitor<Path>() {
					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
						Files.delete(file);
						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
						Files.delete(dir);
						return FileVisitResult.CONTINUE;
					}
				});
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
