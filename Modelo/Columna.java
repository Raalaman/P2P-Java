package Modelo;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;

import Utils.IdentificadorNodo;

public class Columna implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 126L;

	private String hash;

	//Esta tabla es ConcurrentHashMap dado que es posible que cuando un cliente esté iterando 
	//otro cliente añada o borre una Entry.
	private ConcurrentHashMap<IdentificadorNodo, String> identificadorConNombreArchivo;

	private String tamanio;

	public Columna(String hash, ConcurrentHashMap<IdentificadorNodo, String> identificadorConNombreArchivo,
			String tamanio) {
		this.hash = hash;
		this.identificadorConNombreArchivo = identificadorConNombreArchivo;
		this.tamanio = tamanio;
	}

	public Columna(Columna columna) {
		this.hash = columna.hash;
		this.identificadorConNombreArchivo = columna.identificadorConNombreArchivo;
		this.tamanio = columna.tamanio;
	}

	public String getHash() {
		return hash;
	}

	public void setHash(String hash) {
		this.hash = hash;
	}

	public ConcurrentHashMap<IdentificadorNodo, String> getIdentificadorConNombreArchivo() {
		return identificadorConNombreArchivo;
	}

	public void setIdentificadorConNombreArchivo(
			ConcurrentHashMap<IdentificadorNodo, String> identificadorConNombreArchivo) {
		this.identificadorConNombreArchivo = identificadorConNombreArchivo;
	}

	public String getTamanio() {
		return tamanio;
	}

	public void setTamanio(String tamanio) {
		this.tamanio = tamanio;
	}

}
