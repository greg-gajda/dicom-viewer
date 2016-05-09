package mpr.control;

import java.awt.Dimension;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RasterFactory;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.TransposeDescriptor;
import javax.media.jai.operator.TransposeType;
import javax.vecmath.Point2d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.BulkData;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.UID;
import org.dcm4che3.data.VR;
import org.dcm4che3.image.PhotometricInterpretation;
import org.dcm4che3.io.DicomOutputStream;
import org.dcm4che3.util.UIDUtils;

import utils.CacheAccessor;
import mpr.boundary.OrthoMprBuilder;
import mpr.entity.DicomImage;
import mpr.entity.InFile;
import mpr.entity.OutFile;
import utils.DicomUtils;
import utils.FileUtils;
import utils.GeometryOfSlice;
import utils.LUTUtils;

public class DefaultOrthoMprBuilder implements OrthoMprBuilder {

	public DefaultOrthoMprBuilder(){}
	
	CacheAccessor cache;
	
	@Override
	public void setCache(CacheAccessor cache) {
		this.cache = cache;
	}
	
	@Override
	public List<OutFile> buildMprForSeries(Executor executor, List<File> images, String seriesInstanceUID, String cacheDir, String storageDir) {

		List<OutFile> created = new ArrayList<>();
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
            
    		final MprParams[] recParams = new MprParams[2];
    		double[] row = geometry.getRowArray();
    		double[] col = geometry.getColumnArray();
    		Vector3d vr = new Vector3d(row);
    		Vector3d vc = new Vector3d(col);
    		Vector3d resr = new Vector3d();
    		Vector3d resc = new Vector3d();
            String frUID = header.getString(Tag.FrameOfReferenceUID);
            if (frUID == null) {
                frUID = UIDUtils.createUID();
            }
            if ("SAGITTAL".equals(geometry.getImageOrientation())) {
                // The reference image is the first of the saggital stack (Left)
                rotate(vc, vr, Math.toRadians(270), resr);
                recParams[0] = new MprParams(".2", "AXIAL", false, null, 
                    new double[] { resr.x,
                        resr.y, resr.z, row[0], row[1], row[2] },
                    true, true, new Object[] { 0.0, false }, pixSize, 
                    frUID, seriesInstanceUID, cacheDir, storageDir);
                recParams[1] = new MprParams(".3", "CORONAL", false, 
                    TransposeDescriptor.ROTATE_270,
                    new double[] { resr.x, resr.y, resr.z, col[0], col[1], col[2] }, true, true,
                    new Object[] { true, 0.0 }, pixSize, 
                    frUID, seriesInstanceUID, cacheDir, storageDir);
            } else if ("CORONAL".equals(geometry.getImageOrientation())) {
                // The reference image is the first of the coronal stack (Anterior)
                rotate(vc, vr, Math.toRadians(90), resc);
                recParams[0] = new MprParams(".2", "AXIAL", false, null, 
                    new double[] { row[0],
                        row[1], row[2], resc.x, resc.y, resc.z },
                    false, true, new Object[] { 0.0, false }, pixSize, 
                    frUID, seriesInstanceUID, cacheDir, storageDir);

                rotate(vc, vr, Math.toRadians(90), resr);
                recParams[1] = new MprParams(".3", "SAGITTAL", true, 
                    TransposeDescriptor.ROTATE_270,
                    new double[] { resr.x, resr.y, resr.z, col[0], col[1], col[2] }, true, false,
                    new Object[] { true, 0.0 }, pixSize, 
                    frUID, seriesInstanceUID, cacheDir, storageDir);
            } else {
                // The reference image is the last of the axial stack (Head)
                rotate(vc, vr, Math.toRadians(270), resc);
                recParams[0] = new MprParams(".2", "CORONAL", true, null, 
                    new double[] {
                        row[0], row[1], row[2], resc.x, resc.y, resc.z },
                    false, false, new Object[] { 0.0, false }, pixSize, 
                    frUID, seriesInstanceUID, cacheDir, storageDir);

                rotate(vr, vc, Math.toRadians(90), resr);
                recParams[1] = new MprParams(".3", "SAGITTAL", true, 
                    TransposeDescriptor.ROTATE_270,
                    new double[] { col[0], col[1], col[2], resr.x, resr.y, resr.z }, false, false,
                    new Object[] { true, 0.0 }, pixSize, 
                    frUID, seriesInstanceUID, cacheDir, storageDir);
            }		          
            
            //List<OutFile> created = buildSeries(header, copy(files), index == 0 ? height : width, UIDUtils.createUID(root), recParams[index], origPixSize, geometry);            
            //return created.stream().map(f -> f.getFile().getAbsolutePath()).collect(Collectors.toList());

            final String series1 = UIDUtils.createUID(root);
            final String series2 = UIDUtils.createUID(root);
            final OutFile[] series1Files = new OutFile[height];
            final OutFile[] series2Files = new OutFile[width];       
            
            CompletableFuture.allOf(
            		CompletableFuture.runAsync(() -> buildSeries(series1Files, header, copy(files), series1, recParams[0], origPixSize, geometry), executor),
            		CompletableFuture.runAsync(() -> buildSeries(series2Files, header, copy(files), series2, recParams[1], origPixSize, geometry), executor)
            ).thenAccept( v -> { 
            	Arrays.stream(series1Files).forEach(f -> created.add(f));
            	Arrays.stream(series2Files).forEach(f -> created.add(f));
            	FileUtils.removeDirectory(new File(cacheDir, series1 + recParams[0].suffix), new File(cacheDir, series2 + recParams[1].suffix));
            }).join();            
		}
		return created;
	}
	
	List<InFile> copy(List<InFile> files){
		return files.stream().map(f -> new InFile(f.getFile())).collect(Collectors.toList());
	}
	
	void buildSeries(OutFile[] newSeries, final Attributes header, final List<InFile> files, /*final int size, */final String seriesID, final MprParams params, final double origPixSize, final GeometryOfSlice geometry){
		//OutFile[] newSeries = new OutFile[size];	
		int size = newSeries.length;
		File dir = new File(params.cacheDir, seriesID + params.suffix);
		dir.mkdirs();
		for (int i = 0; i < size; i++) {
			newSeries[i] = new OutFile(new File(dir, "mpr_" + (i + 1)));
		}			
        if(params.reverseSeriesOrder){
        	Collections.reverse(files);
        } 
		double sPixSize = writeBlock(newSeries, files, params, seriesID);
		buildDicomSeries(header, newSeries, new Dimension(size, files.size()), seriesID, params, origPixSize, sPixSize, geometry);
	}	
	
	void buildDicomSeries(Attributes header, final OutFile[] newSeries, Dimension dim,
			String seriesID, MprParams params, double origPixSize, double sPixSize, GeometryOfSlice geometry){
		
		int dataType = 0;
		int bitsStored = header.getInt(Tag.BitsStored, 8);
		int bitsAllocated = header.getInt(Tag.BitsAllocated, bitsStored);
		//double[] pixSpacing = new double[] { sPixSize, origPixSize };
		double[] pixSpacing = new double[] { origPixSize, sPixSize };
		
        ColorModel cm = null;
        SampleModel sm = null;
		
        if (params.rotateOutputImg) {
            //pixSpacing = new double[] { origPixSize, sPixSize };
        	pixSpacing = new double[] { sPixSize, origPixSize };
            int samplesPerPixel = header.getInt(Tag.SamplesPerPixel, 0);
            boolean banded = samplesPerPixel > 1 && header.getInt(Tag.PlanarConfiguration, 0) != 0;
            int pixelRepresentation = header.getInt(Tag.PixelRepresentation, 0);
            dataType = bitsAllocated <= 8 ? DataBuffer.TYPE_BYTE
                : pixelRepresentation != 0 ? DataBuffer.TYPE_SHORT : DataBuffer.TYPE_USHORT;
            if (bitsAllocated > 16 && samplesPerPixel == 1) {
                dataType = DataBuffer.TYPE_INT;
            }

            String photometricInterpretation = header.getString(Tag.PhotometricInterpretation);
            PhotometricInterpretation pmi = PhotometricInterpretation.fromString(photometricInterpretation);
            cm = pmi.createColorModel(bitsStored, dataType, header);
            sm = pmi.createSampleModel(dataType, dim.width, dim.height, samplesPerPixel, banded);

            int tmp = dim.width;
            dim.width = dim.height;
            dim.height = tmp;
        }		
		String suid = UIDUtils.createUID();
		for (int i = 0; i < newSeries.length; i++) {
			File inFile = newSeries[i].getFile();
		
            if (params.rotateOutputImg) {

                ByteBuffer byteBuffer = FileUtils.getBytesFromFile(inFile);
                DataBuffer dataBuffer = null;
                if (dataType == DataBuffer.TYPE_BYTE) {
                    dataBuffer = new DataBufferByte(byteBuffer.array(), byteBuffer.limit());
                } else if (dataType == DataBuffer.TYPE_SHORT || dataType == DataBuffer.TYPE_USHORT) {
                    ShortBuffer sBuffer = byteBuffer.asShortBuffer();
                    short[] data = null;
                    if (sBuffer.hasArray()) {
                        data = sBuffer.array();
                    } else {
                        data = new short[byteBuffer.limit() / 2];
                        for (int k = 0; k < data.length; k++) {
                            if (byteBuffer.hasRemaining()) {
                                data[k] = byteBuffer.getShort();
                            }
                        }
                    }
                    dataBuffer = dataType == DataBuffer.TYPE_SHORT ? new DataBufferShort(data, data.length)
                        : new DataBufferUShort(data, data.length);
                } else if (dataType == DataBuffer.TYPE_INT) {
                    IntBuffer sBuffer = byteBuffer.asIntBuffer();
                    int[] data = null;
                    if (sBuffer.hasArray()) {
                        data = sBuffer.array();
                    } else {
                        data = new int[byteBuffer.limit() / 4];
                        for (int k = 0; k < data.length; k++) {
                            if (byteBuffer.hasRemaining()) {
                                data[k] = byteBuffer.getInt();
                            }
                        }
                    }
                    dataBuffer = new DataBufferInt(data, data.length);
                }

                WritableRaster raster = RasterFactory.createWritableRaster(sm, dataBuffer, null);
                BufferedImage bufImg = new BufferedImage(cm, raster, false, null);
                bufImg = getImage(bufImg, TransposeDescriptor.ROTATE_90);

                dataBuffer = bufImg.getRaster().getDataBuffer();
                if (dataBuffer instanceof DataBufferByte) {
                    byteBuffer = ByteBuffer.wrap(((DataBufferByte) dataBuffer).getData());
                    FileUtils.writToFile(inFile, byteBuffer);
                } else if (dataBuffer instanceof DataBufferShort || dataBuffer instanceof DataBufferUShort) {
                    short[] data = dataBuffer instanceof DataBufferShort ? ((DataBufferShort) dataBuffer).getData()
                        : ((DataBufferUShort) dataBuffer).getData();

                    byteBuffer = ByteBuffer.allocate(data.length * 2);
                    byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
                    ShortBuffer shortBuffer = byteBuffer.asShortBuffer();
                    shortBuffer.put(data);

                    FileUtils.writToFile(inFile, byteBuffer);
                } else if (dataBuffer instanceof DataBufferInt) {
                    int[] data = ((DataBufferInt) dataBuffer).getData();

                    byteBuffer = ByteBuffer.allocate(data.length * 4);
                    byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
                    IntBuffer intBuffer = byteBuffer.asIntBuffer();
                    intBuffer.put(data);

                    FileUtils.writToFile(inFile, byteBuffer);
                }
            }
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
			int index = i;			
			String uid = String.format("%s.%04d", suid, i + 1);
			attrs.setString(Tag.SOPInstanceUID, VR.UI, uid);
			attrs.setString(Tag.InstanceNumber, VR.IS, Integer.toString(
					params.reverseIndexOrder ? newSeries.length - index : index + 1));
			attrs.setString(Tag.ImageType, VR.CS, new String[] { "DERIVED", "SECONDARY", "MPR" });

			double x = (params.imgPosition[0] instanceof Double) ? Double.class.cast(params.imgPosition[0])
					: Boolean.class.cast(params.imgPosition[0]) ? newSeries.length - index - 1 : index;
			double y = (params.imgPosition[1] instanceof Double) ? Double.class.cast(params.imgPosition[1])
					: Boolean.class.cast(params.imgPosition[1]) ? newSeries.length - index - 1 : index;
			Point3d p = geometry.getPosition(new Point2d(x, y));
			attrs.setString(Tag.ImagePositionPatient, VR.DS, new String[] { Double.toString(p.x), Double.toString(p.y), Double.toString(p.z) });

			LUTUtils.buildLUTs(attrs);
			double[] loc = GeometryOfSlice.computeSlicePositionVector(attrs);
            if (loc != null) {
                attrs.setFloat(Tag.SliceLocation, VR.DS, (float) (loc[0] + loc[1] + loc[2]));
            }            
            
            File out = new File(params.storageDir, uid);
            newSeries[i] = new OutFile(out);
            updateTags(attrs, newSeries[i]);
            newSeries[i].getTags().put(DicomUtils.TAG_PARENT_SERIES, params.seriesUID);            
            try (DicomOutputStream dcm = new DicomOutputStream(out)){
	            BulkData bdl = new BulkData(inFile.toURI().toString(), 0, (int) inFile.length(), false);
	            attrs.setValue(Tag.PixelData, VR.OW, bdl);
	            dcm.writeDataset(attrs.createFileMetaInformation(UID.ImplicitVRLittleEndian), attrs);
            }catch(IOException e){
            	throw new RuntimeException(e);
            }            
		}		
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
	
	private double writeBlock(OutFile[] newSeries, List<InFile> files, MprParams params, String seriesID) {

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
				if(cached != null && params.transposeImage == null){
					File cachedFile = new File(cached);					
					byte [] bytes = FileUtils.readBytes(new FileInputStream(cachedFile));
					int width = Integer.parseInt(cachedFile.getName().split("=")[1]);
					writeRasterInRaw(bytes, width, newSeries);
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
					writeRasterInRaw(getImage(image, params.transposeImage), newSeries);
				}
			}
			return lastSpace;
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			for (int i = 0; i < newSeries.length; i++) {
				if (newSeries[i] != null) {
					newSeries[i].disposeStream();
				}
			}
		}
	}

	private static void writeRasterInRaw(byte[] bytesOut, int width, OutFile[] newSeries) throws IOException {
		int height = newSeries.length;
		for (int j = 0; j < height; j++) {
			newSeries[j].getOutputStream().write(bytesOut, j * width, width);
		}
	}	
	
	private static void writeRasterInRaw(BufferedImage image, OutFile[] newSeries) throws IOException {
		if (newSeries != null && image != null && image.getHeight() == newSeries.length) {

			DataBuffer dataBuffer = image.getRaster().getDataBuffer();
			int width = image.getWidth();
			int height = newSeries.length;
			byte[] bytesOut = null;
			if (dataBuffer instanceof DataBufferByte) {				
				bytesOut = DataBufferByte.class.cast(dataBuffer).getData();
				for (int j = 0; j < height; j++) {
					newSeries[j].getOutputStream().write(bytesOut, j * width, width);
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
				for (int j = 0; j < height; j++) {
					newSeries[j].getOutputStream().write(bytesOut, j * width, width);
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
				for (int j = 0; j < height; j++) {
					newSeries[j].getOutputStream().write(bytesOut, j * width, width);
				}
			}
		}
	}
	
	
	static class MprParams {
		final String suffix;
		final String sliceOrientation;
		final boolean reverseSeriesOrder;
		final TransposeType transposeImage;
		final double[] imgOrientation;
		final boolean rotateOutputImg;
		final boolean reverseIndexOrder;
		final Object[] imgPosition;
		final double[] pixSize;
		final String frameOfReferenceUID;
		final String seriesUID;
		final String cacheDir;
		final String storageDir;
		
		public MprParams(String suffix, String sliceOrientation, boolean reverseSeriesOrder,
				TransposeType transposeImage, double[] imgOrientation, boolean rotateOutputImg,
				boolean reverseIndexOrder, Object[] imgPosition, double[] pixSize, 
				String frameOfReferenceUID, String seriesUID, String cacheDir, String storageDir) {
			this.suffix = suffix;
			this.sliceOrientation = sliceOrientation;
			this.reverseSeriesOrder = reverseSeriesOrder;
			this.transposeImage = transposeImage;
			this.imgOrientation = imgOrientation;
			this.rotateOutputImg = rotateOutputImg;
			this.reverseIndexOrder = reverseIndexOrder;
			this.imgPosition = imgPosition;
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
	
	private static BufferedImage getImage(PlanarImage source, TransposeType rotate) {
		if (rotate == null) {
			return source == null ? null : source.getAsBufferedImage();
		}
		return getRotatedImage(source, rotate);
	}


	private static BufferedImage getImage(BufferedImage source, TransposeType rotate) {
		if (rotate == null) {
			return source == null ? null : source;
		}
		return getRotatedImage(source, rotate);
	}
	
	private static BufferedImage getRotatedImage(RenderedImage source, TransposeType rotate) {
		RenderedOp result;
		if (source instanceof BufferedImage) {
			source = PlanarImage.wrapRenderedImage(source);
		}
		// use Transpose operation
		ParameterBlock pb = new ParameterBlock();
		pb.addSource(source);
		pb.add(rotate);
		result = JAI.create("transpose", pb, new RenderingHints(JAI.KEY_TILE_CACHE, null)); 
		// Handle non square images. Translation is necessary because the
		// transpose operator keeps the same
		// origin (top left not the center of the image)
		float diffw = source.getWidth() / 2.0f - result.getWidth() / 2.0f;
		float diffh = source.getHeight() / 2.0f - result.getHeight() / 2.0f;
		if (Math.signum(diffw) != 0 || Math.signum(diffh) != 0) {
			pb = new ParameterBlock();
			pb.addSource(result);
			pb.add(diffw);
			pb.add(diffh);
			result = JAI.create("translate", pb, new RenderingHints(JAI.KEY_TILE_CACHE, null)); 
		}
		return result.getAsBufferedImage();
	}

}
