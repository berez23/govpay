package it.govpay.pagamento.v1.controller;

import java.io.ByteArrayOutputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.springframework.security.core.Authentication;

import eu.medsea.mimeutil.MimeUtil;
import it.govpay.core.dao.anagrafica.DominiDAO;
import it.govpay.core.dao.anagrafica.dto.FindDominiDTO;
import it.govpay.core.dao.anagrafica.dto.FindDominiDTOResponse;
import it.govpay.core.dao.anagrafica.dto.FindIbanDTO;
import it.govpay.core.dao.anagrafica.dto.FindIbanDTOResponse;
import it.govpay.core.dao.anagrafica.dto.FindTributiDTO;
import it.govpay.core.dao.anagrafica.dto.FindTributiDTOResponse;
import it.govpay.core.dao.anagrafica.dto.FindUnitaOperativeDTO;
import it.govpay.core.dao.anagrafica.dto.FindUnitaOperativeDTOResponse;
import it.govpay.core.dao.anagrafica.dto.GetDominioDTO;
import it.govpay.core.dao.anagrafica.dto.GetDominioDTOResponse;
import it.govpay.core.dao.anagrafica.dto.GetIbanDTO;
import it.govpay.core.dao.anagrafica.dto.GetIbanDTOResponse;
import it.govpay.core.dao.anagrafica.dto.GetTributoDTO;
import it.govpay.core.dao.anagrafica.dto.GetTributoDTOResponse;
import it.govpay.core.dao.anagrafica.dto.GetUnitaOperativaDTO;
import it.govpay.core.dao.anagrafica.dto.GetUnitaOperativaDTOResponse;
import it.govpay.core.utils.GovpayConfig;
import org.openspcoop2.utils.service.context.IContext;
import it.govpay.core.utils.GpThreadLocal;
import it.govpay.core.utils.validator.ValidatoreIdentificativi;
import it.govpay.pagamento.v1.beans.ContiAccredito;
import it.govpay.pagamento.v1.beans.Entrata;
import it.govpay.pagamento.v1.beans.ListaDominiIndex;
import it.govpay.pagamento.v1.beans.ListaEntrate;
import it.govpay.pagamento.v1.beans.ListaIbanAccredito;
import it.govpay.pagamento.v1.beans.ListaUnitaOperative;
import it.govpay.pagamento.v1.beans.UnitaOperativa;
import it.govpay.pagamento.v1.beans.converter.DominiConverter;



public class DominiController extends BaseController {

     public DominiController(String nomeServizio,Logger log) {
		super(nomeServizio,log, GovpayConfig.GOVPAY_PAGAMENTI_OPEN_API_FILE_NAME);
     }



    public Response dominiIdDominioContiAccreditoIbanGET(Authentication user, UriInfo uriInfo, HttpHeaders httpHeaders , String idDominio, String ibanAccredito) {
    	String methodName = "dominiIdDominioIbanAccreditoIbanAccreditoGET";  
		IContext ctx = null;
		String transactionId = null;
		ByteArrayOutputStream baos= null;
		this.log.info(MessageFormat.format(BaseController.LOG_MSG_ESECUZIONE_METODO_IN_CORSO, methodName)); 
		try{
			baos = new ByteArrayOutputStream();
			this.logRequest(uriInfo, httpHeaders, methodName, baos);
			
			ctx =  GpThreadLocal.get();
			transactionId = ctx.getTransactionId();
			
			ValidatoreIdentificativi validatoreId = ValidatoreIdentificativi.newInstance();
			validatoreId.validaIdDominio("idDominio", idDominio);
			validatoreId.validaIdIbanAccredito("ibanAccredito", ibanAccredito);
			
			// Parametri - > DTO Input
			
			GetIbanDTO getIbanDTO = new GetIbanDTO(user, idDominio, ibanAccredito);
			
			// INIT DAO
			
			DominiDAO dominiDAO = new DominiDAO();
			
			// CHIAMATA AL DAO
			
			GetIbanDTOResponse getDominiIbanDTOResponse = dominiDAO.getIban(getIbanDTO);
			
			// CONVERT TO JSON DELLA RISPOSTA
			
			ContiAccredito response = DominiConverter.toIbanRsModel(getDominiIbanDTOResponse.getIbanAccredito());
			
			this.logResponse(uriInfo, httpHeaders, methodName, response.toJSON(null), 200);
			this.log.info(MessageFormat.format(BaseController.LOG_MSG_ESECUZIONE_METODO_COMPLETATA, methodName)); 
			return this.handleResponseOk(Response.status(Status.OK).entity(response.toJSON(null)),transactionId).build();
			
		}catch (Exception e) {
			return this.handleException(uriInfo, httpHeaders, methodName, e, transactionId);
		} finally {
			this.log(ctx);
		}
    }


    public Response dominiIdDominioLogoGET(Authentication user, UriInfo uriInfo, HttpHeaders httpHeaders, String idDominio) {
    	String methodName = "getLogo";  
		IContext ctx = null;
		String transactionId = null;
		ByteArrayOutputStream baos= null;
		this.log.info(MessageFormat.format(BaseController.LOG_MSG_ESECUZIONE_METODO_IN_CORSO, methodName)); 
		try{
			baos = new ByteArrayOutputStream();
			this.logRequest(uriInfo, httpHeaders, methodName, baos);
			
			ctx =  GpThreadLocal.get();
			transactionId = ctx.getTransactionId();
			
			ValidatoreIdentificativi validatoreId = ValidatoreIdentificativi.newInstance();
			validatoreId.validaIdDominio("idDominio", idDominio);
			
			// Parametri - > DTO Input
			
			GetDominioDTO getDominioDTO = new GetDominioDTO(user, idDominio);

			// INIT DAO
			
			DominiDAO dominiDAO = new DominiDAO();
			
			// CHIAMATA AL DAO
			
			byte[] logo = dominiDAO.getLogo(getDominioDTO);
			
			MimeUtil.registerMimeDetector(eu.medsea.mimeutil.detector.MagicMimeMimeDetector.class.getName());
			
			Collection<?> mimeTypes = MimeUtil.getMimeTypes(logo);
			
			String mimeType = MimeUtil.getFirstMimeType(mimeTypes.toString()).toString();

			this.logResponse(uriInfo, httpHeaders, methodName, logo, 200);
			this.log.info(MessageFormat.format(BaseController.LOG_MSG_ESECUZIONE_METODO_COMPLETATA, methodName)); 
			ResponseBuilder entity = Response.status(Status.OK).entity(logo);
			entity.header("CacheControl", "max-age: "+ GovpayConfig.getInstance().getCacheLogo().intValue());
			entity.header("Content-Type", mimeType);
			return this.handleResponseOk(entity,transactionId).build();
			
		}catch (Exception e) {
			return this.handleException(uriInfo, httpHeaders, methodName, e, transactionId);
		} finally {
			this.log(ctx);
		}
    }


    public Response dominiIdDominioUnitaOperativeIdUnitaOperativaGET(Authentication user, UriInfo uriInfo, HttpHeaders httpHeaders , String idDominio, String idUnitaOperativa) {
    	String methodName = "dominiIdDominioUnitaOperativeIdUnitaOperativaGET";  
		IContext ctx = null;
		String transactionId = null;
		ByteArrayOutputStream baos= null;
		this.log.info(MessageFormat.format(BaseController.LOG_MSG_ESECUZIONE_METODO_IN_CORSO, methodName)); 
		try{
			baos = new ByteArrayOutputStream();
			this.logRequest(uriInfo, httpHeaders, methodName, baos);
			
			ctx =  GpThreadLocal.get();
			transactionId = ctx.getTransactionId();
			
			ValidatoreIdentificativi validatoreId = ValidatoreIdentificativi.newInstance();
			validatoreId.validaIdDominio("idDominio", idDominio);
			validatoreId.validaIdUO("idUnitaOperativa", idUnitaOperativa);
			
			// Parametri - > DTO Input
			
			GetUnitaOperativaDTO getDominioUoDTO = new GetUnitaOperativaDTO(user, idDominio, idUnitaOperativa);
			
			// INIT DAO
			
			DominiDAO dominiDAO = new DominiDAO();
			
			// CHIAMATA AL DAO
			
			GetUnitaOperativaDTOResponse listaDominiUoDTOResponse = dominiDAO.getUnitaOperativa(getDominioUoDTO);
			
			// CONVERT TO JSON DELLA RISPOSTA
			
			UnitaOperativa response = DominiConverter.toUnitaOperativaRsModel(listaDominiUoDTOResponse.getUnitaOperativa());
			
			this.logResponse(uriInfo, httpHeaders, methodName, response.toJSON(null), 200);
			this.log.info(MessageFormat.format(BaseController.LOG_MSG_ESECUZIONE_METODO_COMPLETATA, methodName)); 
			return this.handleResponseOk(Response.status(Status.OK).entity(response.toJSON(null)),transactionId).build();
			
		}catch (Exception e) {
			return this.handleException(uriInfo, httpHeaders, methodName, e, transactionId);
		} finally {
			this.log(ctx);
		}
    }



    public Response dominiGET(Authentication user, UriInfo uriInfo, HttpHeaders httpHeaders , Integer pagina, Integer risultatiPerPagina, String ordinamento, String campi, Boolean abilitato, String idStazione) {
    	String methodName = "dominiGET";  
		IContext ctx = null;
		String transactionId = null;
		ByteArrayOutputStream baos= null;
		this.log.info(MessageFormat.format(BaseController.LOG_MSG_ESECUZIONE_METODO_IN_CORSO, methodName)); 
		try{
			baos = new ByteArrayOutputStream();
			this.logRequest(uriInfo, httpHeaders, methodName, baos);
			
			ctx =  GpThreadLocal.get();
			transactionId = ctx.getTransactionId();
			
			// Parametri - > DTO Input
			
			FindDominiDTO listaDominiDTO = new FindDominiDTO(user);
			
			listaDominiDTO.setPagina(pagina);
			listaDominiDTO.setLimit(risultatiPerPagina);
			listaDominiDTO.setOrderBy(ordinamento);
			listaDominiDTO.setAbilitato(abilitato);
			listaDominiDTO.setCodStazione(idStazione);
			
			// INIT DAO
			
			DominiDAO dominiDAO = new DominiDAO();
			
			// CHIAMATA AL DAO
			
			FindDominiDTOResponse listaDominiDTOResponse = dominiDAO.findDomini(listaDominiDTO);
			
			// CONVERT TO JSON DELLA RISPOSTA
			
			List<it.govpay.pagamento.v1.beans.DominioIndex> results = new ArrayList<>();
			for(it.govpay.bd.model.Dominio dominio: listaDominiDTOResponse.getResults()) {
				results.add(DominiConverter.toRsModelIndex(dominio));
			}
			
			ListaDominiIndex response = new ListaDominiIndex(results, this.getServicePath(uriInfo),
					listaDominiDTOResponse.getTotalResults(), pagina, risultatiPerPagina);
			
			this.logResponse(uriInfo, httpHeaders, methodName, response.toJSON(campi), 200);
			this.log.info(MessageFormat.format(BaseController.LOG_MSG_ESECUZIONE_METODO_COMPLETATA, methodName)); 
			return this.handleResponseOk(Response.status(Status.OK).entity(response.toJSON(campi)),transactionId).build();
			
		}catch (Exception e) {
			return this.handleException(uriInfo, httpHeaders, methodName, e, transactionId);
		} finally {
			this.log(ctx);
		}

    }



    public Response dominiIdDominioGET(Authentication user, UriInfo uriInfo, HttpHeaders httpHeaders , String idDominio) {
    	String methodName = "dominiIdDominioGET";  
		IContext ctx = null;
		String transactionId = null;
		ByteArrayOutputStream baos= null;
		this.log.info(MessageFormat.format(BaseController.LOG_MSG_ESECUZIONE_METODO_IN_CORSO, methodName)); 
		try{
			baos = new ByteArrayOutputStream();
			this.logRequest(uriInfo, httpHeaders, methodName, baos);
			
			ctx =  GpThreadLocal.get();
			transactionId = ctx.getTransactionId();
			
			ValidatoreIdentificativi validatoreId = ValidatoreIdentificativi.newInstance();
			validatoreId.validaIdDominio("idDominio", idDominio);
			
			// Parametri - > DTO Input
			
			GetDominioDTO getDominioDTO = new GetDominioDTO(user, idDominio);

			// INIT DAO
			
			DominiDAO dominiDAO = new DominiDAO();
			
			// CHIAMATA AL DAO
			
			GetDominioDTOResponse listaDominiDTOResponse = dominiDAO.getDominio(getDominioDTO);
			
			// CONVERT TO JSON DELLA RISPOSTA
			
			it.govpay.pagamento.v1.beans.Dominio response = DominiConverter.toRsModel(listaDominiDTOResponse.getDominio(), listaDominiDTOResponse.getUo(), listaDominiDTOResponse.getTributi(), listaDominiDTOResponse.getIban());
			
			this.logResponse(uriInfo, httpHeaders, methodName, response.toJSON(null), 200);
			this.log.info(MessageFormat.format(BaseController.LOG_MSG_ESECUZIONE_METODO_COMPLETATA, methodName)); 
			return this.handleResponseOk(Response.status(Status.OK).entity(response.toJSON(null)),transactionId).build();
			
		}catch (Exception e) {
			return this.handleException(uriInfo, httpHeaders, methodName, e, transactionId);
		} finally {
			this.log(ctx);
		}
    }

    public Response dominiIdDominioEntrateGET(Authentication user, UriInfo uriInfo, HttpHeaders httpHeaders , String idDominio, Integer pagina, Integer risultatiPerPagina, String ordinamento, String campi, Boolean abilitato) {
    	String methodName = "dominiIdDominioEntrateGET";  
		IContext ctx = null;
		String transactionId = null;
		ByteArrayOutputStream baos= null;
		this.log.info(MessageFormat.format(BaseController.LOG_MSG_ESECUZIONE_METODO_IN_CORSO, methodName)); 
		try{
			baos = new ByteArrayOutputStream();
			this.logRequest(uriInfo, httpHeaders, methodName, baos);
			
			ctx =  GpThreadLocal.get();
			transactionId = ctx.getTransactionId();
			
			ValidatoreIdentificativi validatoreId = ValidatoreIdentificativi.newInstance();
			validatoreId.validaIdDominio("idDominio", idDominio);
			
			// Parametri - > DTO Input
			
			FindTributiDTO listaDominiEntrateDTO = new FindTributiDTO(user, idDominio);
			
			listaDominiEntrateDTO.setPagina(pagina);
			listaDominiEntrateDTO.setLimit(risultatiPerPagina);
			listaDominiEntrateDTO.setOrderBy(ordinamento);
			listaDominiEntrateDTO.setAbilitato(abilitato);
			
			// INIT DAO
			
			DominiDAO dominiDAO = new DominiDAO();
			
			// CHIAMATA AL DAO
			
			FindTributiDTOResponse listaDominiEntrateDTOResponse = dominiDAO.findTributi(listaDominiEntrateDTO);
			
			// CONVERT TO JSON DELLA RISPOSTA
			
			List<it.govpay.pagamento.v1.beans.Entrata> results = new ArrayList<>();
			for(GetTributoDTOResponse tributo: listaDominiEntrateDTOResponse.getResults()) {
				results.add(DominiConverter.toEntrataRsModel(tributo.getTributo(), tributo.getIbanAppoggio()));
			}
			
			ListaEntrate response = new ListaEntrate(results, this.getServicePath(uriInfo),
					listaDominiEntrateDTOResponse.getTotalResults(), pagina, risultatiPerPagina);
			
			this.logResponse(uriInfo, httpHeaders, methodName, response.toJSON(campi), 200);
			this.log.info(MessageFormat.format(BaseController.LOG_MSG_ESECUZIONE_METODO_COMPLETATA, methodName)); 
			return this.handleResponseOk(Response.status(Status.OK).entity(response.toJSON(campi)),transactionId).build();
			
		}catch (Exception e) {
			return this.handleException(uriInfo, httpHeaders, methodName, e, transactionId);
		} finally {
			this.log(ctx);
		}
    }



    public Response dominiIdDominioUnitaOperativeGET(Authentication user, UriInfo uriInfo, HttpHeaders httpHeaders , String idDominio, Integer pagina, Integer risultatiPerPagina, String ordinamento, String campi, Boolean abilitato) {    	
    	String methodName = "dominiIdDominioUnitaOperativeGET";  
		IContext ctx = null;
		String transactionId = null;
		ByteArrayOutputStream baos= null;
		this.log.info(MessageFormat.format(BaseController.LOG_MSG_ESECUZIONE_METODO_IN_CORSO, methodName)); 
		try{
			baos = new ByteArrayOutputStream();
			this.logRequest(uriInfo, httpHeaders, methodName, baos);
			
			ctx =  GpThreadLocal.get();
			transactionId = ctx.getTransactionId();
			
			ValidatoreIdentificativi validatoreId = ValidatoreIdentificativi.newInstance();
			validatoreId.validaIdDominio("idDominio", idDominio);
			
			// Parametri - > DTO Input
			
			FindUnitaOperativeDTO listaDominiUoDTO = new FindUnitaOperativeDTO(user, idDominio);
			
			listaDominiUoDTO.setPagina(pagina);
			listaDominiUoDTO.setLimit(risultatiPerPagina);
			listaDominiUoDTO.setOrderBy(ordinamento);
			listaDominiUoDTO.setAbilitato(abilitato);
			
			// INIT DAO
			
			DominiDAO dominiDAO = new DominiDAO();
			
			// CHIAMATA AL DAO
			
			FindUnitaOperativeDTOResponse listaDominiUoDTOResponse = dominiDAO.findUnitaOperative(listaDominiUoDTO);
			
			// CONVERT TO JSON DELLA RISPOSTA
			
			List<it.govpay.pagamento.v1.beans.UnitaOperativa> results = new ArrayList<>();
			for(it.govpay.bd.model.UnitaOperativa uo: listaDominiUoDTOResponse.getResults()) {
				results.add(DominiConverter.toUnitaOperativaRsModel(uo));
			}
			
			ListaUnitaOperative response = new ListaUnitaOperative(results, this.getServicePath(uriInfo),
					listaDominiUoDTOResponse.getTotalResults(), pagina, risultatiPerPagina);
			
			this.logResponse(uriInfo, httpHeaders, methodName, response.toJSON(campi), 200);
			this.log.info(MessageFormat.format(BaseController.LOG_MSG_ESECUZIONE_METODO_COMPLETATA, methodName)); 
			return this.handleResponseOk(Response.status(Status.OK).entity(response.toJSON(campi)),transactionId).build();
			
		}catch (Exception e) {
			return this.handleException(uriInfo, httpHeaders, methodName, e, transactionId);
		} finally {
			this.log(ctx);
		}
    }



    public Response dominiIdDominioEntrateIdEntrataGET(Authentication user, UriInfo uriInfo, HttpHeaders httpHeaders , String idDominio, String idEntrata) {
    	String methodName = "dominiIdDominioEntrateIdEntrataGET";  
		IContext ctx = null;
		String transactionId = null;
		ByteArrayOutputStream baos= null;
		this.log.info(MessageFormat.format(BaseController.LOG_MSG_ESECUZIONE_METODO_IN_CORSO, methodName)); 
		try{
			baos = new ByteArrayOutputStream();
			this.logRequest(uriInfo, httpHeaders, methodName, baos);
			
			ctx =  GpThreadLocal.get();
			transactionId = ctx.getTransactionId();
			
			ValidatoreIdentificativi validatoreId = ValidatoreIdentificativi.newInstance();
			validatoreId.validaIdDominio("idDominio", idDominio);
			validatoreId.validaIdEntrata("idEntrata", idEntrata);
			
			// Parametri - > DTO Input
			
			GetTributoDTO getDominioEntrataDTO = new GetTributoDTO(user, idDominio, idEntrata);
			
			// INIT DAO
			
			DominiDAO dominiDAO = new DominiDAO();
			
			// CHIAMATA AL DAO
			
			GetTributoDTOResponse listaDominiEntrateDTOResponse = dominiDAO.getTributo(getDominioEntrataDTO);
			
			// CONVERT TO JSON DELLA RISPOSTA
			
			Entrata response = DominiConverter.toEntrataRsModel(listaDominiEntrateDTOResponse.getTributo(), listaDominiEntrateDTOResponse.getIbanAppoggio());
			
			this.logResponse(uriInfo, httpHeaders, methodName, response.toJSON(null), 200);
			this.log.info(MessageFormat.format(BaseController.LOG_MSG_ESECUZIONE_METODO_COMPLETATA, methodName)); 
			return this.handleResponseOk(Response.status(Status.OK).entity(response.toJSON(null)),transactionId).build();
			
		}catch (Exception e) {
			return this.handleException(uriInfo, httpHeaders, methodName, e, transactionId);
		} finally {
			this.log(ctx);
		}
    }

    public Response dominiIdDominioContiAccreditoGET(Authentication user, UriInfo uriInfo, HttpHeaders httpHeaders , String idDominio, Integer pagina, Integer risultatiPerPagina, String campi, Boolean abilitato, String ordinamento) {
    	String methodName = "dominiIdDominioIbanAccreditoGET";  
		IContext ctx = null;
		String transactionId = null;
		ByteArrayOutputStream baos= null;
		this.log.info(MessageFormat.format(BaseController.LOG_MSG_ESECUZIONE_METODO_IN_CORSO, methodName)); 
		try{
			baos = new ByteArrayOutputStream();
			this.logRequest(uriInfo, httpHeaders, methodName, baos);
			
			ctx =  GpThreadLocal.get();
			transactionId = ctx.getTransactionId();
			
			ValidatoreIdentificativi validatoreId = ValidatoreIdentificativi.newInstance();
			validatoreId.validaIdDominio("idDominio", idDominio);
			
			// Parametri - > DTO Input
			
			FindIbanDTO listaDominiIbanDTO = new FindIbanDTO(user, idDominio);
			
			listaDominiIbanDTO.setPagina(pagina);
			listaDominiIbanDTO.setLimit(risultatiPerPagina);
			listaDominiIbanDTO.setOrderBy(ordinamento);
			listaDominiIbanDTO.setAbilitato(abilitato);
			
			// INIT DAO
			
			DominiDAO dominiDAO = new DominiDAO();
			
			// CHIAMATA AL DAO
			
			FindIbanDTOResponse listaDominiIbanDTOResponse = dominiDAO.findIban(listaDominiIbanDTO);
			
			// CONVERT TO JSON DELLA RISPOSTA
			
			List<it.govpay.pagamento.v1.beans.ContiAccredito> results = new ArrayList<>();
			for(it.govpay.bd.model.IbanAccredito ibanAccredito: listaDominiIbanDTOResponse.getResults()) {
				results.add(DominiConverter.toIbanRsModel(ibanAccredito));
			}
			
			ListaIbanAccredito response = new ListaIbanAccredito(results, this.getServicePath(uriInfo),
					listaDominiIbanDTOResponse.getTotalResults(), pagina, risultatiPerPagina);
			
			this.logResponse(uriInfo, httpHeaders, methodName, response.toJSON(campi), 200);
			this.log.info(MessageFormat.format(BaseController.LOG_MSG_ESECUZIONE_METODO_COMPLETATA, methodName)); 
			return this.handleResponseOk(Response.status(Status.OK).entity(response.toJSON(campi)),transactionId).build();
			
		}catch (Exception e) {
			return this.handleException(uriInfo, httpHeaders, methodName, e, transactionId);
		} finally {
			this.log(ctx);
		}
    }


}

