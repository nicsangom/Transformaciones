public class Transformacion {
	public static void main(String[] args) throws Exception {
	List<String> result;
	String resultado;
	
	//Leer archivos de la carpeta indicada
	String directorio=args[0]+File.separator+"entrada"+File.separator+"modelos";
	File modelos = new File(directorio);
	String[] lista = modelos.list();

	//Lee los ficheros que se encuentran en la carpeta indicada
	for (int i = 0; i < lista.length; i++) {
		File archivoInterno = new File(directorio + File.separator + lista[i]);
		directorio=args[0]+File.separator+"entrada"+File.separator+"modelos"+
		File.separator+archivoInterno.getName();
		String clase = "";
	}
	List<String> atributos = new ArrayList<>();
	
	try (Stream<String> lines = Files.lines(Paths.get(directorio))) {
		result = lines.collect(Collectors.toList());
		for(String line : result){
			if Transformacion1(line){
				Transformacion2(line);
		}
	 
         }
     }
}