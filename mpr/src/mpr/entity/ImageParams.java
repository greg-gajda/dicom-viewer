package mpr.entity;

import java.util.Arrays;

import org.dcm4che3.data.BulkData;
import org.dcm4che3.data.Fragments;
import org.dcm4che3.image.PhotometricInterpretation;

/**
 * Class for storing DICOM image parameters
 * @author GGajda
 *
 */
public class ImageParams {

	int bitsStored;
    int bitsAllocated;
    int highBit;
                        	
	int samplesPerPixel;
    boolean banded;
    int pixelRepresentation;
    int dataType;

    int numberOfFrames;
    
    int frameLength;
    
    double[] pixelSpacing;
    Integer overlayBitMask;
    
    BulkData pixeldata;
    Fragments pixeldataFragments;
    
    String tsuid;
	String photometricInterpretation; 
	PhotometricInterpretation pmi;
	
	
	/**
	 * Human readable (more or less) image params
	 */
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append("bitsStored: ").append(bitsStored).append("\n");
		sb.append("bitsAllocated: ").append(bitsAllocated).append("\n");
		sb.append("highBit: ").append(highBit).append("\n");
		sb.append("samplesPerPixel: ").append(samplesPerPixel).append("\n");
		sb.append("banded: ").append(banded).append("\n");
		sb.append("pixelRepresentation: ").append(pixelRepresentation).append("\n");
		sb.append("dataType: ").append(dataType).append("\n");
		sb.append("numberOfFrames: ").append(numberOfFrames).append("\n");
		sb.append("frameLength: ").append(frameLength).append("\n");
		sb.append("overlayBitMask: ").append(overlayBitMask).append("\n");		
		sb.append("pixelSpacing: [");
		if(pixelSpacing != null){
			Arrays.stream(pixelSpacing).forEach(ps -> sb.append(ps).append(","));
		}
		sb.append("]\n");
		sb.append("Has pixeldata: ").append(pixeldata != null).append("\n");
		sb.append("Has pixeldataFragments: ").append(pixeldataFragments != null).append("\n");
		sb.append("tsuid: ").append(tsuid).append("\n");
		sb.append("photometricInterpretation: ").append(photometricInterpretation).append("\n");
		return sb.toString();
	}
    
}
