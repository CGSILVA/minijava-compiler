package Semantic;

public class Node {

	private NodeType nodeType;
	public String iam;
	
	public Node(NodeType nodeType){
		this.nodeType = nodeType;
	}
	
	public NodeType getType(){
		return this.nodeType;
	}
	
	public String getIam(){
		return this.iam;
	}
	
}
