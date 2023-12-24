package servidor;

import java.util.ArrayList;
import java.util.List;


public class GestorHundirFlota {
    public List<ClienteHandler> clientes = new ArrayList<>();
          
    public synchronized void bienvenidaServidor(String mensaje, ClienteHandler remitente) {
    	for (ClienteHandler cliente : clientes) {
            if (cliente != remitente) {
                cliente.enviarMensaje(mensaje);
            }
        }
    }
    
    public synchronized void menzajepatos(String mensaje, ClienteHandler remitente) {
        for (ClienteHandler cliente : clientes) {
            if (cliente != remitente) {
                cliente.enviarMensaje(remitente.getNombre() + ": " + mensaje);
            }
        }
    }
    
    public synchronized void desconexionServidor(ClienteHandler remitente) {
    	for(ClienteHandler cliente: clientes) {
    		if(cliente==remitente) {
    			cliente.enviarMensaje("bye");
    			break;
    		}
    	}
    }
}
