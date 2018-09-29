package Semantic;

import java.util.HashMap;
import java.util.Map;

public class ClassNode extends Node{
	
	private Map<String, Node> members;
	private String name;
	
	public ClassNode(String name){
		super(NodeType.CLASS);
		members = new HashMap<String, Node>();
		this.name = name;
	}
	
	public Map<String, Node> getMembers(){
		return this.members;
	}
	
	public String getName(){
		return this.name;
	}

}
