package mpr.control;

import java.awt.Point;
import java.io.File;
import java.util.List;
import java.util.concurrent.Executor;

import utils.CacheAccessor;
import mpr.boundary.ObliqueMprBuilder;
import mpr.boundary.OrthoMprBuilder;
import mpr.entity.OutFile;

public class DefaultMprBuilder implements ObliqueMprBuilder, OrthoMprBuilder {

	@Override
	public void setCache(CacheAccessor cache) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<OutFile> buildMprForSeries(Executor executor, List<File> images, String series, String cacheDir,
			String storageDir) {
		throw new UnsupportedOperationException();
	}

	@Override
	public OutFile buildMprForSeries(List<File> images, Point[] points, String seriesInstanceUID, String cacheDir,
			String storageDir) {
		throw new UnsupportedOperationException();
	}

}
