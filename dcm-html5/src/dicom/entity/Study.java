package dicom.entity;

import java.io.Serializable;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@Entity
@Table(name = "_study")
public class Study implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@XmlElement
	@Column(name = "study_instance_uid", length = 64, nullable = false)
	private String studyInstanceUID;

	@XmlElement
	@Column(name = "study_description", length = 64, nullable = true)
	private String studyDescription;

	@XmlElement
	@Column(name = "accession_number", length = 32, nullable = true)
	private String accessionNumber;

	@XmlElement
	@Column(name = "study_date", length = 8, nullable = true)
	private String studyDate;

	@XmlElement
	@Column(name = "modalities_in_study", length = 16, nullable = true)
	private String modalitiesInStudy;

	@XmlElement
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "patient_id", referencedColumnName = "patient_id", insertable = false, updatable = false)
	private Patient patient;

	@XmlElement
	@Column(name = "patient_id", length = 64, nullable = true)
	private String patientID;

	@XmlElement
	@Column(name = "thumbnail", length = 64, nullable = true)
	private String thumbnail;

	@XmlElement
	@Column(name = "downloadable", nullable = false)
	private Boolean downloadable;

	@XmlElement
	@Column(name = "reconstructed", nullable = false)
	private Boolean reconstructed;

	@OneToMany(mappedBy = "study", cascade = {CascadeType.REMOVE, CascadeType.REFRESH})
	private List<Series> series;

	public String getStudyInstanceUID() {
		return studyInstanceUID;
	}

	public void setStudyInstanceUID(String studyInstanceUID) {
		this.studyInstanceUID = studyInstanceUID;
	}

	public String getStudyDescription() {
		return studyDescription;
	}

	public void setStudyDescription(String studyDescription) {
		this.studyDescription = studyDescription;
	}

	public String getAccessionNumber() {
		return accessionNumber;
	}

	public void setAccessionNumber(String accessionNumber) {
		this.accessionNumber = accessionNumber;
	}

	public Patient getPatient() {
		return patient;
	}

	public void setPatient(Patient patient) {
		this.patient = patient;
	}

	public String getPatientID() {
		return patientID;
	}

	public void setPatientID(String patientID) {
		this.patientID = patientID;
	}

	public List<Series> getSeries() {
		return series;
	}

	public void setSeries(List<Series> series) {
		this.series = series;
	}

	public String getStudyDate() {
		return studyDate;
	}

	public void setStudyDate(String studyDate) {
		this.studyDate = studyDate;
	}

	public String getModalitiesInStudy() {
		return modalitiesInStudy;
	}

	public void setModalitiesInStudy(String modalitiesInStudy) {
		this.modalitiesInStudy = modalitiesInStudy;
	}

	public String getThumbnail() {
		return thumbnail;
	}

	public void setThumbnail(String thumbnail) {
		this.thumbnail = thumbnail;
	}

	public Boolean getDownloadable() {
		return downloadable;
	}

	public void setDownloadable(Boolean downloadable) {
		this.downloadable = downloadable;
	}

	public Boolean getReconstructed() {
		return reconstructed;
	}

	public void setReconstructed(Boolean reconstructed) {
		this.reconstructed = reconstructed;
	}
		
}
