package it.govpay.core.dao.pagamenti.dto;

import java.util.Date;

import org.openspcoop2.generic_project.beans.IField;

import it.govpay.bd.model.PagamentoPortale.STATO;
import it.govpay.core.dao.anagrafica.dto.BasicFindRequestDTO;
import it.govpay.core.exceptions.InternalException;
import it.govpay.core.exceptions.RequestParamException;
import it.govpay.model.IAutorizzato;
import it.govpay.model.Rpt.StatoRpt;
import it.govpay.orm.RPT;

public class ListaRptDTO extends BasicFindRequestDTO{
	
	public enum Field {
		dataRichiesta(RPT.model().DATA_MSG_RICHIESTA),
		stato(RPT.model().STATO);
		
		private IField ifield;

		private Field(IField ifield) {
			this.ifield = ifield;
		}
		
		public IField getField(){
			return ifield;
		}
	}

	public ListaRptDTO(IAutorizzato user) {
		super(user);
	}
	private Date dataA;
	private Date dataDa;
	private StatoRpt stato;
	private String idDominio;
	private String iuv;
	private String ccp;
	private String idA2A;
	private String idPendenza;
	private String idPagamento;
	
	public Date getDataA() {
		return dataA;
	}
	public void setDataA(Date dataA) {
		this.dataA = dataA;
	}
	public Date getDataDa() {
		return dataDa;
	}
	public void setDataDa(Date dataDa) {
		this.dataDa = dataDa;
	}
	public StatoRpt getStato() {
		return stato;
	}
	public void setStato(StatoRpt stato) {
		this.stato = stato;
	}
	public String getIdDominio() {
		return idDominio;
	}
	public void setIdDominio(String idDominio) {
		this.idDominio = idDominio;
	}
	public String getIuv() {
		return iuv;
	}
	public void setIuv(String iuv) {
		this.iuv = iuv;
	}
	public String getCcp() {
		return ccp;
	}
	public void setCcp(String ccp) {
		this.ccp = ccp;
	}
	public String getIdA2A() {
		return idA2A;
	}
	public void setIdA2A(String idA2A) {
		this.idA2A = idA2A;
	}
	public String getIdPendenza() {
		return idPendenza;
	}
	public void setIdPendenza(String idPendenza) {
		this.idPendenza = idPendenza;
	}
	public String getIdPagamento() {
		return idPagamento;
	}
	public void setIdPagamento(String idPagamento) {
		this.idPagamento = idPagamento;
	}
	public void setOrderBy(String orderBy) throws RequestParamException, InternalException {
		setOrderBy(Field.class, orderBy);
	}
}
