public class Proceso {
	
	private Integer id;
	private String nombre;
	private String tipo;
	private String fecha;

	public Integer getId() {
		return this.id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getNombre() {
		return this.nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public String getTipo() {
		return this.tipo;
	}

	public void setTipo(String tipo) {
		this.tipo = tipo;
	}

	public String getFecha() {
		return this.fecha;
	}

	public void setFecha(String fecha) {
		this.fecha = fecha;
	}

	// This is a note for getCadena method
	public String getCadena() throws Exception {

		String result = "";

		// Pre-condition checkVacios
		if (!(this.id != null && this.nombre != null && this.tipo != null)) {
			throw new Exception("Error due to checkVacios constraint");
		}
		// Pre-condition checkIdValido
		if (!(this.id > 0)) {
			throw new Exception("Error due to checkIdValido constraint");
		}

		result = this.id + "-" + this.nombre + "-" + this.tipo;

		return result;
	}
}
