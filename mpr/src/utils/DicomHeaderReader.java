package utils;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Fragments;
import org.dcm4che3.data.Keyword;
import org.dcm4che3.data.Sequence;
import org.dcm4che3.data.SpecificCharacterSet;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.dcm4che3.io.DicomInputHandler;
import org.dcm4che3.io.DicomInputStream;
import org.dcm4che3.util.TagUtils;

public class DicomHeaderReader implements DicomInputHandler {

	Map<Integer, Object> values;

	public DicomHeaderReader() {
		values = new LinkedHashMap<>();
	}

	public Map<Integer, Object> getValues() {
		return values;
	}

	@Override
	public void readValue(DicomInputStream dis, Attributes attrs) throws IOException {
		VR vr = dis.vr();
		int vallen = dis.length();
		boolean undeflen = vallen == -1;
		if (vr == VR.SQ || undeflen) {
			dis.readValue(dis, attrs);
		} else {
			int tag = dis.tag();
			byte[] b = dis.readValue();
			Object obj = vr.toStrings(b, dis.bigEndian(), SpecificCharacterSet.DEFAULT);
			if (check(tag)) {
				values.put(tag, obj == null ? "" : obj);
			}
			if (tag == Tag.FileMetaInformationGroupLength) {
				dis.setFileMetaInformationGroupLength(b);
			} else if (tag == Tag.TransferSyntaxUID || tag == Tag.SpecificCharacterSet
					|| TagUtils.isPrivateCreator(tag)) {
				attrs.setBytes(tag, vr, b);
			}
		}
	}

	@Override
	public void readValue(DicomInputStream dis, Sequence seq) throws IOException {
		// ignore sequences
	}

	@Override
	public void readValue(DicomInputStream dis, Fragments frags) throws IOException {
		byte[] b = dis.readValue();
		Object obj = frags.vr().toStrings(b, dis.bigEndian(), SpecificCharacterSet.DEFAULT);
		if (check(frags.tag())) {
			values.put(frags.tag(), obj == null ? "" : obj);
		}
	}

	boolean check(int tag) {
		String key = Keyword.valueOf(tag);
		if (key != null && key.trim() != "") {
			if ("PrivateCreatorID".equals(key) || "GroupLength".equals(key) || "SourceImageIDs".equals(key)) {
				return false;
			}
			return true;
		}
		return false;
	}

	@Override
	public void startDataset(DicomInputStream dis) throws IOException {

	}

	@Override
	public void endDataset(DicomInputStream dis) throws IOException {

	}

}
