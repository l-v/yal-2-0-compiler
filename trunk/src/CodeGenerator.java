import java.io.*;
import java.util.*;

public class CodeGenerator extends Object {

  SymbolTable st;
  String filename;
  HashMap<Variable,Integer> variables;
  int regCounter;

  
  CodeGenerator(String file, SymbolTable codeSt, Node rootNode) {
      
      st = codeSt;
      filename = file;
      variables = new HashMap<Variable, Integer>();
      regCounter = 0;
      
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
      
      System.out.println(result);
      
      createFile(result);

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
			  body += translateFuncBody(newNode.jjtGetChild(0),name);
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
	  
	  SymbolTable functionST = st.getSymbolTable("Func", funcName); //Tabela de simbolos correspondente a funcao
	  
	  String limitStack = ".limit stack ";
	  String limitLocals = ".limit locals ";
	  
	  int numArgs = functionST.funcArgs.size();
	  int numLocal= functionST.localVars.size();
	  int numStack = 0;
	  
	  limitLocals += (numArgs + numLocal);
	  
	  int numOfStmtLst = bodyNode.jjtGetNumChildren();
	  
	  for(int i = 0; i < numOfStmtLst; i++)
	  {
		  Node newNode = bodyNode.jjtGetChild(i);
		  
		  if(newNode.toString().equals("Assign"))
		  {
			  Node left = newNode.jjtGetChild(0);
			  Node right = newNode.jjtGetChild(1);
			  
		  }
		  else if(newNode.toString().equals("While"))
		  {
			  
		  }
		  else if(newNode.toString().equals("If"))
		  {
			  
		  }
		  else if(newNode.toString().equals("Call"))
		  {
			  
		  }
	  }
	    
	  result += limitStack + numStack + "\n";
	  result += limitLocals + "\n";
	  
	  return result;
  }
  
  public String leftElement (Node leftnode)
  {
	  String result = "";
	  
	  //if()
	  
	  return result;
  }
  
  public void createFile(String contents) {
      try{
	// Create file 
	
    FileWriter fstream = new FileWriter("out.j");
	BufferedWriter out = new BufferedWriter(fstream);
	out.write(contents);
	//Close the output stream
	out.close();
	
	System.out.println("File writed!");
	
    }catch (Exception e){//Catch exception if any
	 System.err.println("Error: " + e.getMessage());
	}
  }

}