package dicom.boundary;

import java.util.Map;

public interface DicomHeader {
	Map<Integer, Object> readDicomHeader(byte[] content);
}
