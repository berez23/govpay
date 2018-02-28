package it.govpay.pagamento.api.rs.v1.pagamenti;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.MediaType;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

import it.govpay.rs.v1.controllers.base.PagamentiController;

import it.govpay.rs.v1.BaseRsServiceV1;
import it.govpay.rs.v1.costanti.Costanti;


@Path("/pagamenti")

public class Pagamenti extends BaseRsServiceV1{


	private PagamentiController controller = null;

	public Pagamenti() {
		super("pagamenti");
		this.controller = new PagamentiController(this.nomeServizio,this.log);
	}



    @POST
    @Path("/")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    public Response pagamentiPOST(@Context UriInfo uriInfo, @Context HttpHeaders httpHeaders, java.io.InputStream is, @QueryParam("idSessionePortale") String idSessionePortale){
        this.controller.setRequestResponse(this.request, this.response);
        return this.controller.pagamentiPOST(this.getUser(), uriInfo, httpHeaders, is, idSessionePortale);
    }

    @GET
    @Path("/{id}")
    
    @Produces({ "application/json" })
    public Response pagamentiIdGET(@Context UriInfo uriInfo, @Context HttpHeaders httpHeaders, @PathParam("id") String id){
        this.controller.setRequestResponse(this.request, this.response);
        return this.controller.pagamentiIdGET(this.getUser(), uriInfo, httpHeaders,  id);
    }

    @GET
    @Path("/")
    
    @Produces({ "application/json" })
    public Response pagamentiGET(@Context UriInfo uriInfo, @Context HttpHeaders httpHeaders, @QueryParam(value=Costanti.PARAMETRO_PAGINA) @DefaultValue(value="1") Integer pagina, @QueryParam(value=Costanti.PARAMETRO_RISULTATI_PER_PAGINA) @DefaultValue(value="25") Integer risultatiPerPagina, @QueryParam("ordinamento") String ordinamento, @QueryParam("campi") String campi, @QueryParam("stato") String stato, @QueryParam("versante") String versante, @QueryParam("idSessionePortale") String idSessionePortale){
        this.controller.setRequestResponse(this.request, this.response);
        return this.controller.pagamentiGET(this.getUser(), uriInfo, httpHeaders, pagina, risultatiPerPagina, ordinamento, campi, stato, versante, idSessionePortale);
    }

}


