package servidor;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class GestorHundirFlota {
	//TODO cambiar lista de clientes por hashmap
    public List<ClienteHandler> clientes = new ArrayList<>();
    //Hashmap con objeto clase ClienteHandler almacenado
    public HashMap<String,ClienteHandler> clientesConectadosObjetos = new HashMap<>();
    //Hashmap con id usuario;
    public HashMap<String,Integer> clientesConectadosID = new HashMap<>();
    //Hashmap de los jugadores esperando para jugar
    public HashMap<Integer,String> clientesEsperandoParaJugar = new HashMap<>();
    //ArrayList con las partidas
    public ArrayList<Partida> partidas = new ArrayList<Partida>();
    //Informacion Registro Base de datos
    private String nameDatabase = "hundirflota";
	private String user = "root";
	private String pass = "root";
          
    public synchronized void bienvenidaServidor(String mensaje, ClienteHandler remitente) {
    	for(Map.Entry entry: clientesConectadosObjetos.entrySet()) {
    		ClienteHandler cliente = (ClienteHandler) entry.getValue();
    		if(cliente != remitente) {
    			cliente.enviarMensaje(mensaje);;
    		}
    	}
//    	for (ClienteHandler cliente : clientes) {
//            if (cliente != remitente) {
//                cliente.enviarMensaje(mensaje);
//            }
//        }
    }
    
    public synchronized void menzajepatos(String mensaje, ClienteHandler remitente) {
    	for(Map.Entry entry: clientesConectadosObjetos.entrySet()) {
    		ClienteHandler cliente = (ClienteHandler) entry.getValue();
    		if(cliente != remitente) {
    			cliente.enviarMensaje(remitente.getNombre() + ": " + mensaje);
    		}
    	}
//        for (ClienteHandler cliente : clientes) {
//            if (cliente != remitente) {
//                cliente.enviarMensaje(remitente.getNombre() + ": " + mensaje);
//            }
//        }
    }
    
    public synchronized void desconexionServidor(ClienteHandler remitente) {
    	for(ClienteHandler cliente: clientes) {
    		if(cliente==remitente) {
    			cliente.enviarMensaje("bye");
    			break;
    		}
    	}
    }
    
    public synchronized void enviarMensajeCliente(String mensaje,ClienteHandler remitente) {
    	for(ClienteHandler cliente: clientes) {
    		if(cliente==remitente) {
    			cliente.enviarMensaje(mensaje);
    			break;
    		}
    	}
    }
    
    public synchronized String comprobacionDatosUsuario(String nombreUsuario, String passwordUsuario) {
    	Connection conexion = null;
    	try {
    		conexion = DriverManager.getConnection("jdbc:mysql://localhost:3306/" + nameDatabase, user, pass);
    		Statement statement = conexion.createStatement();
    		ResultSet resultSet = statement.executeQuery("SELECT id_usuario FROM usuarios WHERE nombre_usuario = '"+nombreUsuario+"' "
    				+ "AND contra_usuario = '"+passwordUsuario+"';");
    		int idUsuario;
    		if(resultSet.next()) {
    			idUsuario = resultSet.getInt("id_usuario");
    			return String.valueOf(idUsuario);
    		}
    		
    	}catch(Exception e) {
			e.printStackTrace();
		}
    	return null;
    }
    
    public synchronized boolean comprobacionConexionMultiple(String nombreUsuario) {
    	if(clientesConectadosObjetos.containsKey(nombreUsuario)) {
    		return true;
    	}
    	return false;
    }
}
