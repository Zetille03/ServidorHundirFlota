package servidor;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;


public class GestorHundirFlota {
    public List<ClienteHandler> clientes = new ArrayList<>();
    //Informacion Registro Base de datos
    private String nameDatabase = "hundirflota";
	private String user = "root";
	private String pass = "root";
          
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
}
