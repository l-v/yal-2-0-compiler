	
import java.util.*;



public class SymbolTable extends Object {


	  String type;  //tipo funcao
	  String name; //nome funcao

	  LinkedList<SymbolTable> childTables;
	  SymbolTable parentTable;
	  LinkedList<Variable> localVars;

	  Boolean isFunc; //verdadeiro se esta tabela de simbolos esta associada a uma funcao
	  LinkedList<Variable> funcArgs; //argumentos da funcao
	  String returnType;
	  
	 
 
	  /*** Construtores ***/
	  public SymbolTable() {
	    childTables = new LinkedList<SymbolTable>();
	    localVars = new LinkedList<Variable>();

	
	    isFunc = false;
	    funcArgs = new LinkedList<Variable>();
	    parentTable = null;
	  }

	  public SymbolTable(String sType, SymbolTable parent) {
	    childTables = new LinkedList<SymbolTable>();
	    localVars = new LinkedList<Variable>();
	    type = sType;

	
	    isFunc = false;
	    funcArgs = new LinkedList<Variable>();
	    parentTable = null;
	  }

	  public SymbolTable(Node root) {
	    childTables = new LinkedList<SymbolTable>();
	    localVars = new LinkedList<Variable>();
	
	    isFunc = false;
	    funcArgs = new LinkedList<Variable>();
	    parentTable = null;
	    

	    addTree(root);

	  }

	  public void addTree(Node root) {

	    int childNum = root.jjtGetNumChildren();
	    for (int i=0; i!=childNum; i++) {
		
		Node newNode = root.jjtGetChild(i);
		System.out.println("--> "+newNode.toString() + ":" + newNode.getVal());

		String varName = newNode.getVal();
		String varType = newNode.toString();


		if (varType.equals("MID"))
		    editTable(varName, varType);

		else if (varType.equals("Decl")) 
		    addDeclNode(newNode);
      
		else if (varType.equals("Func"))
		    addFuncNode(newNode);
		
	    }
	  }

	  /*** Adiciona um nó contendo uma declaracao de variavel ***/
	  public void addDeclNode(Node node) {
		
		Node DeclID = node.jjtGetChild(0);
		System.out.println("--> "+DeclID.toString() + ":" + DeclID.getVal());
		

		for (int i=1; i!=node.jjtGetNumChildren(); i++) {
		    
		   if (node.jjtGetChild(i).toString().equals("ArraySize")) { 

		      Node arraySize = (node.jjtGetChild(i)).jjtGetChild(0);


		      addVar(DeclID.getVal(), "int[]", arraySize.getVal());
		      return;
		   }
		}

		addVar(DeclID.getVal(), "int");
	  }

	  /*** Adiciona um nó de funcao ***/ // FALTA VER RETORNO DE FUNCAO
	  public void addFuncNode(Node node) {

	     SymbolTable newTable = new SymbolTable("Func", this); 
	     newTable.setFunc(1);

		
	     Node FuncID = node.jjtGetChild(0);
	     System.out.println("--> "+FuncID.toString() + ":" + FuncID.getVal());

	     newTable.editTable(FuncID.getVal(), node.toString());
	     
	    String funcReturn = "void";

	     // adiciona argumentos da funcao, se estes existirem
	     if (node.jjtGetNumChildren()>2) {  
		  
		  int index=1;
		  for (int i=1; i!=node.jjtGetNumChildren(); i++) {

		      Node childNode = node.jjtGetChild(i);

		      // verifica por retorno da funcao
		      if (childNode.toString().equals("AssignID")) {
			  funcReturn = childNode.getVal();
		      }

		      // procura argumentos
		      else if (childNode.toString().equals("Args"))  {
			  index = i;
			  break;
		      }
		  }

		  Node Args = node.jjtGetChild(index);
		 

		  int numArgs = Args.jjtGetNumChildren();
		  for (int i=0; i!=numArgs; i++) {
		      Node newArg = Args.jjtGetChild(i);

		      // verifica se arg é do tipo array (MELHORAR METODO...)
		      if (i<numArgs-1 && Args.jjtGetChild(i+1).toString().equals("IsArray")) {

			newTable.addArg(newArg.getVal(), "int[]");
			i++;
		      }

		      else {
			newTable.addArg(newArg.getVal(), "int");
		      }
		  }
	     }

	      // adiciona returnType
	     newTable.setReturn(funcReturn);

	     // passa nó com resto da funcao (stmtLst)
	    Node stmtLst = (node.jjtGetChild(node.jjtGetNumChildren()-1)).jjtGetChild(0);
	    addFuncBody(stmtLst, newTable);

  
	    // adiciona nova tabela à tabela de simbolos principal
	    addTable(newTable);

	  }


	  /*** Processa corpo de uma funcao ***/ // FALTA VER EXPRESSOES DE TESTE
	  public void addFuncBody(Node node, SymbolTable mainTable) {

	     int numStmt = node.jjtGetNumChildren();
	     for (int i=0; i!=numStmt; i++) {
		  Node stmt = node.jjtGetChild(i);
		  String stmtType = stmt.toString();
		  

		  if (stmtType.equals("Assign")) {
		      mainTable.addAssign(stmt, mainTable);
		  }

		  else if (stmtType.equals("While")) { // adiciona novo 'nivel' da tabela de simbolos
		      SymbolTable newTable = new SymbolTable("While", mainTable);
		      Node whileNode = stmt.jjtGetChild(1);
		      addFuncBody(whileNode, newTable);
		
		      mainTable.addTable(newTable);
		  }

		  else if (stmtType.equals("If")) {
		      
		      int numStmtLst = stmt.jjtGetNumChildren();
		      for (int j=0; j!=numStmtLst; j++) {

			Node ifNode = stmt.jjtGetChild(j);
			if (ifNode.toString().equals("Stmtlst")) {

			  // Distingue entre if/else
			  String ifType="If";
			  if (j==numStmtLst-1) {
			     ifType="Else";
			  }

			  SymbolTable newTable = new SymbolTable(ifType, mainTable);
			  addFuncBody(ifNode, newTable);

			  mainTable.addTable(newTable);
			}
		      }		      
		  }

		// apenas para analise erros semanticos 
		else if (stmtType.equals("CallID")) {
		    
		    // verifica se chamada à funcao é válida
		    if (!functionExists(stmt.getVal(), "call")) 
			return;
		}
	     }


	  }


	  /*** Adiciona uma instrução de Assign ***/
	  public void addAssign(Node node, SymbolTable table) {

	     Node lhs = node.jjtGetChild(0);
	    // System.out.println("==="+lhs.getVal());

	     if (lhs.jjtGetNumChildren() !=0) {

		//inserir tamanho array
		table.addVar(lhs.getVal(), "int[]");

		
	     }

	     else {
		table.addVar(lhs.getVal(), "int");
	     }
	  }

	  /*** Edita atributos type e name ***/
	  public void editTable(String sName, String sType) {
	    type = sType;
	    name = sName;
	  }

	  public void setFunc(int isFunction) {
	    
	    if (isFunction == 1)
		isFunc = true;
	    else
		isFunc = false;
	  }

	  public void setReturn(String funcReturn) {
	    returnType = funcReturn;

	  }

	  /*** Adiciona nova variavel a localVars ***/
	  public void addVar(String vName, String vType) {
	    Variable newVar = new Variable(vName, vType);
	    localVars.add(newVar);
	  }

	  public void addVar(String vName, String vType, String vSize) {
	    Variable newVar = new Variable(vName, vType, vSize);
	    localVars.add(newVar);
	  }

	  /*** Adiciona nova tabela de simbolos a childTables***/ 
	  public void addTable(SymbolTable table) {
	     childTables.add(table);

	  }

	  /*** Adiciona novo argumento de funcao ***/
	  public void addArg(String vName, String vType) {
	     Variable newVar = new Variable(vName, vType);
	     funcArgs.add(newVar);
	  }



	

	  public void printTable() {
	      System.out.println("\n--SymbolTable--\n");
	      this.printTable("");
	  }

	  // POR MAIS LEGIVEL!
	  public void printTable(String spacing) {

	    System.out.println(spacing + type + " : " + name 
				+ "\n" + spacing + "-localVars: " + localVars.size()
				+ ";\t childTables: " + childTables.size());
	    
	    // imprime retorno e argumentos funcao 
	    
	    if (isFunc && funcArgs.size()>0) {
		System.out.println(spacing + "return: " + returnType);
		System.out.println("__FuncArgs:__");
		for (int i=0; i!=funcArgs.size(); i++) {
		    funcArgs.get(i).printVar();
		}
		System.out.println("_____________");
	    }
	   


	    // imprime variaveis locais
	    System.out.println("--localVars:--");
	    for (int i=0; i!=localVars.size(); i++) {
		Variable var = localVars.get(i);
		System.out.println("\n"+spacing); 
		var.printVar();
		 
	    }  
	    System.out.println("--------------");

	    // print 'child' functions
	    String space = spacing + "   ";
	    for (int i=0; i!=childTables.size(); i++) {
		childTables.get(i).printTable(space);
	    }
	  
	  }









	   /*** funcoes de ANALISE SEMANTICA ***/
	  public Integer variableExists(String varName) {
		
	     
	     for (int i=0; i!=localVars.size(); i++) {

		  Variable var = localVars.get(i);

		  if (var.getAttribute("name").equalsIgnoreCase(varName)) {
		      return 1;
		  }


	      }

	      System.out.println("semantic error: line x, variable " + varName + " not declared");
	      return -1;

	  }


	  /*** Verifica se existe uma funcao com nome, argumentos e retorno compativeis à chamada 
	  ** funcName: nome da funcao
	  ** callType: 'call' ou 'assign'
	  ***/
	  public Boolean functionExists(String funcName, String callType) {
	      
	      for (int i=0; i!=childTables.size(); i++) {

		  SymbolTable func = childTables.get(i);

		  if (func.isFunc && (func.name.equalsIgnoreCase(funcName) || func.returnType.equalsIgnoreCase(funcName))) {

		      //checkReturnType
		      if (callType.equalsIgnoreCase("call") && !func.returnType.equals("void")) {
			  System.out.println("semantic error: line x, function " + funcName + " returns scalar");
			  return false;
		      }

		      //checkArguments(func);
		      

		      return true;	
		  }


	      }

	      System.out.println("semantic error: line x, function " + funcName + " is not included in this module");
	      return false;
	      
	  }

	  


	}

class Variable {
      
	String type;
	String name; 

	Integer arraySize; // tamanho da variavel tipo array; valor -1 para var. escalares

	public Variable(String vName, String vType) {
	    type = vType;
	    name = vName;
	    arraySize=-1;
	}

	public Variable(String vName, String vType, String vSize) {
	    type = vType;
	    name = vName;
	    setArraySize(vSize);
	}

	public void setArraySize(String size) {
	    arraySize = Integer.parseInt(size);
	}

	public String getAttribute(String attribute) {
	    if (attribute.equalsIgnoreCase("type"))
		return type;
	    else if (attribute.equalsIgnoreCase("arraySize"))
		return Integer.toString(arraySize);
	    else
		return name;
	}

	public void printVar() {

	    if (arraySize == -1)
	      System.out.println(name + ":" + type);
	    else
	      System.out.println(name + ":" + type + "(" + arraySize + ")");

	}
	
}

