package AST;
import AST.Visitor.Visitor;

public class VarDecl extends ASTNode {
  public Type t;
  public Identifier i;
  public Exp exp;
  
  public VarDecl(Type at, Identifier ai, int ln, Exp exp) {
    super(ln);
    t=at; i=ai; this.exp=exp;
  }

  public void accept(Visitor v) {
    v.visit(this);
  }
}
