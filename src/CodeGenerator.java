import java.io.*;
import java.util.*;

public class CodeGenerator extends Object {

  SymbolTable st;
  String filename;
  LinkedList<Variable> globalVars;
  int numStack;
  int numLocals;
  int labelCounter;

  SymbolTable currentTable;

  CodeGenerator(String file, SymbolTable codeSt, Node rootNode) {

      st = codeSt;
      filename = file;
      currentTable = codeSt;
      int tableChild = 0;
      labelCounter = 0;
      
      String result = "";
      String header = "";
      String globals = "";
      String arrayInitializations = "";
      String functions = "";
      String moduleName = file;    
          int childNum = rootNode.jjtGetNumChildren();
          
          for(int i = 0; i < childNum ; i++)
          {
                  Node newNode = rootNode.jjtGetChild(i);
                  
                  String varName = newNode.getVal();
                  String varType = newNode.toString();
                  
                        if (varType.equals("MID"))
                        {
                                header = ".class public " + varName + "\n";
                            header += ".super java/lang/Object\n";
                            moduleName = varName;
                        }
        
                        else if (varType.equals("Decl"))
                        {
                                globals += declareGlobal(newNode);
                                arrayInitializations += initializeArrays(newNode);
                        }
              
                        else if (varType.equals("Func")) {
                           // currentTable = st.childTables.get(tableChild);
			    functions += declareFunction(newNode);

			    currentTable = st;
			    //tableChild++;
			}
          }

      result += header + "\n" + globals + "\n" + arrayInitializations + "\n" + functions;
      
      createFile((moduleName+".j"), result);
  }

  public String declareGlobal(Node declNode)
  {
          String result = "";
          Boolean isArray = false;
          
          int childNum = declNode.jjtGetNumChildren();
          
          for(int i = 0; i < childNum ; i++)
          {
                  Node newNode = declNode.jjtGetChild(i);
                  
                  if(newNode.toString().equals("DeclID"))
                  {
                          String name = newNode.getVal();
                          result += ".field static ";
                          result += name + " ";
                          
                          if(i == childNum-1)
                                  result += "I\n";
                  }
                  else if(newNode.toString().equals("AddSubOP"))
                  {
                          String signal = newNode.getVal();
                          i++;
                          String value = declNode.jjtGetChild(i).getVal();
                          
                          result += "I = " + signal + value + "\n";
                  }
                  else if(newNode.toString().equals("InicVar"))
                  {
                          String value = newNode.getVal();
                          result += "I = " + value + "\n";
                  }
                  else if(newNode.toString().equals("ArraySize") ||
                                  newNode.toString().equals("IsArray"))
                  {
                	  if(!isArray)
                	  {
                		  result += "[I\n";
                		  isArray = true;
                	  }
                  }
          }
          
          return result;
  }
  
  public String initializeArrays(Node declNode)
  {
          String result = "";
          
          int childNum = declNode.jjtGetNumChildren();
          
          for(int i = 0; i < childNum ; i++)
          {
                  Node newNode = declNode.jjtGetChild(i);
                  
                  if(newNode.toString().equals("ArraySize"))
                  {                       
                          String name = declNode.jjtGetChild(0).getVal();
                          String size = newNode.jjtGetChild(0).getVal();
                          
                          result += ".method static public <clinit>()V\n";
                          result += " .limit stack 2\n";
                          result += " .limit locals 0\n";
                          
                          result += " " + loadInteger(size);
                          result += " newarray int\n";
                          result += " putstatic " + st.name + "/" + name  + " [I\n";

                          result += " return\n";
                          result += ".end method\n\n";
                  }  
          }
          return result;
  }
  
  public String declareFunction(Node funcNode)
  {
          String result = "";
          String header = ".method public static";
          String name = "";
          String rettype = "V";
          String retname = "";
          String args = "";
          String body = "";
          String footer = ".end method";
          
          int childNum = funcNode.jjtGetNumChildren();
          
          for(int i = 0; i < childNum; i++)
          {
                  Node newNode = funcNode.jjtGetChild(i);
                  
                  if(newNode.toString().equals("FuncID"))
                  {
                          if(funcNode.jjtGetChild(i+1).toString().equals("IsArray"))
                          {
                        	  rettype = "[I";
                        	  retname = newNode.getVal();
                          }
                          else if(funcNode.jjtGetChild(i+1).toString().equals("AssignID"))
                          {
                        	  rettype = "I";
                        	  retname = newNode.getVal();
                          }
                          else
                          {
                        	  name = newNode.getVal();
                              retname = "void";
                          }
                  }
                  else if(newNode.toString().equals("AssignID"))
                          name = newNode.getVal();
                  else if(newNode.toString().equals("Args"))
                          args += translateArgs(newNode);
                  else if(newNode.toString().equals("FuncBody"))
                          body += translateFuncBody(newNode,name, retname);
          }
        
          if(name.equals("main"))
                  args = "[Ljava/lang/String;";
          
	// currentTable = st.getSymbolTable("Func",  name);

          result += header + " ";
          result += name;
          result += "(" + args + ")";
          result += rettype + "\n";
          result += body + "\n";
          result += footer + "\n\n";
          
          return result;
  }
  
  public String translateArgs(Node argsNode)
  {
          String result = "";
          
          int argsNum = argsNode.jjtGetNumChildren();
          
          for(int i = 0; i < argsNum; i++)
          {
                  Node newNode = argsNode.jjtGetChild(i);
                  
                  if(newNode.toString().equals("VarListID"))
                  {
                          result += "I";
                          
                          if(i != argsNum-1 && argsNode.jjtGetChild(i+1).toString().equals("IsArray"))
                          {
                                  result = "[I";
                                  i++;
                          }
                  }
          }
          
          return result;
  }
  
  public String translateFuncBody(Node bodyNode, String funcName, String retname)
  {
          String result = "";
          
          numLocals = 0;
          numStack = 20;
          
          LinkedList<Variable> localVariables = getLocalVariables("Func", funcName);
          globalVars = st.localVars;
          
          String limitStack = ".limit stack ";
          String limitLocals = ".limit locals ";
          String body = "";

          String ret = translateReturn(retname, localVariables);
          
          if(funcName.equals("main"))
        	  numLocals += (localVariables.size() + 1);  
          else
        	  numLocals += (localVariables.size());
          
          body += translateStmtLst(bodyNode.jjtGetChild(0), localVariables);
            
          result += limitStack + numStack + "\n";
          result += limitLocals + numLocals + "\n\n";
          result += body + "\n";
          result += ret;
          
          return result;
  }
  
  public String translateReturn(String ret, LinkedList<Variable> localVariables)
  {
	  String result = "";
	  boolean isArray = false;
	  
	  if(ret.equals("void"))
		  return "return\n";
	  
	  if(ret.endsWith("[]"))
	  {
		  int endIndex = ret.indexOf("[");
		  ret = ret.substring(0, endIndex);
		  isArray = true;
	  }
	  
	  result += loadVars(ret, isArray, localVariables);
	  
	  if(isArray)
		  result += "areturn\n";
	  else
		  result += "ireturn\n";
	  
	  return result;
  }
  
  public String translateStmtLst(Node stmtNode, LinkedList<Variable> localVariables)
  {
          String result = "";

	  //SymbolTable funcTable = currentTable;
	  int funcChild = 0; // regista indice das tabelas 'filho' percorridas
         
          
          int numOfStmtLst = stmtNode.jjtGetNumChildren();
          
          for(int i = 0; i < numOfStmtLst; i++)
          {
                  Node newNode = stmtNode.jjtGetChild(i);
                  
                  if(newNode.toString().equals("Assign"))
                  {
                	  String rightContent = "";
                	  
                      Node left = newNode.jjtGetChild(0);
                      Node right = newNode.jjtGetChild(1);
                          
                      result += translateLeftElement(left, localVariables, false);
                      rightContent += translateRightElement(right, localVariables);
                      
                      int typeleft = isArray(left, localVariables);
                      
                      if (right.jjtGetChild(0).toString().equals("ArrSize"))
                    	  typeleft = 2;

                      String typeright = getRightElementType(right, localVariables);
                      
                      System.out.println("TIPO: ");
                      System.out.println("\tLeft: " + typeleft);
                      System.out.println("\tRight: " + typeright);
                      
                      if(typeleft == 2 && typeright.equals("int")) //array = int
                      {
                    	  System.out.println("tipos diferentes!!!!!");
                    	  result += initializeArrayInt(left, rightContent, localVariables);
                      }
                      else
                      {
                    	  result += rightContent;
                    	  result += storeVars(left.getVal(), typeleft, localVariables);
                      }
                    	  
                  }
                  else if(newNode.toString().equals("While")) {

                	  result += translateWhile(newNode, localVariables);
			  funcChild++;
			  
		  }
		  else if(newNode.toString().equals("If")) {
                
			  result += translateIf(newNode, localVariables);
			  funcChild++;
		  }
                  else if (newNode.toString().equals("CallID"))
                  {
                	  Node call2 = null;
                	  Node args = null;

                	  if (i + 1 < numOfStmtLst)
                	  {
                		  Node temp = stmtNode.jjtGetChild(i + 1);

                		  if (temp.toString().equals("CallID2"))
								call2 = temp;
                		  else if (temp.toString().equals("ArgList"))
								args = temp;
							
                	  }
                	  if (i + 2 != numOfStmtLst)
                	  {
                		  Node temp = stmtNode.jjtGetChild(i + 2);

                		  if (temp.toString().equals("ArgList")) 
                			  args = temp;
					
                	  }
			  
                	  result += translateCall(newNode, call2, args, localVariables);

                  }
		}
          
          return result;
  }
  
 public String initializeArrayInt(Node left, String rightContent, LinkedList<Variable> localVariables)
 {
	  String array = left.getVal();
	  String result = "";
	  
	  int tagCounter = labelCounter;
	  labelCounter++;
	  numLocals++;
	  
	  result += "iconst_0\n";
	  result += "istore " + localVariables.size() + "\n";
	  
	  result += "\nLabel" + tagCounter + ":\n";
	  
	  result += loadVars(array, true, localVariables); //first the reference
	  result += "iload " + localVariables.size() + "\n"; //second the index
	  result += rightContent;
	  result += "iastore\n"; //then the store
	  
	  result += "iinc " + localVariables.size() + " 1\n"; //increment the index
	  
	  result += "iload " + localVariables.size() + "\n"; //index
	  result += loadVars(array, true, localVariables);
	  result += "arraylength\n"; //arraysize
	  
	  result += "if_icmpne Label" + tagCounter + "\n";

	  return result;
 } 
  
  public String translateLeftElement(Node leftnode, LinkedList<Variable> localVariables, Boolean inExprTest)
  {
          String result = "";
          
          String var = leftnode.getVal();
          
          if(leftnode.jjtGetNumChildren() != 0) //index
          {      	  
        	  result += loadVars(var, true, localVariables);
        	  result += translateIndex(leftnode.jjtGetChild(0), localVariables);
          }
	  else if (inExprTest) {
		  result += loadVars(var, false, localVariables);
	  }

          return result;
  }

  public String getRightElementType(Node right, LinkedList<Variable> localVariables)
  {
	  
	  Node firstnode = right.jjtGetChild(0);
	  
	  if(firstnode.toString().equals("ArrSize"))
		  return "int[]";
	  else
	  {
		  if(right.jjtGetNumChildren() > 1)
			  return "int";
		  
		  if(firstnode.toString().equals("Term"))
			  return getTermType(firstnode, localVariables);
	  }
	  
	  return "int";
  }
  
  public String translateRightElement(Node right, LinkedList<Variable> localVariables)
  {
	  String result = "";
	  
	  Node firstnode = right.jjtGetChild(0);
	  
	  if(firstnode.toString().equals("ArrSize"))
	  {
		Node newnode = firstnode.jjtGetChild(0);
		
		if(newnode.toString().equals("INTEGER"))
			result += loadInteger(newnode.getVal());
		else if(newnode.toString().equals("Scalar"))
			result += translateScalarAccess(newnode, localVariables);
		
		result += "newarray int\n";
	  }
	  else
	  {
		  if(firstnode.toString().equals("Term"))
			  result += translateTerm(firstnode, localVariables);
		  
		  if(right.jjtGetNumChildren() > 1)
		  {
			  
			  String Op = right.jjtGetChild(1).getVal();
			  
			  result += translateTerm(right.jjtGetChild(2), localVariables);
			  
			  result +=translateOperation(Op);
				  
		  }
	  }
	  
	  return result;
  }
  
  public LinkedList<Variable> getLocalVariables(String type, String name)
  {
          LinkedList<Variable> result = new LinkedList<Variable>();
          
          SymbolTable functionST = st.getSymbolTable(type, name); //Tabela de simbolos correspondente a funcao
          
          int argsNum = functionST.funcArgs.size();
          int localsNum = functionST.localVars.size();
          
          int i;
          
          for(i = 0; i < argsNum; i++)
                  result.add(functionST.funcArgs.get(i));
          
          for(i = 0; i < localsNum; i++)
                  result.add(functionST.localVars.get(i));
          
          return result;
  }

  public String exprTest(Node testNode, LinkedList<Variable> localVariables) {
      String result = "\n;exprTest\n";
      
      //check if retrieving values from the correct nodes, and applying them correctly
      String test = testNode.getVal();

      int numChildren = testNode.jjtGetNumChildren();
      String varLeftName = null;
      for (int i=0; i!=numChildren; i++) {
	    Node child = testNode.jjtGetChild(i);

	    if (child.toString().equals("Lhs")) {		 
		 varLeftName = child.getVal();
		 result += translateLeftElement(child, localVariables, true) + "\n";
		 System.out.println("left: " + varLeftName + result);
		System.out.println("l:" + translateLeftElement(child, localVariables, true) + "\n");
	   }
	    else if (child.toString().equals("Rhs")) {
 
		  if (varLeftName == null) {	
			System.out.println("\nErro em exprTest");
		  }

		  result += translateRightElement(child, localVariables) + "\n";
System.out.println("right: " + child.jjtGetChild(0).jjtGetChild(0).getVal());
System.out.println("r: " + translateRightElement(child, localVariables) + "\n");
	    }

      }

      // test expression here --> arrays needed too??
      if (test.equals("!=")) 
	  result += "\nif_icmpeq"; // int == int
      else if (test.equals("==")) 
	  result += "\nif_icmpne"; // int != int
      else if (test.equals(">"))
	  result += "\nif_icmple"; // int <= int
      else if (test.equals("<"))
	  result += "\nif_icmpge"; // int >= int
      else if (test.equals(">=")) 
	  result += "\nif_icmplt"; // int > int
      else if (test.equals("<="))
	  result += "\nif_icmpgt"; // int < int
          
      return result;
  }

  public String translateTerm(Node termNode, LinkedList<Variable> localVariables)
  {
	  String result = "";
	  
	  String negative = "";
	  
	  String id = "";
	  String id2 = "";
	  boolean isFunc = false;
	  Node argList = null;
	  Node index = null;
	  boolean size = false;
	  
	  int childsNum = termNode.jjtGetNumChildren();
	  
	  for(int i = 0; i < childsNum; i++)
	  {
		  Node newnode = termNode.jjtGetChild(i);
		  
		   if (newnode.toString().equals("INTEGER"))
			  result += loadInteger(newnode.getVal());
		   else if(newnode.toString().equals("AddSubOP"))
		   {
			   if(newnode.getVal().equals("-"))
			   {
				   negative += loadInteger("-1");
				   negative += translateOperation("*");
			   }   
		   }
		   else if(newnode.toString().equals("ID"))
			   id = newnode.getVal();
		   else if(newnode.toString().equals("ID2"))
			   id2 = newnode.getVal();
		   else if(newnode.toString().equals("ArgList"))
			   argList = newnode;
		   else if(newnode.toString().equals("IsFunc"))
			   isFunc = true;
		   else if(newnode.toString().equals("Index"))
			   index = newnode;
		   else if(newnode.toString().equals("Size"))
			   size = true;
	  }
	  
	  if(isFunc) //call listContainsVar(String var, LinkedList<Variable> list )
	  {
		  SimpleNode s1 = new SimpleNode(0);
		  s1.val = id;
		  
		  SimpleNode s2 = new SimpleNode(1);
		  s2.val = id2;
		  
		  result += translateCall(s1, s2, argList, localVariables);
	  }
	  else if(!id.equals("")) //array e scalar access
	  { 	//if (listContainsVar(id, localVariables) != 1) {
		  if(isArray(id,localVariables))
			  result += loadVars(id, true, localVariables);
		  else
			  result += loadVars(id, false, localVariables);
		  
		  if(size)
			  result += "arraylength\n";
		  else if(index != null)
			  {
			  	result += translateIndex(index,localVariables);
			  	result +="iaload\n";
			  }
	  }
	  
	  result += negative;
	  
	  return result;
  }
  
  public String getTermType(Node termNode, LinkedList<Variable> localVariables)
  {
	  String id = "";
	  String id2 = "";
	  boolean isFunc = false;
	  Node argList = null;
	  Node index = null;
	  boolean size = false;
	  
	  int childsNum = termNode.jjtGetNumChildren();
	  
	  for(int i = 0; i < childsNum; i++)
	  {
		  Node newnode = termNode.jjtGetChild(i);
		  
		   if (newnode.toString().equals("INTEGER"))
			  return "int";
		   else if(newnode.toString().equals("AddSubOP"))
			   return "int";
		   else if(newnode.toString().equals("ID"))
			   id = newnode.getVal();
		   else if(newnode.toString().equals("ID2"))
			   id2 = newnode.getVal();
		   else if(newnode.toString().equals("ArgList"))
			   argList = newnode;
		   else if(newnode.toString().equals("IsFunc"))
			   isFunc = true;
		   else if(newnode.toString().equals("Index"))
			   index = newnode;
		   else if(newnode.toString().equals("Size"))
			   size = true;
	  }
	  
	  if(isFunc) //call
	  {
		  if(!id2.equals(""))
			  return "int";
		  else
		  {
			  SymbolTable functionST = st.getSymbolTable("Func", id);
			  String ret = functionST.returnType;
			  
			  if(ret.endsWith("[]"))
				  return "int[]";
			  else
				  return "int";
		  }
	  }
	  else if(!id.equals("")) //array e scalar access
	  { 
		  if(isArray(id,localVariables))
			  return "int[]";
		  else
			  return "int";
	  }
	  
	  return "int";
  }
  
  public String translateWhile(Node whileNode, LinkedList<Variable> localVariables) {

	  String result = "\n;WHILE";
	  int tagCounter = labelCounter;
	  labelCounter++;

	  result += "\nloop" + tagCounter + ":\n";


	  int numChildren = whileNode.jjtGetNumChildren();
	  for (int i=0; i!=2; i++) {

	      Node whileChild = whileNode.jjtGetChild(i);

	      //load comparison variables
	      if (whileChild.toString().equals("Exprtest")) {
		  result += exprTest(whileChild, localVariables);
		  result += " loop_end" + tagCounter + "\n";
	      }

	      // execute statement
	      else if (whileChild.toString().equals("Stmtlst")) {
		  result += translateStmtLst(whileChild, localVariables);
	      }

	  }

	  result += "\ngoto loop" + tagCounter;
	  result += "\nloop_end" + tagCounter + ":\n";

	  return result;
  }  

  public String translateIf(Node ifNode, LinkedList<Variable> localVariables) {
	  String result = "\n;IF\n";

	  int tagCounter = labelCounter;
	  labelCounter++;

	  int numChildren = ifNode.jjtGetNumChildren();
	  Boolean elseSwitch = false; // verifica se já se passou por um 'if'

	  String elseTag = "\nelse_tag" + tagCounter + ":\n";
	  String ifStatement = null;
	  String elseStatement = null;

	  for (int i=0; i!=numChildren; i++) {

	      Node ifChild = ifNode.jjtGetChild(i);

	      //load comparison variables
	      if (ifChild.toString().equals("Exprtest")) {
		  result += exprTest(ifChild, localVariables);
		  //result += " else_tag\n";
	      }

	      // execute statement
	      else if (ifChild.toString().equals("Stmtlst")) {
		
		  if (!elseSwitch)
		      ifStatement = translateStmtLst(ifChild, localVariables);
		  else
		      elseStatement = translateStmtLst(ifChild, localVariables);

		  if (!elseSwitch) { 
		      elseSwitch = true;
		  } 
		      
	      }

	  }


	  if (elseStatement == null) { // apenas if 
	      result += " end_if_tag" + tagCounter + "\n" + ifStatement + "\nend_if_tag" + tagCounter + ": \n";
	  }
	  else {
	      result += " else_tag" + tagCounter + "\n" + ifStatement + "\ngoto end_if_tag" + tagCounter + "\nelse_tag" + tagCounter + ": \n" + elseStatement + "\nend_if_tag" + tagCounter + ":\n";
	  }



/* SKETCH
if(5<4) else_tag
... 
...
goto end_if_tag
else_tag
...
... 
...
end_if_tag
*//*
if (5<4) end_if_tag
....
...
end_if_tag 

*/
	  return result;
  }

   public String translateCall(Node callNode1, Node callNode2, Node argList, LinkedList<Variable> localVariables) {
      
	String result = "\n;Call\n";

	String moduleName;
	String functionName;
	Boolean inModule = true;

	if (callNode1.getVal().equalsIgnoreCase("io")) {
	    functionName = callNode2.getVal();
	    moduleName = callNode1.getVal();
	    inModule = false;
	}
	else if(callNode1.getVal().equals(st.name))
	{
		moduleName = st.name;
		functionName = callNode2.getVal();
	}
	else /*if (callNode2 == null)*/ {
	    functionName = callNode1.getVal();
	    moduleName = st.name;
	}
/*
	else {
	    functionName = callNode2.getVal();
	    moduleName = callNode1.getVal();
	    inModule = false;
	}
*/
;
	// load arguments

	SymbolTable calledFunc = st.getSymbolTable("Func", functionName);
	String funcReturn = "V";
  
	if (inModule) {
	      if (calledFunc.returnType.contains("[]")) {
		  funcReturn = "[I";
	      }
	      else if (!calledFunc.returnType.equalsIgnoreCase("void")) {
		  funcReturn = "I";
	      }
	}

	String args = "(";

	if (argList != null) {
	    int numArgs = argList.jjtGetNumChildren(); 
	
	    for (int i=0; i!=numArgs; i++) {
		Node arg = argList.jjtGetChild(i);
		
		// treat argument (fazer load)
		if (arg.jjtGetChild(0).toString().equals("ArgID")) {

		    Boolean isArray = false; 
		    Variable var = getVariable(arg.getVal(), localVariables);

		    if (var == null) System.out.println("Erro em translateCall");
		    
		    if (var.type.contains("[]")) {
			isArray = true;
			args += "[";
		    }
		
		    args += "I";
		    result += "\n" + loadVars(arg.getVal(), isArray, localVariables);
		}

		else if (arg.jjtGetChild(0).toString().equals("STRING")) {
		    args += "Ljava/lang/String;";
		    result += "\nldc " + arg.getVal() + "";
		}

		else if (arg.jjtGetChild(0).toString().equals("INTEGER")) {
		    args += "I"; 
		    result += loadInteger(arg.getVal()); 
		}
	      
	    }
	} 


	result += "\ninvokestatic " + moduleName + "/" + functionName;

	result += args + ")" + funcReturn + "\n";
	return result;
  }
   
  public String translateScalarAccess(Node scalaraccess, LinkedList<Variable> localVariables)
  {
	  
	  String result = "";
	  
	  String var = scalaraccess.jjtGetChild(0).getVal();

	  if(scalaraccess.jjtGetNumChildren() > 1)
	  {
		  result += loadVars(var, true, localVariables);
		  result += "arraylength\n";
	  }
	  else
	  {
		  if(isArray(var,localVariables))
			  result += loadVars(var, true, localVariables);
		  else
			  result += loadVars(var, false, localVariables);
	  }
		  
	  return result;
  }
  
  public String translateArrayAccess(Node arrayaccess, LinkedList<Variable> localVariables)
  {
	  String result = "";
	  result += loadVars(arrayaccess.jjtGetChild(0).getVal(),true,localVariables);
	  result += translateIndex(arrayaccess.jjtGetChild(1), localVariables);
	  
	  return result;
  }
  
  public String translateIndex(Node index, LinkedList<Variable> localVariables)
  {
	  String result = "";
	  
	  Node newnode = index.jjtGetChild(0);
	  
	  if(newnode.toString().equals("INTEGER"))
	  {
		  result += loadInteger(newnode.getVal());
	  }
	  else if(newnode.toString().equals("IndexID"))
		  result+=loadVars(newnode.getVal(),false,localVariables);
	  
	  return result;
  }
  
  public String translateOperation(String Op)
  {
	  String result = "";
	  
	  if(Op.equals("+"))
		  result = "iadd\n";
	  else if(Op.equals("-"))
		  result = "isub\n";
	  else if(Op.equals("*"))
		  result = "imul\n";
	  else if(Op.equals("/"))
		  result = "idiv\n";
	  else if(Op.equals("&"))
		  result = "iand\n";
	  else if(Op.equals("|"))
		  result = "ior\n";
	  else if(Op.equals("^"))
		  result = "ixor\n";
	  else if(Op.equals(">>"))
		  result = "ishr\n";
	  else if(Op.equals("<<"))
		  result = "ishl\n";
	  else if(Op.equals(">>>"))
		  result = "iushr\n";
	  
	  return result;
  }
  
  public String loadInteger(String integer)
  {
	  String result = "";
	  
	  if(integer.equals("0") || integer.equals("1") || integer.equals("2") ||
	     integer.equals("3") || integer.equals("4") || integer.equals("5"))
		  result += "iconst_" + integer + "\n";
	  else
		  result += "bipush " + integer + "\n";
	  
	  return result;
  }
  
  public String loadVars(String var, boolean isArray, LinkedList<Variable> localVariables)
  {
	  String result = "";
	  
      if(listContainsVar(var, globalVars) != -1) //array global
      {
    	  result += "getstatic " + st.name + "/" + var;
    	  
    	  if(isArray)
    		  result += " [I\n";
    	  else
    		  result += " I\n";
      }
      else //array local
      {
    	  int indice = listContainsVar(var,localVariables);
    	  
    	  if(indice != -1)
    	  {
    		  if(isArray)
    			  result += "aload";
    		  else
    			  result+= "iload";
    		  
    		  if(indice <= 3 && indice >= 0)
    			  result += "_" + indice + "\n";
    		  else
    			  result += " " + indice + "\n";
    	  }
    		  
      }
      
      return result;
  }
  
  //type 1 - integer; 2 - array; 3 - arrayacess
  public String storeVars(String var, int type, LinkedList<Variable> localVariables)
  {
	  String result = "";
	  
	  if(type == 3)
	  {
		  result = "iastore\n";
		  return result;
	  }
	  
	  if(listContainsVar(var, globalVars) != -1) //array global
      {
    	  result += "putstatic " + st.name + "/" + var;
    	  
    	  if(type == 2)
    		  result += " [I\n";
    	  else
    		  result += " I\n";
      }
      else //array local
      {
    	  int indice = listContainsVar(var,localVariables);
    	  
    	  if(indice != -1)
    	  {
    		  if(type == 2)
    			  result += "astore";
    		  else
    			  result+= "istore";
    		  
    		  if(indice <= 3 && indice >= 0)
    			  result += "_" + indice + "\n";
    		  else
    			  result += " " + indice + "\n";
    	  }
    		  
      }
      
      return result;
  }
  
  public void createFile(String fileName, String contents) {
      try{
        // Create file 
    	  
    FileWriter fstream = new FileWriter(fileName);
        BufferedWriter out = new BufferedWriter(fstream);
        out.write(contents);
        //Close the output stream
        out.close();
        
        System.out.println("File written!");
        
    }catch (Exception e){//Catch exception if any
         System.err.println("Error: " + e.getMessage());
        }
  }

  public int listContainsVar(String var, LinkedList<Variable> list )
  {  
                for(int i = 0; i < list.size(); i++)
                {              	
                        Variable v = list.get(i);
                        
                        if(v.name.equalsIgnoreCase(var))
                                return i;
                }
                
                return -1;
 }
 
  public Variable getVariable(String var, LinkedList<Variable> list)
  {
	  int localindex = listContainsVar(var, list);
	  
	  if(localindex != -1)
		  return list.get(localindex);
	  
	  int globalindex = listContainsVar(var, globalVars);
	  return globalVars.get(globalindex);

  }

  public int isArray(Node leftnode, LinkedList<Variable> list)
  {
      String varname = leftnode.getVal();
      
      if(leftnode.jjtGetNumChildren() != 0) //index
      {      	  
    	 return 3; //arrayaccess
      }
      else //var integer
      {
    	  Variable var = getVariable(varname, list);
    	  
    	  if(var.type.equals("int"))
    		  return 1; //integer
    	  
    	  if(var.type.equals("int[]"))
    		  return 2; //array
      }
      
      return -1;
  }
  
  public boolean isArray(String varname, LinkedList<Variable> list)
  {
	  Variable var = getVariable(varname, list);
  
	  if(var.type.equals("int"))
		  return false; //integer
    	  
	  if(var.type.equals("int[]"))
		  return true; //array
      
      return false;
  }
  
}
