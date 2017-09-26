package Utils;

import java.io.Serializable;

public class JsonResponse implements Serializable {

	/**Esta es la clase principal que utilizamos para comunicar entre los nodos
	 */
	
	private static final long serialVersionUID = 2L;
	//Mensaje generalmente informativo. Aunque se puede utilizar pasar
	//pasar información si es necesario.
	private String message;
	//estatus es 0 si ha ido mal.
	//estatus es 1 si ha ido bien.
	private int status;

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public int getStatus() {
		return status;
	}

	
	public void setStatus(int status) {
		this.status = status;
	}

	public JsonResponse(int status, String message) {
		this.message = message;
		this.status = status;
	}
}
