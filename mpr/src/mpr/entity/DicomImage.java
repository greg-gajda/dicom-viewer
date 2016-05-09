package mpr.entity;

import static utils.DicomUtils.getRGBImageFromPaletteColorModel;

import java.awt.Dimension;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BandedSampleModel;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferUShort;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.Arrays;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.stream.ImageInputStream;
import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.operator.NullDescriptor;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.BulkData;
import org.dcm4che3.data.Fragments;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.UID;
import org.dcm4che3.data.VR;
import org.dcm4che3.image.PhotometricInterpretation;
import org.dcm4che3.imageio.codec.ImageReaderFactory;
import org.dcm4che3.imageio.codec.ImageReaderFactory.ImageReaderParam;
import org.dcm4che3.imageio.plugins.dcm.DicomImageReadParam;
import org.dcm4che3.imageio.plugins.dcm.DicomMetaData;
import org.dcm4che3.imageio.stream.ImageInputStreamAdapter;
import org.dcm4che3.imageio.stream.SegmentedImageInputStream;
import org.dcm4che3.io.BulkDataDescriptor;
import org.dcm4che3.io.DicomInputStream;
import org.dcm4che3.io.DicomInputStream.IncludeBulkData;

import com.sun.media.imageio.stream.RawImageInputStream;
import com.sun.media.imageioimpl.plugins.raw.RawImageReader;
import com.sun.media.imageioimpl.plugins.raw.RawImageReaderSpi;

import utils.FileUtils;
import utils.RectifySignedShortDataDescriptor;
import utils.RectifyUShortToShortDataDescriptor;

public class DicomImage {

	InFile file;

	public DicomImage(InFile file) {
		super();
		this.file = file;
		params = new ImageParams();
	}

	DicomMetaData metadata;
	ImageParams params;
	ImageReader decompressor;

	DicomMetaData getDicomMetaData() {
		if (metadata == null) {
			DicomInputStream dis = null;
			try {
				File f = file.getFile();
				ImageInputStream iis = ImageIO.createImageInputStream(f);
				if (iis == null) {
					throw new IllegalStateException(String.format("Empty ImageInputStream for file %s", f.getName()));
				}
				iis.seek(0L);
				dis = new DicomInputStream(new ImageInputStreamAdapter(iis));
				dis.setIncludeBulkData(IncludeBulkData.URI);
				dis.setBulkDataDescriptor(BulkDataDescriptor.DEFAULT);
				dis.setURI(f.toURI().toString());
				Attributes fmi = dis.readFileMetaInformation();
				Attributes ds = dis.readDataset(-1, -1);
				if (fmi == null) {
					fmi = ds.createFileMetaInformation(dis.getTransferSyntax());
				}
				metadata = new DicomMetaData(fmi, ds);
				params.tsuid = dis.getTransferSyntax();
			} catch (IOException e) {
				throw new RuntimeException(e);
			} finally {
				FileUtils.safeClose(dis);
			}
		}
		return metadata;
	}

	public Attributes getHeader() {
		return getDicomMetaData().getAttributes();
	}

	public void readPixelData() {

		Attributes header = getHeader();

		double[] pixs = header.getDoubles(Tag.PixelSpacing);
		if (pixs == null || pixs.length != 2) {
			pixs = header.getDoubles(Tag.ImagerPixelSpacing);
		}
		params.pixelSpacing = pixs;

		params.bitsStored = header.getInt(Tag.BitsStored, 8);
		params.bitsAllocated = header.getInt(Tag.BitsAllocated, params.bitsStored);
		params.highBit = header.getInt(Tag.HighBit, params.bitsStored - 1);

		params.photometricInterpretation = header.getString(Tag.PhotometricInterpretation, "MONOCHROME2");
		params.pmi = PhotometricInterpretation.fromString(params.photometricInterpretation);

		params.samplesPerPixel = header.getInt(Tag.SamplesPerPixel, 1);
		params.banded = params.samplesPerPixel > 1 && header.getInt(Tag.PlanarConfiguration, 0) != 0;
		int pixelRepresentation = header.getInt(Tag.PixelRepresentation, 0);
		int dataType = params.bitsAllocated <= 8 ? DataBuffer.TYPE_BYTE
				: pixelRepresentation != 0 ? DataBuffer.TYPE_SHORT : DataBuffer.TYPE_USHORT;
		if (params.bitsAllocated > 16 && params.samplesPerPixel == 1) {
			dataType = DataBuffer.TYPE_INT;
		}

		if (header.getInt(Tag.OverlayBitsAllocated, 0) > 1 && params.bitsStored < params.bitsAllocated
				&& dataType >= DataBuffer.TYPE_BYTE && dataType < DataBuffer.TYPE_INT) {

			int high = params.highBit + 1;
			int val = (1 << high) - 1;
			if (high > params.bitsStored) {
				val -= (1 << (high - params.bitsStored)) - 1;
			}
			params.overlayBitMask = val;
		}

		Object pixdata = header.getValue(Tag.PixelData, new VR.Holder());
		if (pixdata != null) {
			params.numberOfFrames = header.getInt(Tag.NumberOfFrames, 1);
			boolean hasPixel = header.getInt(Tag.BitsStored, header.getInt(Tag.BitsAllocated, 0)) > 0;

			if (!params.tsuid.startsWith("1.2.840.10008.1.2.4.10") && hasPixel) {

				if (pixdata instanceof BulkData) {
					int width = header.getInt(Tag.Columns, 512);
					int height = header.getInt(Tag.Rows, 512);
					//iis.setByteOrder(header.bigEndian() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
					params.frameLength = params.pmi
							.frameLength(width, height, params.samplesPerPixel, params.bitsAllocated);
					params.pixeldata = BulkData.class.cast(pixdata);
					// Handle JPIP
				} else if (header.getString(Tag.PixelDataProviderURL) != null) {
					if (params.numberOfFrames == 0) {
						params.numberOfFrames = 1;
						// compressed = true;
					}
				} else if (pixdata instanceof Fragments) {
					ImageReaderParam param = ImageReaderFactory.getImageReaderParam(params.tsuid);
					decompressor = ImageReaderFactory.getImageReader(param);
					if (decompressor == null) {
						throw new RuntimeException(String.format("Unsupported Transfer Syntax: %s", params.tsuid));
					}
					// this.patchJpegLS = param.patchJPEGLS;
					params.pixeldataFragments = Fragments.class.cast(pixdata);
				}
			}
		}

	}
	
	public PlanarImage readImage() {
		RenderedImage buffer = readAsRenderedImage(0);
        PlanarImage img = null;
        if (buffer != null) {
            if (params.dataType == DataBuffer.TYPE_SHORT && buffer.getSampleModel().getDataType() == DataBuffer.TYPE_USHORT) {
                ImageLayout layout = new ImageLayout(buffer);
                layout.setTileWidth(512);
                layout.setTileHeight(512);
                RenderingHints hints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, layout);	                
                img = RectifyUShortToShortDataDescriptor.create(buffer, hints);	                                
//            } else if (ImageUtil.isBinary(buffer.getSampleModel())) {	            	
//                ParameterBlock pb = new ParameterBlock();
//                pb.addSource(buffer);
//                // Tile size are set in this operation
//                img = JAI.create("formatbinary", pb, null); 
            } else if (buffer.getTileWidth() != 512 || buffer.getTileHeight() != 512) {
                ImageLayout layout = new ImageLayout();
                layout.setTileWidth(512);
                layout.setTileHeight(512);
                RenderingHints hints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, layout);
                ParameterBlock pb = new ParameterBlock();
                pb.addSource(buffer);
                pb.add(buffer.getSampleModel().getDataType());
                img = JAI.create("format", pb, hints); 	    
            } else {
                ImageLayout layout = new ImageLayout();
                layout.setTileWidth(512);
                layout.setTileHeight(512);
                RenderingHints hints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, layout);	            	
                img = NullDescriptor.create(buffer, hints);
            }
            img = getRGBImageFromPaletteColorModel(img, getHeader());
        }
        return img;
	}

	public RenderedImage readAsRenderedImage(int frameIndex) {
		Attributes header = getHeader();
		PhotometricInterpretation pmi = params.pmi;
		try {
			ImageReadParam param = new DicomImageReadParam();
			if (frameIndex < 0 || frameIndex >= getHeader().getInt(Tag.NumberOfFrames, 1)) {
				throw new IndexOutOfBoundsException(String.format("imageIndex: %d out of NumberOfFrames", frameIndex));
			}
			RenderedImage bi = null;
			ImageInputStream iis = ImageIO.createImageInputStream(file.getFile());

			ColorModel cm = pmi.createColorModel(params.bitsStored, params.dataType, header);
			SampleModel sm = params.pmi
					.createSampleModel(params.dataType, header.getInt(Tag.Columns, 512), header
							.getInt(Tag.Rows, 512), header.getInt(Tag.SamplesPerPixel, 512), params.banded);

			if (decompressor != null) {
				decompressor.setInput(SegmentedImageInputStream
						.ofFrame(iis, params.pixeldataFragments, frameIndex, params.numberOfFrames));

				ImageReadParam decompressParam = decompressor.getDefaultReadParam();
				ImageTypeSpecifier imageType = param.getDestinationType();
				BufferedImage dest = param.getDestination();
				if (params.tsuid.equals(UID.RLELossless) && imageType == null && dest == null) {
					imageType = new ImageTypeSpecifier(cm, sm);
				}
				decompressParam.setDestinationType(imageType);
				decompressParam.setDestination(dest);
				bi = decompressor.readAsRenderedImage(0, decompressParam);
			} else {
				// Rewrite image with subsampled model (otherwise cannot not be displayed as RenderedImage)
				// Convert YBR_FULL into RBG as the ybr model is not well supported.
				if (pmi.isSubSambled() || pmi.name().startsWith("YBR")) { 
					// TODO improve this
					iis.seek(params.pixeldata.offset() + frameIndex * params.frameLength);

					WritableRaster raster = Raster.createWritableRaster(sm, null);
					DataBuffer buf = raster.getDataBuffer();
					if (buf instanceof DataBufferByte) {
						byte[][] data = ((DataBufferByte) buf).getBankData();
						for (byte[] bs : data) {
							iis.readFully(bs);
						}
					} else {
						short[] data = ((DataBufferUShort) buf).getData();
						iis.readFully(data, 0, data.length);
					}

					ColorModel cmodel = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB),
							new int[] { 8, 8, 8 }, false, // has alpha
							false, // alpha premultipled
							Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
					int width = raster.getWidth();
					int height = raster.getHeight();
					SampleModel sampleModel = cmodel.createCompatibleSampleModel(width, height);
					DataBuffer dataBuffer = sampleModel.createDataBuffer();
					WritableRaster rasterDst = Raster.createWritableRaster(sampleModel, dataBuffer, null);

					ColorSpace cs = cm.getColorSpace();
					for (int i = 0; i < height; i++) {
						for (int j = 0; j < width; j++) {
							byte[] ba = (byte[]) raster.getDataElements(j, i, null);
							float[] fba = new float[] { (ba[0] & 0xFF) / 255f, (ba[1] & 0xFF) / 255f,
									(ba[2] & 0xFF) / 255f };
							float[] rgb = cs.toRGB(fba);
							ba[0] = (byte) (rgb[0] * 255);
							ba[1] = (byte) (rgb[1] * 255);
							ba[2] = (byte) (rgb[2] * 255);
							rasterDst.setDataElements(j, i, ba);
						}
					}
					bi = new BufferedImage(cmodel, rasterDst, false, null);

				} else {
					ImageReader reader = initRawImageReader(iis);
					bi = reader.readAsRenderedImage(frameIndex, param);
				}
			}
			return validateSignedShortDataBuffer(bi);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private ImageReader initRawImageReader(ImageInputStream iis) {
		Attributes header = getHeader();

		long[] frameOffsets = new long[params.numberOfFrames];
		frameOffsets[0] = params.pixeldata.offset();
		for (int i = 1; i < frameOffsets.length; i++) {
			frameOffsets[i] = frameOffsets[i - 1] + params.frameLength;
		}
		Dimension[] imageDimensions = new Dimension[params.numberOfFrames];
		int width = header.getInt(Tag.Columns, 512);
		int height = header.getInt(Tag.Rows, 512);
		Arrays.fill(imageDimensions, new Dimension(width, height));

		// PhotometricInterpretation pmi = imageParams.pmi;
		// ColorModel cmodel = pmi.createColorModel(imageParams.bitsStored,
		// imageParams.dataType, header);
		// SampleModel smodel = pmi.createSampleModel(imageParams.dataType,
		// width, height, imageParams.samplesPerPixel, imageParams.banded);
		//
		// if (pmi.isSubSambled() == false && (width >= 1024 || height >= 1024))
		// {
		// width = Math.min(width, 512);
		// height = Math.min(height, 512);
		// smodel = pmi.createSampleModel(imageParams.dataType, width, height,
		// imageParams.samplesPerPixel, imageParams.banded);
		// }

		int bps = params.bitsAllocated;
		int spp = params.samplesPerPixel;
		int pixelRepresentation = header.getInt(Tag.PixelRepresentation, 0);

		int dataType = bps <= 8 ? DataBuffer.TYPE_BYTE
				: pixelRepresentation != 0 ? DataBuffer.TYPE_SHORT : DataBuffer.TYPE_USHORT;
		if (bps > 16 && spp == 1) {
			dataType = DataBuffer.TYPE_INT;
		}
		ColorSpace cs = null;
		if (spp == 1) {
			cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
		} else if (spp == 3) {
			cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
		} else {
			throw new IllegalArgumentException(String.format("Unsupported Samples per Pixel: %d ", spp));
		}
		if (cs == null) {
			throw new IllegalArgumentException(
					String.format("Unsupported Photometric Interpretation with Samples per Pixel: %d", spp));
		}
		int[] bits = new int[spp];
		Arrays.fill(bits, bps);
		ComponentColorModel cm = new ComponentColorModel(cs, bits, false, false, Transparency.OPAQUE, dataType);

		SampleModel sm = null;
		if (spp == 1) {
			sm = new PixelInterleavedSampleModel(dataType, width, height, 1, width, new int[] { 0 });
		} else if (params.banded) {// samples == 3
			sm = new BandedSampleModel(dataType, width, height, width, new int[] { 0, 1, 2 }, new int[] { 0, 0, 0 });
		} else {
			sm = new PixelInterleavedSampleModel(dataType, width, height, 3, width * 3, new int[] { 0, 1, 2 });
		}
		ImageTypeSpecifier its = new ImageTypeSpecifier(cm, sm);
		RawImageInputStream riis = new RawImageInputStream(iis, its, frameOffsets, imageDimensions);
		riis.setByteOrder(ByteOrder.LITTLE_ENDIAN);
		//riis.setByteOrder(bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
		ImageReader reader = new RawImageReader(new RawImageReaderSpi());
		//ImageReader reader = ImageIO.getImageReadersByFormatName("RAW").next();
//		if (reader == null) {
//			FileUtils.safeClose(riis);
//			throw new UnsupportedOperationException("No RAW Reader available");
//		}
		reader.setInput(riis);
		return reader;
	}

	/*
	 * Issue in ComponentColorModel when signed short DataBuffer, only 16 bits is supported see
	 * http://java.sun.com/javase/6/docs/api/java/awt/image/ComponentColorModel. html Instances of ComponentColorModel
	 * created with transfer types DataBuffer.TYPE_SHORT, DataBuffer.TYPE_FLOAT, and DataBuffer.TYPE_DOUBLE use all the
	 * bits of all sample values. Thus all color/alpha components have 16 bits when using DataBuffer.TYPE_SHORT, 32 bits
	 * when using DataBuffer.TYPE_FLOAT, and 64 bits when using DataBuffer.TYPE_DOUBLE. When the
	 * ComponentColorModel(ColorSpace, int[], boolean, boolean, int, int) form of constructor is used with one of these
	 * transfer types, the bits array argument is ignored.
	 */
	// Bits Allocated = 16 (Bits allou�s )
	// Bits Stored = 12 (Bits enregistr�s )
	// High Bit = 11 (Bit le plus significatif)
	// |<------------------ pixel ----------------->|
	// ______________ ______________ ______________ ______________
	// |XXXXXXXXXXXXXX| | | |
	// |______________|______________|______________|______________|
	// 15 12 11 8 7 4 3 0
	//
	// ---------------------------
	//
	// Bits Allocated = 16
	// Bits Stored = 12
	// High Bit = 15
	// |<------------------ pixel ----------------->|
	// ______________ ______________ ______________ ______________
	// | | | |XXXXXXXXXXXXXX|
	// |______________|______________|______________|______________|
	// 15 12 11 8 7 4 3 0
	RenderedImage validateSignedShortDataBuffer(RenderedImage source) {

		// TODO test with all decoders (works with raw decoder)
		if (source != null && params.dataType == DataBuffer.TYPE_SHORT
				&& source.getSampleModel().getDataType() == DataBuffer.TYPE_SHORT
				&& (params.highBit + 1) < params.bitsAllocated) {
			source = RectifySignedShortDataDescriptor.create(source, new int[] { params.highBit + 1 }, null);
		}
		return source;
	}
	
	public boolean isCompressed(){
		return decompressor != null;
	}

}
