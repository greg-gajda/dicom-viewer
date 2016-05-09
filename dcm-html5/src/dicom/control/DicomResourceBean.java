package dicom.control;

import java.awt.Point;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Response;

import utils.CacheAccessor;
import cache.boundary.ImageCache;
import cache.control.DistributedCache;
import dicom.boundary.DicomResource;
import dicom.entity.Image;
import mpr.boundary.ObliqueMprBuilder;
import mpr.boundary.OrthoMprBuilder;
import mpr.entity.OutFile;
import storage.boundary.ImageStorage;
import utils.FileUtils;

@Local(DicomResource.class)
@Stateless
public class DicomResourceBean implements DicomResource {

	ImageStorage storage;
	
	@Resource
    ManagedExecutorService executor;
	
	@EJB
	ImageCache cache;
	
	@EJB
	DistributedCache dcache;
	
	@EJB
	DicomBean dicomBean;
	
	//TODO: support for multi users		
	
	@Override
	public Response ping() {
		return Response.ok().build();
	}	
	
	@Override
	public Response studies(String modality, String tag) {
		return Response.ok(dicomBean.filterStudies(modality, tag)).build();
	}

	@Override
	public Response image(String study) {
		return Response.ok(dicomBean.getImageByStudyId(study)).build();
	}

	@Override
	public Response series(String series, String image) {
		if(series == null || series.isEmpty()){
			return Response.ok(dicomBean.getSeriesByImageId(image)).build();
		}else{
			return Response.ok(dicomBean.getSeriesBySeriesId(series)).build();
		}
	}

	@Override
	public Response images(String series) {
		return Response.ok(dicomBean.getImagesBySeriesId(series)).build();
	}

	@Override
	public Response header(String image) {		
		return Response.ok(dicomBean.getImageTags(image)).build();
	}
	
	CacheAccessor getCache(){
		return new CacheAccessor(){
			@Override
			public String tryGet(String image) {
				return DicomResourceBean.this.cache.tryGet(image);
			}							
		};
	}

	@Override
	public void mprAsync(String series, AsyncResponse response) {
		List<Image> list = dicomBean.getImagesByParentSeriesId(series);
		if(list == null || list.isEmpty()){
			//suppose none exists
			ServiceLoader<OrthoMprBuilder> loader = ServiceLoader.load(OrthoMprBuilder.class);
			OrthoMprBuilder builder = loader.iterator().next();
			if(builder != null){		
				builder.setCache(getCache());
				CompletableFuture.supplyAsync(() -> {
	            	return completeImages(series).stream().map(i -> new File(cache.getCacheDir(), i.getSopInstanceUID())).collect(Collectors.toList());
	            }).thenApplyAsync(files -> {
	            	return builder.buildMprForSeries(executor, files, series, cache.getCacheDir(), cache.getCacheDir());
	            }, executor).thenAccept(files -> {
	            	List<Image> images = files.stream().map(f -> dicomBean.getImage(f.getFile().getName(), f.getTags(), true)).collect(Collectors.toList());
	            	response.resume(Response.ok(images).build());	            	
	            });
			}else{
				response.resume(Response.noContent().build());
			} 						
		} else {
			updateCache(list);
			response.resume(Response.ok(list).build());
		}		
	}

	@Override
	public Response mpr(String series) {
		List<Image> list = dicomBean.getImagesByParentSeriesId(series);
		if(list == null || list.isEmpty()){
			//suppose none exists
			ServiceLoader<OrthoMprBuilder> loader = ServiceLoader.load(OrthoMprBuilder.class);
			OrthoMprBuilder builder = loader.iterator().next();
			if(builder != null){
				builder.setCache(getCache());				
				List<File> files = completeImages(series).stream().map(i -> new File(cache.getCacheDir(), i.getSopInstanceUID())).collect(Collectors.toList());				
	            List<Image> mprs = builder.buildMprForSeries(executor, files, series, cache.getCacheDir(), cache.getCacheDir()).stream().map(f -> dicomBean.getImage(f.getFile().getName(), f.getTags(), true)).collect(Collectors.toList());	            
	            return Response.ok(mprs).build();	            		            
			}else{
				return Response.noContent().build();
			} 						
		} else {
			updateCache(list);
			return Response.ok(list).build();
		}		
	}

	//not used
	@Override
	public Response download(String id) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			if(cache.contains(id) == false){
				byte [] content = storage.getImage(id);
				cache.put(id, content);
			}
			FileUtils.transfer(new BufferedInputStream(new FileInputStream(new File(cache.getCacheDir(), id))), baos);
		} catch (FileNotFoundException e) {
			return Response.serverError().build();
		}
		return Response.ok(baos.toString()).build();
	}

	@Override
	public Response oblique(String series, String points) {
		List<Point> list = new ArrayList<>();
		String[] tab = points.split(",");
		for (int i = 0; i < tab.length; i += 2) {
			list.add(new Point(Integer.parseInt(tab[i]), Integer.parseInt(tab[i+1])));
		}
		ServiceLoader<ObliqueMprBuilder> loader = ServiceLoader.load(ObliqueMprBuilder.class);
		ObliqueMprBuilder builder = loader.iterator().next();
		if(builder != null){
			builder.setCache(getCache());		
			List<File> files = completeImages(series).stream().map(i -> new File(cache.getCacheDir(), i.getSopInstanceUID())).collect(Collectors.toList());			
			OutFile of = builder.buildMprForSeries(files, list.toArray(new Point[0]), series, cache.getCacheDir(), cache.getCacheDir());
			//non persistent image
			Image image = dicomBean.getImage(of.getFile().getName(), of.getTags(), false);			
			return Response.ok(image).build();
		}else{
			return Response.noContent().build();
		}				
	}

	List<Image> completeImages(String series){
		List<Image> images = dicomBean.getImagesBySeriesId(series);
		images.stream().filter(i -> !new File(cache.getCacheDir(), i.getSopInstanceUID()).exists()).forEach(i -> {
			byte [] content = storage.getImage(i.getSopInstanceUID());
			cache.put(i.getSopInstanceUID(), content);					
		});
		return images;
	}
	
	void updateCache(List<Image> list){
		list.stream().filter(i -> !new File(cache.getCacheDir(), i.getSopInstanceUID()).exists()).forEach(i -> {
			dcache.getOptional(i.getSopInstanceUID()).ifPresent(content -> cache.put(i.getSopInstanceUID(), content));				
		});				
	}
}
