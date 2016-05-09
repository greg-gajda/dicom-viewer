package dicom.boundary;

import dicom.entity.Image;

public interface DicomWriter {
	Image writeImage(String fileName, byte[] content);
}
