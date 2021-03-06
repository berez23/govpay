package it.govpay.core.dao.pagamenti;

import java.util.ArrayList;
import java.util.List;

import org.openspcoop2.generic_project.exception.NotFoundException;
import org.openspcoop2.generic_project.exception.ServiceException;
import org.openspcoop2.utils.service.context.ContextThreadLocal;

import it.govpay.bd.BasicBD;
import it.govpay.bd.model.Notifica;
import it.govpay.bd.pagamento.NotificheBD;
import it.govpay.bd.pagamento.filters.NotificaFilter;
import it.govpay.core.dao.commons.BaseDAO;
import it.govpay.core.dao.pagamenti.dto.ListaNotificheDTO;
import it.govpay.core.dao.pagamenti.dto.ListaNotificheDTOResponse;
import it.govpay.core.exceptions.NotAuthenticatedException;
import it.govpay.core.exceptions.NotAuthorizedException;

public class NotificheDAO extends BaseDAO{


	public ListaNotificheDTOResponse listaNotifiche(ListaNotificheDTO listaNotificheDTO) throws ServiceException, NotAuthorizedException, NotAuthenticatedException, NotFoundException{ 
		BasicBD bd = null;

		try {
			bd = BasicBD.newInstance(ContextThreadLocal.get().getTransactionId());
			NotificheBD notificheBD = new NotificheBD(bd);
			NotificaFilter filter = notificheBD.newFilter();
			
			filter.setOffset(listaNotificheDTO.getOffset());
			filter.setLimit(listaNotificheDTO.getLimit());
			filter.setDataInizio(listaNotificheDTO.getDataDa());
			filter.setDataFine(listaNotificheDTO.getDataA());
			
			if(listaNotificheDTO.getStato() != null) {
				try {
					it.govpay.model.Notifica.StatoSpedizione statoSpedizione = listaNotificheDTO.getStato();
					filter.setStato(statoSpedizione.toString());
				} catch(Exception e) {
					return new ListaNotificheDTOResponse(0, new ArrayList<>());
				}
			}
			
			if(listaNotificheDTO.getTipo() != null) {
				try {
					it.govpay.model.Notifica.TipoNotifica tipoNotifica =  listaNotificheDTO.getTipo();
					filter.setTipo(tipoNotifica.toString());
				} catch(Exception e) {
					return new ListaNotificheDTOResponse(0, new ArrayList<>());
				}
			}
			
			long count = notificheBD.count(filter);

			if(count > 0) {
				List<Notifica> lst = notificheBD.findAll(filter);
				
				for (Notifica notifica : lst) {
					this.populateNotifica(notifica, bd);
				}
				
				return new ListaNotificheDTOResponse(count, lst);
			} else {
				return new ListaNotificheDTOResponse(count, new ArrayList<>());
			}
		}finally {
			if(bd != null)
				bd.closeConnection();
		}
	}
	
	private void populateNotifica(Notifica notifica, BasicBD bd) throws ServiceException, NotFoundException {
		notifica.getApplicazione(bd);
	}
}
