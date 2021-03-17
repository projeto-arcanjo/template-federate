package br.com.cmabreu.services;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import br.com.cmabreu.FederateAmbassador;
import br.com.cmabreu.misc.EncoderDecoder;
import br.com.cmabreu.threads.FederateExecutorThread;
import br.com.cmabreu.threads.FederateSampleThread;
import hla.rti1516e.AttributeHandleSet;
import hla.rti1516e.CallbackModel;
import hla.rti1516e.ObjectInstanceHandle;
import hla.rti1516e.RTIambassador;
import hla.rti1516e.ResignAction;
import hla.rti1516e.RtiFactoryFactory;
import hla.rti1516e.exceptions.FederatesCurrentlyJoined;
import hla.rti1516e.exceptions.FederationExecutionAlreadyExists;
import hla.rti1516e.exceptions.FederationExecutionDoesNotExist;
import hla.rti1516e.exceptions.NotConnected;
import hla.rti1516e.exceptions.RTIinternalError;

@Service
public class FederateService {
	private RTIambassador rtiamb;
	private FederateAmbassador fedamb;  			
	private Logger logger = LoggerFactory.getLogger( FederateService.class );
	private EncoderDecoder encoder;	
	private Runnable frCollectorThread;
	private Runnable federateExecutorThread;
	private ScheduledFuture<?> scheduled;
	private ScheduledExecutorService scheduler;
	
    @Value("${federation.fomfolder}")
    String fomFolder;	

    @Value("${federation.name}")
    String federationName;	
    
    @Value("${federation.federateName}")
    String federateName;	

    @Value("${frcollector.firstrun}")
    Integer frFirstRun; 
    
    @Value("${frcollector.interval}")
    Integer frInterval; 

    
    @PreDestroy
	public void onExit() {
		logger.info("Encerando Federado...");
		this.quit();
	}
    
    
    
    // O ciclo de vida do Federado comaca aqui.
    // Estes passos sao padronizados e previstos na especificacao da HLA
    // sendo quase que obrigatorios em todo Federado.
    public void startRti() throws Exception {
    	
    	if( !fomFolder.endsWith("/") ) fomFolder = fomFolder + "/";
    	
		/////////////////////////////////////////////////
		// 1 & 2. create the RTIambassador and Connect //
		/////////////////////////////////////////////////    	
		createRtiAmbassador();
		connect();
		
		//////////////////////////////
		// 3. create the federation //
		//////////////////////////////		
		createFederation( federationName );
		
		////////////////////////////
		// 4. join the federation //
		////////////////////////////		
		joinFederation( federationName, federateName);
		

		///////////////////////////////
		// 5. Inicia o Gerenciador //
		///////////////////////////////	
		// Aqui voce devera publicar a classe deste Federado (publish)
		// e seus atributos. Criar o Array de controle das instancias
		// e se inscrever nas classes e atributos de interesse (subscribe)
		
		// ATENCAO: 
		// ESTE FEDERADO NAO PUBLICA NEM SE INSCREVE EM NADA POIS EH UM FEDERADO DE
		// EXEMPLO. TUDO O QUE ELE FAZ EH ENTRAR NA FEDERACAO E MAIS NADA.
		// NAO RESPONDE A NENHUM EVENTO NEM ENVIA NADA PARA A FEDERACAO
		
		
		/////////////////////////////////////
		// 10. do the main simulation loop //
		/////////////////////////////////////
		// Do not block the web browser interface!
		this.federateExecutorThread = new FederateExecutorThread( this );
		new Thread( this.federateExecutorThread ).start();
		
    }


	// Inicia a thread de trabalho do Federado
    // Isso possibilita ao Federado trabalhar em segundo plano
    // sem travar a aplicação
	// Acionado somente pelo Controller REST. Veja em FederateController
    public void startCollector() {
    	// Chama o metodo abaixo (de sobregarga) com o parametro de tempo padrao
    	// do sistema (configurado na variavel de ambiente "FIRST_COLLECT" e "COLLECT_INTERVAL")
    	this.doStartCollector( this.frInterval );
    }
    // Sobrecarga do metodo de inicializacao da thread para aceitar 
    // um parametro de configuracao do intervalo em SEGUNDOS.
    // Modifique o "TimeUnit.SECONDS" abaixo para outras unidades de medida.
    public void startCollector( int interval ) {
    	this.doStartCollector( interval );
    }
    private void doStartCollector( int interval ) {
		this.scheduler = Executors.newSingleThreadScheduledExecutor();
		this.frCollectorThread = new FederateSampleThread( );
        this.scheduled =  this.scheduler.scheduleAtFixedRate( this.frCollectorThread, this.frFirstRun, interval , TimeUnit.SECONDS );
    }
    // ***************************************************

    
	// Encerra a execucao do Federado e rtira ele da Federacao.
    // Acionado pelo FederateController atraves de uma requisicao REST do usuario
    // usando um browser web
    public void quit()  {
	
    	( (FederateExecutorThread)this.federateExecutorThread ).finish();
    	( (FederateSampleThread)this.frCollectorThread ).finish();
    	this.scheduled.cancel( false );
    	
    	
		////////////////////////////////////
		// 12. resign from the federation //
		////////////////////////////////////
		try {
			resignFederationExecution();
			destroyFederation();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    }
    
	public void resignFederationExecution() throws Exception {
		rtiamb.resignFederationExecution( ResignAction.DELETE_OBJECTS );
	}	

	
	// This method now will be fired by a REST endpoint. See FederateController.destroyFederation()
	public void destroyFederation() {
		////////////////////////////////////////
		// 13. try and destroy the federation //
		////////////////////////////////////////
		// NOTE: we won't die if we can't do this because other federates
		//       remain. in that case we'll leave it for them to clean up
		try	{
			rtiamb.destroyFederationExecution( federationName );
			logger.info( "Destroyed Federation" );
		} catch( FederationExecutionDoesNotExist dne ) {
			logger.warn( "No need to destroy federation, it doesn't exist" );
		} catch( FederatesCurrentlyJoined fcj ) 	{
			logger.warn( "Didn't destroy federation, federates still joined" );
		} catch (NotConnected e) {
			logger.error( "Not Connected" );
		} catch (RTIinternalError e) {
			logger.error( e.getMessage() + " : " + e.getCause() );
		}
		
		// disconnect
		try {
			rtiamb.disconnect();
			logger.info( "Disconnected" );
		} catch( Exception e ) {
			logger.error( e.getMessage() + " : " + e.getCause() );
			e.printStackTrace();
		}			
		
	}
    
    
	private void createRtiAmbassador() throws Exception {
		logger.info( "Creating RTIambassador." );
		this.rtiamb = RtiFactoryFactory.getRtiFactory().getRtiAmbassador();
		encoder = new EncoderDecoder();
	}   
	
	
	// Aqui voce fornece o FederateAmbassador para responder aos eventos que a RTI 
	// lhe enviar. Va no FederateAmbassador para preencher os eventos de seu interesse de acordo
	// com o proposito deste Federado.
	// ATUALMENTE NAO HA NADA A FAZER POIS ESTE EH UM FEDERADO DE EXEMPLO E ELE 
	// NAO FAZ ABSOLUTAMENTE NADA ALEM DE ENTRAR NA FEDERACAO 
	private void connect() throws Exception {
		logger.info( "Connecting..." );
		fedamb = new FederateAmbassador( this );
		rtiamb.connect( fedamb, CallbackModel.HLA_IMMEDIATE );
	}	
	
	private void createFederation( String federationName ) throws Exception {
		// We attempt to create a new federation with the first three of the
		// restaurant FOM modules covering processes, food and drink		
		logger.info( "Creating Federation " + federationName );
		try	{
			URL[] modules = new URL[]{
				// The MIM file MUST be present	
				(new File( fomFolder + "HLAstandardMIM.xml")).toURI().toURL(),
				(new File( fomFolder + "RPR_FOM_v2.0_1516-2010.xml")).toURI().toURL(),
				(new File( fomFolder + "NETN-Base_v1.0.2.xml")).toURI().toURL(),
				(new File( fomFolder + "NETN-Aggregate_v1.0.4.xml")).toURI().toURL(),
				(new File( fomFolder + "NETN-Physical_v1.1.2.xml")).toURI().toURL(),
			};
			rtiamb.createFederationExecution( federationName, modules );
			logger.info( "Created Federation. HLA Version " + rtiamb.getHLAversion() );
		} catch( FederationExecutionAlreadyExists exists ) {
			logger.error( "Didn't create federation, it already existed." );
		} catch( MalformedURLException urle )	{
			logger.error( "Exception loading one of the FOM modules from disk: " + urle.getMessage() );
			urle.printStackTrace();
			return;
		}
	}	

	
	private void joinFederation( String federationName, String federateName ) throws Exception  {
		rtiamb.joinFederationExecution( federateName, "ExampleFederateType", federationName );   
		logger.info( "Joined Federation as " + federateName );
	}	

	
	public EncoderDecoder getEncoder() {
		return encoder;
	}
	
	// Gera uma tag contendo o tempo atual.
	// util para controlar os pacotes de dados mas nao eh obrigatorio
	private byte[] generateTag() {
		return ( "" + System.currentTimeMillis()).getBytes();
	}	
	
	public void provideAttributeValueUpdate(ObjectInstanceHandle theObject, AttributeHandleSet theAttributes, byte[] userSuppliedTag) {
		// A RTI esta pedindo meus atributos
	}

	public void evokeCallBacks() {
		// chamar o metodo this.rtiamb.evokeCallback(arg0) se necessario.
		
	}
	
	
}
