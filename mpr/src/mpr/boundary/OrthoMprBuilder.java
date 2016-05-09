package mpr.boundary;

import java.io.File;
import java.util.List;
import java.util.concurrent.Executor;

import utils.CacheAccessor;
import mpr.entity.OutFile;

public interface OrthoMprBuilder {
	void setCache(CacheAccessor cache);
	List<OutFile> buildMprForSeries(Executor executor, List<File> images, String series, String cacheDir, String storageDir);
}
