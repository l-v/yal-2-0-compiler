

/** Keeps information about errors to check symboltable as soon as completely constructed **/
public class OnHoldError {

	String funcName;
	String callType;
	int line;
	Node calledArgs;

  
	public OnHoldError (String fName, String cType, int errorLine, Node args) {
	    funcName = fName;
	    callType = cType;
	    line = errorLine;
	    calledArgs = args;
	}
}

