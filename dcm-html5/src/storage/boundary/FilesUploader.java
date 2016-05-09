package storage.boundary;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cache.boundary.ImageCache;
import config.ConfigParams;
import dicom.control.Decoder;
import dicom.control.DicomBean;
import dicom.entity.Image;
import utils.FileUtils;

@MultipartConfig
@WebServlet(urlPatterns = "/uploader")
public class FilesUploader extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Logger log = LoggerFactory.getLogger(getClass());
	
	ImageStorage storage;
	
	@EJB DicomBean dicomBean;
	
	@EJB Decoder decoder;
	
	@EJB ImageCache cache;

	@Resource(name = ConfigParams.DISABLE_UPLOAD)
	Boolean disable;
	
	private String getFileName(final Part part) {
	    final String partHeader = part.getHeader("content-disposition");
	    log.info(String.format("Part Header = %s", partHeader));
	    for (String s: part.getHeader("content-disposition").split(";")) {
	        if (s.trim().startsWith("filename")) {
	            return s.substring(s.indexOf('=') + 1).trim().replace("\"", "");
	        }
	    }
	    return null;
	}
	
	ByteArrayOutputStream transfer(Set<Part> parts){
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			for(Part p: parts){			
				FileUtils.transfer(p.getInputStream(), baos);
			}
		} catch (IOException e) {
			throw new RuntimeException(e); 
		}		
		return baos;
	}
	
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    	
    	if(disable){
	    	resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
    	}else if (req.getHeader("Content-Type") != null && req.getHeader("Content-Type").startsWith("multipart/form-data")) {    			
	    	BinaryOperator<Map<String, Set<Part>>> combiner = (m1, m2) -> {
	    		Map<String, Set<Part>> m = new HashMap<>();
	    		m.putAll(m1);
	    		m.putAll(m2);	    		
	    		return m;	    		
	    	};

	    	BiFunction<Map<String, Set<Part>>, Part, Map<String, Set<Part>>> accumulator = (m, p) -> {
	    		String fn = getFileName(p); 
	    		Set<Part> set = m.get(fn);
	    		if(set == null){
	    			set = new HashSet<Part>();
	    			m.put(fn, set);
	    		}
	    		set.add(p);	    		
	    		return m;
	    	};
	    	
	    	StringBuilder sb = new StringBuilder("[ ");
	    	req.getParts().stream().reduce(new HashMap<String, Set<Part>>(), accumulator, combiner).forEach((file, parts) -> {
	    		ByteArrayOutputStream baos = transfer(parts);
	    		byte[] content = baos.toByteArray();
	    		sb.append("{").append("\"file\":\"").append(file).append("\"},");
	    		//dicomHeader.read(content).forEach( (t, v) -> log.info(String.format("Tag %d, %s", t, v)));
	    		Image image = dicomBean.writeImage(file, content);
	    		storage.store(image.getSopInstanceUID(), content);
	    		cache.put(image.getSopInstanceUID(), content);
	    		decoder.decode(image.getSopInstanceUID());
	    		log.info(String.format("File '%s' was written after upload", file));	    		
	    	});
	    	sb.delete(sb.length()-1, sb.length());
	    	sb.append(" ]");
	    	
	    	resp.setContentType("text/html;charset=UTF-8");
	    	resp.setCharacterEncoding("utf-8");
            resp.getWriter().write(sb.toString());
            log.info(sb.toString());
    	}
    	resp.flushBuffer();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

}
