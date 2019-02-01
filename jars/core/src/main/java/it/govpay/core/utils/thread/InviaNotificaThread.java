/*
 * GovPay - Porta di Accesso al Nodo dei Pagamenti SPC 
 * http://www.gov4j.it/govpay
 * 
 * Copyright (c) 2014-2017 Link.it srl (http://www.link.it).
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3, as published by
 * the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package it.govpay.core.utils.thread;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.openspcoop2.generic_project.exception.ServiceException;
import org.openspcoop2.utils.LoggerWrapperFactory;
import org.openspcoop2.utils.UtilsException;
import org.openspcoop2.utils.logger.beans.Property;
import org.openspcoop2.utils.service.context.IContext;
import org.slf4j.Logger;

import it.govpay.bd.BasicBD;
import it.govpay.bd.model.Applicazione;
import it.govpay.bd.model.Dominio;
import it.govpay.bd.model.Notifica;
import it.govpay.bd.model.Pagamento;
import it.govpay.bd.model.Rpt;
import it.govpay.bd.model.Versamento;
import it.govpay.bd.model.eventi.EventoCooperazione;
import it.govpay.bd.model.eventi.EventoIntegrazione;
import it.govpay.bd.model.eventi.EventoNota;
import it.govpay.bd.pagamento.NotificheBD;
import it.govpay.core.business.GiornaleEventi;
import it.govpay.core.exceptions.GovPayException;
import it.govpay.core.utils.GpContext;
import it.govpay.core.utils.GpThreadLocal;
import it.govpay.core.utils.client.BasicClient.ClientException;
import it.govpay.core.utils.client.NotificaClient;
import it.govpay.model.Connettore;
import it.govpay.model.Evento.CategoriaEvento;
import it.govpay.model.Notifica.StatoSpedizione;
import it.govpay.model.Notifica.TipoNotifica;
import it.govpay.model.Versionabile.Versione;

public class InviaNotificaThread implements Runnable {

	public static final String PA_NOTIFICA_TRANSAZIONE = "paNotificaTransazione";
	private static Logger log = LoggerWrapperFactory.getLogger(InviaNotificaThread.class);
	private Notifica notifica;
	private Versamento versamento;
	private Rpt rpt;
	private Dominio dominio = null;
	private boolean completed = false;
	private Applicazione applicazione = null;
	private Connettore connettoreNotifica = null;
	private IContext ctx = null;

	public InviaNotificaThread(Notifica notifica, BasicBD bd, IContext ctx) throws ServiceException {
		// Verifico che tutti i campi siano valorizzati
		this.notifica = notifica;
		this.notifica.getApplicazione(bd);
		this.versamento = this.notifica.getRpt(bd).getVersamento(bd);
		this.dominio = this.versamento.getDominio(bd);
		this.rpt = this.notifica.getRpt(bd);
		this.applicazione = this.notifica.getApplicazione(bd);
		this.connettoreNotifica = this.applicazione.getConnettoreNotifica();
		List<Pagamento> pagamenti = this.rpt.getPagamenti(bd);
		if(pagamenti != null) {
			for(Pagamento pagamento : pagamenti)
				pagamento.getSingoloVersamento(bd);
		}
		this.ctx = ctx;
		
	}

	@Override
	public void run() {
		GpThreadLocal.set(this.ctx);
		
		IContext ctx = GpThreadLocal.get();
		GpContext appContext = (GpContext) ctx.getApplicationContext();
		BasicBD bd = null;
		TipoNotifica tipoNotifica = this.notifica.getTipo();
		GiornaleEventi giornaleEventi = null;
		EventoIntegrazione evento = new EventoIntegrazione();
		String messaggioRichiesta = null;
		String messaggioRisposta = null;
		try {
			
			String url = this.connettoreNotifica!= null ? this.connettoreNotifica.getUrl() : GpContext.NOT_SET;
			Versione versione = this.connettoreNotifica != null ? this.connettoreNotifica.getVersione() : Versione.GP_REST_01;
			String operationId = appContext.setupPaClient(applicazione.getCodApplicazione(), PA_NOTIFICA_TRANSAZIONE, url, versione);
			
			if(this.rpt.getCodCarrello() != null) {
				appContext.getServerByOperationId(operationId).addGenericProperty(new Property("codCarrello", rpt.getCodCarrello()));
			} 
			appContext.getServerByOperationId(operationId).addGenericProperty(new Property("codDominio", this.notifica.getRpt(null).getCodDominio()));
			appContext.getServerByOperationId(operationId).addGenericProperty(new Property("iuv", this.notifica.getRpt(null).getIuv()));
			appContext.getServerByOperationId(operationId).addGenericProperty(new Property("ccp", this.notifica.getRpt(null).getCcp()));
			appContext.getServerByOperationId(operationId).addGenericProperty(new Property("tipoNotifica", tipoNotifica.name().toLowerCase()));
			
			switch (tipoNotifica) {
			case ATTIVAZIONE:
				ctx.getApplicationLogger().log("notifica.rpt");
				break;
			case ANNULLAMENTO:
				ctx.getApplicationLogger().log("notifica.annullamentoRpt");
				break;
			case FALLIMENTO:
				ctx.getApplicationLogger().log("notifica.fallimentoRpt");
				break;
			case RICEVUTA:
				ctx.getApplicationLogger().log("notifica.rt");
				break;
			}
			
			bd = BasicBD.newInstance(GpThreadLocal.get().getTransactionId());
			giornaleEventi = new GiornaleEventi(bd);
			
//			MDC.put(MD5Constants.OPERATION_ID, "InviaNotificaThread");
//			MDC.put(MD5Constants.TRANSACTION_ID, ctx.getTransactionId());
			
			log.info("Spedizione della notifica di "+tipoNotifica.name().toLowerCase()+" pagamento della transazione [" + this.notifica.getRptKey(bd) +"] all'applicazione [CodApplicazione: " + this.notifica.getApplicazione(null).getCodApplicazione() + "]");
			if(connettoreNotifica == null || connettoreNotifica.getUrl() == null) {
				
				
				ctx.getApplicationLogger().log("notifica.annullata");
				log.info("Connettore Notifica non configurato per l'applicazione [CodApplicazione: " + applicazione.getCodApplicazione() + "]. Spedizione inibita.");
				NotificheBD notificheBD = new NotificheBD(bd);
				long tentativi = this.notifica.getTentativiSpedizione() + 1;
				Date prossima = new GregorianCalendar(9999,1,1).getTime();
				notificheBD.updateAnnullata(this.notifica.getId(), "Connettore Notifica non configurato, notifica annullata.", tentativi, prossima);
				
				EventoNota eventoNota = new EventoNota();
				eventoNota.setAutore(EventoNota.UTENTE_SISTEMA);
				eventoNota.setOggetto("Notifica " +tipoNotifica.name().toLowerCase()
						+ " pagamento annullata: connettore non configurato.");
				eventoNota.setTesto("Notifica " +tipoNotifica.name().toLowerCase()
						+ " pagamento annullata: connettore di notifica dell'applicazione "+this.applicazione.getCodApplicazione()+" non configurato.");
				eventoNota.setPrincipal(null);
				eventoNota.setData(new Date());
				eventoNota.setTipoEvento(it.govpay.bd.model.eventi.EventoNota.TipoNota.SistemaInfo);
				eventoNota.setCodDominio(this.rpt.getCodDominio());
				eventoNota.setIuv(this.rpt.getIuv());
				eventoNota.setCcp(this.rpt.getCcp());
				eventoNota.setIdPagamentoPortale(this.rpt.getIdPagamentoPortale());
				eventoNota.setIdVersamento(this.rpt.getIdVersamento());					
				giornaleEventi.registraEventoNota(eventoNota);
				
				return;
			}
			
			ctx.getApplicationLogger().log("notifica.spedizione");
			
			NotificaClient client = new NotificaClient(applicazione, operationId);
			
			messaggioRichiesta = client.getMessaggioRichiesta(this.notifica, bd);
			
			byte[] byteResponse = client.invoke(this.notifica,bd);
			
			messaggioRisposta = byteResponse != null ? new String(byteResponse) : "";
			
			this.notifica.setStato(StatoSpedizione.SPEDITO);
			this.notifica.setDescrizioneStato(null);
			this.notifica.setDataAggiornamento(new Date());
			if(bd == null)
				bd = BasicBD.newInstance(GpThreadLocal.get().getTransactionId());
			NotificheBD notificheBD = new NotificheBD(bd);
			notificheBD.updateSpedito(this.notifica.getId());
			
			switch (tipoNotifica) {
			case ATTIVAZIONE:
				if(rpt.getCodCarrello() != null) {
					ctx.getApplicationLogger().log("notifica.carrellook");
				} else {
					ctx.getApplicationLogger().log("notifica.rptok");
				}
				break;
			case ANNULLAMENTO:
				ctx.getApplicationLogger().log("notifica.annullamentoRptok");
				break;
			case FALLIMENTO:
				ctx.getApplicationLogger().log("notifica.fallimentoRptok");
				break;
			case RICEVUTA:
				ctx.getApplicationLogger().log("notifica.rtok");
				break;
			}
			
			buildEventoIntegrazione(evento, ctx.getTransactionId(), applicazione.getCodApplicazione(), PA_NOTIFICA_TRANSAZIONE, tipoNotifica.name(), "OK", null, messaggioRichiesta, messaggioRisposta, bd);
			giornaleEventi.registraEventointegrazione(evento);
			 
			log.info("Notifica consegnata con successo");
		} catch(Exception e) {
			if(e instanceof GovPayException || e instanceof ClientException)
				log.warn("Errore nella consegna della notifica: " + e.getMessage());
			else
				log.error("Errore nella consegna della notifica", e);
			try {
				if(bd == null)
					bd = BasicBD.newInstance(GpThreadLocal.get().getTransactionId());
				
				Rpt rpt = this.notifica.getRpt(bd);
				
				long tentativi = this.notifica.getTentativiSpedizione() + 1;
				NotificheBD notificheBD = new NotificheBD(bd);
				
				Date today = new Date();
				Date tomorrow = new Date(today.getTime() + (1000 * 60 * 60 * 24));
				Date prossima = new Date(today.getTime() + (tentativi * tentativi * 60 * 1000));
				
				// Limito la rispedizione al giorno dopo.
				if(prossima.after(tomorrow)) prossima = tomorrow;
				
				if(tentativi == 1 || !e.getMessage().equals(this.notifica.getDescrizioneStato())) {
					switch (tipoNotifica) {
					case ATTIVAZIONE:
						if(rpt.getCodCarrello() != null) {
							ctx.getApplicationLogger().log("notifica.carrelloko", e.getMessage());
						} else {
							ctx.getApplicationLogger().log("notifica.rptko", e.getMessage());
						}
						break;
					case ANNULLAMENTO:
						ctx.getApplicationLogger().log("notifica.annullamentoRptko", e.getMessage());
						break;
					case FALLIMENTO:
						ctx.getApplicationLogger().log("notifica.fallimentoRptko", e.getMessage());
						break;
					case RICEVUTA:
						ctx.getApplicationLogger().log("notifica.rtko", e.getMessage());
						break;
					}
				} else {
					switch (tipoNotifica) {
					case ATTIVAZIONE:
						if(rpt.getCodCarrello() != null) {
							ctx.getApplicationLogger().log("notifica.carrelloRetryko", e.getMessage(), prossima.toString());
						} else {
							ctx.getApplicationLogger().log("notifica.rptRetryko", e.getMessage(), prossima.toString());
						}
						break;
					case ANNULLAMENTO:
						ctx.getApplicationLogger().log("notifica.annullamentoRptRetryko", e.getMessage(), prossima.toString());
						break;
					case FALLIMENTO:
						ctx.getApplicationLogger().log("notifica.fallimentoRptRetryko", e.getMessage(), prossima.toString());
						break;
					case RICEVUTA:
						ctx.getApplicationLogger().log("notifica.rtRetryko", e.getMessage(), prossima.toString());
						break;
					}
				}
				
				notificheBD.updateDaSpedire(this.notifica.getId(), e.getMessage(), tentativi, prossima);
				
				if(giornaleEventi != null) {
					if(e instanceof ClientException) {
						byte[] byteResponse = ((ClientException) e).getResponseContent();
						messaggioRisposta = byteResponse != null ? new String(byteResponse) : "";
					}
					
					buildEventoIntegrazione(evento, ctx.getTransactionId(), applicazione.getCodApplicazione(), PA_NOTIFICA_TRANSAZIONE, tipoNotifica.name(), "KO", e.getMessage(), messaggioRichiesta, messaggioRisposta, bd);
					giornaleEventi.registraEventointegrazione(evento);
				}
				
			} catch (Exception ee) {
				// Andato male l'aggiornamento. Non importa, verra' rispedito.
			}
		} finally {
			this.completed = true;
			if(bd != null) bd.closeConnection(); 
//			if(ctx != null)
//				try {
//					ctx.getApplicationLogger().log();
//				} catch (UtilsException e) {
//					log.error("Errore durante il log dell'operazione: " + e.getMessage(), e);
//				}
			GpThreadLocal.unset();
		}
	}

	public boolean isCompleted() {
		return this.completed;
	}
	
	private void buildEventoIntegrazione(EventoIntegrazione evento, String idTransazione, 
			String codApplicazione, String tipoEvento, 
			String sottotipoEvento, String esito, String descrizioneEsito,  
			String messaggioRichiesta, String messaggioRisposta, BasicBD bd) {
		evento.setAltriParametriRichiesta(null);
		evento.setAltriParametriRisposta(null);
		evento.setCategoriaEvento(CategoriaEvento.INTERFACCIA_INTEGRAZIONE);
		evento.setCodDominio(this.dominio.getCodDominio());
		evento.setComponente(EventoCooperazione.COMPONENTE);
		evento.setDataRisposta(new Date());
		evento.setErogatore(codApplicazione);
		evento.setEsito(esito);
		evento.setDescrizioneEsito(descrizioneEsito);
		evento.setFruitore(EventoCooperazione.COMPONENTE);
		evento.setIuv(this.rpt.getIuv());
		evento.setCcp(this.rpt.getCcp());
		evento.setSottotipoEvento(sottotipoEvento);
		evento.setTipoEvento(tipoEvento);
		evento.setAltriParametriRichiesta(messaggioRichiesta);
		evento.setAltriParametriRisposta(messaggioRisposta);
		evento.setIdVersamento(this.versamento.getId());
		evento.setIdTransazione(idTransazione);
	}
}