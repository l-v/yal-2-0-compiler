

/** Keeps information about errors to check in SymbolTable **/
public class OnHoldError {

	String funcName;
	String callType;
	int line;
	Node calledArgs;
	String assignTo;

  
	public OnHoldError (String fName, String cType, int errorLine, Node args, String assignLeft) {
	    funcName = fName;
	    callType = cType;
	    line = errorLine;
	    calledArgs = args;
	    assignTo = assignLeft;
	}
}

