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
package it.govpay.core.business;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.openspcoop2.generic_project.exception.NotFoundException;
import org.openspcoop2.generic_project.exception.ServiceException;

import it.govpay.bd.BasicBD;
import it.govpay.bd.anagrafica.AnagraficaManager;
import it.govpay.bd.model.SingoloVersamento;
import it.govpay.bd.model.Versamento;
import it.govpay.core.exceptions.GovPayException;
import it.govpay.core.exceptions.VersamentoAnnullatoException;
import it.govpay.core.exceptions.VersamentoDuplicatoException;
import it.govpay.core.exceptions.VersamentoScadutoException;
import it.govpay.core.exceptions.VersamentoSconosciutoException;
import it.govpay.core.utils.AclEngine;
import it.govpay.core.utils.GpContext;
import it.govpay.core.utils.GpThreadLocal;
import it.govpay.core.utils.client.BasicClient.ClientException;
import it.govpay.core.utils.client.VerificaClient;
import it.govpay.model.Acl.Diritti;
import it.govpay.model.Acl.Servizio;
import it.govpay.bd.model.Applicazione;
import it.govpay.model.Dominio;
import it.govpay.model.SingoloVersamento.StatoSingoloVersamento;
import it.govpay.model.SingoloVersamento.TipoBollo;
import it.govpay.model.Tributo;
import it.govpay.model.Tributo.TipoContabilita;
import it.govpay.model.Versamento.CausaleSemplice;
import it.govpay.model.Versamento.StatoVersamento;
import it.govpay.servizi.commons.Anagrafica;
import it.govpay.servizi.commons.EsitoOperazione;

public class VersamentoUtils {
	
	public static void validazioneSemantica(Versamento versamento, boolean generaIuv, BasicBD bd) throws GovPayException, ServiceException {
		if(generaIuv && versamento.getSingoliVersamenti(bd).size() != 1) {
			throw new GovPayException(EsitoOperazione.VER_000, versamento.getApplicazione(bd).getCodApplicazione(), versamento.getCodVersamentoEnte());
		}
		
		BigDecimal somma = BigDecimal.ZERO;
		List<String> codSingoliVersamenti = new ArrayList<String>();
		for(SingoloVersamento sv : versamento.getSingoliVersamenti(bd)) {
			if(codSingoliVersamenti.contains(sv.getCodSingoloVersamentoEnte()))
				throw new GovPayException(EsitoOperazione.VER_001, versamento.getApplicazione(bd).getCodApplicazione(), versamento.getCodVersamentoEnte(), sv.getCodSingoloVersamentoEnte());
			
			codSingoliVersamenti.add(sv.getCodSingoloVersamentoEnte());
			somma = somma.add(sv.getImportoSingoloVersamento());
		}
		
		if(somma.compareTo(versamento.getImportoTotale()) != 0) {
			throw new GovPayException(EsitoOperazione.VER_002, versamento.getApplicazione(bd).getCodApplicazione(), versamento.getCodVersamentoEnte(), Double.toString(somma.doubleValue()), Double.toString(versamento.getImportoTotale().doubleValue()));
		}
	}

	public static Versamento toVersamentoModel(it.govpay.core.dao.commons.Versamento versamento, BasicBD bd) throws ServiceException, GovPayException {
		Versamento model = new Versamento();
		model.setAggiornabile(versamento.isAggiornabile() == null ? true : versamento.isAggiornabile());
		model.setAnagraficaDebitore(toAnagraficaModel(versamento.getDebitore()));
		
		CausaleSemplice causale = model.new CausaleSemplice();
		causale.setCausale(versamento.getCausale());
		model.setCausaleVersamento(causale);
		
		model.setCodAnnoTributario(versamento.getAnnoTributario());
		model.setCodBundlekey(versamento.getBundlekey());
		model.setCodLotto(versamento.getCodDebito()); 
		model.setCodVersamentoEnte(versamento.getCodVersamentoEnte());
		model.setDataCreazione(new Date());
		model.setDataScadenza(versamento.getDataScadenza());
		model.setDataUltimoAggiornamento(new Date());
		model.setDescrizioneStato(null);
		model.setNome(versamento.getNome());
		model.setId(null);
		try {
			model.setApplicazione(versamento.getCodApplicazione(), bd);
		} catch (NotFoundException e) {
			throw new GovPayException(EsitoOperazione.APP_000, versamento.getCodApplicazione());
		}
		
		Dominio dominio = null;
		try {
			dominio = AnagraficaManager.getDominio(bd, versamento.getCodDominio());
			model.setIdDominio(dominio.getId()); 
		} catch (NotFoundException e) {
			throw new GovPayException(EsitoOperazione.DOM_000, versamento.getCodDominio());
		}
		
		try {
			String codUnitaOperativa = (versamento.getCodUnitaOperativa() == null) ? Dominio.EC : versamento.getCodUnitaOperativa();
			model.setUo(dominio.getId(), codUnitaOperativa, bd);
		} catch (NotFoundException e) {
			throw new GovPayException(EsitoOperazione.UOP_000, versamento.getCodUnitaOperativa(), versamento.getCodDominio());
		}
		
		model.setImportoTotale(versamento.getImportoTotale());
		model.setStatoVersamento(StatoVersamento.NON_ESEGUITO);
		
		for(it.govpay.core.dao.commons.Versamento.SingoloVersamento singoloVersamento : versamento.getSingoloVersamento()) {
			model.addSingoloVersamento(toSingoloVersamentoModel(model, singoloVersamento, bd));
		}
		
		return model;
	}

	
	public static SingoloVersamento toSingoloVersamentoModel(Versamento versamento, it.govpay.core.dao.commons.Versamento.SingoloVersamento singoloVersamento, BasicBD bd) throws ServiceException, GovPayException {
		SingoloVersamento model = new SingoloVersamento();
		model.setVersamento(versamento);
		model.setCodSingoloVersamentoEnte(singoloVersamento.getCodSingoloVersamentoEnte());
		model.setId(null);
		model.setIdVersamento(0);
		model.setImportoSingoloVersamento(singoloVersamento.getImporto());
		model.setStatoSingoloVersamento(StatoSingoloVersamento.NON_ESEGUITO);
		model.setNote(singoloVersamento.getNote()); 
		if(singoloVersamento.getBolloTelematico() != null) {
			try {
				model.setTributo(Tributo.BOLLOT, bd);
			} catch (NotFoundException e) {
				throw new GovPayException(EsitoOperazione.TRB_000, versamento.getUo(bd).getDominio(bd).getCodDominio(), Tributo.BOLLOT);
			}
			model.setHashDocumento(singoloVersamento.getBolloTelematico().getHash());
			model.setProvinciaResidenza(singoloVersamento.getBolloTelematico().getProvincia());
			model.setTipoBollo(TipoBollo.toEnum(singoloVersamento.getBolloTelematico().getTipo()));
		} 
		
		List<Diritti> diritti = new ArrayList<Diritti>(); // TODO controllare quale diritto serve in questa fase
		diritti.add(Diritti.SCRITTURA);
		diritti.add(Diritti.ESECUZIONE);
		if(singoloVersamento.getCodTributo() != null) {
			try {
				model.setTributo(singoloVersamento.getCodTributo(), bd);
			} catch (NotFoundException e) {
				throw new GovPayException(EsitoOperazione.TRB_000, versamento.getUo(bd).getDominio(bd).getCodDominio(), singoloVersamento.getCodTributo());
			}
			
			if(!versamento.getApplicazione(bd).isTrusted() && !AclEngine.isAuthorized(versamento.getApplicazione(bd).getUtenza(), Servizio.PAGAMENTI_E_PENDENZE, versamento.getUo(bd).getDominio(bd).getCodDominio(), singoloVersamento.getCodTributo(),diritti)) {
				throw new GovPayException(EsitoOperazione.VER_022, versamento.getUo(bd).getDominio(bd).getCodDominio(), singoloVersamento.getCodTributo());
			}
		}
		
		if(singoloVersamento.getTributo() != null) {
			
			if(!versamento.getApplicazione(bd).isTrusted())
				throw new GovPayException(EsitoOperazione.VER_019);
			
			if(!AclEngine.isAuthorized(versamento.getApplicazione(bd).getUtenza(), Servizio.PAGAMENTI_E_PENDENZE, versamento.getUo(bd).getDominio(bd).getCodDominio(), null,diritti))
				throw new GovPayException(EsitoOperazione.VER_021);
			
			try {
				model.setIbanAccredito(AnagraficaManager.getIbanAccredito(bd, versamento.getUo(bd).getDominio(bd).getId(), singoloVersamento.getTributo().getIbanAccredito()));
				model.setTipoContabilita(TipoContabilita.valueOf(singoloVersamento.getTributo().getTipoContabilita().toString()));
				model.setCodContabilita(singoloVersamento.getTributo().getCodContabilita());
			} catch (NotFoundException e) {
				throw new GovPayException(EsitoOperazione.VER_020, versamento.getUo(bd).getDominio(bd).getCodDominio(), singoloVersamento.getTributo().getIbanAccredito());
			}
		}
		
		return model;
	}

	public static void validazioneSemanticaAggiornamento(Versamento versamentoLetto, Versamento versamentoNuovo, BasicBD bd) throws GovPayException, ServiceException {
		
		if(!versamentoLetto.getStatoVersamento().equals(StatoVersamento.NON_ESEGUITO) && !versamentoLetto.getStatoVersamento().equals(StatoVersamento.ANNULLATO)) {
			throw new GovPayException(EsitoOperazione.VER_003, versamentoNuovo.getApplicazione(bd).getCodApplicazione(), versamentoLetto.getCodVersamentoEnte(), versamentoLetto.getStatoVersamento().toString());
		}
		if(versamentoLetto.getIdUo() != versamentoNuovo.getIdUo()) {
			throw new GovPayException(EsitoOperazione.VER_004, versamentoNuovo.getApplicazione(bd).getCodApplicazione(), versamentoLetto.getCodVersamentoEnte(), versamentoLetto.getUo(bd).getCodUo(), versamentoNuovo.getUo(bd).getCodUo());
		}
		versamentoNuovo.setId(versamentoLetto.getId());
		versamentoNuovo.setDataCreazione(versamentoLetto.getDataCreazione());
		
		List<SingoloVersamento> singoliversamentiLetti = versamentoLetto.getSingoliVersamenti(bd);
		List<SingoloVersamento> singoliVersamentiNuovi = versamentoNuovo.getSingoliVersamenti(bd);
		
		if(singoliversamentiLetti.size() != singoliVersamentiNuovi.size()) {
			throw new GovPayException(EsitoOperazione.VER_005, versamentoNuovo.getApplicazione(bd).getCodApplicazione(), versamentoNuovo.getCodVersamentoEnte(), Integer.toString(singoliversamentiLetti.size()), Integer.toString(singoliVersamentiNuovi.size()));
		}
		Collections.sort(singoliversamentiLetti);
		Collections.sort(singoliVersamentiNuovi);
		
		for(int i=0; i<singoliversamentiLetti.size(); i++) {
			SingoloVersamento letto = singoliversamentiLetti.get(i);
			SingoloVersamento nuovo = singoliVersamentiNuovi.get(i);
			
			if(!letto.getCodSingoloVersamentoEnte().equals(nuovo.getCodSingoloVersamentoEnte())) {
				throw new GovPayException(EsitoOperazione.VER_006, versamentoNuovo.getApplicazione(bd).getCodApplicazione(), versamentoNuovo.getCodVersamentoEnte(), letto.getCodSingoloVersamentoEnte());
			}
			
			if(letto.getIdTributo() != null || nuovo.getIdTributo() != null) { // se sono entrambi null OK
				if(letto.getIdTributo() == null && nuovo.getIdTributo() != null) { // se uno e' null e uno no Eccezione
					throw new GovPayException(EsitoOperazione.VER_007, versamentoNuovo.getApplicazione(bd).getCodApplicazione(), versamentoNuovo.getCodVersamentoEnte(), letto.getCodSingoloVersamentoEnte(), "null", Long.toString(nuovo.getIdTributo()));
				}
				
				if(letto.getIdTributo() != null && nuovo.getIdTributo() == null) { // se uno e' null e uno no Eccezione
					throw new GovPayException(EsitoOperazione.VER_007, versamentoNuovo.getApplicazione(bd).getCodApplicazione(), versamentoNuovo.getCodVersamentoEnte(), letto.getCodSingoloVersamentoEnte(), Long.toString(letto.getIdTributo()), "null");
				}
				
				if(letto.getIdTributo().longValue() != nuovo.getIdTributo().longValue()) { // se sono tutti e due non null controllo l'uguaglianza dei long value
					throw new GovPayException(EsitoOperazione.VER_007, versamentoNuovo.getApplicazione(bd).getCodApplicazione(), versamentoNuovo.getCodVersamentoEnte(), letto.getCodSingoloVersamentoEnte(), Long.toString(letto.getIdTributo()), Long.toString(nuovo.getIdTributo()));
				}
			}

			if(!(letto.getIbanAccredito(bd) == null && nuovo.getIbanAccredito(bd) == null)) {
				if(letto.getIbanAccredito(bd) == null || nuovo.getIbanAccredito(bd).getId() == null || !letto.getIbanAccredito(bd).getId().equals(nuovo.getIbanAccredito(bd).getId())) {
					throw new GovPayException(EsitoOperazione.VER_023, versamentoNuovo.getApplicazione(bd).getCodApplicazione(), versamentoNuovo.getCodVersamentoEnte(), letto.getCodSingoloVersamentoEnte());
				}
			}
			nuovo.setId(letto.getId());
			nuovo.setIdVersamento(letto.getIdVersamento());
		}
	}
	
	public static it.govpay.model.Anagrafica toAnagraficaModel(Anagrafica anagrafica) {
		if(anagrafica == null) return null;
		it.govpay.model.Anagrafica anagraficaModel = new it.govpay.model.Anagrafica();
		anagraficaModel.setCap(anagrafica.getCap());
		anagraficaModel.setCellulare(anagrafica.getCellulare());
		anagraficaModel.setCivico(anagrafica.getCivico());
		anagraficaModel.setCodUnivoco(anagrafica.getCodUnivoco());
		anagraficaModel.setEmail(anagrafica.getEmail());
		anagraficaModel.setFax(anagrafica.getFax());
		anagraficaModel.setIndirizzo(anagrafica.getIndirizzo());
		anagraficaModel.setLocalita(anagrafica.getLocalita());
		anagraficaModel.setNazione(anagrafica.getNazione());
		anagraficaModel.setProvincia(anagrafica.getProvincia());
		anagraficaModel.setRagioneSociale(anagrafica.getRagioneSociale());
		anagraficaModel.setTelefono(anagrafica.getTelefono());
		return anagraficaModel;
	}
	
	public static it.govpay.model.Anagrafica toAnagraficaModel(it.govpay.core.dao.commons.Anagrafica anagrafica) {
		if(anagrafica == null) return null;
		it.govpay.model.Anagrafica anagraficaModel = new it.govpay.model.Anagrafica();
		anagraficaModel.setCap(anagrafica.getCap());
		anagraficaModel.setCellulare(anagrafica.getCellulare());
		anagraficaModel.setCivico(anagrafica.getCivico());
		anagraficaModel.setCodUnivoco(anagrafica.getCodUnivoco());
		anagraficaModel.setEmail(anagrafica.getEmail());
		anagraficaModel.setFax(anagrafica.getFax());
		anagraficaModel.setIndirizzo(anagrafica.getIndirizzo());
		anagraficaModel.setLocalita(anagrafica.getLocalita());
		anagraficaModel.setNazione(anagrafica.getNazione());
		anagraficaModel.setProvincia(anagrafica.getProvincia());
		anagraficaModel.setRagioneSociale(anagrafica.getRagioneSociale());
		anagraficaModel.setTelefono(anagrafica.getTelefono());
		return anagraficaModel;
	}
	
	
	public static Versamento aggiornaVersamento(Versamento versamento, BasicBD bd) throws VersamentoScadutoException, VersamentoAnnullatoException, VersamentoDuplicatoException, VersamentoSconosciutoException, ServiceException, ClientException, GovPayException {
		// Se il versamento non e' in attesa, non aggiorno un bel niente
		if(!versamento.getStatoVersamento().equals(StatoVersamento.NON_ESEGUITO))
			return versamento;
		
		// Controllo se la data di scadenza e' indicata ed e' decorsa
		if(versamento.getDataScadenza() != null && versamento.getDataScadenza().before(new Date())) {
			if(versamento.isAggiornabile() && versamento.getApplicazione(bd).getConnettoreVerifica() != null) {
				versamento = acquisisciVersamento(versamento.getApplicazione(bd), versamento.getCodVersamentoEnte(), versamento.getCodBundlekey(), versamento.getAnagraficaDebitore().getCodUnivoco(), versamento.getUo(bd).getDominio(bd).getCodDominio(), null, bd);
			} else {
				throw new VersamentoScadutoException(versamento.getDataScadenza());
			}
		}
		return versamento;
	}
	
	
	public static Versamento acquisisciVersamento(Applicazione applicazione, String codVersamentoEnte, String bundlekey, String debitore, String dominio, String iuv, BasicBD bd) throws VersamentoScadutoException, VersamentoAnnullatoException, VersamentoDuplicatoException, VersamentoSconosciutoException, ServiceException, ClientException, GovPayException {
		
		String codVersamentoEnteD = codVersamentoEnte != null ? codVersamentoEnte : "-";
		String bundlekeyD = bundlekey != null ? bundlekey : "-";
		String debitoreD = debitore != null ? debitore : "-";
		String dominioD = dominio != null ? dominio : "-";
		String iuvD = iuv != null ? iuv : "-";
		
		GpContext ctx = GpThreadLocal.get();
		ctx.log("verifica.avvio", applicazione.getCodApplicazione(), codVersamentoEnteD, bundlekeyD, debitoreD, dominioD, iuvD);
		if(applicazione.getConnettoreVerifica() == null) {
			ctx.log("verifica.nonConfigurata");
			throw new VersamentoSconosciutoException();
		}
		VerificaClient verificaClient = new VerificaClient(applicazione);
		Versamento versamento = null;
		try {
			versamento = verificaClient.invoke(codVersamentoEnte, bundlekey, debitore, dominio, iuv, bd);
			ctx.log("verifica.Ok", applicazione.getCodApplicazione(), codVersamentoEnteD, bundlekeyD, debitoreD, dominioD, iuvD);
		} catch (ClientException e){
			ctx.log("verifica.Fail", applicazione.getCodApplicazione(), codVersamentoEnteD, bundlekeyD, debitoreD, dominioD, iuvD, e.getMessage());
			throw e;
		} catch (VersamentoScadutoException e) {
			ctx.log("verifica.Scaduto", applicazione.getCodApplicazione(), codVersamentoEnteD, bundlekeyD, debitoreD, dominioD, iuvD);
			throw e;
		} catch (VersamentoAnnullatoException e) {
			ctx.log("verifica.Annullato", applicazione.getCodApplicazione(), codVersamentoEnteD, bundlekeyD, debitoreD, dominioD, iuvD);
			throw e;
		} catch (VersamentoDuplicatoException e) {
			ctx.log("verifica.Duplicato", applicazione.getCodApplicazione(), codVersamentoEnteD, bundlekeyD, debitoreD, dominioD, iuvD);
			throw e;
		} catch (VersamentoSconosciutoException e) {
			ctx.log("verifica.Sconosciuto", applicazione.getCodApplicazione(), codVersamentoEnteD, bundlekeyD, debitoreD, dominioD, iuvD);
			throw e;
		} 
		
		it.govpay.core.business.Versamento versamentoBusiness = new it.govpay.core.business.Versamento(bd);
		versamentoBusiness.caricaVersamento(applicazione, versamento, false, true);
		return versamento;
	}
}