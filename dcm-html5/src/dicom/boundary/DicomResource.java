package dicom.boundary;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/dicom")
@Produces({ MediaType.APPLICATION_JSON })
@Consumes({ MediaType.APPLICATION_JSON })
public interface DicomResource {

	@Path("/ping")
	@GET
	Response ping();

	@Path("/studies")
	@GET
	Response studies(@DefaultValue("ALL") @QueryParam("modality") String modality, @QueryParam("tag") String tag);

	@Path("/series")
	@GET
	Response series(@QueryParam("series") String series, @QueryParam("image") String image);

	@Path("/image")
	@GET
	Response image(@QueryParam("study") String study);
	
	@Path("/images")
	@GET
	Response images(@QueryParam("series") String series);

	@Path("/header")
	@GET
	Response header(@QueryParam("image") String image);
	
	@Path("/download")
	@GET
	Response download(@QueryParam("id") String id);

	@Path("/mpr")
	@GET
	Response mpr(@QueryParam("series") String series);

	@Path("/mprAsync")
	@GET
	void mprAsync(@QueryParam("series") String series, @Suspended AsyncResponse response);

	@Path("/oblique")
	@GET
	Response oblique(@QueryParam("series") String series, @QueryParam("points") String points);

}
