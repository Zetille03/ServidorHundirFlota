package servidor;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;

public class ClienteHandler implements Runnable {
	private Socket socketCliente;
	//Informacion usuario
	private String nombre;
	private int id_usuario;
	private ObjectInputStream inputStream;
	private ObjectOutputStream outputStream;
	private GestorHundirFlota gestor;
	private ArrayList<Partida> partidas = new ArrayList<Partida>();
	
	
	private boolean usuarioDuplicado = false;

	public ClienteHandler(Socket socket, GestorHundirFlota gestor) {
		this.socketCliente = socket;
		this.gestor = gestor;

	}

	public String getNombre() {
		return nombre;
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
				nombre = mensajeArray[1];
				if (this.gestor.comprobacionConexionMultiple(nombre) == true) {
					usuarioDuplicado = true;
					this.gestor.enviarMensajeCliente("L@R", this);
				} else {
					this.gestor.clientesConectadosObjetos.put(nombre, this);
					this.gestor.clientesConectadosID.put(nombre, Integer.parseInt(mensaje));
					this.id_usuario = Integer.parseInt(mensaje);
					this.gestor.bienvenidaServidor(nombre + " se ha unido al chat.", this);
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
					case "P":
						switch(mensajeArray[1]) {
						case "N":
							String hashmapString = this.gestor.clientesEsperandoParaJugar.toString();
							this.gestor.enviarMensajeCliente("P@H@"+hashmapString, this);
							this.gestor.clientesEsperandoParaJugar.put(id_usuario, nombre);
							break;
						case "C":
							this.gestor.clientesEsperandoParaJugar.get(id_usuario);
							break;
						}
						break;
					case "D":
						System.out.println(mensaje);
						break;
					default:
                    	 System.out.println(mensaje);
						break;
					}
				}
			} while (!mensaje.equals("bye"));

			this.gestor.menzajepatos(nombre + " ha abandonado el chat.", this);
			if (this.gestor.clientesConectadosObjetos.containsKey(nombre)) {
				this.gestor.clientesConectadosObjetos.remove(nombre);
				this.gestor.clientesConectadosID.remove(nombre);
			}
			if(this.gestor.clientesEsperandoParaJugar.containsKey(nombre)) {
				this.gestor.clientesEsperandoParaJugar.remove(nombre);
			}
			this.gestor.clientes.remove(this);
			socketCliente.close();
		} catch (IOException | ClassNotFoundException e) {
			if (e instanceof SocketException && e.getMessage().equals("Connection reset")) {
				System.out.println("Cliente desconectado forzosamente");
				this.gestor.menzajepatos(nombre + " ha abandonado el chat.", this);
				if (this.gestor.clientesConectadosObjetos.containsKey(nombre) && usuarioDuplicado == false) {
					this.gestor.clientesConectadosObjetos.remove(nombre);
					this.gestor.clientesConectadosID.remove(nombre);
				}
				if(this.gestor.clientesEsperandoParaJugar.containsKey(nombre)) {
					this.gestor.clientesEsperandoParaJugar.remove(nombre);
				}
				this.gestor.clientes.remove(this);
			} else if (e instanceof IOException && e.getMessage().equals("Cliente se desconecto")) {
				System.out.println("Cliente se desconecto");
				this.gestor.desconexionServidor(this);
				if (this.gestor.clientesConectadosObjetos.containsKey(nombre) && usuarioDuplicado == false) {
					this.gestor.clientesConectadosObjetos.remove(nombre);
					this.gestor.clientesConectadosID.remove(nombre);
				}
				if(this.gestor.clientesEsperandoParaJugar.containsKey(nombre)) {
					this.gestor.clientesEsperandoParaJugar.remove(nombre);
				}
				this.gestor.clientes.remove(this);
			} else {
				e.printStackTrace();
			}
		}
	}
}