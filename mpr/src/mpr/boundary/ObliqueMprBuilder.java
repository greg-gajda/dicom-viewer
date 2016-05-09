package mpr.boundary;

import java.awt.Point;
import java.io.File;
import java.util.List;

import utils.CacheAccessor;
import mpr.entity.OutFile;

public interface ObliqueMprBuilder {
	void setCache(CacheAccessor cache);
	OutFile buildMprForSeries(List<File> images, Point[] points, String seriesInstanceUID, String cacheDir, String storageDir);
}
