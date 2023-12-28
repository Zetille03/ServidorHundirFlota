package servidor;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;

public class ClienteHandler implements Runnable {
    private Socket socketCliente;
    private String nombre;
    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;
    private GestorHundirFlota gestor;
    
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
            mensaje = this.gestor.comprobacionDatosUsuario(mensajeArray[1],mensajeArray[2]);
            if(mensaje==null) {
            	this.gestor.enviarMensajeCliente("L@0", this);
            }else {
            	nombre = mensajeArray[1];
            	if(this.gestor.comprobacionConexionMultiple(nombre)==true) {
            		usuarioDuplicado = true;
            		this.gestor.enviarMensajeCliente("L@R", this);
            	}else {
	            	this.gestor.clientesConectados.put(nombre, this);
	            	this.gestor.bienvenidaServidor(nombre + " se ha unido al chat.", this);
	            	System.out.println(mensaje);
	            	this.gestor.enviarMensajeCliente("L@1@"+mensaje, this);
            	}
            }
            

            
            do {
            	mensaje = null;
            	mensajeArray = null;
                mensaje = (String) inputStream.readObject();
                if(mensaje.equals("bye")) {
                	throw new IOException("Cliente se desconecto");
                }else {
                	this.gestor.menzajepatos(mensaje, this);
                }
            } while (!mensaje.equals("bye"));
            
            this.gestor.menzajepatos(nombre + " ha abandonado el chat.", this);
            if(this.gestor.clientesConectados.containsKey(nombre)) {
            	this.gestor.clientesConectados.remove(nombre);
            }
            this.gestor.clientes.remove(this);
            socketCliente.close();
        } catch (IOException | ClassNotFoundException e) {
        	if(e instanceof SocketException && e.getMessage().equals("Connection reset")) {
        		System.out.println("Cliente desconectado forzosamente");
        		this.gestor.menzajepatos(nombre + " ha abandonado el chat.", this);
        		if(this.gestor.clientesConectados.containsKey(nombre) && usuarioDuplicado == false) {
                	this.gestor.clientesConectados.remove(nombre);
                }
                this.gestor.clientes.remove(this);
        	} else if(e instanceof IOException && e.getMessage().equals("Cliente se desconecto")) {
        		System.out.println("Cliente se desconecto");
        		this.gestor.desconexionServidor(this);
        		if(this.gestor.clientesConectados.containsKey(nombre) && usuarioDuplicado == false) {
                	this.gestor.clientesConectados.remove(nombre);
                }
                this.gestor.clientes.remove(this);
        	}else {
                e.printStackTrace();
        	}
        }
    }
}