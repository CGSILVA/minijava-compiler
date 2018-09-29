package AST;

import AST.Visitor.Visitor;

public class DoubleType extends Type {
	public DoubleType(int ln) {
		super(ln);
	}

	public void accept(Visitor v) {
		v.visit(this);
	}
}
