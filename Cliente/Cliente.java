package Cliente;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import Modelo.Columna;
import Utils.Enumerators;
import Utils.HashSHA256;
import Utils.JsonResponse;

public class Cliente {
	static File directorioDescarga = null;
	static int port = 0;
	static String ip = null;

	public static void main(String[] args) {
		BufferedReader br = null;
		Thread cliente = null;
		try {
			br = new BufferedReader(new InputStreamReader(System.in));
			//Comprobamos que se introduce un puerto correcto
			port = introducirPuertoCorrecto(br);
			//Comprobamos que se introduce un directorio correcto
			directorioDescarga = introducirDirectorioCorrecto(br);
			//Obtemos la IP
			ip = getIP();
			//Creamos los socket, ObjectOutputStream y ObjectInputStream al principio. La razón es que
			//cuando se crea un ObjectOutputStream se envía una cabecera  y tiene que leerla el ObjectInputStream
			//del otro lado del Socket, eso implica que el orden de creación es inverso en el servidor. 
			Socket socket = new Socket(Enumerators.IP_SERVER, Enumerators.PUERTO_SERVER);
			ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
			out.flush();
			ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
			//Creamos un hilo cliente
			cliente = startSender(br, socket, out, in);
			cliente.start();
		} catch (IOException e) {
			e.printStackTrace();
			if (br != null) {
				try {
					br.close();
				} catch (IOException ex) {
					e.printStackTrace();
				}
			}
		}
	}

	public static Thread startSender(BufferedReader br, Socket socket, ObjectOutputStream out, ObjectInputStream in)
			throws IOException {
		Thread myThread = new Thread() {
			public void run() {
				Thread server = null;
				//Enviamos la petición al server.
				System.out.println("Vamos a conectarnos con el servidor");
				PeticionesServer peticion = new PeticionesServer(directorioDescarga, ip, port, socket, out, in);
				//Esperamos su respuesta, si la operación no ha tenido errores se ejecuta el menú.
				JsonResponse response = peticion.peticionServer(Enumerators.PETICION_CONECTARSE);
				if (response.getStatus() == 1) {
				    //Creamos un thread que cada 5 minutos pide que la actualize la tabla al servidor.
					Timer timer = new Timer();
					//No es necezsario pedir la tabla al principio por eso he puesto currentDate+300000
					Date time = new Date(System.currentTimeMillis() + 300000);
					timer.scheduleAtFixedRate(new TablaActualizada(peticion), time, 300000);
					int resultado = -1;
					int eleccion = 0;
					//iniciamos el servidor
					server = startServer();
					server.start();
					System.out.println("Nos hemos conectado al server con exito");
					System.out.println("En este momento tenemos " + peticion.getTablaCliente().numeroColumnas()
							+ " archivos diferentes");
					System.out.println("El total de archivos distribuidos en todos los  nodos es "
							+ peticion.getTablaCliente().tamanioTabla());
					System.out.println("Vamos a presentar la tabla del servidor");
					try {
						do {
							//un menú que solo deja descargarte otros archivos o desconectarte.
							peticion.getTablaCliente().mosTrarElementosTabla();
							System.out.println("¿Qué quieres hacer?");
							System.out.println("DESCARGAR ALGUN FILE: PULSA 0");
							System.out.println("DESCONECTARTE: PULSA 1");
							try {
								eleccion = Integer.parseInt(br.readLine());
							} catch (NumberFormatException ex) {
								eleccion = -1;
							}
							if (eleccion == 0) {
								int numeroColumnas = peticion.getTablaCliente().numeroColumnas();
								System.out.println(
										"En nuestro sistema te avisaremos si intentas descargarte un archivo que ya tenías antes");
								System.out.println("Has elegido que quieres descargar un file");
								do {
									System.out.println("¿Introduzca el código del fichero quieres descargar?");
									try {
										resultado = Integer.parseInt(br.readLine());
									} catch (NumberFormatException ex) {
										resultado = -1;
									}
								} while (resultado < 0 || resultado > numeroColumnas);
								//Obtenemos una columna(Formada por varios archivos con el mismo Hash) dado su código.
								//Podría pedir varios archivos a la vez, pero prefiero que sea uno a uno para que el cliente
								//sepa que ficheros ha podido descargar y cuales no.
								List<Columna> columnasEnvio = new ArrayList<Columna>();
								Columna columnaEnvio = peticion.getTablaCliente().ElegirElementoTabla(resultado);
								columnasEnvio.add(columnaEnvio);
								//mandamos la petición con una columna.
								response = peticion.peticionServer(columnasEnvio,
										Enumerators.PETICION_ANIADIR_COLUMNAS);
								if (response.getStatus() == 1) {
									System.out.println("TODO CORRECTO");
								} else {
									System.out.println("Ha habido problemas");
									System.out.println(response.getMessage());
								}
							}
						} while (eleccion != 1);
						//nos desconectamos
						response = peticion.peticionServer(Enumerators.PETICION_DESCONECTARSE);
						if (response.getStatus() == 1) {
							System.out.println("HASTA LUEGO");
						} else {
							System.out.println("HEMOS TENIDO PROBLEMAS");
							System.out.println(response.getMessage());
						}

					} catch (IOException ex) {
						ex.printStackTrace();
					} finally {
					   //cerramos este thread
						Thread.currentThread().interrupt();
						try {
						    //cerramos el socket
							socket.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
						//cerramos el timer
						timer.cancel();
					}
				}
			}

		};
		return myThread;
	}

	public static Thread startServer() {
		Thread serverThread = new Thread() {
			@Override
			public void run() {
				while (true) {
					try (ServerSocket server = new ServerSocket(port)) {
						try (Socket socket = server.accept()) {
						   //Es un servidor bastante sencillo, solo recibe una petición. Por si acaso compruebo que la cabecera 
						   //del envio es la que debe ser. Además es más generalizable por si en un futuo quiero que acepete diferentes tipos de peticiones.
							BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
							String lineaConPeticionAndhash = br.readLine();
							//leemos la petición que está con formato :   peticion | hash 
							int peticion = Integer.valueOf(lineaConPeticionAndhash.substring(0, 1));
							//el hash del fichero que queremos
							String hash = lineaConPeticionAndhash.substring(2, lineaConPeticionAndhash.length());
							if (peticion == Enumerators.PETICION_DESCARGAR_FILE) {
								for (File file : directorioDescarga.listFiles()) {
									if (HashSHA256.getHash(file).compareTo(hash) == 0) {
									//le enviams el fichero
										try (FileInputStream input = new FileInputStream(file)) {
											OutputStream out = socket.getOutputStream();
											byte[] buff = new byte[1024 * 32];
											int leidos = input.read(buff);
											while (leidos > 0) {
												out.write(buff, 0, leidos);
												leidos = input.read(buff);
											}
											break;
										}
									}
								}
							}

						} catch (IOException e) {
							e.printStackTrace();
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		};
		//Dado que este thread es daemon, cuando se quede solo este thread ejecutando se cerrará.
		serverThread.setDaemon(true);
		return serverThread;
	}
	//Comprobamos que el puerto introducido es correcto.
	private static int introducirPuertoCorrecto(BufferedReader br) throws IOException {
		int resultado = 0;
		do {
			System.out.println("Introduce el numero del puerto");
			try {
				resultado = Integer.parseInt(br.readLine());
			} catch (NumberFormatException ex) {
				resultado = -1;
			}

		} while (resultado <= 0 && !availablePort(resultado) && port > 0xFFFF);
		return resultado;
	}

	//comprobamos si se está usando el puerto.
	private static boolean availablePort(int port) {
		try (Socket ignored = new Socket("localhost", port)) {
			return true;
		} catch (Exception ex) {
			return false;
		}
	}
	//obtenemos la Ip del cliente.
	private static String getIP() {
		String resultado = "localhost";
		try {
			resultado = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		return resultado;
	}

	//Obtenemos el directorio donde se harán descargar. No impedimos que este directorio se esté usando ya.
	private static File introducirDirectorioCorrecto(BufferedReader br) throws IOException {
		Path currentRelativePath = Paths.get("");
		String s = currentRelativePath.toAbsolutePath().toString();
		String nombreDirectorio = "";
		File directorio = null;
		boolean directorioBienIntroducido = false;
		do {
			System.out.println("Introduce el nombre del directorio");
			nombreDirectorio = br.readLine();
			if (!nombreDirectorio.isEmpty()) {
				directorio = new File(s + "//" + nombreDirectorio);
				if (directorio.exists() && directorio.isDirectory()) {
					directorioBienIntroducido = true;
				} else if (!directorio.exists()) {
					directorioBienIntroducido = directorio.mkdir();
				}
			} else {
				directorio = new File(s + "//" + "Compartidos");
				directorio.mkdir();
				directorioBienIntroducido = true;
			}
		} while (directorioBienIntroducido == false);
		return directorio;
	}
}