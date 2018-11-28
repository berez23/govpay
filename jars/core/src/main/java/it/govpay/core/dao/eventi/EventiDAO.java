package it.govpay.core.dao.eventi;

import java.util.ArrayList;
import java.util.List;

import org.openspcoop2.generic_project.exception.NotFoundException;
import org.openspcoop2.generic_project.exception.ServiceException;

import it.govpay.bd.BasicBD;
import it.govpay.bd.anagrafica.AnagraficaManager;
import it.govpay.bd.model.Versamento;
import it.govpay.bd.pagamento.EventiBD;
import it.govpay.bd.pagamento.VersamentiBD;
import it.govpay.bd.pagamento.filters.EventiFilter;
import it.govpay.core.autorizzazione.AuthorizationManager;
import it.govpay.core.dao.commons.BaseDAO;
import it.govpay.core.dao.eventi.dto.ListaEventiDTO;
import it.govpay.core.dao.eventi.dto.ListaEventiDTOResponse;
import it.govpay.core.exceptions.NotAuthenticatedException;
import it.govpay.core.exceptions.NotAuthorizedException;
import it.govpay.core.utils.GpThreadLocal;
import it.govpay.model.Acl.Diritti;
import it.govpay.model.Acl.Servizio;
import it.govpay.bd.model.Evento;

public class EventiDAO extends BaseDAO {

	public ListaEventiDTOResponse listaEventi(ListaEventiDTO listaEventiDTO) throws ServiceException, NotAuthenticatedException, NotAuthorizedException {
		
		BasicBD bd = null;
		
		try {
			bd = BasicBD.newInstance(GpThreadLocal.get().getTransactionId());
			this.autorizzaRichiesta(listaEventiDTO.getUser(), Servizio.GIORNALE_DEGLI_EVENTI, Diritti.LETTURA,bd);
			// Autorizzazione sui domini
			List<String> codDomini = AuthorizationManager.getDominiAutorizzati(listaEventiDTO.getUser(), Servizio.GIORNALE_DEGLI_EVENTI, Diritti.LETTURA);
			if(codDomini == null) {
				throw new NotAuthorizedException("L'utenza autenticata ["+listaEventiDTO.getUser().getPrincipal()+"] non e' autorizzata ai servizi " + Servizio.GIORNALE_DEGLI_EVENTI + " per alcun dominio");
			}
			
			EventiBD eventiBD = new EventiBD(bd);
			EventiFilter filter = eventiBD.newFilter();
			
			if(codDomini != null && codDomini.size() > 0)
				filter.setCodDomini(codDomini);

			filter.setOffset(listaEventiDTO.getOffset());
			filter.setLimit(listaEventiDTO.getLimit());
			
			filter.setCodDominio(listaEventiDTO.getIdDominio());
			filter.setIuv(listaEventiDTO.getIuv());
			
			if(listaEventiDTO.getIdA2A()!=null && listaEventiDTO.getIdPendenza() != null) {
				VersamentiBD versamentiBD = new VersamentiBD(bd);
				Versamento versamento = versamentiBD.getVersamento(AnagraficaManager.getApplicazione(bd, listaEventiDTO.getIdA2A()).getId(), listaEventiDTO.getIdPendenza());
				
				filter.setCodDominio(versamento.getUo(bd).getDominio(bd).getCodDominio());
				filter.setIuv(versamento.getIuvVersamento());
				
			} else {
				filter.setCodApplicazione(listaEventiDTO.getIdA2A());
				filter.setCodVersamentoEnte(listaEventiDTO.getIdPendenza());
			}
			
			filter.setFilterSortList(listaEventiDTO.getFieldSortList());

			long count = eventiBD.count(filter);

			List<Evento> resList = new ArrayList<>();
			if(count > 0) {
				resList = eventiBD.findAll(filter);
			} 

			return new ListaEventiDTOResponse(count, resList);
		} catch (NotFoundException e) {
			throw new ServiceException(e);
		} finally {
			if(bd != null)
				bd.closeConnection();
		}

	}

}
