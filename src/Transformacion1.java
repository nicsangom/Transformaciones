import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Transformacion1 {
    public static void main(String[] args) throws Exception {
        List<String> result;

        String resultado="'use strict';\n"+
        "// SDK Library to asset with writing the logic\n"+
        "const { Contract } = require('fabric-contract-api');\n"+
        "const util = require('util');\n\n"+
        "class "+args[1]+" extends Contract {\n"+
          "\tconstructor() {\n"+
            "\t\tsuper('"+args[1]+"');\n"+
          "\t}\n";
        //   +"\tasync InitLedger(ctx) {\n"+
        //     "\t\tconst assets = [];\n\t}\n";

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
              //Método readProceso
            resultado+="\n\n\tasync read"+clase+"(ctx, id) {\n"+
              "\t\tconst procJSON = await ctx.stub.getState(id.toString()); // get the asset from chaincode state\n"+
              "\t\tif (!procJSON || procJSON.length === 0) {\n"+
              "\t\t\tthrow new Error(`The process ${id.toString()} does not exist`);\n"+
              "\t\t}\n\n\t\treturn JSON.parse(procJSON.toString('utf-8'));\n\t}\n";
            
              String readProcess="read"+clase;
            //Método getHistory
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
            //Método addEvent
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
    }
        

        //Declaración de reglas
        resultado+="\n\tcheckRules(proceso=null,etapa=null,evento=null){\n";

        String directorioReglas=args[0]+File.separator+"entrada"+File.separator+"reglas";
        File reglas = new File(directorioReglas);
        String[] listaReglas = reglas.list();
        String resultadoReglas="";
        for (int i = 0; i < listaReglas.length; i++) {
            File archivoInterno = new File(directorio + File.separator + listaReglas[i]);
            directorio=args[0]+File.separator+"entrada"+File.separator+"reglas"+File.separator+archivoInterno.getName();
            try (Stream<String> lines = Files.lines(Paths.get(directorio))) {
                result=lines.filter(x->!x.isEmpty()).collect(Collectors.toList());
                int count=0;
                while(count<result.size()){

                while(!result.get(count).contains("when")){
                    if(result.get(count).contains("rule")){
                        String line=result.get(count).replace("rule ", "");
                        resultadoReglas+="\t\t//Regla "+line+"\n";
                    }
                    count++;
                }
                String existAttributes="";
                String evalResult="";
                while(!result.get(count).contains("then")){
                    //Comprueba si existen los valores
                    if(result.get(count).contains(":")){
                        String line=result.get(count);
                        existAttributes+=line.trim().split(":")[0]+"!= null && ";
                    }

                    //Crea la condición eval
                    if(result.get(count).contains("eval")){
                        //Multiples condiciones de eval
                        if(result.get(count).contains("&&")){
                            String[] evals=result.get(count).split(" && ");
                            evalResult+="\t\tif(";
                            for(int e=0; evals.length-1>=e;e++){
                                String rule=evals[e].trim().substring(5, evals[e].trim().length()-1);
                                rule=formatString(rule).replace("size", "lenght");
                                if(e<evals.length-1){
                                    evalResult+=rule+ " && ";
                                }else{
                                    evalResult+=rule;
                                }
                            }
                            evalResult+="){\n";
                        }else{
                            //Una condición de eval
                            String rule=result.get(count).trim().substring(5, result.get(count).trim().length()-1);
                            rule=formatString(rule).replace("size", "lenght");
                            evalResult+="\t\tif("+rule+"){\n";
                        }
                    }
                    count++;

                }
                resultadoReglas+="\t\tif("+existAttributes.substring(0, existAttributes.length()-4)+"){\n";
                resultadoReglas+="\t"+evalResult;

                String then="";
                while(!result.get(count).contains("end")){
                    if(!result.get(count).contains("then")){
                        then+=result.get(count).trim().replace("Exception", "Error");
                    }
                    count++;
                }
                count++;
                resultadoReglas+="\t\t\t\t"+then+"\n\t\t\t}\n\t\t}\n";
            }
            count++;
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

        //Archivo Index
        PrintWriter writer = new PrintWriter(new FileWriter(directorioCarpeta+"/index.js", true));
        String index="'use strict';\n\n"+
        "const "+args[1].toLowerCase()+" = require('./lib/"+args[1].toLowerCase()+".js');\n\n"+
        "module.exports."+args[1]+" = "+args[1].toLowerCase()+";\n"+
        "module.exports.contracts = ["+args[1].toLowerCase()+"];\n";
        writer.println(index);
        writer.close();

        //Archivo package
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

        //Archivo del contrato
        writer = new PrintWriter(new FileWriter(lib.getPath()+"/"+args[1].toLowerCase()+".js", true));
        writer.println(resultado);
        writer.close();
    }

    static String formatString(String rule){
        while(rule.indexOf("get")!=-1){
            int index=rule.indexOf("get")+3;
            char letra=Character.toLowerCase(rule.charAt(rule.indexOf("get")+3));
            StringBuilder ruleBuilder = new StringBuilder(rule);
            ruleBuilder.setCharAt(index, letra);
            rule=ruleBuilder.toString();
            rule=rule.replaceFirst("get", "").replace("()", "");
        }
        return rule;
    }

    static void deleteDir(File file) {
        File[] contents = file.listFiles();
        if (contents != null) {
            for (File f : contents) {
                if (! Files.isSymbolicLink(f.toPath())) {
                    deleteDir(f);
                }
            }
        }
        file.delete();
    }
}