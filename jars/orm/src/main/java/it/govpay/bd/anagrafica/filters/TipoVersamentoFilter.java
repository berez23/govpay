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
package it.govpay.bd.anagrafica.filters;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.openspcoop2.generic_project.beans.CustomField;
import org.openspcoop2.generic_project.dao.IExpressionConstructor;
import org.openspcoop2.generic_project.exception.ExpressionException;
import org.openspcoop2.generic_project.exception.ExpressionNotImplementedException;
import org.openspcoop2.generic_project.exception.NotImplementedException;
import org.openspcoop2.generic_project.exception.ServiceException;
import org.openspcoop2.generic_project.expression.IExpression;
import org.openspcoop2.generic_project.expression.LikeMode;
import org.openspcoop2.generic_project.expression.SortOrder;
import org.openspcoop2.utils.sql.ISQLQueryObject;

import it.govpay.bd.AbstractFilter;
import it.govpay.bd.ConnectionManager;
import it.govpay.bd.FilterSortWrapper;
import it.govpay.orm.TipoVersamento;
import it.govpay.orm.dao.jdbc.converter.TipoVersamentoFieldConverter;

public class TipoVersamentoFilter extends AbstractFilter {
	
	private String codTipoVersamento = null;
	private boolean searchModeEquals = false; 
	private String descrizione = null;
	private List<Long> listaIdTipiVersamento = null;
	private List<Long> listaIdTipiVersamentoDaEscludere = null;
	private CustomField cf;
	private String tipo;
	private Boolean form;
	private Boolean trasformazione;
	
	public enum SortFields { }
	
	public TipoVersamentoFilter(IExpressionConstructor expressionConstructor) {
		this(expressionConstructor,false);
	}
	
	public TipoVersamentoFilter(IExpressionConstructor expressionConstructor, boolean simpleSearch) {
		super(expressionConstructor, simpleSearch);
		
		try{
			TipoVersamentoFieldConverter converter = new TipoVersamentoFieldConverter(ConnectionManager.getJDBCServiceManagerProperties().getDatabase()); 
			this.cf = new CustomField("id", Long.class, "id", converter.toTable(it.govpay.orm.TipoVersamento.model()));
			this.listaFieldSimpleSearch.add(TipoVersamento.model().COD_TIPO_VERSAMENTO);
			this.listaFieldSimpleSearch.add(TipoVersamento.model().DESCRIZIONE);
			this.fieldAbilitato = it.govpay.orm.TipoVersamento.model().ABILITATO;
		} catch(Exception e){
			
		}
	}

	@Override
	public IExpression _toExpression() throws ServiceException {
		try {
			IExpression newExpression = this.newExpression();
			boolean addAnd = false;
			if(this.codTipoVersamento != null && StringUtils.isNotEmpty(this.codTipoVersamento)){
				if(addAnd)
					newExpression.and();
				if(!this.searchModeEquals)
					newExpression.ilike(it.govpay.orm.TipoVersamento.model().COD_TIPO_VERSAMENTO, this.codTipoVersamento,LikeMode.ANYWHERE);
				else 
					newExpression.equals(it.govpay.orm.TipoVersamento.model().COD_TIPO_VERSAMENTO, this.codTipoVersamento);
				addAnd = true;
			}
			
			if(this.descrizione != null && StringUtils.isNotEmpty(this.descrizione)){
				if(addAnd)
					newExpression.and();
				
				IExpression orExpr = this.newExpression();
				
				orExpr.ilike(it.govpay.orm.TipoVersamento.model().DESCRIZIONE, this.descrizione,LikeMode.ANYWHERE);
				orExpr.or().ilike(it.govpay.orm.TipoVersamento.model().COD_TIPO_VERSAMENTO, this.descrizione,LikeMode.ANYWHERE);
					
				newExpression.and(orExpr);	
				addAnd = true;
			}
			
			if(this.tipo != null && StringUtils.isNotEmpty(this.tipo)){
				if(addAnd)
					newExpression.and();
				newExpression.equals(it.govpay.orm.TipoVersamento.model().TIPO, this.tipo);
				addAnd = true;
			}
			
			if(this.listaIdTipiVersamento != null && this.listaIdTipiVersamento.size() > 0){
				if(addAnd)
					newExpression.and();
				newExpression.in(this.cf, this.listaIdTipiVersamento);
				
				addAnd = true;
			}
			
			if(this.listaIdTipiVersamentoDaEscludere != null && this.listaIdTipiVersamentoDaEscludere.size() > 0){
				if(addAnd)
					newExpression.and();
				
				IExpression notExpression = this.newExpression();
				notExpression.not().in(this.cf, this.listaIdTipiVersamentoDaEscludere);
				newExpression.and(notExpression);
				addAnd = true;
			}
			
			if(this.form != null){
				if(addAnd)
					newExpression.and();
				
				if(this.form) {
					newExpression.isNotNull(it.govpay.orm.TipoVersamento.model().FORM_DEFINIZIONE);
					newExpression.isNotNull(it.govpay.orm.TipoVersamento.model().FORM_TIPO);
				} else {
					newExpression.isNull(it.govpay.orm.TipoVersamento.model().FORM_DEFINIZIONE);
					newExpression.isNull(it.govpay.orm.TipoVersamento.model().FORM_TIPO);
				}
				
				addAnd = true;
			}
			
			if(this.trasformazione != null){
				if(addAnd)
					newExpression.and();
				
				if(this.trasformazione) {
					newExpression.isNotNull(it.govpay.orm.TipoVersamento.model().TRAC_CSV_HEADER_RISPOSTA);
					newExpression.isNotNull(it.govpay.orm.TipoVersamento.model().TRAC_CSV_TEMPLATE_RICHIESTA);
					newExpression.isNotNull(it.govpay.orm.TipoVersamento.model().TRAC_CSV_TEMPLATE_RISPOSTA);
					newExpression.isNotNull(it.govpay.orm.TipoVersamento.model().TRAC_CSV_TIPO);
				} else {
					newExpression.isNull(it.govpay.orm.TipoVersamento.model().TRAC_CSV_HEADER_RISPOSTA);
					newExpression.isNull(it.govpay.orm.TipoVersamento.model().TRAC_CSV_TEMPLATE_RICHIESTA);
					newExpression.isNull(it.govpay.orm.TipoVersamento.model().TRAC_CSV_TEMPLATE_RISPOSTA);
					newExpression.isNull(it.govpay.orm.TipoVersamento.model().TRAC_CSV_TIPO);
				}
				
				addAnd = true;
			}

			addAnd = this.setFiltroAbilitato(newExpression, addAnd);
			
			return newExpression;
		} catch (NotImplementedException e) {
			throw new ServiceException(e);
		} catch (ExpressionNotImplementedException e) {
			throw new ServiceException(e);
		} catch (ExpressionException e) {
			throw new ServiceException(e);
		}
	}
	
	public void addSortField(SortFields field, boolean asc) {
		FilterSortWrapper filterSortWrapper = new FilterSortWrapper();
		filterSortWrapper.setSortOrder((asc ? SortOrder.ASC : SortOrder.DESC));
		this.filterSortList.add(filterSortWrapper);
	}
	
	@Override
	public ISQLQueryObject toWhereCondition(ISQLQueryObject sqlQueryObject) throws ServiceException {
		return null;
	}

	@Override
	public Object[] getParameters(ISQLQueryObject sqlQueryObject) throws ServiceException {
		return null;
	}

	public List<Long> getListaIdTipiVersamento() {
		return listaIdTipiVersamento;
	}

	public void setListaIdTipiVersamento(List<Long> listaIdTipiVersamento) {
		this.listaIdTipiVersamento = listaIdTipiVersamento;
	}

	public List<Long> getListaIdTipiVersamentoDaEscludere() {
		return listaIdTipiVersamentoDaEscludere;
	}

	public void setListaIdTipiVersamentoDaEscludere(List<Long> listaIdTipiVersamentoDaEscludere) {
		this.listaIdTipiVersamentoDaEscludere = listaIdTipiVersamentoDaEscludere;
	}

	public String getCodTipoVersamento() {
		return codTipoVersamento;
	}

	public void setCodTipoVersamento(String codTipoVersamento) {
		this.codTipoVersamento = codTipoVersamento;
	}

	public String getDescrizione() {
		return this.descrizione;
	}

	public void setDescrizione(String descrizione) {
		this.descrizione = descrizione;
	}

	public String getTipo() {
		return tipo;
	}

	public void setTipo(String tipo) {
		this.tipo = tipo;
	}

	public Boolean getForm() {
		return form;
	}

	public void setForm(Boolean form) {
		this.form = form;
	}

	public Boolean getTrasformazione() {
		return trasformazione;
	}

	public void setTrasformazione(Boolean trasformazione) {
		this.trasformazione = trasformazione;
	}
	
	public boolean isSearchModeEquals() {
		return this.searchModeEquals;
	}

	public void setSearchModeEquals(boolean searchModeEquals) {
		this.searchModeEquals = searchModeEquals;
	}
	
}
