package br.com.cmabreu.threads;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.cmabreu.services.FederateService;

public class FederateExecutorThread implements Runnable {
	private Logger logger = LoggerFactory.getLogger( FederateExecutorThread.class );
	private FederateService federateService;
	private boolean running;
	
	public FederateExecutorThread( FederateService federateService ) {
		this.federateService = federateService;
	}
	
	public void finish() {
		this.running = false;
	}
	
	@Override
	public void run(  ) {
		// Loop principal da simulacao
		logger.info("Executando processo principal");
		this.running = true;
		while( this.running ) {
			// Aqui voce fara coisas relativas ao processo principal da simulacao.
			// Eh aqui que seu Federado "pensa" no que vai fazer... realiza calculos, 
			// movimenta suas instancias e etc.
			federateService.evokeCallBacks();	
		}
		logger.info("Processo principal encerrado");
	}

}
