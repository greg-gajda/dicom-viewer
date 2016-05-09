package dicom.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Map;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@NamedQueries({
		@NamedQuery(name = "Image.getTags", query = "select entry(t) from Image i join i.tags t where i.sopInstanceUID = :image"),
		@NamedQuery(name = "Image.getImageByStudyId", query = "select i from Image i, Series s where i.seriesId = s.seriesInstanceUID and s.studyId = :study order by i.sliceLocation, i.sopInstanceUID"),
		@NamedQuery(name = "Image.getImagesBySeriesId", query = "select i from Image i where i.seriesId = :series order by i.sliceLocation, i.sopInstanceUID"),
		@NamedQuery(name = "Image.getImagesByParentSeriesId", query = "select i from Image i where i.series.parentId = :series order by i.sliceLocation, i.sopInstanceUID"),
		@NamedQuery(name = "Image.getImageById", query = "select i from Image i where i.sopInstanceUID = :image") })
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@Entity
@Table(name = "_image")
public class Image implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@XmlElement
	@Column(name = "sop_instance_uid", length = 64, nullable = false)
	private String sopInstanceUID;

	@XmlElement
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "series_id", referencedColumnName = "series_instance_uid", insertable = false, updatable = false)
	private Series series;

	@XmlElement
	@Column(name = "series_id", length = 64, nullable = false)
	private String seriesId;

	@XmlElement
	@Column(name = "rows", nullable = true)
	private Integer rows;

	@XmlElement
	@Column(name = "columns", nullable = true)
	private Integer columns;

	@XmlElement
	@Column(name = "image_position", length = 128, nullable = true)
	private String imagePosition;

	@XmlElement
	@Column(name = "image_orientation", length = 128, nullable = true)
	private String imageOrientation;

	@XmlElement
	@Column(name = "pixel_spacing", length = 128, nullable = true)
	private String pixelSpacing;

	@XmlElement
	@Column(name = "file_name", length = 256, nullable = false)
	private String fileName;

	@XmlElement
	@Column(name = "window_center", length = 64)
	private String windowCenter;

	@XmlElement
	@Column(name = "window_width", length = 64)
	private String windowWidth;

	@XmlElement
	@Column(name = "slice_thickness", length = 64)
	private String sliceThickness;

	@XmlElement
	@Column(name = "instance_number", nullable = true)
	private Integer instanceNumber;

	@XmlElement
	@Column(name = "slice_location", nullable = true)
	private BigDecimal sliceLocation;

	@XmlElement
	@ElementCollection(fetch = FetchType.EAGER)
	@MapKeyColumn(name = "tag_name")
	@Column(name = "tag_value")
	@CollectionTable(name = "_image_tag")
	private Map<String, String> tags;

	public String getSopInstanceUID() {
		return sopInstanceUID;
	}

	public void setSopInstanceUID(String sopInstanceUID) {
		this.sopInstanceUID = sopInstanceUID;
	}

	public Series getSeries() {
		return series;
	}

	public void setSeries(Series series) {
		this.series = series;
	}

	public String getSeriesId() {
		return seriesId;
	}

	public void setSeriesId(String seriesId) {
		this.seriesId = seriesId;
	}

	public Integer getRows() {
		return rows;
	}

	public void setRows(Integer rows) {
		this.rows = rows;
	}

	public Integer getColumns() {
		return columns;
	}

	public void setColumns(Integer columns) {
		this.columns = columns;
	}

	public Integer getInstanceNumber() {
		return instanceNumber;
	}

	public void setInstanceNumber(Integer instanceNumber) {
		this.instanceNumber = instanceNumber;
	}

	public BigDecimal getSliceLocation() {
		return sliceLocation;
	}

	public void setSliceLocation(BigDecimal sliceLocation) {
		this.sliceLocation = sliceLocation;
	}

	public String getImagePosition() {
		return imagePosition;
	}

	public void setImagePosition(String imagePosition) {
		this.imagePosition = imagePosition;
	}

	public String getImageOrientation() {
		return imageOrientation;
	}

	public void setImageOrientation(String imageOrientation) {
		this.imageOrientation = imageOrientation;
	}

	public String getPixelSpacing() {
		return pixelSpacing;
	}

	public void setPixelSpacing(String pixelSpacing) {
		this.pixelSpacing = pixelSpacing;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getWindowCenter() {
		return windowCenter;
	}

	public void setWindowCenter(String windowCenter) {
		this.windowCenter = windowCenter;
	}

	public String getWindowWidth() {
		return windowWidth;
	}

	public void setWindowWidth(String windowWidth) {
		this.windowWidth = windowWidth;
	}

	public String getSliceThickness() {
		return sliceThickness;
	}

	public void setSliceThickness(String sliceThickness) {
		this.sliceThickness = sliceThickness;
	}

	public Map<String, String> getTags() {
		return tags;
	}

	public void setTags(Map<String, String> tags) {
		this.tags = tags;
	}

}
