package Cliente;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import Modelo.Columna;
import Modelo.TablaCliente;
import Utils.Enumerators;
import Utils.HashSHA256;
import Utils.IdentificadorNodo;
import Utils.JsonResponse;

public class PeticionesServer {

	private TablaCliente tablaCliente;
	private File directorioDescarga;
	private String ipCliente;
	private int puertoCliente;
	private Socket socket;
	private ObjectOutputStream out;
	private ObjectInputStream in;
	private IdentificadorNodo nodo;

	public File getDirectorioDescarga() {
		return directorioDescarga;
	}

	public PeticionesServer(File directorioDescarga, String ipCliente, int puertoCliente, Socket socket,
			ObjectOutputStream out, ObjectInputStream in) {
		super();
		this.directorioDescarga = directorioDescarga;
		this.ipCliente = ipCliente;
		this.puertoCliente = puertoCliente;
		this.socket = socket;
		this.in = in;
		this.out = out;
		this.nodo = new IdentificadorNodo(ipCliente, puertoCliente);
	}

	public void setDirectorioDescarga(File directorioDescarga) {
		this.directorioDescarga = directorioDescarga;
	}

	public String getIpCliente() {
		return ipCliente;
	}

	public void setIpCliente(String ipCliente) {
		this.ipCliente = ipCliente;
	}

	public int getPuertoCliente() {
		return puertoCliente;
	}

	public void setPuertoCliente(int puertoCliente) {
		this.puertoCliente = puertoCliente;
	}

	public TablaCliente getTablaCliente() {
		return tablaCliente;
	}

	public void setTablaCliente(TablaCliente tablaCliente) {
		this.tablaCliente = tablaCliente;
	}

	//Para las peticiones de  DESCONECTARSE, CONECTARSE y PETICION_TABLA el numero de columnas que pasamos es null.
	//Quedaba muy feo poner en todas esas peticiones el null por lo que he hecho este método.
	public JsonResponse peticionServer(int peticion) {
		JsonResponse respuesta = new JsonResponse(0, "Peticion mal hecha");
		if (peticion == Enumerators.PETICION_DESCONECTARSE || peticion == Enumerators.PETICION_CONECTARSE
				|| peticion == Enumerators.PETICION_TABLA) {
			respuesta = peticionServer(null, peticion);
		}
		return respuesta;
	}

	//Este método es el principal. Es synchronized dado que puede pasar que justo cuando estemos usando el socket para hacer una peticion
	//de PETICION_ANIADIR_COLUMNAS o PETICION_DESCONECTARSE se hace una petición de PETICION_TABLA. 
	public synchronized JsonResponse peticionServer(List<Columna> columnas, int peticion) {
		JsonResponse response = new JsonResponse(0, "Error FATAL");
		if (peticion == Enumerators.PETICION_ANIADIR_COLUMNAS) {
			//obtenemos los archivos de otros nodos que tengan el hash de las columnas.
			response = mandarPeticionNodos(columnas, response);
			if (response.getStatus() == 1) {
			    //Añadimos a las columnas la información de que este nodo también tiene ese archivo.
				for (Columna columna : columnas) {
					columna.getIdentificadorConNombreArchivo().put(nodo, response.getMessage());
				}
				//Enviamos al server la información de las nuevas columnas para que las gestione.
				response = mandarInfoColumnasServer(columnas, response, peticion);
			}
		} else if (peticion == Enumerators.PETICION_CONECTARSE) {
		   //obtenemos todas las columnas del directorio descarga.
			columnas = ObtenerTodasLasColumnasCliente();
			//Si tiene archivos se añaden al sever.
			if (!columnas.isEmpty()) {
				System.out.println("Hay columnas enviamos la informacion al server");
				response = mandarInfoColumnasServer(columnas, response, Enumerators.PETICION_ANIADIR_COLUMNAS);
				System.out.println("Informacion enviada");
			} else {
				response.setStatus(1);
				response.setMessage("No tenemos files en el directorio");
			}
			//Si se han cargado bien los archivos o no tenía archivos mandamos una petición para que nos de la tabla.
			if (response.getStatus() == 1) {
				response = recibirTabla(response);
			}
			
		} else if (peticion == Enumerators.PETICION_DESCONECTARSE) {
			try {
			   //enviamos cabecera desconexión
				System.out.println("Enviamos peticion de desconexion");
				enviarCabeceraPeticion(peticion, socket);
				//Enviamos  el nodo para eliminar de la columan la aparición de este nodo como poseedor del archivo.
				System.out.println("Enviamos nodo");
				out.writeObject(nodo);
				out.flush();
				System.out.println("Recibimos resultado");
				response = (JsonResponse) obtenerObjectFromStream(socket);
			} catch (UnknownHostException e) {
				response.setStatus(0);
				response.setMessage("UnknownHostException al desconectarte");
			} catch (IOException e) {
				response.setStatus(0);
				response.setMessage("IOException en Conexion al desconectarte");
			} catch (ClassNotFoundException e) {
				response.setStatus(0);
				response.setMessage("ClassNotFoundException al desconectarte");
			}
		} else if (peticion == Enumerators.PETICION_TABLA) {
			response = recibirTabla(response);
		}
		return response;
	}

	//Crea columnas con la información de los archivos que tiene en el fichero de descarga.
	private List<Columna> ObtenerTodasLasColumnasCliente() {
		List<Columna> columnas = new ArrayList<Columna>();
		IdentificadorNodo nodo = new IdentificadorNodo(ipCliente, puertoCliente);
		for (File file : directorioDescarga.listFiles()) {
			ConcurrentHashMap<IdentificadorNodo, String> map = new ConcurrentHashMap<IdentificadorNodo, String>();
			map.putIfAbsent(nodo, file.getName());
			Columna columna = new Columna(HashSHA256.getHash(file), map, String.valueOf(file.length()));
			columnas.add(columna);
		}
		return columnas;
	}
	//Crea columnas con la información de los archivos que tiene en el fichero de descarga. 
	private JsonResponse mandarInfoColumnasServer(List<Columna> columnas, JsonResponse response, int peticion) {
		try {

			System.out.println("Enviamos cabecera para mandar info columnas al server");
			enviarCabeceraPeticion(peticion, socket);
			System.out.println("Enviamos las columnas");
			out.writeObject(columnas);
			out.flush();
			System.out.println("Columnas enviadas");
			Object object = obtenerObjectFromStream(socket);
			response = (JsonResponse) object;
			System.out.println("Respuesta del server obtenida");
		} catch (FileNotFoundException e) {
			response.setStatus(0);
			response.setMessage("FileNotFoundException en envio de columnas");
		} catch (IOException e) {
			response.setStatus(0);
			response.setMessage("IOException en envio de columnas");
		} catch (ClassNotFoundException e) {
			response.setStatus(0);
			response.setMessage("ClassNotFoundException en envio de columnas");
		}
		return response;
	}
	//Intentamos obtener el fichero mandándole la petición secuencialmente a cada nodo, si una falla se pasa
	//a la siguiente hasta que no quede ningún nodo con ese archivo o se descargue el fichero correctamente.
	private JsonResponse mandarPeticionNodos(List<Columna> columnas, JsonResponse response) {
		response.setStatus(0);
		response.setMessage(
				"No he descargado ningún file: posibles problemas: ya tienes el file o problemas en la descarga");
		for (Columna columna : columnas) {
			ConcurrentHashMap<IdentificadorNodo, String> nodos = columna.getIdentificadorConNombreArchivo();
			if (!nodos.keySet().stream()
					.anyMatch(x -> x.getIp().compareTo(nodo.getIp()) == 0 && x.getPuerto() == nodo.getPuerto())) {
				for (Entry<IdentificadorNodo, String> nodo : nodos.entrySet()) {
					response = ObtenerFile(columna.getHash(), nodo, response);
					if (response.getStatus() == 1) {
						System.out.println("Se ha descargado correctamente");
						break;
					}
				}
			}
		}
		return response;
	}

	//Este método obtiene el file dado un hash de un nodo, devuelve una respuesta dependiendo si ha ido bien la descarga o no.
	private JsonResponse ObtenerFile(String hash, Entry<IdentificadorNodo, String> nodo, JsonResponse response) {
		int linea = 0;
		OutputStream output = null;
		try (Socket Othersocket = new Socket(nodo.getKey().getIp(), nodo.getKey().getPuerto())) {
			PrintWriter print = new PrintWriter(Othersocket.getOutputStream());
			print.println(Enumerators.PETICION_DESCARGAR_FILE + "|" + hash);
			print.flush();
			InputStream input = Othersocket.getInputStream();
			output = new FileOutputStream(directorioDescarga + File.separator + nodo.getValue());
			byte buff[] = new byte[1024 * 32];
			linea = input.read(buff);
			while (linea > 0) {
				output.write(buff, 0, linea);
				linea = input.read(buff);
			}
			if (HashSHA256.getHash(directorioDescarga + File.separator + nodo.getValue()).compareTo(hash) == 0) {
				response.setMessage(nodo.getValue());
				response.setStatus(1);
			}
			else
			{
				File file=new File(directorioDescarga + File.separator + nodo.getValue());
				file.delete();
				response.setMessage("Fichero mal descargado");
				response.setStatus(0);
			}
		} catch (UnknownHostException e) {
			response.setStatus(0);
			response.setMessage("UnknowHostException gestionando el fichero");
		} catch (IOException e) {
			response.setMessage("IOException gestionando el  fichero");
			response.setStatus(0);
		} finally {
			if (output != null) {
				try {
					output.close();
				} catch (IOException e) {
					response.setStatus(0);
					response.setMessage("IOException cerrando el output del fichero");
				}
			}
		}
		return response;
	}

	//Método que manda una cabecera con la petición de pedir la tabla y recibe 
	//la tabla.
	private JsonResponse recibirTabla(JsonResponse response) {
		try {
			System.out.println("***********Obtenemos la tabla*****************");
			System.out.println("Enviamos peticion de tablas");
			enviarCabeceraPeticion(Enumerators.PETICION_TABLA, socket);
			System.out.println("Intentamos recibir  la tabla");
			tablaCliente = (TablaCliente) obtenerObjectFromStream(socket);
			System.out.println("Hemos recibido la tabla");
			response = (JsonResponse) obtenerObjectFromStream(socket);
			System.out.println("***********Tabla recibida*****************");
		} catch (UnknownHostException e) {
			response.setStatus(0);
			response.setMessage("UnknownHostException en Conexion");
		} catch (IOException e) {
			response.setStatus(0);
			response.setMessage("IOException en Conexion");
		} catch (ClassNotFoundException e) {
			response.setStatus(0);
			response.setMessage("ClassNotFoundException en Conexion");
		}
		return response;
	}

	//metodo con el que envías peticiones
	private void enviarCabeceraPeticion(int peticion, Socket socket) throws IOException {
		out.writeInt(peticion);
		out.flush();
	}
   //metodo que obtiene un objeto  contenido en el outputstream de un socket.
	private Object obtenerObjectFromStream(Socket socket) throws IOException, ClassNotFoundException {
		return in.readObject();
	}

}
