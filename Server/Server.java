package Server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import Modelo.TablaServer;
import Utils.Enumerators;

public class Server {

	private static TablaServer tablaServer = new TablaServer();

	public static void main(String[] args) {
		System.out.println("EL SERVIDOR ESTA EJECUTANDOSE");
		ExecutorService pool = Executors.newCachedThreadPool();
		//abre un  servidor en el puerto 6666 que recibirá las peticiones.
		try (ServerSocket server = new ServerSocket(6666)) {
			Enumerators.IP_SERVER = server.getInetAddress().getHostAddress();
			while (true) {
				try {
					System.out.println("ESPERANDO PETICION");
					Socket socket = server.accept();
					ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
					out.flush();
					ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
					AceptarPeticionServer aceptar = new AceptarPeticionServer(tablaServer, out, in);
					System.out.println("PETICION ACEPTADA");
					pool.execute(aceptar);
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			pool.shutdown();
		}
	}

}
