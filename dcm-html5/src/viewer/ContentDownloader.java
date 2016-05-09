package viewer;

import java.io.BufferedOutputStream;
import java.io.IOException;

import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cache.boundary.ImageCache;
import storage.boundary.ImageStorage;

@WebServlet(urlPatterns = "/download")
public class ContentDownloader extends HttpServlet {

	private static final long serialVersionUID = 1L;

	ImageStorage storage;

	@EJB
	ImageCache cache;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String id = req.getParameter("id");
		// byte[] content = cache.getOptional(study).orElseGet(() -> {
		// byte[] bytes = storage.getImage(study);
		// cache.putAsync(study, bytes);
		// return bytes;
		// });
		byte[] content = cache.get(id);
		if (content == null) {
			content = storage.getImage(id);
			cache.putAsync(id, content);
		}
		// ?distributedcache
		try (BufferedOutputStream bos = new BufferedOutputStream(resp.getOutputStream())) {
			resp.setHeader("Cache-Control", "private, max-age=100000");
			resp.setHeader("Pragma", "cache");
			resp.setHeader("Content-disposition", "attachment;filename=".concat(id));
			if (content != null) {
				resp.setContentType("text/plain");
				bos.write(content);
			} else {
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
