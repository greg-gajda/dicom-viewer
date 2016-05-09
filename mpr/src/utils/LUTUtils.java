/*******************************************************************************
luts
 * Copyright (c) 2010 Nicolas Roduit.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nicolas Roduit - initial API and implementation
 ******************************************************************************/
package utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.media.jai.LookupTableJAI;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.dcm4che3.util.TagUtils;

public class LUTUtils {
	public static void buildLUTs(Attributes dicomTags) {
		Integer pixelRepresentation = dicomTags.getInt(Tag.PixelRepresentation, 0);
		boolean isPixelRepresentationSigned = (pixelRepresentation != null && pixelRepresentation != 0);

		// NOTE : Either a Modality LUT Sequence containing a single Item or
		// Rescale Slope and Intercept values
		// shall be present but not both (@see Dicom Standard 2011 - PS 3.3 �
		// C.11.1 Modality LUT Module)

		Attributes[] mLutItems = (Attributes[]) dicomTags.getValue(Tag.ModalityLUTSequence);

		LookupTableJAI modalityLUTData = null;
		LookupTableJAI[] voiLUTsData = null;
		String[] voiLUTsExplanation = null;

		if (mLutItems != null && mLutItems.length > 0 && containsRequiredModalityLUTDataAttributes(mLutItems[0])) {
			boolean canApplyMLUT = true;
			String modlality = dicomTags.getString(Tag.Modality);
			if ("XA".equals(modlality) || "XRF".equals(modlality)) {
				// See PS 3.4 N.2.1.2.
				String pixRel = dicomTags.getString(Tag.PixelIntensityRelationship);
				if (pixRel != null && ("LOG".equalsIgnoreCase(pixRel) || "DISP".equalsIgnoreCase(pixRel))) {
					canApplyMLUT = false;
					System.out.println(
							"Modality LUT Sequence shall NOT be applied according to PixelIntensityRelationship");
				}
			}

			if (canApplyMLUT) {

				modalityLUTData = createLut(mLutItems[0], isPixelRepresentationSigned);
				// DicomMediaUtils.setTagNoNull(dicomTags, Tag.ModalityLUTData,
				// createLut(mLutItems[0], isPixelRepresentationSigned));
				String val = mLutItems[0].getString(Tag.ModalityLUTType);
				if (val != null) {
					dicomTags.setString(Tag.ModalityLUTType, VR.LO, val);
				}
				// DicomMediaUtils.setTagNoNull(dicomTags,
				// Tag.ModalityLUTExplanation, // Optional Tag
				// getStringFromDicomElement(mLutItems[0], Tag.LUTExplanation));

				System.out.println("Tag.LUTExplanation");
				System.out.println(mLutItems[0].getString(Tag.LUTExplanation));
			}
		}
		// NOTE : If any VOI LUT Table is included by an Image, a Window Width
		// and Window Center or the VOI LUT
		// Table, but not both, may be applied to the Image for display.
		// Inclusion of both indicates that multiple
		// alternative views may be presented. (@see Dicom Standard 2011 - PS
		// 3.3 � C.11.2 VOI LUT Module)

		Attributes[] voiLUTSequence = Attributes[].class.cast(dicomTags.getValue(Tag.VOILUTSequence));

		if (voiLUTSequence != null && voiLUTSequence.length > 0) {
			voiLUTsData = new LookupTableJAI[voiLUTSequence.length];
			voiLUTsExplanation = new String[voiLUTSequence.length];

			boolean isOutModalityLutSigned = isPixelRepresentationSigned;

			// Evaluate outModality min value if signed
			LookupTableJAI modalityLookup = modalityLUTData;// (LookupTableJAI)
															// dicomTags.get(Tag.ModalityLUTData);

			Integer smallestPixelValue = dicomTags.getInt(Tag.SmallestImagePixelValue, -1);
			float minPixelValue = (smallestPixelValue == -1) ? 0.0f : smallestPixelValue.floatValue();

			if (modalityLookup == null) {
				Float intercept = dicomTags.getFloat(Tag.RescaleIntercept, Float.MIN_VALUE);
				Float slope = dicomTags.getFloat(Tag.RescaleSlope, Float.MIN_VALUE);

				slope = (slope > Float.MIN_VALUE) ? 1.0f : slope;
				intercept = (intercept > Float.MIN_VALUE) ? 0.0f : intercept;

				if ((minPixelValue * slope + intercept) < 0) {
					isOutModalityLutSigned = true;
				}
			} else {
				int minInLutValue = modalityLookup.getOffset();
				int maxInLutValue = modalityLookup.getOffset() + modalityLookup.getNumEntries() - 1;

				if (minPixelValue >= minInLutValue && minPixelValue <= maxInLutValue
						&& modalityLookup.lookup(0, (int) minPixelValue) < 0) {
					isOutModalityLutSigned = true;
				}
			}

			for (int i = 0; i < voiLUTSequence.length; i++) {
				Attributes voiLUTobj = voiLUTSequence[i];
				if (containsLUTAttributes(voiLUTobj)) {
					voiLUTsData[i] = createLut(voiLUTobj, isOutModalityLutSigned);
					voiLUTsExplanation[i] = voiLUTobj.getString(Tag.LUTExplanation);
				} else {
					System.out.println("Cannot read VOI LUT Data " + i);
				}
			}
		}
	}

	public static LookupTableJAI createLut(Attributes dicomLutObject, boolean isValueRepresentationSigned) {
		if (dicomLutObject == null || dicomLutObject.isEmpty()) {
			return null;
		}

		LookupTableJAI lookupTable = null;

		// Three values of the LUT Descriptor describe the format of the LUT
		// Data in the corresponding Data Element
		int[] descriptor = DicomUtils.getIntArray(dicomLutObject, Tag.LUTDescriptor, null);

		if (descriptor == null) {
			System.out.println("Missing LUT Descriptor");
		} else if (descriptor.length != 3) {
			System.out.println("Illegal number of LUT Descriptor values " + descriptor.length);
		} else {

			// First value is the number of entries in the lookup table.
			// When this value is 0 the number of table entries is equal to
			// 65536 <=> 0x10000.
			int numEntries = (descriptor[0] == 0) ? 65536 : descriptor[0];

			// Second value is mapped to the first entry in the LUT.
			int offset = (numEntries <= 65536) ? //
					((numEntries <= 256) ? (byte) descriptor[1] : (short) descriptor[1]) : //
					descriptor[1]; // necessary to cast in order to get negative
									// value when present

			// Third value specifies the number of bits for each entry in the
			// LUT Data.
			int numBits = descriptor[2];

			int dataLength = 0; // number of entry values in the LUT Data.

			if (numBits <= 8) { // LUT Data should be stored in 8 bits allocated
								// format

				// LUT Data contains the LUT entry values, assuming data is
				// always unsigned data
				byte[] bData = null;
				try {
					bData = dicomLutObject.getBytes(Tag.LUTData);
				} catch (IOException e) {
					e.printStackTrace();
				}

				if (bData != null && numEntries <= 256 && (bData.length == (numEntries << 1))) {
					// Some implementations have encoded 8 bit entries with 16
					// bits allocated, padding the high bits

					byte[] bDataNew = new byte[numEntries];
					int byteShift = (dicomLutObject.bigEndian() ? 1 : 0);
					for (int i = 0; i < numEntries; i++) {
						bDataNew[i] = bData[(i << 1) + byteShift];
					}

					dataLength = bDataNew.length;
					lookupTable = new LookupTableJAI(bDataNew, offset);

				} else {
					if(bData != null){
						dataLength = bData.length;
					}
					// LUT entry value range should be [0,255]
					lookupTable = new LookupTableJAI(bData, offset); 
				}
			} else if (numBits <= 16) { // LUT Data should be stored in 16 bits
										// allocated format

				if (numEntries <= 256) {

					// LUT Data contains the LUT entry values, assuming data is
					// always unsigned data
					byte[] bData = null;
					try {
						bData = dicomLutObject.getBytes(Tag.LUTData);
					} catch (IOException e) {
						e.printStackTrace();
					}

					if (bData != null && bData.length == (numEntries << 1)) {

						// Some implementations have encoded 8 bit entries with
						// 16 bits allocated, padding the high bits

						byte[] bDataNew = new byte[numEntries];
						int byteShift = (dicomLutObject.bigEndian() ? 1 : 0);
						for (int i = 0; i < numEntries; i++) {
							bDataNew[i] = bData[(i << 1) + byteShift];
						}

						dataLength = bDataNew.length;
						lookupTable = new LookupTableJAI(bDataNew, offset);
					}

				} else {

					// LUT Data contains the LUT entry values, assuming data is
					// always unsigned data
					// short[] sData = dicomLutObject.getShorts(Tag.LUTData);
					int[] iData = DicomUtils.getIntArray(dicomLutObject, Tag.LUTData, null);
					if (iData != null) {
						short[] sData = new short[iData.length];
						for (int i = 0; i < iData.length; i++) {
							sData[i] = (short) iData[i];
						}

						dataLength = sData.length;
						lookupTable = new LookupTableJAI(sData, offset, true);
					}
				}
			} else {
				System.out.println("Illegal number of bits for each entry in the LUT Data");
			}

			if (lookupTable != null) {
				if (dataLength != numEntries) {
					System.out.println(
							String.format("LUT Data length \"%d\" mismatch number of entries \"%d\" in LUT Descriptor ",
									dataLength, numEntries));
				}
				if (dataLength > (1 << numBits)) {
					System.out.println(String.format(
							"Illegal LUT Data length \"%d\" with respect to the number of bits in LUT descriptor \"%d\"",
							dataLength, numBits));
				}
			}
		}
		return lookupTable;
	}

	public static final int[] LUTAttributes = new int[] { Tag.LUTDescriptor, Tag.LUTData };

	public static boolean containsLUTAttributes(Attributes dcmItems) {
		return containsRequiredAttributes(dcmItems, LUTAttributes);
	}

	public static boolean containsRequiredModalityLUTDataAttributes(Attributes dcmItems) {
		return containsRequiredAttributes(dcmItems, Tag.ModalityLUTType) && containsLUTAttributes(dcmItems);
	}

	public static boolean containsRequiredAttributes(Attributes dcmItems, int... requiredTags) {
		if (dcmItems == null || requiredTags == null || requiredTags.length == 0) {
			return false;
		}

		int countValues = 0;
		List<String> missingTagList = null;

		for (int tag : requiredTags) {
			if (dcmItems.containsValue(tag)) {
				countValues++;
			} else {
				if (missingTagList == null) {
					missingTagList = new ArrayList<String>(requiredTags.length);
				}
				missingTagList.add(TagUtils.toString(tag));
			}
		}
		return (countValues == requiredTags.length);
	}

}
