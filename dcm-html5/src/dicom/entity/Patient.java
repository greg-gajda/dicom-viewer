package dicom.entity;

import java.io.Serializable;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@Entity
@Table(name = "_patient")
public class Patient implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@XmlElement
	@Column(name = "patient_id", length = 64, nullable = false)
	private String patientID;

	@XmlElement
	@Column(name = "patients_name", length = 64, nullable = true)
	private String patientsName;

	@XmlElement
	@Column(name = "patients_age", length = 16, nullable = true)
	private String patientsAge;

	@XmlElement
	@Column(name = "patients_sex", length = 16, nullable = true)
	private String patientsSex;

	@XmlElement
	@Column(name = "patients_birthdate", length = 16, nullable = true)
	private String patientsBirthdate;

	@OneToMany(mappedBy = "patient")
	private List<Study> studies;

	public String getPatientID() {
		return patientID;
	}

	public void setPatientID(String patientID) {
		this.patientID = patientID;
	}

	public String getPatientsName() {
		return patientsName;
	}

	public void setPatientsName(String patientsName) {
		this.patientsName = patientsName;
	}

	public String getPatientsAge() {
		return patientsAge;
	}

	public void setPatientsAge(String patientsAge) {
		this.patientsAge = patientsAge;
	}

	public String getPatientsSex() {
		return patientsSex;
	}

	public void setPatientsSex(String patientsSex) {
		this.patientsSex = patientsSex;
	}

	public String getPatientsBirthdate() {
		return patientsBirthdate;
	}

	public void setPatientsBirthdate(String patientsBirthdate) {
		this.patientsBirthdate = patientsBirthdate;
	}

	public List<Study> getStudies() {
		return studies;
	}

	public void setStudies(List<Study> studies) {
		this.studies = studies;
	}

}
