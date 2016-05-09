package mpr.entity;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.awt.image.RenderedImage;
import java.io.File;

import org.dcm4che3.imageio.plugins.dcm.DicomMetaData;
import org.junit.Test;

public class TestDicomImage {

	@Test
	public void testGetMetadata() {
		DicomImage di = new DicomImage(new InFile(new File("data/IM-0001-0001.dcm")));
		assertNull(di.metadata);
		DicomMetaData md = di.getDicomMetaData();
		assertNotNull(md);
		assertNotNull(di.metadata);
		assertNotNull(di.params.tsuid);	
	}

	@Test
	public void testReadPixelData() {
		DicomImage di = new DicomImage(new InFile(new File("data/IM-0001-0002.dcm")));
		assertNull(di.metadata);
		di.readPixelData();
		System.out.println(di.params.toString());
	}
	
	@Test
	public void testReadAsRenderedImage(){
		DicomImage di1 = new DicomImage(new InFile(new File("data/IM-0001-0002.dcm")));
		di1.readPixelData();
		RenderedImage ri1 = di1.readAsRenderedImage(0);
		assertNotNull(ri1);
		
		//DicomImage di2 = new DicomImage(new InFile(new File("data/IM-0001-0005.dcm")));
		DicomImage di2 = new DicomImage(new InFile(new File("data/IM-0001-0003.dcm")));
		di2.readPixelData();
		RenderedImage ri2 = di2.readAsRenderedImage(0);
		assertNotNull(ri2);
		
	}
}
