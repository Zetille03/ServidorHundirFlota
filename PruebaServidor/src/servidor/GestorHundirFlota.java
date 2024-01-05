package servidor;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import infocompartida.Partida;


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
    
    public synchronized String insertarRowPartida(int idUsuario1,int idUsuario2) {
    	Connection conexion = null;
    	try {
    		conexion = DriverManager.getConnection("jdbc:mysql://localhost:3306/" + nameDatabase, user, pass);
    		Statement statement = conexion.createStatement();
    		int filasAfectadas = statement.executeUpdate("INSERT INTO partidas (id_jugador1,id_jugador2) VALUES "
    				+ "("+idUsuario1+","+idUsuario2+");");
    		ResultSet resultSet = statement.executeQuery("SELECT id_partida FROM partidas WHERE id_jugador1 = '"+idUsuario1+"' AND id_jugador2 = '"+idUsuario2+"';");
    		if(resultSet.next()) {
    			return String.valueOf(resultSet.getInt("id_partida"));
    		}
    	}catch(Exception e) {
			if(e instanceof SQLException) {
				System.out.println("error base de datos");
			}
		}
    	return "";
    }
    
    public synchronized void actualizarDisparo(String casillaDisparo,int usuarioEjecutor,int id_partida) {
    	Connection conexion = null;
    	try {
    		conexion = DriverManager.getConnection("jdbc:mysql://localhost:3306/" + nameDatabase, user, pass);
    		Statement statement = conexion.createStatement();
    		ResultSet resultSet = statement.executeQuery("SELECT disparos FROM partidas WHERE id_partida = "+id_partida+";");
    		if(resultSet.next()) {
    			String disparos = resultSet.getString("disparos");
        		if(disparos==null) {
        			disparos = usuarioEjecutor+","+casillaDisparo;
        		}else {
        			disparos = resultSet.getString("disparos");
        			disparos = disparos+";"+usuarioEjecutor+","+casillaDisparo;
        			
        		}
        		int filas = statement.executeUpdate("UPDATE partidas SET disparos= '"+disparos+"' WHERE id_partida = "+id_partida+";");
    		}else {
    			System.out.println("no tuvo resultado la busqueda del disparo");
    		}
    		
    	}catch(Exception e) {
			if(e instanceof SQLException) {
				System.out.println("error base de datos");
				e.printStackTrace();
			}
		}
    }
    
    public synchronized void borrarDatosPartidas() {
    	Connection conexion = null;
    	try {
    		conexion = DriverManager.getConnection("jdbc:mysql://localhost:3306/" + nameDatabase, user, pass);
    		Statement statement = conexion.createStatement();
    		statement.executeUpdate("DELETE FROM partidas;");
    		statement.executeUpdate("ALTER TABLE partidas AUTO_INCREMENT = 1;");
    	}catch(Exception e) {
			if(e instanceof SQLException) {
				System.out.println("error base de datos");
				e.printStackTrace();
			}
		}
    }
    
    public synchronized void insertarGanadorPartida(int id_partida,int id_ganador) {
    	Connection conexion = null;
    	try {
    		conexion = DriverManager.getConnection("jdbc:mysql://localhost:3306/" + nameDatabase, user, pass);
    		Statement statement = conexion.createStatement();
    		statement.executeUpdate("DELETE FROM partidas;");
    	}catch(Exception e) {
			if(e instanceof SQLException) {
				System.out.println("error base de datos");
				e.printStackTrace();
			}
		}
    }
    
    public synchronized boolean comprobacionConexionMultiple(String nombreUsuario) {
    	if(clientesConectadosObjetos.containsKey(nombreUsuario)) {
    		return true;
    	}
    	return false;
    }
}
