import java.io.*;
import java.util.*;

public class CodeGenerator extends Object {

  SymbolTable st;
  String filename;
  LinkedList<Variable> globalVars;

  
  CodeGenerator(String file, SymbolTable codeSt, Node rootNode) {
      
      st = codeSt;
      filename = file;
      
      String result = "";
      String header = "";
      String globals = "";
      String arrayInitializations = "";
      String functions = "";
          
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
                        }
        
                        else if (varType.equals("Decl"))
                        {
                                globals += declareGlobal(newNode);
                                arrayInitializations += initializeArrays(newNode);
                        }
              
                        else if (varType.equals("Func"))
                            functions += declareFunction(newNode);
          }

      result += header + "\n" + globals + "\n" + arrayInitializations + "\n" + functions;
      
      createFile(file, result);

  }

  public String declareGlobal(Node declNode)
  {
          String result = "";
          
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
                          
                          result += "I " + signal + value + "\n";
                  }
                  else if(newNode.toString().equals("InicVar"))
                  {
                          String value = newNode.getVal();
                          result += "I " + value + "\n";
                  }
                  else if(newNode.toString().equals("ArraySize") ||
                                  newNode.toString().equals("IsArray"))
                          result += "[I\n";               
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
                          
                          result += " bipush " + size + "\n";
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
          String ret = "V";
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
                                  ret = "[I";
                          else if(funcNode.jjtGetChild(i+1).toString().equals("AssignID"))
                                  ret = "I";
                          else
                                  name = newNode.getVal();
                  }
                  else if(newNode.toString().equals("AssignID"))
                          name = newNode.getVal();
                  else if(newNode.toString().equals("Args"))
                          args += translateArgs(newNode);
                  else if(newNode.toString().equals("FuncBody"))
                          body += translateFuncBody(newNode,name);
          }
        
          if(name.equals("main"))
                  args = "[Ljava/lang/String;";
          
          result += header + "\n";
          result += name;
          result += "(" + args + ")";
          result += ret + "\n";
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
  
  public String translateFuncBody(Node bodyNode, String funcName)
  {
          String result = "";
          
          LinkedList<Variable> localVariables = getLocalVariables("Func", funcName);
          globalVars = st.localVars;
          
          String limitStack = ".limit stack ";
          String limitLocals = ".limit locals ";
          String body = "";
          
          limitLocals += (localVariables.size());
          int numStack = 0;
          
          body += translateStmtLst(bodyNode.jjtGetChild(0), localVariables);
            
          result += limitStack + numStack + "\n";
          result += limitLocals + "\n\n";
          result += body + "\n";
          
          return result;
  }
  
  public String translateStmtLst(Node stmtNode, LinkedList<Variable> localVariables)
  {
          String result = "";
          
          int numOfStmtLst = stmtNode.jjtGetNumChildren();
          
          for(int i = 0; i < numOfStmtLst; i++)
          {
                  Node newNode = stmtNode.jjtGetChild(i);
                  
                  if(newNode.toString().equals("Assign"))
                  {
                          Node left = newNode.jjtGetChild(0);
                          Node right = newNode.jjtGetChild(1);
                          
                          result += translateLeftElement(left, localVariables) + "\n";
                          
                  }
                  else if(newNode.toString().equals("While"))
                  {
                          result += translateWhile(newNode, localVariables);
                  }
                  else if(newNode.toString().equals("If"))
                  {
                          result += translateIf(newNode, localVariables);
                  }
                  else if(newNode.toString().equals("CallID"))
                  {
			Node call2 = null;
			Node args = null;

			if (i+1 != numOfStmtLst) {
			      Node temp = newNode.jjtGetChild(i+1);

			      if (temp.toString().equals("CallID2")) {
				  call2 = temp;
			      }

			      else if (temp.toString().equals("ArgList")) {
				  args = temp;
			      }
			}

			else if (i+2 != numOfStmtLst) {
			      Node temp = newNode.jjtGetChild(i+2);
				
			      if (temp.toString().equals("ArgList")) {
				  args = temp;
			      }
			}

			result += translateCall(newNode, call2, args, localVariables);
			    
                  }
          }
          
          return result;
  }
  
  public String translateLeftElement(Node leftnode, LinkedList<Variable> localVariables)
  {
          String result = "";
          
          String var = leftnode.getVal();
          
          if(leftnode.jjtGetNumChildren() != 0) //index
          {
                  Node index = leftnode.jjtGetChild(0).jjtGetChild(0);
                  
                  if(index.toString().equals("INTEGER"))
                  {
                          if(containsGlobal(var, globalVars))
                          {
                                  result += "getstatic fields/" + var + "[I";
                          }
                          else
                          {
                                  //int number = ;
                          }
                          
                  }
                  else if(index.toString().equals("IndexID"))
                  {
                          result += "[" + index.getVal() + "]";
                  }
          }
          else //var integer ou size
          {
                  
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


  public String exprTest(Node testNode) {
      String result = "\n;exprTest";
      
      //check if retrieving values from the correct nodes, and applying them correctly
      String test = testNode.getVal();

      int numChildren = testNode.jjtGetNumChildren();
      for (int i=0; i!=numChildren; i++) {
	    Node child = testNode.jjtGetChild(i);

	    if (child.toString().equals("Lhs")) {
		   result += translateLeftElement(left, localVariables) + "\n";
	    }
	    else if (child.toString().equals("Rhs")) {
		  // rhs method
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
      else if (test.equals("<=")) 
	  result += "\nif_icmplt"; // int > int
      else if (test.equals(">="))
	  result += "\nif_icmpgt"; // int < int
      

     
      return result;
  }


  public String translateWhile(Node whileNode, LinkedList<Variable> localVariables) {

	  String result = "\n;WHILE";
	  result += "\nloop:";


	  int numChildren = whileNode.jjtGetNumChildren();
	  for (int i=0; i!=numChildren; i++) {

	      Node whileChild = whileNode.jjtGetChild(i);

	      //load comparison variables
	      if (whileChild.toString().equals("ExprTest")) {
		  result += exprTest(whileChild);
		  result += " loop_end";
	      }

	      // execute statement
	      else if (whileChild.toString().equals("Stmtlst")) {
		  result += translateStmtLst(whileChild, localVariables);
	      }

	  }

	  result += "\ngoto loop";
	  result += "\nloop_end:";

	  return result;
  }  

  public String translateIf(Node whileNode, LinkedList<Variable> localVariables) {
	  String result = "";
	  



	  return result;
  }


  public String translateCall(Node callNode1, Node callNode2, Node argList, LinkedList<Variable> localVariables) {
      
	String result = "\n;Call";

	String moduleName;
	String functionName;

	if (callNode2 == null) {
	    functionName = callNode1.getVal();
	    moduleName = st.name;
	}

	else {
	    functionName = callNode2.getVal();
	    moduleName = callNode1.getVal();
	}


	// load arguments
	String args = "(";

	if (argList != null) {
	    int numArgs = argList.jjtGetNumChildren(); 

	    for (int i=0; i!=numArgs; i++) {
		Node arg = argList.jjtGetChild(i);

		// treat argument (descobrir quais sao int e quais sao int[]
		if (arg.jjtGetChild(0).toString().equals("ArgID")) {

		}

		else if (arg.jjtGetChild(0).toString().equals("STRING")) {

		}

		else if (arg.jjtGetChild(0).toString().equals("INTEGER")) {

		}
	      
	    }
	}


	result += "\ninvokestatic " + moduleName + "/" + functionName;
	result += args + ")V";
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


  public boolean containsGlobal(String var, LinkedList<Variable> globals )
        {
                for(int i = 0; i < globals.size(); i++)
                {
                        Variable v = globals.get(i);
                        
                        if(v.name == var)
                                return true;
                }
                
                return false;
        }

}