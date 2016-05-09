package viewer;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.Optional;

import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cache.boundary.ImageCache;
import config.ConfigParams;
import converter.boundary.ImageConverter;
import dicom.control.Decoder;
import dicom.control.DicomBean;
import storage.boundary.ImageStorage;

@WebServlet(urlPatterns = "/viewer")
public class ImageViewer extends HttpServlet {

	private static final long serialVersionUID = 1L;
	
	ImageStorage storage;

	@EJB
	ImageConverter converter;
	
	@EJB
	DicomBean dicomBean;
	
	@EJB
	ImageCache cache;
	
	@EJB 
	Decoder decoder;		
		
	static {
		ImageIO.scanForPlugins();		
		javax.imageio.spi.IIORegistry.getDefaultInstance().registerApplicationClasspathSpis();
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		String defaultFormat = req.getServletContext().getInitParameter(ConfigParams.DEFAULT_IMAGE_FORMAT);
		String image = req.getParameter("image");
		String format = Optional.ofNullable(req.getParameter("format")).orElse(defaultFormat);
		String size = req.getParameter("size");		
		String wc = req.getParameter("wc");
		String ww = req.getParameter("ww");
		
		byte[] content = null;
		if(image != null && image.isEmpty() == false){			
			String cacheId = String.format("image=%s,format=%s,size=%s,wc=%s,ww=%s", image, format, size, wc, ww);
//			content = cache.getOptional(cacheId).orElseGet(() -> {
//				byte [] cached = cache.getOptional(image).orElseGet(() -> {
//					try{
//						byte [] stored = storage.getImage(image);
//						cache.put(image, stored);
//						decoder.decode(image);
//						return stored;
//					}catch(StorageException e){
//						return null;
//					}							
//				});				
//				cached = converter.convert(cached, defaultFormat, size == null ? null : Integer.parseInt(size), wc == null ? null : Float.parseFloat(wc), ww == null ? null : Float.parseFloat(ww));
//				cache.putAsync(cacheId, cached);
//				return cached;
//			});			
			content = cache.get(cacheId);
			if(content == null){
				content = cache.get(image);
				if(content == null){
					try{
						content = storage.getImage(image);
						cache.putAsync(image, content);
					}catch(EJBException e){
						content = null;
					}						
				}
				if(content != null){
					content = converter.convert(content, format, size == null ? null : Integer.parseInt(size), wc == null ? null : Float.parseFloat(wc), ww == null ? null : Float.parseFloat(ww));
					cache.putAsync(cacheId, content);
				}
			}
		}else{			
			String study = req.getParameter("study");
			String cacheId = String.format("study=%s,format=%s,size=%s", study, format, size);
//			content = cache.getOptional(cacheId).orElseGet(() -> {
//				byte [] stored = storage.getImage(dicomBean.getImageByStudyId(study).getSopInstanceUID());
//				stored = converter.convert(stored, defaultFormat, size == null ? null : Integer.parseInt(size), wc == null ? null : Float.parseFloat(wc), ww == null ? null : Float.parseFloat(ww));
//				cache.putAsync(cacheId, stored);
//				return stored;
//			});			
			content = cache.get(cacheId);
			if(content == null){
				content = storage.getImage(dicomBean.getImageByStudyId(study).getSopInstanceUID());
				content = converter.convert(content, format, size == null ? null : Integer.parseInt(size), wc == null ? null : Float.parseFloat(wc), ww == null ? null : Float.parseFloat(ww));
				cache.putAsync(cacheId, content);
			}			
		}

		try (BufferedOutputStream bos = new BufferedOutputStream(resp.getOutputStream())) {			
			resp.setHeader("Cache-Control", "private, max-age=100000");
			resp.setHeader("Pragma", "cache");
			if(content != null){
				resp.setContentType(String.format("image/%s", format));
				bos.write(content);
			}else{
				resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
			}			
			bos.flush();		
			resp.flushBuffer();
		}

	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doGet(req, resp);
	}
}
