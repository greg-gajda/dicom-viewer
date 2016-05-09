package converter.control;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import org.junit.Test;

import utils.FileUtils;

public class TestImageConverterBean {

	@Test
	public void testConvertToPng() throws FileNotFoundException {
		String f = "data/IM-0001-0001.dcm";
		byte[] content = FileUtils.readBytes(new FileInputStream(new File(f)));
		byte[] converted = new ImageConverterBean().convert(content, "PNG", null, null, null);
		assertTrue(converted.length > 0);
	}

}
