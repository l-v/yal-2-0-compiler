

import java.util.*;

/***
** Picks possible errors of the symbol table (linkedList 'errors' stored in SAnalysis), and does a semantic analysis of them 
***/
public class SAnalysisFunctions {


	    SymbolTable st;
	    LinkedList<String> errorsFound;


	    public SAnalysisFunctions(SymbolTable symbolTab) {
		  st = symbolTab;
		  errorsFound = new LinkedList<String>();

	    }

	    public void doAnalysis() {
;
	      LinkedList<OnHoldError> possibleErrors = SAnalysis.errors;

	      for (int i=0; i!=possibleErrors.size(); i++) {
		  OnHoldError error = possibleErrors.get(i);
		
		  functionExists(error.funcName, error.callType, error.line, error.calledArgs, error.assignTo);
	      }

	    }



	   /*** funcoes de ANALISE SEMANTICA ***/



    	  /*** Verifica se existe uma variavel com nome requerido 
	  ** varName: nome da variavel a procurar
	  **
	  ** returns: a variável encontrada ou null
	  ***/
	  public Variable variableExists(String varName) {

	      SymbolTable table = st;

	      // search module declarations too
	      int numArgs = table.localVars.size();  
	      for (int j=0; j!=numArgs; j++) {
		      Variable temp = table.localVars.get(j);
		      if (temp.name.equalsIgnoreCase(varName))
			  return temp;
	      }

	      while (true) {

	      for (int i=0; i!=table.childTables.size(); i++) {

		  SymbolTable func = table.childTables.get(i);

		  // see if variable called is an argument of a function
		  numArgs = func.funcArgs.size();  
		  for (int j=0; j!=numArgs; j++) {
		      Variable temp = func.funcArgs.get(j);
		      if (temp.name.equalsIgnoreCase(varName))
			  return temp;
		  }

		  // check return of function
		  //System.out.println("function: " + func.returnType + "---varName: " + varName);
		  if (func.returnType.equals(varName) || func.returnType.equals(varName+"[]")) {
		      Variable returnVar = new Variable(varName, func.returnType);
		      return returnVar;
		  }

		  //return null;
	      }

	      // verifica se ja chegamos a tabela 'raiz'
	      if (table.parentTable == null) {
		  //System.out.println("NULL");
		  break;
	      }

	      table = table.parentTable;
	      return null;
	    }

	      return null;
	  }


	  /*** Verifica se existe uma funcao com nome, argumentos e retorno compativeis à chamada 
	  ** funcName: nome da funcao
	  ** callType: 'call' ou 'assign'
	  ** line: linha a ser analisada
	  ** calledArgs: argumentos usados para invocar a funcao
	  ** assignTo: variavel a que se esta a atribuir um valor (apenas para callType = 'assign')
	  ***/
	  public Boolean functionExists(String funcName, String callType, int line, Node calledArgs, String assignTo) {
	      
	      
	      SymbolTable table = st;

	      while (true) {


	      	   // see if variable called is not a function, but an argument of a function
		  int numArgs = table.funcArgs.size();  
		  for (int j=0; j!=numArgs; j++) {
		      if (table.funcArgs.get(j).name.equalsIgnoreCase(funcName))
			  return true;
		  }


	      for (int i=0; i!=table.childTables.size(); i++) {

		  SymbolTable func = table.childTables.get(i);

		  if (func.isFunc && (func.name.equalsIgnoreCase(funcName))) {
		
		      //checkReturnType
		      if (callType.equalsIgnoreCase("call") && !func.returnType.equals("void")) {
			  errorsFound.add("semantic error: line " + line + ", function " + funcName + " returns scalar");
			  return false;
		      }

		      else if (callType.equalsIgnoreCase("assign") && func.returnType.equals("void")) {
			  errorsFound.add("semantic error: line " + line + ", function " + funcName + " returns void");
			  return false;
		      }

		      // caso em que retorna array
		      else if (callType.equalsIgnoreCase("assign")) {
	 		  Variable leftSide = variableExists(assignTo);
			  
			  // variavel nao declarada - caso nao incluido na analise semantica
			  if (leftSide == null) return false;


			  // ver se returnTypes com array  coincidem
			  if (!leftSide.type.contains("[]") && func.returnType.contains("[]")) {
			      errorsFound.add("semantic error: line " + line + ", return of function " + funcName + " is an array ");
			      return false;
			  }  
			  else if (leftSide.type.contains("[]") && !func.returnType.contains("[]")) {
			      errorsFound.add("semantic error: line " + line + ", return of function " + funcName + " is scalar ");
			      return false;

			  }
		      }

		      //checkArguments(func);
		      if (calledArgs != null)
			  return checkArguments(func.funcArgs, calledArgs, line, funcName);
		      

		      return true;	
		  }

		  // see if variable called is not a function, but an argument of a function
		  numArgs = func.funcArgs.size();  
		  for (int j=0; j!=numArgs; j++) {
		      if (func.funcArgs.get(j).name.equalsIgnoreCase(funcName))
			  return true;
		  }


	      }

	      // verifica se ja chegamos a tabela 'raiz'
	      if (table.parentTable == null) {
		  //System.out.println("NULL");
		  break;
	      }

	      table = table.parentTable;

	  }

	      errorsFound.add("semantic error: line " + line + ", function " + funcName + " is not included in this module");
	      return false;
	      


	  }


	  /*** Verifica se argumentos sao compativeis 
	  ** functionArgs: lista dos argumentos na declaracao da funcao
	  ** calledArgs: argumentos usados na chamada da funcao
	  ** line: linha a ser analisada
	  ** funcName: nome da funcao a ser analisada
	  ***/
	  public boolean checkArguments(LinkedList<Variable> functionArgs, Node calledArgs, int line, String funcName) {

	      int numFuncArgs = functionArgs.size();
	      

	      if (calledArgs == null && numFuncArgs == 0)
		    return true;

	      else if (calledArgs == null && numFuncArgs != 0) {
		    errorsFound.add("semantic error: line " + line + ", function " + funcName + " has arguments");
		    return false;
	      }


	      int numCalledArgs = calledArgs.jjtGetNumChildren();

	      if (numFuncArgs ==0) {
		    errorsFound.add("semantic error: line " + line + ", function " + funcName + " does not have arguments");
		    return false;
	      }

	      else if (numCalledArgs > 1 && numFuncArgs == 1) {
		    errorsFound.add("semantic error: line " + line + ", function " + funcName + " has only one argument");
		    return false;
	      }

	      //second argument of function "f1" must be an array
	      else if (numCalledArgs != numFuncArgs) {
		    errorsFound.add("semantic error: line " + line + ", function " + funcName + " has only different number of arguments");
		    return false;
	      }

	      // check if argument type matches
	      for (int i=0; i!=numFuncArgs; i++) {

		  Variable funcVar = functionArgs.get(i); // needed variable/argument to call the function
		  Node calledVar = calledArgs.jjtGetChild(i); // variable used

		  Variable argVar = variableExists(calledVar.getVal()); // get variable declaration

		  // variable not declared (not covered by semantic analysis)
		  if (argVar == null) return false;
		 

		  // check if variable types match
		  //System.out.println("--------------> " + funcVar.type + " ----- " + argVar.type);
		  if (funcVar.type.equals("int[]") && !argVar.type.contains("[]")) {
		      errorsFound.add("semantic error: line " + line + ", argument " + (i+1) + " (" + argVar.name + ") of function " + funcName + " must be an array");
		  }

		  else if (!funcVar.type.equals("int[]") && argVar.type.contains("[]")) {
		      errorsFound.add("semantic error: line " + line + ", argument " + (i+1) + " (" + argVar.name + ") of function " + funcName + " must be scalar");
		  }

	      }


	      return true;
	  }



}