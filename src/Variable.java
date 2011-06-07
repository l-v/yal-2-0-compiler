

/**
* Class that represents a variable 
**/
public class Variable {
      
	String type;
	String name; 

	Integer arraySize; // tamanho da variavel tipo array; valor -1 para var. escalares
	//LinkedList<String> funcID; // id das funcoes necessarios para obter o valor da variavel
	

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