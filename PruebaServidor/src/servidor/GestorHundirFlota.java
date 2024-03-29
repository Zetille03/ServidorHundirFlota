package servidor;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import info.Partida;

public class GestorHundirFlota {
	// TODO cambiar lista de clientes por hashmap
	public List<ClienteHandler> clientes = new ArrayList<>();
	// Hashmap con objeto clase ClienteHandler almacenado
	public HashMap<String, ClienteHandler> clientesConectadosObjetos = new HashMap<>();
	// Hashmap con id usuario;
	public HashMap<String, Integer> clientesConectadosID = new HashMap<>();
	// Hashmap de los jugadores esperando para jugar
	public HashMap<Integer, String> clientesEsperandoParaJugar = new HashMap<>();
	// ArrayList con las partidas
	public ArrayList<Partida> partidas = new ArrayList<Partida>();
	// Informacion Registro Base de datos
	private String nameDatabase = "hundirflota";
	private String user = "root";
	private String pass = "MANAGER";

	public synchronized void bienvenidaServidor(String mensaje, ClienteHandler remitente) {
		for (Map.Entry<String,ClienteHandler> entry : clientesConectadosObjetos.entrySet()) {
			ClienteHandler cliente = entry.getValue();
			if (cliente != remitente) {
				cliente.enviarMensaje(mensaje);
				;
			}
		}
	}

	public synchronized void menzajepatos(String mensaje, ClienteHandler remitente) {
		for (Map.Entry<String,ClienteHandler> entry : clientesConectadosObjetos.entrySet()) {
			ClienteHandler cliente = entry.getValue();
			if (cliente != remitente) {
				cliente.enviarMensaje(remitente.getNombre() + ": " + mensaje);
			}
		}
	}

	public synchronized void enviarMensajeCliente(String mensaje, ClienteHandler remitente) {
		for (ClienteHandler cliente : clientes) {
			if (cliente == remitente) {
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
			ResultSet resultSet = statement.executeQuery("SELECT id_usuario FROM usuarios WHERE nombre_usuario = '"
					+ nombreUsuario + "' " + "AND contra_usuario = '" + passwordUsuario + "';");
			int idUsuario;
			if (resultSet.next()) {
				idUsuario = resultSet.getInt("id_usuario");
				return String.valueOf(idUsuario);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public synchronized int insertarRowPartida(int idUsuario1, int idUsuario2) {
		Connection conexion = null;
		try {
			conexion = DriverManager.getConnection("jdbc:mysql://localhost:3306/" + nameDatabase, user, pass);
			Statement statement = conexion.createStatement();
			LocalDateTime fechaActual = generarFechaActual();
			PreparedStatement preparedStatement = conexion.prepareStatement("INSERT INTO partidas (id_jugador1,id_jugador2,fecha_creacion) VALUES " + "("
					+ idUsuario1 + "," + idUsuario2 + ",?);");
			preparedStatement.setObject(1, fechaActual);
			int filasAfectadas = preparedStatement.executeUpdate();
			ResultSet resultSet = statement.executeQuery("SELECT id_partida FROM partidas WHERE (id_jugador1 = "
					+ idUsuario1 + " AND id_jugador2 = " + idUsuario2 + ") OR (id_jugador1 = "+ idUsuario1 + " AND id_jugador2 = " + idUsuario2 + ")"
							+ "ORDER BY id_partida DESC LIMIT 1;");
			if (resultSet.next()) {
				return resultSet.getInt("id_partida");
			}
		} catch (Exception e) {
			if (e instanceof SQLException) {
				System.out.println("error base de datos");
				e.printStackTrace();
			}
		}
		return 0;
	}

	public synchronized void actualizarDisparo(String casillaDisparo, int usuarioEjecutor, int id_partida) {
		Connection conexion = null;
		try {
			conexion = DriverManager.getConnection("jdbc:mysql://localhost:3306/" + nameDatabase, user, pass);
			Statement statement = conexion.createStatement();
			ResultSet resultSet = statement
					.executeQuery("SELECT disparos FROM partidas WHERE id_partida = " + id_partida + ";");
			if (resultSet.next()) {
				String disparos = resultSet.getString("disparos");
				if (disparos == null) {
					disparos = usuarioEjecutor + "," + casillaDisparo;
				} else {
					disparos = resultSet.getString("disparos");
					disparos = disparos + ";" + usuarioEjecutor + "," + casillaDisparo;

				}
				int filas = statement.executeUpdate(
						"UPDATE partidas SET disparos= '" + disparos + "' WHERE id_partida = " + id_partida + ";");
			} else {
				System.out.println("no tuvo resultado la busqueda del disparo");
			}

		} catch (Exception e) {
			if (e instanceof SQLException) {
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
		} catch (Exception e) {
			if (e instanceof SQLException) {
				System.out.println("error base de datos");
				e.printStackTrace();
			}
		}
	}

	public synchronized void insertarGanadorPartida(int id_partida, int id_ganador) {
		Connection conexion = null;
		try {
			conexion = DriverManager.getConnection("jdbc:mysql://localhost:3306/" + nameDatabase, user, pass);
			Statement statement = conexion.createStatement();
			statement.executeUpdate("UPDATE partidas SET id_ganador = " + id_ganador + ", terminada = true"
					+ " WHERE id_partida = " + id_partida + ";");
		} catch (Exception e) {
			if (e instanceof SQLException) {
				System.out.println("error base de datos");
				e.printStackTrace();
			}
		}
	}

	public synchronized boolean comprobacionConexionMultiple(String nombreUsuario) {
		if (clientesConectadosObjetos.containsKey(nombreUsuario)) {
			return true;
		}
		return false;
	}
	
	public synchronized void insercionColocacionBarcos(int id_partida, int id_jugador, String arrayBarcosString) {
		Connection conexion = null;
		try {
			conexion = DriverManager.getConnection("jdbc:mysql://localhost:3306/" + nameDatabase, user, pass);
			Statement statement = conexion.createStatement();
			statement.executeUpdate("INSERT INTO barcos (id_partida,id_jugador,colocacion_barcos) VALUES ("+id_partida+","+id_jugador+",'"+arrayBarcosString+"')");
		} catch (Exception e) {
			if (e instanceof SQLException) {
				System.out.println("error base de datos");
				e.printStackTrace();
			}
		}
	}
	
	public synchronized String seleccionarPartidasTerminadas() {
		String partidas = "";
		Connection conexion = null;
		try {
			conexion = DriverManager.getConnection("jdbc:mysql://localhost:3306/" + nameDatabase, user, pass);
			Statement statement = conexion.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			ResultSet resultSet = statement.executeQuery("SELECT id_partida,id_jugador1,id_jugador2 FROM partidas WHERE terminada = true;");
			resultSet.last();
			int nResultados = resultSet.getRow();
			if(nResultados!=0) {
				resultSet.beforeFirst();
				for(int i = 0;i<nResultados;i++) {
					if(resultSet.next()) {
						int id_partida = resultSet.getInt("id_partida");
						int id_jugador1 = resultSet.getInt("id_jugador1");
						int id_jugador2 = resultSet.getInt("id_jugador2");
						partidas+= id_partida+","+id_jugador1+","+id_jugador2;
						if(i!=nResultados-1) {
							partidas += ";";
						}
					}
				}
			}
			return partidas;
		} catch (Exception e) {
			if (e instanceof SQLException) {
				System.out.println("error base de datos");
				e.printStackTrace();
			}
		}
		return partidas;
	}
	
	public synchronized String seleccionarPartidaTerminada(int eleccion) {
		String datosPartida = "";
		int id_jugador1 = 0;
		int id_jugador2=0;
		int id_ganador = 0;
		String ganador = "";
		Connection conexion = null;
		try {
			conexion = DriverManager.getConnection("jdbc:mysql://localhost:3306/" + nameDatabase, user, pass);
			Statement statement = conexion.createStatement();
			ResultSet resultSet =  statement.executeQuery("SELECT id_jugador1,id_jugador2,id_ganador,disparos FROM partidas WHERE id_partida = "+eleccion+";");
			if(resultSet.next()) {
				id_jugador1 = resultSet.getInt("id_jugador1");
				id_jugador2 = resultSet.getInt("id_jugador2");
				id_ganador = resultSet.getInt("id_ganador");
				datosPartida+=resultSet.getString("disparos");
			}
			resultSet = statement.executeQuery("SELECT colocacion_barcos FROM barcos WHERE id_partida = "+eleccion+" AND id_jugador = "+id_jugador1+";");
			if(resultSet.next()) {
				datosPartida+="@"+resultSet.getString("colocacion_barcos");
			}
			resultSet = statement.executeQuery("SELECT colocacion_barcos FROM barcos WHERE id_partida = "+eleccion+" AND id_jugador = "+id_jugador2+";");
			if(resultSet.next()) {
				datosPartida+="@"+resultSet.getString("colocacion_barcos");
			}
			resultSet = statement.executeQuery("SELECT nombre_usuario FROM usuarios WHERE id_usuario = "+id_jugador1+";");
			if(resultSet.next()) {
				datosPartida+="@"+resultSet.getString("nombre_usuario");
			}
			resultSet = statement.executeQuery("SELECT nombre_usuario FROM usuarios WHERE id_usuario = "+id_jugador2+";");
			if(resultSet.next()) {
				datosPartida+="@"+resultSet.getString("nombre_usuario");
			}
			resultSet = statement.executeQuery("SELECT nombre_usuario FROM usuarios WHERE id_usuario = "+id_ganador+";");
			if(resultSet.next()) {
				 ganador = resultSet.getString("nombre_usuario");
			}
			datosPartida+="@"+id_jugador1+"@"+id_jugador2+"@"+eleccion+"@"+ganador;
			return datosPartida;
		} catch (Exception e) {
			if (e instanceof SQLException) {
				System.out.println("error seleccion de partida terminada");
				e.printStackTrace();
			}
		}
		return datosPartida;
	}
	
	public synchronized LocalDateTime generarFechaActual() {
		LocalDateTime fechaHoraActual = LocalDateTime.now();

        LocalDateTime fechaHoraConDiasYHoras = fechaHoraActual.plusDays(3)
                                                              .plusHours(4)
                                                              .plusMinutes(30)
                                                              .plusSeconds(15);

        return fechaHoraConDiasYHoras;
	}
}
