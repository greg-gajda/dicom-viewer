package cache.boundary;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

@Path("/cache")
public interface CacheResource {

	byte[] get(String image);
	
	@Path("{image}")
	@GET
	default Response getImage(@PathParam("image") String image){
		byte [] content = get(image);
		return content == null ? Response.noContent().build() : Response.ok(content).build();
	}

}
