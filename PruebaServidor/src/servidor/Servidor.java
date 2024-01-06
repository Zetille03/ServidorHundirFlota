package servidor;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Servidor {

    private static final int PUERTO = 12345;

    public static void main(String[] args) {
        new Servidor().iniciarServidor();
    }

    /*
     * Metodo que acepta las conexiones de los clientes
     * En él se crea el gestor y cada cliente handler tiene el mismo gestor al
     * cual acceden para guardar la información y ver información con la base de datos
     */
    private void iniciarServidor() {
        try {
            ServerSocket serverSocket = new ServerSocket(PUERTO);
            System.out.println("ServidorGPT1 esperando conexiones en el puerto " + PUERTO);
            GestorHundirFlota gestor = new GestorHundirFlota();
//            gestor.borrarDatosPartidas();
            while (true) {
                Socket clienteSocket = serverSocket.accept();
                System.out.println("Cliente conectado desde " + clienteSocket.getInetAddress());

                ClienteHandler clienteHandler = new ClienteHandler(clienteSocket,gestor);
                gestor.clientes.add(clienteHandler);
                new Thread(clienteHandler).start();
            }
        } catch (IOException e) {
        	e.printStackTrace();
        }
    }

    

    
}