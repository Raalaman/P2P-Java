package Modelo;

import java.util.concurrent.CopyOnWriteArrayList;

//esta es la tabla del servidor
public class TablaServer{


	//es una lista CopyOnWriteArrayList dado que es posible que cuando un cliente esté iterando 
	//otro cliente añada o borre una columna.
	private CopyOnWriteArrayList<Columna> columnasServer;

	public TablaServer() {
		columnasServer=new CopyOnWriteArrayList<Columna>();
	}

	public CopyOnWriteArrayList<Columna> getColumnas() {
		return columnasServer;
	}

	public void setColumnas(CopyOnWriteArrayList<Columna> columnas) {
		columnasServer = columnas;
	}

	//Permite crear una tabla cliente dado una tablaServer, para enviarsela a los clientes.
	public TablaCliente createTablaCliente() {
		TablaCliente tablaCliente = new TablaCliente();
		for (Columna columna : columnasServer)
			tablaCliente.columnas.add(new Columna(columna));
		return tablaCliente;
	}

	
}
