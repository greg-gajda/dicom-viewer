package dicom.control;

import static utils.DicomUtils.getDouble;
import static utils.DicomUtils.getDoubleArray;
import static utils.DicomUtils.getInteger;
import static utils.DicomUtils.getString;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.dcm4che3.data.Keyword;
import org.dcm4che3.data.Tag;
import org.dcm4che3.io.DicomInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dicom.boundary.DicomHeader;
import dicom.boundary.DicomWriter;
import dicom.entity.Image;
import dicom.entity.Patient;
import dicom.entity.Series;
import dicom.entity.Study;
import utils.DicomHeaderReader;
import utils.DicomUtils;
import utils.GeometryOfSlice;

@Stateless
@LocalBean
public class DicomBean implements DicomHeader, DicomWriter {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@PersistenceContext(unitName = "dcm-viewer")
	EntityManager em;
	
	@Override
	public Map<Integer, Object> readDicomHeader(byte[] content) {
		Boolean bigEndian = false;
		DicomHeaderReader handler = new DicomHeaderReader();
		try (DicomInputStream dis = new DicomInputStream(new BufferedInputStream(new ByteArrayInputStream(content)))) {
			dis.setDicomInputHandler(handler);
			dis.readDataset(-1, -1);
			bigEndian = dis.bigEndian();
		} catch (IOException e) {
			log.error("Error reading DICOM file header", e);
		}
		Map<Integer, Object> tags = handler.getValues();
		tags.put(DicomUtils.TAG_BIG_ENDIAN, bigEndian);
		return tags;
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override
	public Image writeImage(String fileName, byte[] content) {
		Map<Integer, Object> tags = readDicomHeader(content);		
		Image image = getImage(fileName, tags, true);
		return image;
	}
	
	public Object getImageTags(String image) {
		return em.createNamedQuery("Image.getTags").setParameter("image", image).getResultList();
	}

	public Image getImageByStudyId(String study) {
		Image img = em.createNamedQuery("Image.getImageByStudyId", Image.class).setParameter("study", study)
				.setFirstResult(0).setMaxResults(1).getSingleResult();
		return img;
	}

	public List<Study> filterStudies(String modality, String tag) {
		StringBuilder query = new StringBuilder();
		if ("ALL".equals(modality) && (tag == null || tag.isEmpty())) {
			query.append("select s from Study s");
		} else if (tag != null && tag.isEmpty() == false) {
			query.append("select distinct i.series.study from Image i join i.tags t where UPPER(t) like '%")
					.append(tag.toUpperCase()).append("%'");
			if ("ALL".equals(modality) == false) {
				query.append(" and i.series.modality='").append(modality).append("'");
			}
		} else if ("ALL".equals(modality) == false) {
			query.append("select distinct s from Study s join s.series ss where ss.modality='").append(modality)
					.append("'");
		}
		List<Study> studies = em.createQuery(query.toString(), Study.class).getResultList();
		return studies;
	}

	public List<Image> getImagesByParentSeriesId(String seriesInstanceUID) {
		String query = String.format("select s from Series s where s.seriesInstanceUID = '%s'", seriesInstanceUID);
		Series series = em.createQuery(query, Series.class).setFirstResult(0).setMaxResults(1).getSingleResult();
		String param = seriesInstanceUID;
		if(series.getParentId() != null){
			param = series.getParentId();
		}		
		List<Image> images = em.createNamedQuery("Image.getImagesByParentSeriesId", Image.class)
				.setParameter("series", param).getResultList();
		return images;
	}
	
	public List<Series> getSeriesByImageId(String image) {
		String query = String.format(
				"select s from Series s where s.parentId is null and s.studyId in (select i.series.studyId from Image i where i.sopInstanceUID = '%s') order by s.seriesInstanceUID",
				image);
		List<Series> series = em.createQuery(query, Series.class).getResultList();
		return series;
	}
	
	public List<Series> getSeriesBySeriesId(String series) {
		String query = String.format("select s from Series s where s.seriesInstanceUID= '%s'",series);
		List<Series> list = em.createQuery(query, Series.class).getResultList();
		return list;
	}

	public List<Image> getImagesBySeriesId(String series) {
		List<Image> images = em.createNamedQuery("Image.getImagesBySeriesId", Image.class)
				.setParameter("series", series).getResultList();
		return images;
	}

	public Image getImage(String fileName, Map<Integer, Object> headers, boolean persist) {

		List<Image> list = em.createNamedQuery("Image.getImageById", Image.class)
				.setParameter("image", getString(headers.get(Tag.SOPInstanceUID))).getResultList();

		if (list == null || list.size() == 0) {
			Image img = new Image();
			//TODO:parse arrays
			img.setSopInstanceUID(getString(headers.get(Tag.SOPInstanceUID)));
			Integer rows = getInteger(headers.get(Tag.Rows));
			img.setRows(rows);
			Integer cols = getInteger(headers.get(Tag.Columns));
			img.setColumns(cols);
			Integer instanceNumber = getInteger(headers.get(Tag.InstanceNumber));
			img.setInstanceNumber(instanceNumber);
			Double sliceLocation = getDouble(headers.get(Tag.SliceLocation));
			img.setSliceLocation(sliceLocation != null ? new BigDecimal(sliceLocation) : instanceNumber == null ? null : new BigDecimal(instanceNumber));			
			double [] io = getDoubleArray(headers.get(Tag.ImageOrientationPatient));
			img.setImageOrientation(io == null ? "" : Arrays.toString(io).replaceAll("\\[|\\]", ""));
			double [] ip = getDoubleArray(headers.get(Tag.ImagePositionPatient));
			img.setImagePosition( Arrays.toString(ip).replaceAll("\\[|\\]", ""));
			double [] ps = getDoubleArray(headers.get(Tag.PixelSpacing));
			img.setPixelSpacing(ps == null ? "" : Arrays.toString(ps).replaceAll("\\[|\\]", ""));			
			double [] wc = getDoubleArray(headers.get(Tag.WindowCenter));
			img.setWindowCenter(wc == null ? "" : Arrays.toString(wc).replaceAll("\\[|\\]", ""));
			double [] ww = getDoubleArray(headers.get(Tag.WindowWidth));
			img.setWindowWidth(ww == null ? "" : Arrays.toString(ww).replaceAll("\\[|\\]", ""));			
			Double st = getDouble(headers.get(Tag.SliceThickness));
			img.setSliceThickness(st == null ? "" : st.toString());		
			img.setTags(headers.entrySet().stream().filter(e -> e.getValue() instanceof String)
					.collect(Collectors.toMap(e -> Keyword.valueOf(e.getKey()), e -> e.getValue().toString())));
			img.setFileName(fileName);
			img.setSeries(getSeries(headers, persist));
			img.setSeriesId(img.getSeries().getSeriesInstanceUID());			
			if(persist){
				em.persist(img);
				em.flush();
				log.info(String.format("New DICOM image persisted %s", img.getSopInstanceUID()));
			}			
			return img;
		} else {
			return list.get(0);
		}
	}

	private Series getSeries(Map<Integer, Object> headers, boolean persist) {
		String query = String.format("select s from Series s where %s='%s'", "seriesInstanceUID",
				getString(headers.get(Tag.SeriesInstanceUID)));
		List<Series> list = em.createQuery(query, Series.class).getResultList();
		if (list == null || list.size() == 0) {			
			GeometryOfSlice geometry = GeometryOfSlice.getDispSliceGeometry(headers);			
			Series ser = new Series();
			ser.setModality(getString(headers.get(Tag.Modality)));
			ser.setSeriesDescription(getString(headers.get(Tag.SeriesDescription)));
			ser.setSeriesInstanceUID(getString(headers.get(Tag.SeriesInstanceUID)));
			ser.setSeriesNumber(getString(headers.get(Tag.SeriesNumber)));
			ser.setStudy(getStudy(headers, persist));
			ser.setStudyId(ser.getStudy().getStudyInstanceUID());
			ser.setProjection(geometry == null ? GeometryOfSlice.IMAGE_ORIENTATION[0] : geometry.getImageOrientation());
			ser.setParentId(getString(headers.get(DicomUtils.TAG_PARENT_SERIES)));
			if(persist){
				em.persist(ser);
				em.flush();
				log.info(String.format("New DICOM series persisted %s", ser.getSeriesInstanceUID()));
			}
			return ser;
		} else {
			return list.get(0);
		}
	}

	private Study getStudy(Map<Integer, Object> headers, boolean persist) {
		String query = String.format("select s from Study s where %s='%s'", "studyInstanceUID",
				getString(headers.get(Tag.StudyInstanceUID)));
		List<Study> list = em.createQuery(query, Study.class).getResultList();
		if (list == null || list.size() == 0) {
			Study study = new Study();
			study.setAccessionNumber(getString(headers.get(Tag.AccessionNumber)));
			study.setStudyDescription(getString(headers.get(Tag.StudyDescription)));
			study.setStudyInstanceUID(getString(headers.get(Tag.StudyInstanceUID)));
			study.setStudyDate(getString(headers.get(Tag.StudyDate)));
			study.setModalitiesInStudy(getString(headers.get(Tag.ModalitiesInStudy)));
			study.setDownloadable(false);
			study.setReconstructed(false);
			Patient patient = getPatient(headers, persist);
			if (patient != null) {
				study.setPatientID(patient.getPatientID());
			}
			if(persist){
				em.persist(study);
				em.flush();
				log.info(String.format("New DICOM study persisted %s", study.getStudyInstanceUID()));
			}
			return study;
		} else {
			return list.get(0);
		}
	}

	private Patient getPatient(Map<Integer, Object> headers, boolean persist) {
		String patientID = getString(headers.get(Tag.PatientID));
		if (patientID != null && patientID.isEmpty() == false) {
			String query = String.format("select p from Patient p where %s='%s'", "patientID", patientID);
			List<Patient> list = em.createQuery(query, Patient.class).getResultList();
			if (list == null || list.size() == 0) {
				Patient patient = new Patient();
				patient.setPatientID(patientID);
				patient.setPatientsAge(getString(headers.get(Tag.PatientAge)));
				patient.setPatientsBirthdate(getString(headers.get(Tag.PatientBirthDate)));
				patient.setPatientsName(getString(headers.get(Tag.PatientName)));
				patient.setPatientsSex(getString(headers.get(Tag.PatientSex)));
				if(persist){
					em.persist(patient);
					em.flush();
					log.info(String.format("New DICOM patient persisted %s", patient.getPatientID()));
				}
				return patient;
			} else {
				return list.get(0);
			}
		}
		return null;
	}

}
