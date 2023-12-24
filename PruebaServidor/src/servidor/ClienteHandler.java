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
        	
            outputStream.writeObject("Bienvenido al chat. Por favor, introduce tu nombre:");
            nombre = (String) inputStream.readObject();
            this.gestor.bienvenidaServidor(nombre + " se ha unido al chat.", this);

            String mensaje;
            do {
                mensaje = (String) inputStream.readObject();
                if(mensaje.equals("bye")) {
                	throw new IOException("Cliente se desconecto");
                }else {
                	this.gestor.menzajepatos(mensaje, this);
                }
            } while (!mensaje.equals("bye"));

    		System.out.println("Cliente desconectado forzosamente");
            this.gestor.menzajepatos(nombre + " ha abandonado el chat.", this);
            this.gestor.clientes.remove(this);
            socketCliente.close();
        } catch (IOException | ClassNotFoundException e) {
        	if(e instanceof SocketException && e.getMessage().equals("Connection reset")) {
        		System.out.println("Cliente desconectado forzosamente");
        		this.gestor.menzajepatos(nombre + " ha abandonado el chat.", this);
                this.gestor.clientes.remove(this);
        	} else if(e instanceof IOException && e.getMessage().equals("Cliente se desconecto")) {
        		System.out.println("Cliente se desconecto");
        		this.gestor.desconexionServidor(this);
                this.gestor.clientes.remove(this);
        	}else {
                e.printStackTrace();
        	}
        }
    }
}