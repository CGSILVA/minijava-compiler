package AST;
import AST.Visitor.Visitor;

public class NotEqual extends Exp{
	public Exp e1,e2;
	  
	  public NotEqual(Exp ae1, Exp ae2, int ln) {
		  super(ln);
		  e1=ae1; e2=ae2;
	  }

	  public void accept(Visitor v) {
	    v.visit(this);
	  }
}
