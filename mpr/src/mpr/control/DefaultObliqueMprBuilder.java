package mpr.control;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.vecmath.Point2d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.BulkData;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.UID;
import org.dcm4che3.data.VR;
import org.dcm4che3.io.DicomOutputStream;
import org.dcm4che3.util.UIDUtils;

import utils.CacheAccessor;
import mpr.boundary.ObliqueMprBuilder;
import mpr.entity.DicomImage;
import mpr.entity.InFile;
import mpr.entity.OutFile;
import utils.FileUtils;
import utils.GeometryOfSlice;
import utils.LUTUtils;

public class DefaultObliqueMprBuilder implements ObliqueMprBuilder{

	public DefaultObliqueMprBuilder(){}

	CacheAccessor cache;
	
	@Override
	public void setCache(CacheAccessor cache) {
		this.cache = cache;
	}
	
	double length(Point p1, Point p2){
		return Math.sqrt( Math.pow((double)(p2.x - p1.x), 2) + Math.pow((double)(p2.y - p1.y), 2));
	}

	double tan(Point p1, Point p2){
		return (double)(p2.y - p1.y) / (p2.x - p1.x);
	}

	Point[] oblique(Point ... points) {
		List<Point> list = new ArrayList<>();
		for(int i = 0; i < points.length - 1; ++i){
			Point p1 = points[i];
			Point p2 = points[i + 1];
			double A = tan(p1, p2);
			double angle = Math.abs(180 * Math.atan(A) / Math.PI);
			if(angle < 45){
				int min = Math.min(p1.x, p2.x);
				if(i > 0){
					min++;
				}
				int max = Math.max(p1.x, p2.x);			
				for (int x = min; x <= max; ++x) {
					Long y = Math.round(p1.y + (A * x) - (A * p1.x));
					if(x >= 0 && y >= 0 /*&& x < 512 && y < 512*/){
						list.add(new Point(x, y.intValue()));
					}
				}			
			} else {
				int min = Math.min(p1.y, p2.y);
				int max = Math.max(p1.y, p2.y);
				if(i > 0){
					min++;
				}
				if(p1.y < p2.y){
					for (int y = min; y <= max; ++y) {
						Long x = Math.round((y - p1.y + (A * p1.x)) / A);
						if(x >= 0 && y >= 0 /*&& x < 512 && y < 512*/){
							list.add(new Point(x.intValue(), y));
						}
					}								
				} else{					
					for (int y = max; y >= min; --y) {
						Long x = Math.round((y - p1.y + (A * p1.x)) / A);
						if(x >= 0 && y >= 0 /*&& x < 512 && y < 512*/){
							list.add(new Point(x.intValue(), y));
						}
					}		
				}				
			}			
		}
		return list.toArray(new Point[0]);
	}
	
	@Override
	public OutFile buildMprForSeries(List<File> images, Point[] points, String seriesInstanceUID, String cacheDir, String storageDir) {
		double angle = 0;
		Point[] oblique = null;
		if(points.length < 2){
			throw new IllegalStateException();
		} else {
			oblique = oblique(points);
			angle = 180 * Math.atan(tan(points[0], points[points.length - 1])) / Math.PI;
		}
		
		OutFile created = null;
		String root = seriesInstanceUID.length() > 24 ? seriesInstanceUID.substring(0, 24) : seriesInstanceUID;
		
		List<InFile> files = images.stream().map(f -> new InFile(f)).collect(Collectors.toList());
		
		DicomImage di = new DicomImage(files.get(0)); // new DicomImage(files.get(files.size() / 2));
		Attributes header = di.getHeader();
		GeometryOfSlice geometry = GeometryOfSlice.getDispSliceGeometry(header);
		if(geometry != null){
			di.readPixelData();
            int width = header.getInt(Tag.Columns, 0);
            int height = header.getInt(Tag.Rows, 0);
			double[] pixSize = header.getDoubles(Tag.PixelSpacing);
			if (pixSize == null || pixSize.length != 2) {
				pixSize = header.getDoubles(Tag.ImagerPixelSpacing);
			}
			double origPixSize = GeometryOfSlice.getPixelSize(pixSize);			
            if (GeometryOfSlice.getRescaleX(pixSize) != GeometryOfSlice.getRescaleY(pixSize)) {
                width = GeometryOfSlice.getRescaleWidth(width, pixSize);
                height = GeometryOfSlice.getRescaleHeight(height, pixSize);
            }            	
                		
    		double[] row = geometry.getRowArray();
    		double[] col = geometry.getColumnArray();
    		Vector3d vr = new Vector3d(row);
    		Vector3d vc = new Vector3d(col);
    		Vector3d resc = new Vector3d();
            String frUID = header.getString(Tag.FrameOfReferenceUID);
            if (frUID == null) {
                frUID = UIDUtils.createUID();
            }
            rotate(vc, vr, Math.toRadians(angle), resc);
            MprParams recParams = new MprParams( 
                new double[] { row[0], row[1], row[2], resc.x, resc.y, resc.z },
                pixSize, 
                frUID, seriesInstanceUID, cacheDir, storageDir);

            final String series = UIDUtils.createUID(root);
            File file = new File(cacheDir, seriesInstanceUID + ".oblique");
            final OutFile outFile = new OutFile(file);
            created = buildSeries(outFile, header, files, oblique, series, recParams, origPixSize, geometry);
//            CompletableFuture.runAsync(() -> buildSeries(outFile, header, files, series, recParams[1], origPixSize, geometry), executor).thenAccept( v -> { 
//            	
//            	
//            	Arrays.stream(series1Files).forEach(f -> created.add(f));
//            	Arrays.stream(series2Files).forEach(f -> created.add(f));
//            	Files.delete(file.toPath());
//            });
		}
		return created;
	}
	
	OutFile buildSeries(OutFile outFile, final Attributes header, final List<InFile> files, final Point[] oblique,/*final int size, */final String seriesID, final MprParams params, final double origPixSize, final GeometryOfSlice geometry){
		Collections.reverse(files);
		double sPixSize = writeBlock(outFile, files, oblique, params, seriesID);
		//not true for croocked line
		double sy = Math.abs(180 * Math.atan(tan(oblique[0], oblique[oblique.length - 1])) / Math.PI) / 90;
		double oPixSize = params.pixSize[0] * (1.0 - sy) + params.pixSize[1] * sy;		
		return buildDicomSeries(header, outFile, new Dimension(oblique.length, files.size()), seriesID, params, oPixSize, sPixSize, geometry);
	}	
	
	OutFile buildDicomSeries(Attributes header, OutFile outFile, Dimension dim,
			String seriesID, MprParams params, double origPixSize, double sPixSize, GeometryOfSlice geometry){

		int bitsStored = header.getInt(Tag.BitsStored, 8);
		int bitsAllocated = header.getInt(Tag.BitsAllocated, bitsStored);
		double[] pixSpacing = new double[] { origPixSize, sPixSize };

		String suid = UIDUtils.createUID();
		File inFile = outFile.getFile();
		
		Attributes attrs = new Attributes(header);
		attrs.setString(Tag.SeriesDescription, VR.LO, header.getString(Tag.SeriesDescription, "").concat(" [MPR]")); 
		attrs.setString(Tag.ImageType, VR.CS, new String[] { "DERIVED", "SECONDARY", "MPR" }); 
		attrs.setString(Tag.FrameOfReferenceUID, VR.UI, params.frameOfReferenceUID);
		
		attrs.setString(Tag.TransferSyntaxUID, VR.UI, UID.ImplicitVRLittleEndian);
        attrs.setInt(Tag.Columns, VR.US, dim.width);
        attrs.setInt(Tag.Rows, VR.US, dim.height);
        attrs.setString(Tag.SliceThickness, VR.DS, Double.toString(origPixSize));
        attrs.setDouble(Tag.PixelSpacing, VR.DS, pixSpacing);
		attrs.setString(Tag.SeriesInstanceUID, VR.UI, seriesID);
		attrs.setDouble(Tag.ImageOrientationPatient, VR.DS, params.imgOrientation);
            
		attrs.setInt(Tag.BitsAllocated, VR.US, bitsAllocated);
		attrs.setInt(Tag.BitsStored, VR.US, bitsStored);
            
        Date now = new Date();
        attrs.setDate(Tag.InstanceCreationDate, VR.DA, now);
        attrs.setDate(Tag.InstanceCreationTime, VR.TM, now);			
        attrs.setDate(Tag.AcquisitionDate, VR.DA, now);
        attrs.setDate(Tag.AcquisitionTime, VR.TM, now);
		
		// Image specific tags
		int index = 1;			
		String uid = String.format("%s.%04d", suid, index);
		attrs.setString(Tag.SOPInstanceUID, VR.UI, uid);
		attrs.setString(Tag.InstanceNumber, VR.IS, Integer.toString(index));
		attrs.setString(Tag.ImageType, VR.CS, new String[] { "DERIVED", "SECONDARY", "MPR" });

//		double x = (params.imgPosition[0] instanceof Double) ? (Double) params.imgPosition[0]
//					: (Boolean) params.imgPosition[0] ? newSeries.length - index - 1 : index;
//		double y = (params.imgPosition[1] instanceof Double) ? (Double) params.imgPosition[1]
//					: (Boolean) params.imgPosition[1] ? newSeries.length - index - 1 : index;
		double x = 0d;
		double y = 0d;
		Point3d p = geometry.getPosition(new Point2d(x, y));
		attrs.setString(Tag.ImagePositionPatient, VR.DS, new String[] { Double.toString(p.x), Double.toString(p.y), Double.toString(p.z) });

		LUTUtils.buildLUTs(attrs);
		double[] loc = GeometryOfSlice.computeSlicePositionVector(attrs);
        if (loc != null) {
        	attrs.setFloat(Tag.SliceLocation, VR.DS, (float) (loc[0] + loc[1] + loc[2]));
        }            
            
        File out = new File(params.storageDir, uid);
        outFile = new OutFile(out);
        updateTags(attrs, outFile);
        outFile.getTags().put(0x00010101, params.seriesUID);            
        try (DicomOutputStream dcm = new DicomOutputStream(out)){
            BulkData bdl = new BulkData(inFile.toURI().toString(), 0, (int) inFile.length(), false);
            attrs.setValue(Tag.PixelData, VR.OW, bdl);
            dcm.writeDataset(attrs.createFileMetaInformation(UID.ImplicitVRLittleEndian), attrs);
        }catch(IOException e){
        	throw new RuntimeException(e);
        }
        return outFile;
	}	
		
	private double writeBlock(OutFile outFile, List<InFile> files, Point[] oblique, MprParams params, String seriesID) {

		try {
			double lastPos = 0.0;
			double lastSpace = 0.0;
			int index = 0;

			Iterator<InFile> iter = files.iterator();
			while (iter.hasNext()) {
				InFile inf = iter.next();
				
				DicomImage dcm = new DicomImage(inf);
				dcm.readPixelData();
				double[] sp = GeometryOfSlice.computeSlicePositionVector(dcm.getHeader());
				double pos = (sp[0] + sp[1] + sp[2]);
				if (index > 0) {
					double space = Math.abs(pos - lastPos);
					lastSpace = space;
				}
				lastPos = pos;
				index++;

				String cached = cache.tryGet(inf.getFile().getName());
				if(cached != null){
					File cachedFile = new File(cached);					
					byte [] bytes = FileUtils.readBytes(new FileInputStream(cachedFile));
					String name = cachedFile.getName();
					int width = Integer.parseInt(name.split("=")[1]);
					int take = Integer.parseInt(name.split("=")[2]);
					writeRasterInRaw(bytes, outFile, width, take, oblique);
				}else{								
					PlanarImage image = dcm.readImage();
					if (image == null) {
						throw new RuntimeException("Cannot read an image!");
					}
	
					if (GeometryOfSlice.getRescaleX(params.pixSize) != GeometryOfSlice.getRescaleY(params.pixSize)) {
						ParameterBlock pb = new ParameterBlock();
						pb.addSource(image);
						pb.add((float) GeometryOfSlice.getRescaleX(params.pixSize)).add((float) GeometryOfSlice.getRescaleY(params.pixSize)).add(0.0f).add(0.0f);
						pb.add(Interpolation.getInstance(Interpolation.INTERP_BILINEAR));
						image = JAI.create("scale", pb, new RenderingHints(JAI.KEY_TILE_CACHE, null)); 
					}					
					writeRasterInRaw(image.getAsBufferedImage(), inf.getFile().getName(), outFile, oblique);
				}
			}
			return lastSpace;
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			outFile.disposeStream();
		}
	}

	private static void writeRasterInRaw(byte[] bytesOut, OutFile outFile, int width, int take, Point[] oblique) throws IOException {
		int size = width / take;
		for(Point p: oblique){
			if(p.x < size && p.y < size){
				outFile.getOutputStream().write(bytesOut, (take * p.x) + (p.y * width), take);
			}
		}				
	}
	
	private static void writeRasterInRaw(BufferedImage image, String key, OutFile outFile, Point[] oblique) throws IOException {
		DataBuffer dataBuffer = image.getRaster().getDataBuffer();		
		int width = image.getWidth();
		int size = width;
		byte[] bytesOut = null;
		if (dataBuffer instanceof DataBufferByte) {				
			bytesOut = DataBufferByte.class.cast(dataBuffer).getData();
			for(Point p: oblique){
				if(p.x < size && p.y < size){
					outFile.getOutputStream().write(bytesOut, p.x + (p.y * width), 1);
				}
			}			
		} else if (dataBuffer instanceof DataBufferShort || dataBuffer instanceof DataBufferUShort) {
			short[] data = dataBuffer instanceof DataBufferShort ? DataBufferShort.class.cast(dataBuffer).getData()
					: DataBufferUShort.class.cast(dataBuffer).getData();
			bytesOut = new byte[data.length * 2];
			for (int i = 0; i < data.length; i++) {
				bytesOut[i * 2] = (byte) (data[i] & 0xFF);
				bytesOut[i * 2 + 1] = (byte) ((data[i] >>> 8) & 0xFF);
			}
			width *= 2;
			for(Point p: oblique){
				if(p.x < size && p.y < size){
					outFile.getOutputStream().write(bytesOut, (2 * p.x) + (p.y * width), 2);
				}
			}			
		} else if (dataBuffer instanceof DataBufferInt) {
			int[] data = DataBufferInt.class.cast(dataBuffer).getData();
			bytesOut = new byte[data.length * 4];
			for (int i = 0; i < data.length; i++) {
				bytesOut[i * 4] = (byte) (data[i] & 0xFF);
				bytesOut[i * 4 + 1] = (byte) ((data[i] >>> 8) & 0xFF);
				bytesOut[i * 4 + 2] = (byte) ((data[i] >>> 16) & 0xFF);
				bytesOut[i * 4 + 3] = (byte) ((data[i] >>> 24) & 0xFF);
			}
			width *= 4;
			for(Point p: oblique){
				if(p.x < size && p.y < size){
					outFile.getOutputStream().write(bytesOut, (4 * p.x) + (p.y * width), 4);
				}
			}									
		}		
	}
		
	static class MprParams {
		final double[] imgOrientation;
		final double[] pixSize;
		final String frameOfReferenceUID;
		final String seriesUID;
		final String cacheDir;
		final String storageDir;
		
		public MprParams(double[] imgOrientation, double[] pixSize, 
				String frameOfReferenceUID, String seriesUID, String cacheDir, String storageDir) {
			this.imgOrientation = imgOrientation;
			this.pixSize = pixSize;
			this.frameOfReferenceUID = frameOfReferenceUID;
			this.seriesUID = seriesUID;
			this.cacheDir = cacheDir;
			this.storageDir = storageDir;
		}
	}	
	
	private static void rotate(Vector3d vSrc, Vector3d axis, double angle, Vector3d vDst) {
		axis.normalize();
		vDst.x = axis.x * (axis.x * vSrc.x + axis.y * vSrc.y + axis.z * vSrc.z) * (1 - Math.cos(angle))
				+ vSrc.x * Math.cos(angle) + (-axis.z * vSrc.y + axis.y * vSrc.z) * Math.sin(angle);
		vDst.y = axis.y * (axis.x * vSrc.x + axis.y * vSrc.y + axis.z * vSrc.z) * (1 - Math.cos(angle))
				+ vSrc.y * Math.cos(angle) + (axis.z * vSrc.x - axis.x * vSrc.z) * Math.sin(angle);
		vDst.z = axis.z * (axis.x * vSrc.x + axis.y * vSrc.y + axis.z * vSrc.z) * (1 - Math.cos(angle))
				+ vSrc.z * Math.cos(angle) + (-axis.y * vSrc.x + axis.x * vSrc.y) * Math.sin(angle);
	}

	private void updateTags(Attributes attrs, OutFile of){
		of.getTags().put(Tag.SOPInstanceUID, attrs.getString(Tag.SOPInstanceUID));
		of.getTags().put(Tag.Rows, attrs.getInt(Tag.Rows, 512));
		of.getTags().put(Tag.Columns, attrs.getInt(Tag.Columns, 512));
		of.getTags().put(Tag.ImageOrientationPatient, attrs.getDoubles(Tag.ImageOrientationPatient));
		of.getTags().put(Tag.ImagePositionPatient, attrs.getDoubles(Tag.ImagePositionPatient));
		of.getTags().put(Tag.PixelSpacing, attrs.getDoubles(Tag.PixelSpacing));
		of.getTags().put(Tag.ImagerPixelSpacing, attrs.getDoubles(Tag.ImagerPixelSpacing));
		of.getTags().put(Tag.WindowCenter, attrs.getDoubles(Tag.WindowCenter));
		of.getTags().put(Tag.WindowWidth, attrs.getDoubles(Tag.WindowWidth));
		of.getTags().put(Tag.SliceThickness, attrs.getDouble(Tag.SliceThickness, 1d));
		of.getTags().put(Tag.Modality, attrs.getString(Tag.Modality));
		of.getTags().put(Tag.SeriesDescription, attrs.getString(Tag.SeriesDescription));
		of.getTags().put(Tag.SeriesInstanceUID, attrs.getString(Tag.SeriesInstanceUID));
		of.getTags().put(Tag.SeriesNumber, attrs.getString(Tag.SeriesNumber));		
		of.getTags().put(Tag.AccessionNumber, attrs.getString(Tag.AccessionNumber));
		of.getTags().put(Tag.StudyDescription, attrs.getString(Tag.StudyDescription));
		of.getTags().put(Tag.StudyInstanceUID, attrs.getString(Tag.StudyInstanceUID));
		of.getTags().put(Tag.StudyDate, attrs.getDate(Tag.StudyDate));
		of.getTags().put(Tag.StudyTime, attrs.getDate(Tag.StudyTime));
		of.getTags().put(Tag.ModalitiesInStudy, attrs.getString(Tag.ModalitiesInStudy));
		of.getTags().put(Tag.PatientID, attrs.getString(Tag.PatientID));
		of.getTags().put(Tag.PatientAge, attrs.getString(Tag.PatientAge));
		of.getTags().put(Tag.PatientBirthDate, attrs.getString(Tag.PatientBirthDate));
		of.getTags().put(Tag.PatientName, attrs.getString(Tag.PatientName));
		of.getTags().put(Tag.PatientSex, attrs.getString(Tag.PatientSex));
		of.getTags().put(Tag.InstanceNumber, attrs.getString(Tag.InstanceNumber));
	}
	
}
