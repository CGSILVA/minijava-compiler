package Semantic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MethodNode extends Node{

	private List<Node> parameters;
	private Map<Integer, String> parametersPositions;
	
	private Map<String, Node> localVariables;
	private Node returnType;
	
	public MethodNode(Node type) {
		super(NodeType.METHOD);
		
		this.parameters = new ArrayList<Node>();
		this.parametersPositions = new HashMap<Integer, String>();
		this.localVariables = new HashMap<String, Node>();
		this.returnType = type;
	}

	public List<Node> getParameters() {
		return parameters;
	}

	public Map<Integer, String> getParametersPositions() {
		return parametersPositions;
	}

	public Map<String, Node> getLocalVariables() {
		return localVariables;
	}

	public Node getReturnType() {
		return this.returnType;
	}
	
}
