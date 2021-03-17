package br.com.cmabreu.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import br.com.cmabreu.services.FederateService;

@RestController
public class FederateController {
	
    @Autowired
    private FederateService federateService;	
	
    // Um exemplo de uso atraves do protocolo REST 
    // aqui voce termina a execucao do Federado e retira ele da Federacao.
    // se ele for o ultimo Federado da Federacao entao ela eh extinta
    @RequestMapping(value = "/quit", method = RequestMethod.GET, produces=MediaType.APPLICATION_JSON_UTF8_VALUE )
	public @ResponseBody String quit() {
    	federateService.quit();
    	return "ok";
	}
	

    // Aqui voce inicia a thread secundaria, onde tem um exemplo de alguma tarefa em paralelo.
    // Lembrando que o este Federado possui 2 threads: a principal em "FederateExecutorThread"
    // e a secundaria em "FederateSampleThread". 
    // Nenhuma delas eh obrigatoria, mas eh prudente colocar tudo o que pode gastar tempo de execucao 
    // dentro de threads para nao travar a execucao do Federado e ele poder continuar respondendo eventos da RTI.
    // Caso contrario a RTI pode pensar que ele morreu e desconectar o Federado.
    // Esta thread secundaria serve mais para exemplificar um segundo processo.
    @RequestMapping(value = "/start", method = RequestMethod.GET, produces=MediaType.APPLICATION_JSON_UTF8_VALUE )
	public @ResponseBody String startCollector( @RequestParam(value = "interval", required = false) Integer interval  ) {
    	
    	if( interval != null ) {
    		federateService.startCollector( interval );
    	} else {
    		federateService.startCollector();
    	}
    	return "ok";
	}
    
    

	
}

