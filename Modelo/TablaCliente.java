package Modelo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

//Esta tabla no permitir� modificaciones en los elementos de la lista.
public class TablaCliente implements Serializable {

	private static final long serialVersionUID = 1L;

	protected List<Columna> columnas;

	//este m�todo sirve para mostrar los elementos de la tabla
	public void mosTrarElementosTabla() {
		if (!columnas.isEmpty()) {
			int contador = 0;
			System.out.println("A continuaci�n se muestran los resultados, con el formado:");
			System.out.println("Hash | nombreFichero| ip::puerto | Tama�o");
			for (Columna columna : columnas) {
				System.out.println();
				System.out.println("*********** Codigo fichero " + contador + " ***************");
				columna.getIdentificadorConNombreArchivo().forEach((k, v) -> System.out.println(
						columna.getHash() + "  |  " + k + "  |  " + v.toString() + "  |  " + columna.getTamanio()));
				contador++;
			}
		} else {
			System.out.println("TABLA VACIA");
		}
		System.out.println();
	}

	//Devuelve una columna de la tabla dado un hash.
	public Columna ElegirElementoTabla(String hash) {
		return columnas.stream().filter(x -> x.getHash() == hash).findFirst().get();
	}
	//Devuelve una columna de la tabla dado su posici�n en la lista.
	public Columna ElegirElementoTabla(int numeroTabla) {
		return columnas.get(numeroTabla);
	}

	//numero de archivos en total.
	public int tamanioTabla() {
		return columnas.stream().mapToInt(i -> i.getIdentificadorConNombreArchivo().size()).sum();
	}
	//numero de archivos diferentes en total.
	public int numeroColumnas() {
		return columnas.size();
	}
	public TablaCliente()
	{
		columnas=new ArrayList<Columna>();
	}
}
