package it.govpay.core.dao.pagamenti;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.openspcoop2.generic_project.exception.NotFoundException;
import org.openspcoop2.generic_project.exception.ServiceException;
import org.openspcoop2.utils.logger.beans.Property;

import it.govpay.bd.BasicBD;
import it.govpay.bd.anagrafica.AnagraficaManager;
import it.govpay.bd.model.Applicazione;
import it.govpay.bd.model.Canale;
import it.govpay.bd.model.Dominio;
import it.govpay.bd.model.PagamentoPortale;
import it.govpay.bd.model.PagamentoPortale.CODICE_STATO;
import it.govpay.bd.model.PagamentoPortale.STATO;
import it.govpay.bd.model.Rpt;
import it.govpay.bd.model.SingoloVersamento;
import it.govpay.bd.model.UnitaOperativa;
import it.govpay.bd.model.Versamento;
import it.govpay.bd.pagamento.PagamentiPortaleBD;
import it.govpay.bd.pagamento.filters.PagamentoPortaleFilter;
import it.govpay.core.business.Wisp;
import it.govpay.core.business.model.SceltaWISP;
import it.govpay.core.dao.commons.BaseDAO;
import it.govpay.core.dao.pagamenti.dto.LeggiPagamentoPortaleDTO;
import it.govpay.core.dao.pagamenti.dto.LeggiPagamentoPortaleDTOResponse;
import it.govpay.core.dao.pagamenti.dto.ListaPagamentiPortaleDTO;
import it.govpay.core.dao.pagamenti.dto.ListaPagamentiPortaleDTOResponse;
import it.govpay.core.dao.pagamenti.dto.PagamentiPortaleDTO;
import it.govpay.core.dao.pagamenti.dto.PagamentiPortaleDTO.RefVersamentoAvviso;
import it.govpay.core.dao.pagamenti.dto.PagamentiPortaleDTO.RefVersamentoPendenza;
import it.govpay.core.dao.pagamenti.dto.PagamentiPortaleDTOResponse;
import it.govpay.core.dao.pagamenti.exception.PagamentoPortaleNonTrovatoException;
import it.govpay.core.exceptions.GovPayException;
import it.govpay.core.exceptions.NotAuthenticatedException;
import it.govpay.core.exceptions.NotAuthorizedException;
import it.govpay.core.utils.GovpayConfig;
import it.govpay.core.utils.GpContext;
import it.govpay.core.utils.GpThreadLocal;
import it.govpay.core.utils.UrlUtils;
import it.govpay.core.utils.VersamentoUtils;
import it.govpay.core.utils.WISPUtils;
import it.govpay.model.Acl.Diritti;
import it.govpay.model.Acl.Servizio;
import it.govpay.model.Anagrafica;
import it.govpay.model.Canale.TipoVersamento;
import it.govpay.model.IbanAccredito;
import it.govpay.orm.IdVersamento;
import it.govpay.servizi.commons.EsitoOperazione;

public class PagamentiPortaleDAO extends BaseDAO {

	private static final String ACTION_BACK = "back";
	private static final String ACTION_RETURN = "return";

	public PagamentiPortaleDAO() {
	}

	public PagamentiPortaleDTOResponse inserisciPagamenti(PagamentiPortaleDTO pagamentiPortaleDTO) throws GovPayException, NotAuthorizedException, ServiceException {
		PagamentiPortaleDTOResponse response  = new PagamentiPortaleDTOResponse();
		GpContext ctx = GpThreadLocal.get();
		BasicBD bd = null;

		try {
			bd = BasicBD.newInstance(ctx.getTransactionId());
			List<Versamento> versamenti = new ArrayList<Versamento>();

			// Aggiungo il codSessionePortale al PaymentContext
			ctx.getPagamentoCtx().setCodSessionePortale(pagamentiPortaleDTO.getIdSessionePortale());
			ctx.getContext().getRequest().addGenericProperty(new Property("codSessionePortale", pagamentiPortaleDTO.getIdSessionePortale() != null ? pagamentiPortaleDTO.getIdSessionePortale() : "--Non fornito--"));

			it.govpay.core.business.Applicazione applicazioneBusiness = new it.govpay.core.business.Applicazione(bd);
			Applicazione applicazioneAutenticata = applicazioneBusiness.getApplicazioneAutenticata(pagamentiPortaleDTO.getUser().getPrincipal());
			ctx.log("ws.ricevutaRichiesta");
			String codApplicazione = applicazioneAutenticata.getCodApplicazione();

			applicazioneBusiness.autorizzaApplicazione(codApplicazione, applicazioneAutenticata, bd);
			ctx.log("ws.autorizzazione");

			String codDominio = null;
			String enteCreditore = null;
			String nome = null;
			List<IdVersamento> idVersamento = new ArrayList<IdVersamento>();
			it.govpay.core.business.Versamento versamentoBusiness = new it.govpay.core.business.Versamento(bd);
			StringBuilder sbNomeVersamenti = new StringBuilder();
			// 1. Lista Id_versamento
			for(int i = 0; i < pagamentiPortaleDTO.getPendenzeOrPendenzeRef().size(); i++) {
				Object v = pagamentiPortaleDTO.getPendenzeOrPendenzeRef().get(i);
				Versamento versamentoModel = null;
				if(v instanceof it.govpay.core.dao.commons.Versamento) {
					it.govpay.core.dao.commons.Versamento versamento = (it.govpay.core.dao.commons.Versamento) v;
					ctx.log("rpt.acquisizioneVersamento", versamento.getCodApplicazione(), versamento.getCodVersamentoEnte());
					versamentoModel = versamentoBusiness.chiediVersamento(versamento);
				}  else if(v instanceof RefVersamentoAvviso) {
					versamentoModel = versamentoBusiness.chiediVersamento((RefVersamentoAvviso)v);
				}  else if(v instanceof RefVersamentoPendenza) {
					versamentoModel = versamentoBusiness.chiediVersamento((RefVersamentoPendenza)v);
				}

				UnitaOperativa uo = versamentoModel.getUo(bd);
				if(!uo.isAbilitato()) {
					throw new GovPayException("Il pagamento non puo' essere avviato poiche' uno dei versamenti risulta associato ad una unita' operativa disabilitata [Uo:"+uo.getCodUo()+"].", EsitoOperazione.UOP_001, uo.getCodUo());
				}

				Dominio dominio = uo.getDominio(bd); 
				if(!dominio.isAbilitato()) {
					throw new GovPayException("Il pagamento non puo' essere avviato poiche' uno dei versamenti risulta associato ad un dominio disabilitato [Dominio:"+dominio.getCodDominio()+"].", EsitoOperazione.DOM_001, dominio.getCodDominio());
				}

				if(versamentoModel.getId() != null) {
					IdVersamento idV = new IdVersamento();
					idV.setCodVersamentoEnte(versamentoModel.getCodVersamentoEnte());
					idV.setId(versamentoModel.getId());
					idVersamento.add(idV);
				}

				if(i == 0) {
					// 	2. Codice dominio della prima pendenza
					codDominio = dominio.getCodDominio();
					// 3. ente creditore
					enteCreditore = dominio.getRagioneSociale();
				}

				if(sbNomeVersamenti.length() >0)
					sbNomeVersamenti.append("#");

				sbNomeVersamenti.append(versamentoModel.getCodVersamentoEnte());

				versamenti.add(versamentoModel);
			}

			//3. numero dei pagamenti = somma numero singoli versamenti
			int numeroPagamenti = 0;
			// 4. controllo se c'e' almeno un bollo
			boolean hasBollo = false;
			// 5. somma degli importi delle pendenze
			double sommaImporti = 0;
			// 6. IbanAccredito se num_pagamenti = 1 e l'iban indicato nel sv non e' postale
			IbanAccredito ibanAccredito = null;
			// 7. conto postale se tutti gli iban sono postali
			boolean contoPostale = true;

			boolean pagamentiModello2 = pagamentiPortaleDTO.getIbanAddebito() != null;

			for (Versamento vTmp : versamenti) {
				List<SingoloVersamento> singoliVersamenti = vTmp.getSingoliVersamenti(bd);
				numeroPagamenti += singoliVersamenti.size();

				for (SingoloVersamento singoloVersamento : singoliVersamenti) {
					if(singoloVersamento.getTipoBollo()!= null) {
						hasBollo = true;
					}

					IbanAccredito ibanAccreditoTmp = singoloVersamento.getIbanAccredito(bd);

					if(ibanAccreditoTmp != null)
						contoPostale = contoPostale && ibanAccreditoTmp.isPostale();
				}

				sommaImporti += vTmp.getImportoTotale().doubleValue();

				// init incasso
				vTmp.setIncasso(hasBollo ? null : false);
			}

			if(numeroPagamenti == 1) {
				Versamento vTmp = versamenti.get(0);
				List<SingoloVersamento> singoliVersamenti = vTmp.getSingoliVersamenti(bd);
				SingoloVersamento singoloVersamento = singoliVersamenti.get(0);
				IbanAccredito ibanAccreditoTmp = singoloVersamento.getIbanAccredito(bd);
				if(!ibanAccreditoTmp.isPostale())
					ibanAccredito = ibanAccreditoTmp;
			}

			nome = sbNomeVersamenti.length() > 255 ? (sbNomeVersamenti.substring(0, 252) + "...") : sbNomeVersamenti.toString();
			STATO stato = null;
			CODICE_STATO codiceStato = null;
			String redirectUrl = null;
			String idSessionePsp = null;
			String pspRedirect = null;
			String codCanale = null;		
			String codPsp = null;
			String tipoVersamento = null;
			
			PagamentoPortale pagamentoPortale = new PagamentoPortale();
			pagamentoPortale.setIdSessione(pagamentiPortaleDTO.getIdSessione());
			pagamentoPortale.setIdSessionePortale(pagamentiPortaleDTO.getIdSessionePortale());
			pagamentoPortale.setJsonRequest(pagamentiPortaleDTO.getJsonRichiesta());
			pagamentoPortale.setUrlRitorno(UrlUtils.addParameter(pagamentiPortaleDTO.getUrlRitorno() , "idSession",pagamentiPortaleDTO.getIdSessione()));
			pagamentoPortale.setDataRichiesta(new Date());
			pagamentoPortale.setCodApplicazione(codApplicazione);
			pagamentoPortale.setIdVersamento(idVersamento); 
			pagamentoPortale.setWispIdDominio(codDominio);
			pagamentoPortale.setNome(nome);
			pagamentoPortale.setImporto(sommaImporti); 
			
			String idSessione = pagamentoPortale.getIdSessione();
			
			PagamentiPortaleBD pagamentiPortaleBD = new PagamentiPortaleBD(bd);

			// procedo al pagamento
			Anagrafica versanteModel = VersamentoUtils.toAnagraficaModel(pagamentiPortaleDTO.getVersante());
			it.govpay.core.business.Rpt rptBD = new it.govpay.core.business.Rpt(bd);
			List<Rpt> rpts = null;
			switch (pagamentiPortaleDTO.getVersioneInterfacciaWISP()) { 
			case WISP_1_3:
				// gestione dello stato, url_redirect e id_session_psp
				if(pagamentiPortaleDTO.getKeyPA() != null && pagamentiPortaleDTO.getKeyWISP() != null && pagamentiPortaleDTO.getIdDominio() != null) {
					Wisp wisp = new Wisp(bd);
					Dominio dominio =null;
					try {
						dominio = AnagraficaManager.getDominio(bd, pagamentiPortaleDTO.getIdDominio());
					} catch (NotFoundException e) {
						throw new GovPayException("Il pagamento non puo' essere avviato poiche' il dominio utilizzato non esiste [Dominio:"+pagamentiPortaleDTO.getIdDominio()+"].", EsitoOperazione.DOM_000, pagamentiPortaleDTO.getIdDominio());
					}

					SceltaWISP scelta = null;
					try {
						scelta = wisp.chiediScelta(applicazioneAutenticata, dominio, pagamentiPortaleDTO.getKeyPA(), pagamentiPortaleDTO.getKeyWISP());
					} catch (Exception e) {
						ctx.log("pagamento.risoluzioneWispKo", e.getMessage());
						throw new ServiceException(e); 
					}

					if(!scelta.isSceltaEffettuata()) {
						ctx.log("pagamento.risoluzioneWispOkNoScelta");
						throw new GovPayException(EsitoOperazione.WISP_003);
					}
					if(scelta.isPagaDopo()) {
						ctx.log("pagamento.risoluzioneWispOkPagaDopo");
						throw new GovPayException(EsitoOperazione.WISP_004);
					}

					// decodifica di tipo versamento e id psp dal canale
					codCanale = scelta.getCodCanale();
					tipoVersamento = scelta.getTipoVersamento();
					codPsp = scelta.getCodPsp();


					if(scelta.getCodPsp().equals(Rpt.codPspWISP20) && scelta.getCodCanale().equals(Rpt.codCanaleWISP20))
						rpts = rptBD.avviaTransazione(versamenti, applicazioneAutenticata, null, pagamentiPortaleDTO.getIbanAddebito(), versanteModel, pagamentiPortaleDTO.getAutenticazioneSoggetto(), pagamentiPortaleDTO.getUrlRitorno(), false);
					else {
						try {
							Canale canale = AnagraficaManager.getCanale(bd, codPsp, codCanale, TipoVersamento.toEnum(tipoVersamento));
							rpts = rptBD.avviaTransazione(versamenti, applicazioneAutenticata, canale, pagamentiPortaleDTO.getIbanAddebito(), versanteModel, pagamentiPortaleDTO.getAutenticazioneSoggetto(), pagamentiPortaleDTO.getUrlRitorno(), false);
						} catch (NotFoundException e) {
							throw new GovPayException(EsitoOperazione.WISP_002,  codPsp, codCanale, tipoVersamento);
						}
					}
					Rpt rpt = rpts.get(0);

					// se ho un redirect 			
					if(rpt.getPspRedirectURL() != null) {
						codiceStato = CODICE_STATO.PAGAMENTO_IN_CORSO_AL_PSP;
						stato = STATO.IN_CORSO;
						idSessionePsp = rpt.getCodSessione();
						redirectUrl = rpt.getPspRedirectURL();
					} else {
						stato = STATO.IN_CORSO;
						codiceStato = CODICE_STATO.PAGAMENTO_IN_ATTESA_DI_ESITO;
						redirectUrl = UrlUtils.addParameter(pagamentiPortaleDTO.getUrlRitorno() , "idSession",pagamentiPortaleDTO.getIdSessione());
					}

				} else {
					// sessione di pagamento non in corso
					codiceStato = CODICE_STATO.DA_REDIRIGERE_AL_WISP;
					stato = STATO.IN_CORSO;
					redirectUrl = GovpayConfig.getInstance().getUrlGovpayWC() + "/" + pagamentiPortaleDTO.getIdSessione();
					for(Versamento versamento: versamenti) {
						if(versamento.getId() == null) {
							versamentoBusiness.caricaVersamento(versamento, false, true);
							IdVersamento idV = new IdVersamento();
							idV.setCodVersamentoEnte(versamento.getCodVersamentoEnte());
							idV.setId(versamento.getId());
							idVersamento.add(idV);

						}
					}
				}
				
				// costruire html
				String template = WISPUtils.readTemplate();
				String urlReturn = UrlUtils.addParameter(GovpayConfig.getInstance().getUrlGovpayWC() + "/" + pagamentoPortale.getIdSessione() , "action" , ACTION_RETURN);
				String urlBack = UrlUtils.addParameter(GovpayConfig.getInstance().getUrlGovpayWC() + "/" + pagamentoPortale.getIdSessione() ,"action" , ACTION_BACK);
				String wispHtml = WISPUtils.getWispHtml(GovpayConfig.getInstance().getUrlWISP(), template, pagamentoPortale, urlReturn, urlBack, enteCreditore, numeroPagamenti, ibanAccredito, contoPostale, hasBollo, sommaImporti,pagamentiModello2,
						pagamentiPortaleDTO.getLingua()); 

				pagamentoPortale.setWispHtml(wispHtml);
				break;
			case WISP_2_0:
				// sessione di pagamento non in corso
				codiceStato = CODICE_STATO.PAGAMENTO_IN_CORSO_AL_PSP;
				stato = STATO.IN_CORSO;
				
				try {
					rpts = rptBD.avviaTransazione(versamenti, applicazioneAutenticata, null, pagamentiPortaleDTO.getIbanAddebito(), versanteModel, pagamentiPortaleDTO.getAutenticazioneSoggetto(), pagamentiPortaleDTO.getUrlRitorno(), false);

					Rpt rpt = rpts.get(0);

					// se ho un redirect 			
					if(rpt.getPspRedirectURL() != null) {
						codiceStato = CODICE_STATO.PAGAMENTO_IN_CORSO_AL_PSP;
						stato = STATO.IN_CORSO;
						idSessionePsp = rpt.getCodSessione();
						redirectUrl = rpt.getPspRedirectURL();
						idSessione = UrlUtils.getCodSessione(redirectUrl);
					} else {
						stato = STATO.IN_CORSO;
						codiceStato = CODICE_STATO.PAGAMENTO_IN_ATTESA_DI_ESITO;
						redirectUrl = UrlUtils.addParameter(pagamentiPortaleDTO.getUrlRitorno() , "idSession",pagamentiPortaleDTO.getIdSessione());
					}
					
					
					
				}catch(GovPayException e) {
					pagamentoPortale.setCodiceStato(CODICE_STATO.PAGAMENTO_FALLITO);
					pagamentoPortale.setStato(STATO.FALLITO);
					pagamentoPortale.setDescrizioneStato(e.getMessage());
					pagamentiPortaleBD.insertPagamento(pagamentoPortale);
					throw e;
				}
				break;
			}

			pagamentoPortale.setIdSessionePsp(idSessionePsp);
			pagamentoPortale.setPspRedirectUrl(pspRedirect);
			pagamentoPortale.setCodiceStato(codiceStato);
			pagamentoPortale.setStato(stato);
			pagamentoPortale.setCodPsp(codPsp);
			pagamentoPortale.setTipoVersamento(tipoVersamento);
			pagamentoPortale.setCodCanale(codCanale); 
			
			pagamentiPortaleBD.insertPagamento(pagamentoPortale);

			response.setRedirectUrl(redirectUrl);
			response.setId(pagamentoPortale.getIdSessione());
			response.setIdSessione(idSessione); 

			return response;
		}finally {
			if(bd != null)
				bd.closeConnection();
		}
	}

	public LeggiPagamentoPortaleDTOResponse leggiPagamentoPortale(LeggiPagamentoPortaleDTO leggiPagamentoPortaleDTO) throws ServiceException,PagamentoPortaleNonTrovatoException, NotAuthorizedException, NotAuthenticatedException{
		LeggiPagamentoPortaleDTOResponse leggiPagamentoPortaleDTOResponse = new LeggiPagamentoPortaleDTOResponse();
		BasicBD bd = null;

		try {
			bd = BasicBD.newInstance(GpThreadLocal.get().getTransactionId());
			this.autorizzaRichiesta(leggiPagamentoPortaleDTO.getUser(), Servizio.PAGAMENTI_E_PENDENZE, Diritti.LETTURA,bd);

			PagamentiPortaleBD pagamentiPortaleBD = new PagamentiPortaleBD(bd);
			PagamentoPortale pagamentoPortale = pagamentiPortaleBD.getPagamentoFromCodSessione(leggiPagamentoPortaleDTO.getIdSessione());
			leggiPagamentoPortaleDTOResponse.setPagamento(pagamentoPortale); 

			return leggiPagamentoPortaleDTOResponse;
		}catch(NotFoundException e) {
			throw new PagamentoPortaleNonTrovatoException("Non esiste un pagamento associato all'ID ["+leggiPagamentoPortaleDTO.getIdSessione()+"]");
		}finally {
			if(bd != null)
				bd.closeConnection();
		}
	}

	public ListaPagamentiPortaleDTOResponse listaPagamentiPortale(ListaPagamentiPortaleDTO listaPagamentiPortaleDTO) throws ServiceException, NotAuthorizedException, NotAuthenticatedException{
		BasicBD bd = null;

		try {
			bd = BasicBD.newInstance(GpThreadLocal.get().getTransactionId());
			this.autorizzaRichiesta(listaPagamentiPortaleDTO.getUser(), Servizio.PAGAMENTI_E_PENDENZE, Diritti.LETTURA,bd);

			PagamentiPortaleBD pagamentiPortaleBD = new PagamentiPortaleBD(bd);
			PagamentoPortaleFilter filter = pagamentiPortaleBD.newFilter();

			filter.setOffset(listaPagamentiPortaleDTO.getOffset());
			filter.setLimit(listaPagamentiPortaleDTO.getLimit());
			filter.setDataInizio(listaPagamentiPortaleDTO.getDataDa());
			filter.setDataFine(listaPagamentiPortaleDTO.getDataA());
			filter.setStato(listaPagamentiPortaleDTO.getStato());
			filter.setVersante(listaPagamentiPortaleDTO.getVersante());
			filter.setFilterSortList(listaPagamentiPortaleDTO.getFieldSortList());

			long count = pagamentiPortaleBD.count(filter);

			if(count > 0) {
				return new ListaPagamentiPortaleDTOResponse(count, pagamentiPortaleBD.findAll(filter));
			} else {
				return new ListaPagamentiPortaleDTOResponse(count, new ArrayList<PagamentoPortale>());
			}
		}finally {
			if(bd != null)
				bd.closeConnection();
		}
	}
}
