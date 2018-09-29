package AST;
import AST.Visitor.Visitor;

public class Null extends Exp {
  public Null(int ln) {
    super(ln);
  }
  public void accept(Visitor v) {
    v.visit(this);
  }
}
