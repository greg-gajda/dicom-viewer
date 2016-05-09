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
@Table(name = "_series")
public class Series implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@XmlElement
	@Column(name = "series_instance_uid", length = 64, nullable = false)
	private String seriesInstanceUID;

	@XmlElement
	@Column(name = "modality", length = 16, nullable = false)
	private String modality;

	@XmlElement
	@Column(name = "series_description", length = 64, nullable = true)
	private String seriesDescription;

	@XmlElement
	@Column(name = "series_number", length = 64, nullable = true)
	private String seriesNumber;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "study_id", referencedColumnName = "study_instance_uid", insertable = false, updatable = false)
	private Study study;

	@XmlElement
	@Column(name = "study_id", length = 64, nullable = false)
	private String studyId;

	@XmlElement
	@Column(name = "parent_id", length = 64, nullable = true)
	private String parentId;

	@XmlElement
	@Column(name = "projection", length = 16, nullable = false)
	private String projection;

	@OneToMany(mappedBy = "series", cascade = { CascadeType.REMOVE, CascadeType.REFRESH })
	private List<Image> images;

	public String getSeriesInstanceUID() {
		return seriesInstanceUID;
	}

	public void setSeriesInstanceUID(String seriesInstanceUID) {
		this.seriesInstanceUID = seriesInstanceUID;
	}

	public String getModality() {
		return modality;
	}

	public void setModality(String modality) {
		this.modality = modality;
	}

	public String getSeriesDescription() {
		return seriesDescription;
	}

	public void setSeriesDescription(String seriesDescription) {
		this.seriesDescription = seriesDescription;
	}

	public String getSeriesNumber() {
		return seriesNumber;
	}

	public void setSeriesNumber(String seriesNumber) {
		this.seriesNumber = seriesNumber;
	}

	public Study getStudy() {
		return study;
	}

	public void setStudy(Study study) {
		this.study = study;
	}

	public String getStudyId() {
		return studyId;
	}

	public void setStudyId(String studyId) {
		this.studyId = studyId;
	}

	public List<Image> getImages() {
		return images;
	}

	public void setImages(List<Image> images) {
		this.images = images;
	}

	public String getParentId() {
		return parentId;
	}

	public void setParentId(String parentId) {
		this.parentId = parentId;
	}

	public String getProjection() {
		return projection;
	}

	public void setProjection(String projection) {
		this.projection = projection;
	}

}
