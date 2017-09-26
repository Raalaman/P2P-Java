package Utils;

import java.io.Serializable;

public class IdentificadorNodo implements Serializable {

	//Esta clase identifica a cada nodo.  Dado que es imposible
	//que haya dos clientes con la misma ip y puerto a la vez.
	private static final long serialVersionUID = 12L;

	private String ip;

	private int puerto;

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public int getPuerto() {
		return puerto;
	}

	public void setPuerto(int puerto) {
		this.puerto = puerto;
	}

	public IdentificadorNodo(String ip, int puerto) {
		this.ip = ip;
		this.puerto = puerto;
	}

	public String toString() {
		return ip + "::" + puerto;

	}

}
