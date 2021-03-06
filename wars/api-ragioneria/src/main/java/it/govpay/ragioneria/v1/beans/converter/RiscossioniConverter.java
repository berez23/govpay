package it.govpay.ragioneria.v1.beans.converter;

import java.math.BigDecimal;

import org.apache.commons.codec.binary.Base64;
import org.openspcoop2.generic_project.exception.NotFoundException;
import org.openspcoop2.generic_project.exception.ServiceException;
import org.openspcoop2.utils.LoggerWrapperFactory;

import it.govpay.bd.model.Pagamento;
import it.govpay.ragioneria.v1.beans.Allegato;
import it.govpay.ragioneria.v1.beans.Allegato.TipoEnum;
import it.govpay.core.utils.UriBuilderUtils;
import it.govpay.ragioneria.v1.beans.Riscossione;
import it.govpay.ragioneria.v1.beans.RiscossioneIndex;
import it.govpay.ragioneria.v1.beans.StatoRiscossione;
import it.govpay.ragioneria.v1.beans.TipoRiscossione;
import it.govpay.model.Pagamento.Stato;
import it.govpay.model.Pagamento.TipoPagamento;
import it.govpay.rs.BaseRsService;

public class RiscossioniConverter {
	
	public static Riscossione toRsModel(Pagamento input) throws NotFoundException {
		Riscossione rsModel = new Riscossione();
		try {
			rsModel.setIdDominio(input.getCodDominio());
			rsModel.setIuv(input.getIuv());
			rsModel.setIur(input.getIur());
			rsModel.setIndice(new BigDecimal(input.getIndiceDati()));
			
			rsModel.setImporto(input.getImportoPagato());
			rsModel.setData(input.getDataPagamento());
			Stato stato = input.getStato();
			switch(stato) {
			case INCASSATO: rsModel.setStato(StatoRiscossione.INCASSATA);
				break;
			case PAGATO: rsModel.setStato(StatoRiscossione.RISCOSSA);
				break;
			case PAGATO_SENZA_RPT: rsModel.setStato(StatoRiscossione.RISCOSSA);
				break;
			default:
				break;
			}
			
			if(input.getTipo().equals(TipoPagamento.ENTRATA)) {
				rsModel.setTipo(TipoRiscossione.ENTRATA);
			} else {
				rsModel.setTipo(TipoRiscossione.MBT);
			} 
			
			rsModel.setCommissioni(input.getCommissioniPsp());
			
			if(input.getAllegato() != null) {
	
				Allegato allegato = new Allegato();
				allegato.setTesto(Base64.encodeBase64String(input.getAllegato()));
				if(input.getTipoAllegato() != null)
					allegato.setTipo(TipoEnum.fromValue(input.getTipoAllegato().toString()));
				rsModel.setAllegato(allegato);
			}			
			
			rsModel.setPendenza(PendenzeConverter.toRsIndexModel(input.getSingoloVersamento(null).getVersamento(null)));
			rsModel.setVocePendenza(PendenzeConverter.toRsModelVocePendenza(input.getSingoloVersamento(null), input.getIndiceDati()));
			if(input.getRpt(null) != null)
				rsModel.setRpp(RptConverter.toRsModelIndex(input.getRpt(null), input.getSingoloVersamento(null).getVersamento(null), input.getSingoloVersamento(null).getVersamento(null).getApplicazione(null)));
			
			if(input.getIncasso(null) != null)
				rsModel.setIncasso(IncassiConverter.toRsIndexModel(input.getIncasso(null)));

		} catch(ServiceException e) {
			LoggerWrapperFactory.getLogger(BaseRsService.class).error("Errore nella conversione del pagamento: " + e.getMessage(), e);
		}

		return rsModel;
	}
	
	public static RiscossioneIndex toRsModelIndex(Pagamento input) throws NotFoundException {
		RiscossioneIndex rsModel = new RiscossioneIndex();
		try {
			rsModel.setIdDominio(input.getCodDominio());
			rsModel.setIuv(input.getIuv());
			rsModel.setIur(input.getIur());
			rsModel.setIndice(new BigDecimal(input.getIndiceDati()));
			
			rsModel.setImporto(input.getImportoPagato());
			rsModel.setData(input.getDataPagamento());
			Stato stato = input.getStato();
			switch(stato) {
			case INCASSATO: rsModel.setStato(StatoRiscossione.INCASSATA);
				break;
			case PAGATO: rsModel.setStato(StatoRiscossione.RISCOSSA);
				break;
			case PAGATO_SENZA_RPT: rsModel.setStato(StatoRiscossione.RISCOSSA);
				break;
			default:
				break;
			}
			
			if(input.getTipo().equals(TipoPagamento.ENTRATA)) {
				rsModel.setTipo(TipoRiscossione.ENTRATA);
			} else {
				rsModel.setTipo(TipoRiscossione.MBT);
			} 
			
			rsModel.setCommissioni(input.getCommissioniPsp());
			if(input.getAllegato() != null) {
				Allegato allegato = new Allegato();
				allegato.setTesto(Base64.encodeBase64String(input.getAllegato()));
				if(input.getTipoAllegato() != null)
					allegato.setTipo(TipoEnum.fromValue(input.getTipoAllegato().toString()));
				rsModel.setAllegato(allegato);
			}
			
			rsModel.setCommissioni(input.getCommissioniPsp());
			
			rsModel.setPendenza(UriBuilderUtils.getPendenzaByIdA2AIdPendenza(input.getSingoloVersamento(null).getVersamento(null).getApplicazione(null).getCodApplicazione(), input.getSingoloVersamento(null).getVersamento(null).getCodVersamentoEnte()));
			rsModel.setRpp(UriBuilderUtils.getRppByDominioIuvCcp(input.getRpt(null).getCodDominio(), input.getRpt(null).getIuv(), input.getRpt(null).getCcp()));
			if(input.getIncasso(null)!=null)
				rsModel.setIncasso(UriBuilderUtils.getIncassiByIdDominioIdIncasso(input.getCodDominio(), input.getIncasso(null).getTrn()));
			
		} catch(ServiceException e) {}

		return rsModel;
	}
	
}
