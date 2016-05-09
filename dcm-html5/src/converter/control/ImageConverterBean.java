package converter.control;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DirectColorModel;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

import org.dcm4che3.image.PaletteColorModel;
import org.dcm4che3.imageio.plugins.dcm.DicomImageReadParam;

import converter.boundary.ImageConverter;

@Local(ImageConverter.class)
@Stateless
public class ImageConverterBean implements ImageConverter {

	BufferedImage resize(BufferedImage bi, Integer size){
		if(size == null){
			return bi;
		}else{
			int width = size;
			int height = bi.getHeight() * size / bi.getWidth();
			BufferedImage rbi = new BufferedImage(width, height, bi.getType());
			Graphics graphics = rbi.getGraphics();
			try {
				graphics.drawImage(bi, 0, 0, width, height, null);
			} finally {
				graphics.dispose();
			}
	        return rbi;
		}
	}

	BufferedImage convert(BufferedImage bi, Integer size) {
		ColorModel cm = bi.getColorModel();
		if (cm instanceof DirectColorModel) {
			return resize(bi, size);
		}
		if (cm.getNumComponents() != 3) {
			return resize(bi, size);
		}
		if (cm instanceof PaletteColorModel) {
			return resize(PaletteColorModel.class.cast(cm).convertToIntDiscrete(bi.getRaster()), size);
		}
		int width = size == null ? bi.getWidth() : size;
		int height = size == null ? bi.getHeight() : bi.getHeight() * size / bi.getWidth();
//		int height = size == null ? bi.getHeight() : size;
		BufferedImage intRGB = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		if (intRGB.getColorModel().getColorSpace().equals(cm.getColorSpace())) {
			int[] intData = DataBufferInt.class.cast(intRGB.getRaster().getDataBuffer()).getData();
			DataBufferByte dataBuffer = DataBufferByte.class.cast(bi.getRaster().getDataBuffer());
			if (dataBuffer.getNumBanks() == 3) {
				byte[] r = dataBuffer.getData(0);
				byte[] g = dataBuffer.getData(1);
				byte[] b = dataBuffer.getData(2);
				for (int i = 0; i < intData.length; i++)
					intData[i] = ((r[i] & 0xff) << 16) | ((g[i] & 0xff) << 8) | (b[i] & 0xff);
			} else {
				byte[] b = dataBuffer.getData();
				for (int i = 0, j = 0; i < intData.length; i++)
					intData[i] = ((b[j++] & 0xff) << 16) | ((b[j++] & 0xff) << 8) | (b[j++] & 0xff);
			}
		} else {
			Graphics graphics = intRGB.getGraphics();
			try {
				graphics.drawImage(bi, 0, 0, width, height, null);
			} finally {
				graphics.dispose();
			}
		}
		return intRGB;
	}

	@Override
	public byte[] convert(byte[] content, String format, Integer size, Float wc, Float ww) {
		try {
			BufferedInputStream bis = new BufferedInputStream(new ByteArrayInputStream(content));
			Iterator<ImageReader> iter = ImageIO.getImageReadersByFormatName("DICOM");
			ImageReader reader = iter.next();
			ImageInputStream iis = ImageIO.createImageInputStream(bis);

			reader.setInput(iis, false);

			DicomImageReadParam param = DicomImageReadParam.class.cast(reader.getDefaultReadParam());
			if (wc != null) {
				param.setWindowCenter(wc);
			}
			if (ww != null) {
				param.setWindowWidth(ww);
			}
			
			BufferedImage bi = convert(reader.read(0, param), size);

			Iterator<ImageWriter> imageWriters = ImageIO.getImageWritersByFormatName(format);
			if (!imageWriters.hasNext()) {
				throw new IllegalArgumentException(String.format("Image format %s not supported", format));
			}

			ImageWriter imageWriter = imageWriters.next();
			ImageWriteParam imageWriteParam = imageWriter.getDefaultWriteParam();

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageOutputStream ios = ImageIO.createImageOutputStream(baos);

			imageWriter.setOutput(ios);
			imageWriter.write(null, new IIOImage(bi, null, null), imageWriteParam);

			return baos.toByteArray();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
