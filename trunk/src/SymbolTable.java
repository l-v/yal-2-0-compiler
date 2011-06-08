	
import java.util.*;



/***
** Class that builds the Symbol Table 
** possible errors placed in SAnalysis.errors to be checked later 
***/
public class SymbolTable extends Object {

	  String type;  //tipo funcao
	  String name; //nome funcao

	  LinkedList<SymbolTable> childTables;
	  SymbolTable parentTable;
	  LinkedList<Variable> localVars;

	  Boolean isFunc; //verdadeiro se esta tabela de simbolos esta associada a uma funcao
	  LinkedList<Variable> funcArgs; //argumentos da funcao
	  String returnType;

	  
	  //Construtores
	  public SymbolTable() {
		childTables = new LinkedList<SymbolTable>();
	    localVars = new LinkedList<Variable>();
	    funcArgs = new LinkedList<Variable>();
	    isFunc = false;
	    parentTable = null;
	  }

	  public SymbolTable(String sType, SymbolTable parent) {
	    childTables = new LinkedList<SymbolTable>();
	    localVars = new LinkedList<Variable>();
	    parentTable = parent;
	    type = sType;
	    isFunc = false;
	    funcArgs = new LinkedList<Variable>();
	  }

	  public SymbolTable(Node root) {
	    childTables = new LinkedList<SymbolTable>();
	    localVars = new LinkedList<Variable>();
	    funcArgs = new LinkedList<Variable>();
	    isFunc = false;
	    parentTable = null;
	    
	    addTree(root);
	  }

	  public void addTree(Node root) {

	    int childNum = root.jjtGetNumChildren();
	    
	    for (int i=0; i!=childNum; i++) {
		
			Node newNode = root.jjtGetChild(i);
			//System.out.println("--> "+newNode.toString() + ":" + newNode.getVal());
	
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

	  // Adiciona um no contendo uma declaracao de variavel
	  public void addDeclNode(Node node) {
		
		Node DeclID = node.jjtGetChild(0);
		//System.out.println("--> "+DeclID.toString() + ":" + DeclID.getVal());
		
		for (int i=1; i!=node.jjtGetNumChildren(); i++) {
		    
		   if (node.jjtGetChild(i).toString().equals("ArraySize")) { 

		      Node arraySize = (node.jjtGetChild(i)).jjtGetChild(0);
		      addVar(DeclID.getVal(), "int[]", arraySize.getVal());
		      return;
		   }
		   
		}
		addVar(DeclID.getVal(), "int");
	  }
	  

	  // Adiciona um no de funcao 
	  // TODO: FALTA VER RETORNO DE FUNCAO
	  public void addFuncNode(Node node) {

	     SymbolTable newTable = new SymbolTable("Func", this); 
	     newTable.setFunc(1);

		
	     Node FuncID = node.jjtGetChild(0);
	     //System.out.println("--> "+FuncID.toString() + ":" + FuncID.getVal());

	     newTable.editTable(FuncID.getVal(), node.toString());
	     
	    String funcReturn = "void";

	     // adiciona argumentos da funcao, se estes existirem
	     if (node.jjtGetNumChildren()>2) {  
		  
		  int index=1;
		  for (int i=1; i!=node.jjtGetNumChildren(); i++) {

		      Node childNode = node.jjtGetChild(i);

		      // verifica por retorno da funcao
		      if (childNode.toString().equals("AssignID")) {
			  funcReturn = newTable.name;
			  newTable.name = childNode.getVal();

			  // verifica se retorno é array
			  if (node.jjtGetChild(i-1).toString().equals("IsArray"))
			      funcReturn += "[]";
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


	  // Processa corpo de uma funcao
	  // TODO: FALTA VER EXPRESSOES DE TESTE
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
		   
		    // verifica se funcao pertence a este modulo
		    Boolean checkFunc = true;
		    if (i+1 != numStmt) { 
			if (node.jjtGetChild(i+1).toString().equals("CallID2")) 
			      checkFunc = false;
		    }


		    // verifica se chamada à funcao é válida
		    if (checkFunc) {
			Node argsNode;
			if (i+1 < numStmt && node.jjtGetChild(i+1).toString().equals("ArgList"))
			      argsNode = node.jjtGetChild(i+1);
	    
			else
			      argsNode = null;

			  OnHoldError newError = new OnHoldError(stmt.getVal(), "call", stmt.getLine(), argsNode, null);
			  SAnalysis.errors.add(newError); 	    
		    }
		
		}
	     }


	  }


	  // Adiciona uma instrucao de Assign
	  public void addAssign(Node node, SymbolTable table) {

	     Node lhs = node.jjtGetChild(0);
	     Node rhs = node.jjtGetChild(1);
	     //System.out.println("==="+lhs.getVal());

	     if (lhs.jjtGetNumChildren() !=0) {

		//inserir tamanho array?
		table.addVar(lhs.getVal(), "int[]");
	     }

	     else {
		table.addVar(lhs.getVal(), "int");
	     }


	    // verificacao semantica 
	    Integer numTerms = rhs.jjtGetNumChildren();
	    for (int i=0; i!=numTerms; i++) {
	  
		Node term = rhs.jjtGetChild(i);
		

		if (term.toString().equals("Term") && term.jjtGetChild(0).toString().equals("ID")) {

		      //check if it is a function and not a variable, and if function belongs to this module
		      Boolean isFunction = false;
		      Boolean inModule = true;

		      int termChildren = term.jjtGetNumChildren();
		      for (int j=0; j!=termChildren; j++) { 
			  String termName = term.jjtGetChild(j).toString();

			  if (termName.equals("IsFunc"))
			      isFunction = true;

			  if (termName.equals("ID2"))
			      inModule = false;

		      }
		    
		  
		      if (isFunction && inModule) {

			  Node termChild = term.jjtGetChild(0);
			  Node argsNode = null;

			  if (term.jjtGetNumChildren() !=1 && term.jjtGetChild(1).toString().equals("ArgList"))
			      argsNode = term.jjtGetChild(1);
		    

			  // assign tem variaveis: ver possiveis erros
			  OnHoldError newError = new OnHoldError(termChild.getVal(), "assign", termChild.getLine(), argsNode, lhs.getVal());
			  SAnalysis.errors.add(newError);

		      }
		}

	    }
	      
	  }

	  // Edita atributos type e name
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
	      System.out.println("\n\n-----SymbolTable-----\n");
	      this.printTable("");
	      
	  }

	  //TODO: POR MAIS LEGIVEL!
	  public void printTable(String spacing) {
  
	    System.out.println(spacing + type + " : " + name + "\n");
	    
	    // imprime variaveis locais
	    System.out.println(spacing + "noLocalVars: " + localVars.size());
	    
	    for (int i=0; i!=localVars.size(); i++) {
	    	Variable var = localVars.get(i);
	    	System.out.print("\t" + spacing);
	    	var.printVar();
	    }
	    System.out.println();
	        
	    // imprime retorno e argumentos funcao 
	    
	    if (isFunc && funcArgs.size()>0) {
			
	    	System.out.println(spacing + "Return: " + returnType + "\n");
			System.out.println(spacing + "FuncArgs: ");
			
			for (int i=0; i!=funcArgs.size(); i++)
			{
				System.out.print(spacing + "\t");
			    funcArgs.get(i).printVar();
			}
			
			System.out.println();
	    }
	    

	    // print 'child' functions
	    
	    System.out.println(spacing + "noChildTables: " + childTables.size() + "\n");
	    String space = spacing + "\t";
	    
	    for (int i=0; i!=childTables.size(); i++)
	    {
	    	System.out.println(spacing + "ChildTable no. " + i + ":\n");
	    	childTables.get(i).printTable(space);
	    }
	  
	  }
	  
	  //Percorre as tabelas de simbolos e retorna a tabela com o nome e tipo indicados
	  SymbolTable getSymbolTable(String stype, String sname)
	  {  
		  if(type.equals(stype) && name.equals(sname))
			  return this;
		  
		  int childNum = this.childTables.size();
			  
		  if(childNum == 0)
			  return null;
		  
		  for(int i = 0; i < childNum; i++)
		  {
			  SymbolTable s = childTables.get(i).getSymbolTable(stype, sname);
			  
			  if(s != null)
				  return s;
		  }
		  
		  return null;
	  }
	  
	}


