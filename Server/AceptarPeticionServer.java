package Server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import Modelo.Columna;
import Modelo.TablaCliente;
import Modelo.TablaServer;
import Utils.Enumerators;
import Utils.IdentificadorNodo;
import Utils.JsonResponse;

//esta clase gestiona las peticiones que le pueden enviar los nodos clientes.
public class AceptarPeticionServer implements Runnable {

	private TablaServer tablaServer;
	private ObjectOutputStream out;
	private ObjectInputStream in;

	@Override
	public void run() {
		JsonResponse respuesta = new JsonResponse(0, "No se ha podido gestionar la columna correctamente");
		int peticion = -1;
		try {
			do {
				//primero obtenemos la cabecera de la peticion
				peticion = in.readInt();
				//Después gestionamos la petición
				respuesta = GestionarPeticion(peticion, respuesta);
				System.out.println("El codigo de la peticion es " + peticion);
				System.out.println("El mensaje del JsonResponse es " + respuesta.getMessage());
				System.out.println("El status del JsonResponse es " + respuesta.getStatus());
				//devolvemos el JsonResponse para que el cliente sepa si se ha gestionado
				//todo correctamente.
				enviarRespuesta(respuesta);
			} while (peticion != Enumerators.PETICION_DESCONECTARSE && respuesta.getStatus() == 1);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	//Este método envía la respuesta.
	private void enviarRespuesta(JsonResponse respuesta) {
		try {
			out.writeObject(respuesta);
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	//Este metodo gestionar todas las peticiones que existen en este momento.
	private JsonResponse GestionarPeticion(int peticion, JsonResponse respuesta) {
		if (peticion == Enumerators.PETICION_ANIADIR_COLUMNAS) {
			try {
				//Primero recibo las columas
				System.out.println("Intento obtener las columnas a añadir");
				@SuppressWarnings("unchecked")
				List<Columna> columnas = (List<Columna>) in.readObject();
				System.out.println("Las columnas se han obtenido");
				//Ahora trato las columnas
				respuesta = GestionarColumnas(columnas, respuesta);
			} catch (IOException e) {
				respuesta.setStatus(0);
				respuesta.setMessage("IOException en la peticion aniadir columnas");
			} catch (ClassNotFoundException e) {
				respuesta.setStatus(0);
				respuesta.setMessage("ClassNotFoundException en la peticion aniadir columnas");
			}
		} else if (peticion == Enumerators.PETICION_TABLA) {
			try {
				//Envio la tabla al cliente.
				System.out.println("Intento enviar la tabla");
				TablaCliente tablaCliente = tablaServer.createTablaCliente();
				out.writeObject(tablaCliente);
				out.flush();
				System.out.println("Tabla enviada");
				respuesta.setStatus(1);
				respuesta.setMessage("Conexion correcta, te enviamos la tabla");
			} catch (IOException e) {
				respuesta.setStatus(0);
				respuesta.setMessage("IOException en la peticion conectarse");
			}
		} else if (peticion == Enumerators.PETICION_DESCONECTARSE) {
			try {
			    //Obtengo el nodo que quiere desconectarse.
				System.out.println("Intento leer el objecto nodo");
				IdentificadorNodo nodo = (IdentificadorNodo) in.readObject();
				System.out.println("Objecto nodo leido");
				if (!tablaServer.getColumnas().isEmpty()) {
					for (Columna columna : tablaServer.getColumnas()) {
						for (Entry<IdentificadorNodo, String> entry : columna.getIdentificadorConNombreArchivo()
								.entrySet()) {
							if (entry.getKey().getIp().compareTo(nodo.getIp()) == 0
									&& entry.getKey().getPuerto() == nodo.getPuerto()) {
									//Elimino de la columna el nodo del cliente que se quiere desconectar.
								columna.getIdentificadorConNombreArchivo().remove(entry.getKey());
								if (columna.getIdentificadorConNombreArchivo().isEmpty()) {
									//En caso de no haber ningún nodo en la columna, la elimino.
									tablaServer.getColumnas().remove(columna);
								}
							}
						}
					}
				}
				respuesta.setStatus(1);
				respuesta.setMessage("Hemos borrado los elementos de la tablaServer");

			} catch (IOException e) {
				respuesta.setStatus(0);
				respuesta.setMessage("IOException al desconectarte");
			} catch (ClassNotFoundException e) {
				respuesta.setStatus(0);
				respuesta.setMessage("ClassNotFoundException al desconectarte");
			}
		}
		//enviamos la respuesta
		return respuesta;
	}
	//Este metodo añade una nueva columna si no existe el hash del fichero, o en caso de que exista
	//la añade a la columna existente.
	private JsonResponse GestionarColumnas(List<Columna> columnas, JsonResponse respuesta) {
		int contador = 0;
		for (Columna columna : columnas) {
			Columna columnaServer = null;
			try {
			//Compruebo que está el archivo con el hash.
				columnaServer = tablaServer.getColumnas().stream()
						.filter(x -> x.getHash().compareTo(columna.getHash()) == 0).findFirst().get();
			} catch (NoSuchElementException ex) {
				columnaServer = null;
			}
			if (columnaServer != null) {
			//Si encuentra la columna, entonces añade a la columna correspondiente.	
				for (Entry<IdentificadorNodo, String> entryMisColumnas : columna.getIdentificadorConNombreArchivo()
						.entrySet()) {
					if (!columnaServer.getIdentificadorConNombreArchivo().
							entrySet().stream().
							anyMatch(x->x.getKey().getIp().compareTo(entryMisColumnas.getKey().getIp())==0 &&
									entryMisColumnas.getKey().getPuerto()==x.getKey().getPuerto())) {
						columnaServer.getIdentificadorConNombreArchivo().put(entryMisColumnas.getKey(),
								entryMisColumnas.getValue());
						contador++;
						break;
					}
				}
			} else {
			//Si no encuentra la columna añade esa columna.
				tablaServer.getColumnas().add(columna);
				contador++;
			}
		}
		if (contador == columnas.size()) {
			//comprobar de que todo a ido bien.
			respuesta.setStatus(1);
			respuesta.setMessage("Se han añadido todas las columnas");
		}
		return respuesta;
	}

	public AceptarPeticionServer(TablaServer tablaServer, ObjectOutputStream out, ObjectInputStream in) {
		this.tablaServer = tablaServer;
		this.in = in;
		this.out = out;
	}

}
