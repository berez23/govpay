package it.govpay.bd.reportistica.statistiche.model;

import org.openspcoop2.generic_project.exception.NotFoundException;
import org.openspcoop2.generic_project.exception.ServiceException;

import it.govpay.bd.BasicBD;
import it.govpay.bd.anagrafica.AnagraficaManager;
import it.govpay.bd.model.Applicazione;
import it.govpay.bd.model.Dominio;
import it.govpay.bd.model.UnitaOperativa;
import it.govpay.model.TipoVersamento;

public class StatisticaRiscossione extends it.govpay.model.reportistica.statistiche.StatisticaRiscossione{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L; 
	
	public StatisticaRiscossione() {
		super();
	}
	
	private transient Dominio dominio;
	private transient UnitaOperativa uo;
	private transient TipoVersamento tipoVersamento;
	private transient Applicazione applicazione;
	
	public UnitaOperativa getUo(BasicBD bd) throws ServiceException {
		if(this.getCodUo() != null && this.getDominio(bd) != null && this.uo == null) {
			try {
				this.uo = AnagraficaManager.getUnitaOperativa(bd, this.getDominio(bd).getId(), this.getCodUo());
			} catch (NotFoundException e) {
			}
		}
		return this.uo;
	}

	public Dominio getDominio(BasicBD bd) throws ServiceException {
		if(this.getCodDominio() != null && this.dominio == null) {
			try {
				this.dominio = AnagraficaManager.getDominio(bd, this.getCodDominio());
			} catch (NotFoundException e) {
			}
		}
		return this.dominio;
	}
	
	public TipoVersamento getTipoVersamento(BasicBD bd) throws ServiceException {
		if(this.getCodTipoVersamento() != null && this.tipoVersamento == null) {
			try {
				this.tipoVersamento = AnagraficaManager.getTipoVersamento(bd, this.getCodTipoVersamento());
			} catch (NotFoundException e) {
			}
		} 
		return this.tipoVersamento;
	}
	
	public Applicazione getApplicazione(BasicBD bd) throws ServiceException {
		if(this.getCodApplicazione() != null &&this.applicazione == null) {
			try {
				this.applicazione = AnagraficaManager.getApplicazione(bd, this.getCodApplicazione());
			} catch (NotFoundException e) {
			}
		} 
		return this.applicazione;
	}
}
