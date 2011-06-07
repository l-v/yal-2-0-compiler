import java.io.*;


public class CodeGenerator extends Object {

  SymbolTable st;
  String filename;

  CodeGenerator(String file, SymbolTable codeSt) {
      
      st = codeSt;
      filename = file;


      String initialization = ".class public " + st.name;
      initialization += "\n.super java/lang/Object";



      // get declarations
      int declNum = st.localVars.size();
      String declarations = "";//".field static ";
      for (int i=0; i!=declNum; i++) {
	  Variable decl = st.localVars.get(i);

	  String varName = decl.getAttribute("name");
	  String varSize = decl.getAttribute("arraySize");

	  declarations += "\n";

	  if (!varSize.equals("-1")) { // declaracao do tipo a=[100]
		

		String declStr = "\n.field static " + varName + " [I";
		declStr += "\n\n; static code goes here\n ; we need this to create the array " + varName + " (with " + varSize + " elements)";
			   
		declStr += "\n.method static public <clinit>()V";
		declStr += "\n.limit stack 2";
		declStr += "\n.limit locals 0";

		declStr += "\n\n; " + varName + " = new int[" + varSize + "];";
		declStr += "\nbipush " + varSize;
		declStr += "\nnewarray int";
		declStr += "\nputstatic " + st.name + "/" + varName + " [I";

		declStr += "\nreturn";
		declStr += "\n.end method";

		declarations += declStr;
	  }

      }


      String contents = initialization + declarations;
      createFile(contents);

      /*

      int depth = st.childTables.size(); 
      for (int i=0; i!=depth; i++) {
	  SymbolTable child = st.get(i);
	  

	 // if ()
	  

      }
*/


  }







  public void createFile(String contents) {
      try{
	// Create file 
	FileWriter fstream = new FileWriter("out.j");
	BufferedWriter out = new BufferedWriter(fstream);
	out.write(contents);
	//Close the output stream
	out.close();
	}catch (Exception e){//Catch exception if any
	 System.err.println("Error: " + e.getMessage());
	}
  }





}