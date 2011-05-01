	
import java.util.*;



public class SymbolTable extends Object {


	  String type;  //tipo funcao
	   String value; //nome funcao
	   LinkedList<SymbolTable> functions = new LinkedList<SymbolTable>();
	   HashMap<String, String> localVars = new HashMap<String, String>(); // variaveis dento de modulo/funcao

	  public SymbolTable() {
	    functions = new LinkedList<SymbolTable>();
	    localVars = new HashMap<String, String>();

	  }
	  
	  public SymbolTable(String itype, String ivalue) {
	    
	    type=itype;
	    value=ivalue;
	    
	    functions = new LinkedList<SymbolTable>();
	    localVars = new HashMap<String, String>();

	  }
	  
	  public  void defineMainVars(String itype, String ivalue) {
	    type=itype;
	    value=ivalue;
	  }
	  
	  public void addEntry(String type, String value) {
	    localVars.put(type, value);
	  }
	  
	  public void addFunc(SymbolTable newFunc) {
	    functions.add(newFunc);
	  }
	  
	  public void addFunc(String type, String value) {
	    SymbolTable newFunc = new SymbolTable(type, value);
	    functions.add(newFunc);
	  }
	  
	  public int addToEnd(SymbolTable newFunc) {
	    
	    if (functions.size()==0)
	      return -1;

	    functions.getLast().addFunc(newFunc);
	    return 1;
	  }
	 
	 
	 /* Procura na LinkedList functions por uma funcao de nome/valor 'value' */ 
	  public int getListIndex(String funcValue) {
	    for(int i=0; i!=functions.size(); i++) {
	      if (funcValue.equals(functions.get(i).value))
		return i;
	    
	    }
	    
	    return -1;
	  }
	 
	 /* Insere uma nova funcao (newFunc) dentro de uma funcao especifica (parentFunc) */
	  public int insertIntoFunc(SymbolTable parentFunc, SymbolTable newFunc) {
	    return 0;
	  }
	  
	  public void printST() {
	      System.out.println("\n--SymbolTable--\n");
	      this.printST("");
	  }

	  // print local vars
	  public void printST(String spacing) {

	    System.out.println(spacing + type + " : " + value 
				+ "\n" + spacing + "-localVars: " + localVars.size()
				+ ";\t childFuncs: " + functions.size());
	    
	    Iterator iterator = localVars.keySet().iterator();  
	    while (iterator.hasNext()) {  
		String key = iterator.next().toString();  
		String value = localVars.get(key).toString();  
      
		System.out.println(spacing + "   " + key + " " + value);  
	    }  


	    // print 'child' functions
	  
	    String space = spacing + "   ";
	    
	    for (int i=0; i!=functions.size(); i++) {
		functions.get(i).printST(space);
	    }
	  
	  }

	}