package dicom.control;

import java.awt.RenderingHints;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;

import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;

import org.dcm4che3.data.Tag;

import cache.boundary.ImageCache;
import mpr.entity.DicomImage;
import mpr.entity.InFile;
import utils.GeometryOfSlice;

@LocalBean
@Stateless
public class Decoder {

	@EJB ImageCache cache;
	
	@Asynchronous
	public void decode(String file) {
		DicomImage dcm = new DicomImage(new InFile(new File(cache.getCacheDir(), file)));
		dcm.readPixelData();
		PlanarImage image = dcm.readImage();
		if (image != null) {
			double[] pixSize = dcm.getHeader().getDoubles(Tag.PixelSpacing);
			if (pixSize == null || pixSize.length != 2) {
				pixSize = dcm.getHeader().getDoubles(Tag.ImagerPixelSpacing);
			}
			if (GeometryOfSlice.getRescaleX(pixSize) != GeometryOfSlice.getRescaleY(pixSize)) {
				ParameterBlock pb = new ParameterBlock();
				pb.addSource(image);
				pb.add((float) GeometryOfSlice.getRescaleX(pixSize)).add((float) GeometryOfSlice.getRescaleY(pixSize))
						.add(0.0f).add(0.0f);
				pb.add(Interpolation.getInstance(Interpolation.INTERP_BILINEAR));
				image = JAI.create("scale", pb, new RenderingHints(JAI.KEY_TILE_CACHE, null));
			}
			DataBuffer dataBuffer = image.getAsBufferedImage().getRaster().getDataBuffer();
			int take = 1;
			int width = image.getWidth();
			byte[] bytesOut = null;
			if (dataBuffer instanceof DataBufferByte) {
				bytesOut = DataBufferByte.class.cast(dataBuffer).getData();
			} else if (dataBuffer instanceof DataBufferShort || dataBuffer instanceof DataBufferUShort) {
				short[] data = dataBuffer instanceof DataBufferShort ? DataBufferShort.class.cast(dataBuffer).getData()
						: DataBufferUShort.class.cast(dataBuffer).getData();
				bytesOut = new byte[data.length * 2];
				for (int i = 0; i < data.length; i++) {
					bytesOut[i * 2] = (byte) (data[i] & 0xFF);
					bytesOut[i * 2 + 1] = (byte) ((data[i] >>> 8) & 0xFF);
				}
				width *= 2;
				take = 2;
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
				take = 4;
			}
			cache.put(String.format("%s=%d=%d", file, width, take), bytesOut);
		}
	}

}
