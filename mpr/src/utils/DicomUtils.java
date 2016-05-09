/**
 *
 * @author Benoit Jacquemoud
 *
 * @version $Rev$ $Date$
 */
package utils;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DirectColorModel;
import java.awt.image.RenderedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.media.jai.JAI;
import javax.media.jai.LookupTableJAI;
import javax.media.jai.PlanarImage;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.image.PaletteColorModel;
import org.dcm4che3.imageio.plugins.dcm.DicomImageReadParam;

public class DicomUtils {

	public static final Integer TAG_BIG_ENDIAN = 0x00010100;
	public static final Integer TAG_PARENT_SERIES = 0x00010101;

	public static int[] getIntArray(Attributes dicom, int tag, int[] defValue) {
		if (dicom == null || !dicom.containsValue(tag)) {
			return defValue;
		}
		try {
			return dicom.getInts(tag);
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}
		return defValue;
	}

	public static String getString(Object val) {
		if (val != null) {
			if (val instanceof String[]) {
				return Arrays.toString(String[].class.cast(val));
			} else {
				return val.toString();
			}
		}
		return null;
	}

	public static double[] getDoubleArray(Object val) {
		if (val != null) {
			if (val instanceof double[]) {
				return double[].class.cast(val);
			} else if (val instanceof String[]) {
				return Arrays.stream(String[].class.cast(val)).mapToDouble(s -> Double.parseDouble(s)).toArray();
			} else if (val instanceof String) {
				return new double[] { Double.parseDouble(val.toString()) };
			}
		}
		return null;
	}

	public static Double getDouble(Object val) {
		if (val != null) {
			if (val instanceof Double) {
				return Double.class.cast(val);
			} else if (val instanceof String) {
				return Double.parseDouble(val.toString());
			}
		}
		return null;
	}

	public static Integer getInteger(Object val) {
		if (val != null) {
			if (val instanceof Integer) {
				return Integer.class.cast(val);
			} else if (val instanceof String && val.toString().isEmpty() == false) {
				return Integer.parseInt(val.toString());
			}
		}
		return null;
	}

	public static PlanarImage getImage(byte[] content) {
		try {
			Iterator<ImageReader> iter = ImageIO.getImageReadersByFormatName("DICOM");
			ImageReader reader = iter.next();
			BufferedInputStream bis = new BufferedInputStream(new ByteArrayInputStream(content));
			ImageInputStream iis = ImageIO.createImageInputStream(bis);
			reader.setInput(iis, false);
			DicomImageReadParam param = DicomImageReadParam.class.cast(reader.getDefaultReadParam());
			BufferedImage bi = convert(reader.read(0, param));
			return PlanarImage.wrapRenderedImage(bi);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static BufferedImage convert(BufferedImage bi) {
		ColorModel cm = bi.getColorModel();
		if (cm instanceof DirectColorModel) {
			return bi;
		}
		if (cm.getNumComponents() != 3) {
			return bi;
		}
		if (cm instanceof PaletteColorModel) {
			return PaletteColorModel.class.cast(cm).convertToIntDiscrete(bi.getRaster());
		}
		int width = bi.getWidth();
		int height = bi.getHeight();
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
	
	public static PlanarImage getRGBImageFromPaletteColorModel(RenderedImage source, Attributes ds) {
		if (source == null) {
			return null;
		}

		// Convert images with PaletteColorModel to RGB model
		if (source.getColorModel() instanceof PaletteColorModel) {
			if (ds != null) {
				int[] rDesc = lutDescriptor(ds, Tag.RedPaletteColorLookupTableDescriptor);
				int[] gDesc = lutDescriptor(ds, Tag.GreenPaletteColorLookupTableDescriptor);
				int[] bDesc = lutDescriptor(ds, Tag.BluePaletteColorLookupTableDescriptor);
				byte[] r = lutData(ds, rDesc, Tag.RedPaletteColorLookupTableData,
						Tag.SegmentedRedPaletteColorLookupTableData);
				byte[] g = lutData(ds, gDesc, Tag.GreenPaletteColorLookupTableData,
						Tag.SegmentedGreenPaletteColorLookupTableData);
				byte[] b = lutData(ds, bDesc, Tag.BluePaletteColorLookupTableData,
						Tag.SegmentedBluePaletteColorLookupTableData);
				LookupTableJAI lut = new LookupTableJAI(new byte[][] { r, g, b });

				// Replace the original image with the RGB image.
				return JAI.create("lookup", source, lut);
			}
		}
		return PlanarImage.wrapRenderedImage(source);
	}

	static int[] lutDescriptor(Attributes ds, int descTag) {
		int[] desc = getIntArray(ds, descTag, null);
		if (desc == null) {
			throw new IllegalArgumentException("Missing LUT Descriptor!");
		}
		if (desc.length != 3) {
			throw new IllegalArgumentException("Illegal number of LUT Descriptor values: " + desc.length);
		}
		if (desc[0] < 0) {
			throw new IllegalArgumentException("Illegal LUT Descriptor: len=" + desc[0]);
		}
		int bits = desc[2];
		if (bits != 8 && bits != 16) {
			throw new IllegalArgumentException("Illegal LUT Descriptor: bits=" + bits);
		}
		return desc;
	}

	static byte[] lutData(Attributes ds, int[] desc, int dataTag, int segmTag) {
		int len = desc[0] == 0 ? 0x10000 : desc[0];
		int bits = desc[2];
		byte[] data = ds.getSafeBytes(dataTag);
		if (data == null) {
			int[] segm = getIntArray(ds, segmTag, null);
			if (segm == null) {
				throw new IllegalArgumentException("Missing LUT Data!");
			}
			if (bits == 8) {
				throw new IllegalArgumentException("Segmented LUT Data with LUT Descriptor: bits=8");
			}
			data = new byte[len];
			inflateSegmentedLut(segm, data);
		} else if (bits == 16 || data.length != len) {
			if (data.length != len << 1) {
				throw new IllegalArgumentException(
						String.format("Number of actual LUT entries: %d mismatch specified value: %d in LUT Descriptor",
								data.length, len));
			}
			int hilo = ds.bigEndian() ? 0 : 1;
			if (bits == 8) {
				hilo = 1 - hilo; // padded high bits -> use low bits
			}
			byte[] bs = new byte[data.length >> 1];
			for (int i = 0; i < bs.length; i++) {
				bs[i] = data[(i << 1) | hilo];
			}
			data = bs;
		}
		return data;
	}

	static void inflateSegmentedLut(int[] in, byte[] out) {
		int x = 0;
		try {
			for (int i = 0; i < in.length;) {
				int op = in[i++];
				int n = in[i++];
				switch (op) {
				case 0:
					while (n-- > 0) {
						out[x++] = (byte) in[i++];
					}
					break;
				case 1:
					x = linearSegment(in[i++], out, x, n);
					break;
				case 2: {
					int i2 = (in[i++] & 0xffff) | (in[i++] << 16);
					while (n-- > 0) {
						int op2 = in[i2++];
						int n2 = in[i2++] & 0xffff;
						switch (op2) {
						case 0:
							while (n2-- > 0) {
								out[x++] = (byte) in[i2++];
							}
							break;
						case 1:
							x = linearSegment(in[i2++], out, x, n);
							break;
						default:
							throw new IllegalArgumentException(
									String.format("illegal op code: %d, index: %d", op, i2 - 2));
						}
					}
				}
				default:
					throw new IllegalArgumentException(String.format("illegal op code: %d, index: %d", op, i - 2));
				}
			}
		} catch (IndexOutOfBoundsException e) {
			if (x > out.length) {
				throw new IllegalArgumentException(String.format(
						"Number of entries in inflated segmented LUT exceeds specified value in LUT Descriptor %d",
						out.length));

			} else {
				throw new IllegalArgumentException("Running out of data inflating segmented LUT");
			}
		}
		if (x < out.length) {
			throw new IllegalArgumentException(String.format(
					"Number of actual LUT entries: %d mismatch specified value: %d in LUT Descriptor", x, out.length));
		}
	}

	static int linearSegment(int y1, byte[] out, int x, int n) {
		if (x == 0) {
			throw new IllegalArgumentException("Linear segment cannot be the first segment");
		}

		try {
			int y0 = out[x - 1];
			int dy = y1 - y0;
			for (int j = 1; j <= n; j++) {
				out[x++] = (byte) ((y0 + dy * j / n) >> 8);
			}
		} catch (IndexOutOfBoundsException e) {
			throw new IllegalArgumentException(String.format(
					"Number of entries in inflated segmented LUT exceeds specified value in LUT Descriptor %d",
					out.length));
		}
		return x;
	}

	
}
