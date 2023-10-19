import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class Transformacion1 {
    public static void main(String[] args) throws Exception {
        String archivo = "ruta_del_archivo.txt";
        String urlBaseDatos = "jdbc:mysql://localhost:3306/nombre_base_datos";
        String usuario = "usuario";   // Sustituir por el usuario
        String contraseña = "contraseña";   // Sustituir por la contraseña
        try {
           List<String> result;
           String resultado = "";

           //Leer archivos de la carpeta modelos
           String directorio=args[0]+File.separator+"entrada"+File.separator+"modelos";
           File modelos = new File(directorio);
           String[] lista = modelos.list();

           //Lee los ficheros que se encuentran en la carpeta modelos
           for (int i = 0; i < lista.length; i++) {
               File archivoInterno = new File(directorio + File.separator + lista[i]);
               directorio=args[0]+File.separator+"entrada"+File.separator+"modelos"+File.separator+archivoInterno.getName();
           String clase="";
           List<String> atributos= new ArrayList<>();

           try (Stream<String> lines = Files.lines(Paths.get(directorio))) {
            result = lines.collect(Collectors.toList());
            for(String line : result){
                //Obtener nombre de la clase
                if(line.contains("class")){
                    clase=line.split("class ")[1].split(" ")[0];
                }
                //Obtener lista de atributos
                if(line.contains("private")){
                    atributos.add(line.split(" ")[2].substring(0, line.split(" ")[2].length()-1));
                }
            }
            resultado+="\n\n\tasync create"+clase+"(ctx, ";
            for(int atributo=0; atributos.size()-1>=atributo;atributo++){
                if(atributo<atributos.size()-1){
                    resultado+=atributos.get(atributo)+",";
                    }else{
                        resultado+=atributos.get(atributo);
                    }
            }
            if(clase.equals("Proceso")){
                resultado+=", etapas){\n\t\tlet proc={\n";

            }else{
                resultado+="){\n\t\tlet proc={\n";

            }
            for(String atributo: atributos){
                if(!atributo.equals("etapas")){
                resultado+="\t\t\t"+atributo+":"+atributo+",\n";
                }else{
                    resultado+="\t\t\t"+atributo+": [],\n";
                }
            }
            
            if(clase.equals("Proceso")){
                resultado+="\t\t\tetapas:[],\n";
                resultado+="\t\t};\n\t\tif ((typeof etapas).toUpperCase() === 'STRING') {\n"+
                "\t\t\tconst objEtapas = JSON.parse(etapas);\n\n"+
                "\t\t\tif (Array.isArray(objEtapas)) {\n"+
                  "\t\t\t\tproc.etapas = objEtapas;\n"+
                "\t\t\t}\n\t\t}\n\n"+          
                "\t\tthis.checkRules(proc);\n"+
                "\t\t\n\t\tawait ctx.stub.putState(id.toString(), JSON.stringify(proc));\n\n"+
                "\t\treturn {txid: ctx.stub.getTxID(), Proceso: proc};\n\n\t}\n";              
            
                resultado+="\n\n\tasync read"+clase+"(ctx, id) {\n"+
                "\t\tconst procJSON = await ctx.stub.getState(id.toString()); // get the asset from chaincode state\n"+
                "\t\tif (!procJSON || procJSON.length === 0) {\n"+
                "\t\t\tthrow new Error(`The process ${id.toString()} does not exist`);\n"+
                "\t\t}\n\n\t\treturn JSON.parse(procJSON.toString('utf-8'));\n\t}\n";
            
                String readProcess="read"+clase;
            
                resultado+="\n\n\tasync getHistory(ctx, id) {\n"+
                "\t\tlet iterator = await ctx.stub.getHistoryForKey(id.toString());\n"+
                "\t\tlet result = [];\n"+
                "\t\tlet res = await iterator.next();\n"+
                "\t\twhile (!res.done) {\n"+
                "\t\t\tif (res.value) {\n"+
                "\t\t\t\tconst obj = JSON.parse(res.value.value.toString('utf-8'));\n"+
                "\t\t\t\tresult.push(obj);\n"+
                "\t\t\t}\n"+
                "\t\t\tres = await iterator.next();\n"+
                "\t\t}\n"+
                "\t\tawait iterator.close();\n"+
                "\t\treturn result;\n"+
                "\t}\n";
             
                resultado+="\n\n\tasync addEvent(ctx, idProceso, idEtapa, evento) {\n"+
                "\t\tlet proc = await this."+readProcess+"(ctx, idProceso);\n"+    
                "\t\tconst objEvento = JSON.parse(evento);\n"+
                "\t\tfor (let etapa of proc.etapas.filter(et => Number(et.id) === Number(idEtapa))) {\n"+
                "\t\t\tthis.checkRules(proc, etapa, objEvento);\n"+
                "\t\t\tetapa.eventos.push(objEvento);\n"+
                "\t\t}"+
                "\t\tawait ctx.stub.putState(proc.id.toString(), JSON.stringify(proc));\n"+
                "\t\treturn {txid: ctx.stub.getTxID(), Proceso: proc};\n"+
                "\t}\n\n";
            }else{
               resultado+="\t\t};\n\t}";
            } 

        }
              
        resultado+=resultadoReglas+"\n\t}\n}\n\n";
        resultado+="module.exports ="+args[1]+";";

        //Carpeta general
        File carpeta=new File(args[1]);
        deleteDir(carpeta);
        carpeta.mkdirs();
        String directorioCarpeta= carpeta.getPath();

        //Carpeta lib
        File lib=new File(directorioCarpeta+"/lib");
        lib.mkdirs();

        PrintWriter writer = new PrintWriter(new FileWriter(directorioCarpeta+"/index.js", true));
        String index="'use strict';\n\n"+
        "const "+args[1].toLowerCase()+" = require('./lib/"+args[1].toLowerCase()+".js');\n\n"+
        "module.exports."+args[1]+" = "+args[1].toLowerCase()+";\n"+
        "module.exports.contracts = ["+args[1].toLowerCase()+"];\n";
        writer.println(index);
        writer.close();

        writer = new PrintWriter(new FileWriter(directorioCarpeta+"/package.json", true));
        String packageJson="{\n\t"+
            "\"name\": \""+args[1].toLowerCase()+"\",\n\t"+
            "\"version\": \"1.0.0\",\n\t"+
            "\"description\": \"Prueba de autogeneración de contrato Hyperledger a partir de reglas\",\n\t"+
            "\"main\": \"index.js\",\n\t"+
            "\"scripts\": {\n\t\t"+
                "\"start\": \"fabric-chaincode-node start\",\n\t\t"+
              "\"test\": \"echo \\\"Error: no test specified\\\" && exit 1\"\n\t"+
            "},\n\t"+
            "\"author\": \"\",\n\t"+
            "\"license\": \"ISC\",\n\t"+
            "\"dependencies\": {\n\t\t"+
                "\"fabric-contract-api\": \"^2.2.1\",\n\t\t"+
                "\"fabric-shim\": \"^2.2.1\"\n\t"+
            "},\n\t"+
            "\"devDependencies\": {}\n"+
            "}\n";
        writer.println(packageJson);
        writer.close();

        writer = new PrintWriter(new FileWriter(lib.getPath()+"/"+args[1].toLowerCase()+
                                 ".js", true));
        writer.println(resultado);
        writer.close();

         // Establecer conexión con la base de datos
         Connection conexion = DriverManager.getConnection(urlBaseDatos, usuario, contraseña);

         // Preparar la sentencia SQL para insertar los datos
         String sql = "INSERT INTO tabla (columna1, columna2) VALUES (?, ?)";
         PreparedStatement statement = conexion.prepareStatement(sql);

         // Leer el archivo línea por línea
         BufferedReader lector = new BufferedReader(new FileReader(archivo));
         String linea;
         while ((linea = lector.readLine()) != null) {
                // Separar los datos de la línea (suponiendo que están separados por comas)
                String[] datos = linea.split(",");

                // Asignar los valores a los parámetros de la sentencia SQL
                statement.setString(1, datos[0]);
                statement.setString(2, datos[1]);

                // Ejecutar la sentencia SQL
                statement.executeUpdate();
         }

         // Cerrar recursos
         lector.close();
         statement.close();
         conexion.close();

         System.out.println("Los datos se han guardado en la base de datos correctamente.");
        
           } catch (SQLException e) {
            System.out.println("Error al conectar con la base de datos: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Error al leer el archivo: " + e.getMessage());
        }
    }
}
