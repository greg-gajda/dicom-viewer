package utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;

import org.junit.Test;

import mpr.entity.DicomImage;
import mpr.entity.InFile;

public class TestGeometryOfSlice {

	@Test
	public void testImageOrientation() {
		DicomImage di = new DicomImage(new InFile(new File("../mpr/data/IM-0001-0001.dcm")));
		GeometryOfSlice geom = GeometryOfSlice.getDispSliceGeometry(di.getHeader());
		assertNotNull(geom);
		System.out.println(geom.toString());
		assertEquals(geom.getImageOrientation(), "AXIAL");
	}
	
}
