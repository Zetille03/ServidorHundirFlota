package servidor;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Random;

import infocompartida.Barco;
import infocompartida.Boton;
import infocompartida.Partida;

public class ClienteHandler implements Runnable {
	private Socket socketCliente;
	//Informacion usuario
	private String nombreUsuario;
	private int id_usuario;
	private ObjectInputStream inputStream;
	private ObjectOutputStream outputStream;
	private GestorHundirFlota gestor;
	private ArrayList<Partida> partidas = new ArrayList<Partida>();
	Random r = new Random();
	
	
	private boolean usuarioDuplicado = false;

	public ClienteHandler(Socket socket, GestorHundirFlota gestor) {
		this.socketCliente = socket;
		this.gestor = gestor;

	}

	public String getNombre() {
		return nombreUsuario;
	}

	public void enviarMensaje(String mensaje) {
		try {
			outputStream.writeObject(mensaje);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		try {
			outputStream = new ObjectOutputStream(socketCliente.getOutputStream());
			inputStream = new ObjectInputStream(socketCliente.getInputStream());

			String mensaje;
			String[] mensajeArray;
			mensaje = (String) inputStream.readObject();
			mensajeArray = mensaje.split("@");
			mensaje = this.gestor.comprobacionDatosUsuario(mensajeArray[1], mensajeArray[2]);
			if (mensaje == null) {
				this.gestor.enviarMensajeCliente("L@0", this);
			} else {
				nombreUsuario = mensajeArray[1];
				if (this.gestor.comprobacionConexionMultiple(nombreUsuario) == true) {
					usuarioDuplicado = true;
					this.gestor.enviarMensajeCliente("L@R", this);
				} else {
					this.gestor.clientesConectadosObjetos.put(nombreUsuario, this);
					this.gestor.clientesConectadosID.put(nombreUsuario, Integer.parseInt(mensaje));
					this.id_usuario = Integer.parseInt(mensaje);
					this.gestor.bienvenidaServidor(nombreUsuario + " se ha unido al chat.", this);
					this.gestor.enviarMensajeCliente("L@1@" + mensaje, this);
				}
			}

			do {
				mensaje = null;
				mensajeArray = null;
				mensaje = (String) inputStream.readObject();
				mensajeArray = mensaje.split("@");
				if (mensaje.equals("bye")) {
					throw new IOException("Cliente se desconecto");
				} else {
					switch (mensajeArray[0]) {
					//Mensajes relacionados con el inicio de partida
					case "P":
						switch(mensajeArray[1]) {
						//Mensaje de inicio de partida
						case "N":
							String hashmapString = this.gestor.clientesEsperandoParaJugar.toString();
							this.gestor.enviarMensajeCliente("P@H@"+hashmapString, this);
							this.gestor.clientesEsperandoParaJugar.put(id_usuario, nombreUsuario);
							break;
						//Mensaje de eleccion de contrincante
						case "C":
							String nombreContrincante = this.gestor.clientesEsperandoParaJugar.get(Integer.parseInt(mensajeArray[2]));
							int idContrincante = this.gestor.clientesConectadosID.get(nombreContrincante);
							int idPartida = this.gestor.insertarRowPartida(id_usuario, idContrincante);
							this.gestor.clientesEsperandoParaJugar.remove(Integer.parseInt(mensajeArray[2]));
							this.gestor.clientesEsperandoParaJugar.remove(id_usuario);
							
							
							//Elección aleatoria de quien empieza la partida
							int eleccionContrincante = r.nextInt(2);
							if(eleccionContrincante==1) {
								this.gestor.clientesConectadosObjetos.get(nombreContrincante).outputStream.writeObject("P@E@"+nombreUsuario+"@"+idPartida+"@true");
								this.gestor.clientesConectadosObjetos.get(this.nombreUsuario).outputStream.writeObject("P@E@"+nombreContrincante+"@"+idPartida+"@false");
							}else {
								this.gestor.clientesConectadosObjetos.get(nombreContrincante).outputStream.writeObject("P@E@"+nombreUsuario+"@"+idPartida+"@false");
								this.gestor.clientesConectadosObjetos.get(this.nombreUsuario).outputStream.writeObject("P@E@"+nombreContrincante+"@"+idPartida+"@true");
							}
							
							break;
						//Mensaje de colocación con la posición de los barcos
						case "CO":
							String nombreContricante = mensajeArray[2];
							ArrayList<Barco> arrayBarcos = (ArrayList<Barco>) inputStream.readObject();
							this.gestor.insercionColocacionBarcos(Integer.valueOf(mensajeArray[3]), id_usuario, Barco.pasarArrayBarcosAString(arrayBarcos));
							this.gestor.clientesConectadosObjetos.get(nombreContricante).outputStream.writeObject("P@T");
							this.gestor.clientesConectadosObjetos.get(nombreContricante).outputStream.writeObject(arrayBarcos);
							break;
						}
						break;
					//Mensajes relacionados con los disparos
					case "D":
						switch(mensajeArray[1]) {
						//Mensaje con la casilla disparada y a quién enviarselo
						case "D":
							this.gestor.actualizarDisparo(mensajeArray[3], id_usuario, Integer.parseInt(mensajeArray[2]));
							this.gestor.clientesConectadosObjetos.get(mensajeArray[4]).outputStream.writeObject("D@R@"+mensajeArray[3]);
							break;
						//Mensaje que envia el ganador para notificar al perdedor y cambiar en la base de 
						//de datos el id del ganador y el estado del booleano "terminado"
						case "W":
							this.gestor.insertarGanadorPartida(Integer.parseInt(mensajeArray[3]), id_usuario);
							this.gestor.clientesConectadosObjetos.get(mensajeArray[2]).outputStream.writeObject("D@P");
						}
						
						break;
					default:
                    	 System.out.println(mensaje);
						break;
					}
				}
			} while (!mensaje.equals("bye"));

			desconectarCliente();
			socketCliente.close();
		} catch (IOException | ClassNotFoundException e) {
			if (e instanceof SocketException && e.getMessage().equals("Connection reset")) {
				System.out.println("Cliente desconectado forzosamente");
				desconectarCliente();
			} else if (e instanceof IOException && e.getMessage().equals("Cliente se desconecto")) {
				System.out.println("Cliente se desconecto");
				desconectarCliente();
			} else {
				e.printStackTrace();
			}
		}
	}

	public void desconectarCliente() {
		this.gestor.menzajepatos(nombreUsuario + " ha abandonado el chat.", this);
		if (this.gestor.clientesConectadosObjetos.containsKey(nombreUsuario)) {
			this.gestor.clientesConectadosObjetos.remove(nombreUsuario);
			this.gestor.clientesConectadosID.remove(nombreUsuario);
		}
		if(this.gestor.clientesEsperandoParaJugar.containsKey(id_usuario)) {
			this.gestor.clientesEsperandoParaJugar.remove(id_usuario);
		}
		this.gestor.clientes.remove(this);
	}
	
	
	public static void mostrarArray(ArrayList<Barco> arrayBarcos) {
		for(Barco barco: arrayBarcos) {
			System.out.print("Barco: ");
			for(Boton b : barco.getBotonesBarco()) {
				System.out.print(b.getPosicionTablero()+",");
			}
			System.out.println("");
		}
	}
}