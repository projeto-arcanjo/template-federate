package br.com.cmabreu.threads;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class FederateSampleThread implements Runnable {
    private boolean running = false;
	private Logger logger = LoggerFactory.getLogger( FederateSampleThread.class );
	
	public void finish() {
		logger.info("Thread Secundario Parado");
		this.running = false;
	}
	
    public FederateSampleThread() {
    	logger.info("Thread Secundario Iniciado");
    	this.running = true;
    }  
    
    public void run() {

    	if( !this.running ) {
    		return;
    	}
    	
    	try {
    		logger.info("Estou executando alguma coisa em paralelo...");
    	} catch( Exception se ) {
    		se.printStackTrace();
    		logger.error( se.getMessage() );
    	}
        
    }  
    
		
  
}
