package it.govpay.stampe.pdf.avvisoPagamento;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;

import org.apache.commons.io.IOUtils;
import org.openspcoop2.utils.LoggerWrapperFactory;
import org.openspcoop2.utils.UtilsException;
import org.slf4j.Logger;

import it.govpay.stampe.model.AvvisoPagamentoInput;
import it.govpay.stampe.pdf.avvisoPagamento.utils.AvvisoPagamentoProperties;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRXmlDataSource;
import net.sf.jasperreports.engine.util.JRLoader;

public class AvvisoPagamentoPdf {

	private static AvvisoPagamentoPdf _instance = null;
	private static JAXBContext jaxbContext = null;
	private static byte[] templateAvviso = null;
	private static byte[] templateMonoBand = null;
	private static byte[] templateTriBand = null;
	private static byte[] templateRataUnica = null;
	private static byte[] templateDoppiaRata = null;
	private static byte[] templateTriplaRata = null;
	private static byte[] templateDoppioFormato = null;
	private static byte[] templateBollettinoRata = null;
	private static byte[] templateTriploFormato = null;
	private static byte[] templateBollettinoTriRata = null;
	
	public static AvvisoPagamentoPdf getInstance() {
		if(_instance == null)
			init();

		return _instance;
	}

	public static synchronized void init() {
		if(_instance == null)
			_instance = new AvvisoPagamentoPdf();
		
		if(jaxbContext == null) {
			try {
				jaxbContext = JAXBContext.newInstance(AvvisoPagamentoInput.class);
			} catch (JAXBException e) {
				LoggerWrapperFactory.getLogger(AvvisoPagamentoPdf.class).error("Errore durtante l'inizializzazione JAXB", e); 
			}
		}
	}

	public AvvisoPagamentoPdf() {
		try {
			jaxbContext = JAXBContext.newInstance(AvvisoPagamentoInput.class);
		} catch (JAXBException e) {
			LoggerWrapperFactory.getLogger(AvvisoPagamentoPdf.class).error("Errore durtante l'inizializzazione JAXB", e); 
		}
		
		try {
			templateAvviso = IOUtils.toByteArray(AvvisoPagamentoPdf.class.getResourceAsStream(AvvisoPagamentoCostanti.AVVISO_PAGAMENTO_TEMPLATE_JASPER));
			templateMonoBand = IOUtils.toByteArray(AvvisoPagamentoPdf.class.getResourceAsStream(AvvisoPagamentoCostanti.MONOBAND_TEMPLATE_JASPER));
			templateTriBand = IOUtils.toByteArray(AvvisoPagamentoPdf.class.getResourceAsStream(AvvisoPagamentoCostanti.TRIBAND_TEMPLATE_JASPER));
			templateRataUnica = IOUtils.toByteArray(AvvisoPagamentoPdf.class.getResourceAsStream(AvvisoPagamentoCostanti.RATAUNICA_TEMPLATE_JASPER));
			templateDoppiaRata = IOUtils.toByteArray(AvvisoPagamentoPdf.class.getResourceAsStream(AvvisoPagamentoCostanti.RATADOPPIA_TEMPLATE_JASPER));
			templateTriplaRata = IOUtils.toByteArray(AvvisoPagamentoPdf.class.getResourceAsStream(AvvisoPagamentoCostanti.RATATRIPLA_TEMPLATE_JASPER));
			templateDoppioFormato = IOUtils.toByteArray(AvvisoPagamentoPdf.class.getResourceAsStream(AvvisoPagamentoCostanti.DOPPIOFORMATO_TEMPLATE_JASPER));
			templateBollettinoRata = IOUtils.toByteArray(AvvisoPagamentoPdf.class.getResourceAsStream(AvvisoPagamentoCostanti.BOLLETTINORATA_TEMPLATE_JASPER));
			templateTriploFormato = IOUtils.toByteArray(AvvisoPagamentoPdf.class.getResourceAsStream(AvvisoPagamentoCostanti.TRIPLOFORMATO_TEMPLATE_JASPER));
			templateBollettinoTriRata = IOUtils.toByteArray(AvvisoPagamentoPdf.class.getResourceAsStream(AvvisoPagamentoCostanti.BOLLETTINOTRIRATA_TEMPLATE_JASPER));
		} catch (IOException e) {
			LoggerWrapperFactory.getLogger(AvvisoPagamentoPdf.class).error("Errore durante la lettura del template jasper dell'Avviso di Pagamento", e); 
		}
		
	}


	public JasperPrint creaJasperPrintAvviso(Logger log, AvvisoPagamentoInput input, Properties propertiesAvvisoPerDominio, InputStream jasperTemplateInputStream,JRDataSource dataSource,Map<String, Object> parameters) throws Exception {
		JasperReport jasperReport = (JasperReport) JRLoader.loadObject(jasperTemplateInputStream);
		JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
		return jasperPrint;
	}

	public byte[] creaAvviso(Logger log, AvvisoPagamentoInput input, String codDominio, AvvisoPagamentoProperties avProperties) throws Exception {
		// cerco file di properties esterni per configurazioni specifiche per dominio
		Properties propertiesAvvisoPerDominio = avProperties.getPropertiesPerDominio(codDominio, log);

		this.caricaLoghiAvviso(input, propertiesAvvisoPerDominio);

		Map<String, Object> parameters = new HashMap<String, Object>();
		
		parameters.put("MonoBand", new ByteArrayInputStream(templateMonoBand));
		parameters.put("TriBand", new ByteArrayInputStream(templateTriBand));
		parameters.put("RataUnica", new ByteArrayInputStream(templateRataUnica));
		parameters.put("DoppiaRata", new ByteArrayInputStream(templateDoppiaRata));
		parameters.put("TriplaRata", new ByteArrayInputStream(templateTriplaRata));
		parameters.put("DoppioFormato", new ByteArrayInputStream(templateDoppioFormato));
		parameters.put("BollettinoRata", new ByteArrayInputStream(templateBollettinoRata));
		parameters.put("TriploFormato", new ByteArrayInputStream(templateTriploFormato));
		parameters.put("BollettinoTriRata", new ByteArrayInputStream(templateBollettinoTriRata));
		
		JRDataSource dataSource = this.creaXmlDataSource(log,input);
		JasperPrint jasperPrint = this.creaJasperPrintAvviso(log, input, propertiesAvvisoPerDominio, new ByteArrayInputStream(templateAvviso), dataSource, parameters);

		return JasperExportManager.exportReportToPdf(jasperPrint);
	}

	public JRDataSource creaXmlDataSource(Logger log,AvvisoPagamentoInput input) throws UtilsException, JRException, JAXBException {
//		WriteToSerializerType serType = WriteToSerializerType.XML_JAXB;
		Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
		jaxbMarshaller.setProperty("com.sun.xml.bind.xmlDeclaration", Boolean.FALSE);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		JAXBElement<AvvisoPagamentoInput> jaxbElement = new JAXBElement<AvvisoPagamentoInput>(new QName("", AvvisoPagamentoCostanti.AVVISO_PAGAMENTO_ROOT_ELEMENT_NAME), AvvisoPagamentoInput.class, null, input);
		jaxbMarshaller.marshal(jaxbElement, baos);
		JRDataSource dataSource = new JRXmlDataSource(new ByteArrayInputStream(baos.toByteArray()),AvvisoPagamentoCostanti.AVVISO_PAGAMENTO_ROOT_ELEMENT_NAME);
		return dataSource;
	}

	public void caricaLoghiAvviso(AvvisoPagamentoInput input, Properties propertiesAvvisoPerDominio) {
		// valorizzo la sezione loghi
		if(input.getLogoEnte() == null)
			input.setLogoEnte(propertiesAvvisoPerDominio.getProperty(AvvisoPagamentoCostanti.LOGO_ENTE));
	}
	
}
