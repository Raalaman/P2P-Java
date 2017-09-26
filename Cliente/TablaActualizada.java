package Cliente;

import java.util.TimerTask;

import Utils.Enumerators;

public class TablaActualizada extends TimerTask {

	private PeticionesServer peticion;

	//Cada 5 minutos manda una petición para actualizar la tabla.
	public void run() {
		peticion.peticionServer(Enumerators.PETICION_TABLA);
	}

	public TablaActualizada(PeticionesServer peticion) {
		this.peticion = peticion;
	}

}
