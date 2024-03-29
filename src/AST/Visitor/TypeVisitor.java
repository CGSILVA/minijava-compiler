package AST.Visitor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import AST.And;
import AST.ArrayAssign;
import AST.ArrayLength;
import AST.ArrayLookup;
import AST.Assign;
import AST.Block;
import AST.BooleanType;
import AST.Call;
import AST.ClassDeclExtends;
import AST.ClassDeclList;
import AST.ClassDeclSimple;
import AST.DoubleLiteral;
import AST.DoubleType;
import AST.EqualEqual;
import AST.False;
import AST.FloatLiteral;
import AST.FloatType;
import AST.Formal;
import AST.FormalList;
import AST.GreatThan;
import AST.GreatThanEqual;
import AST.Identifier;
import AST.IdentifierExp;
import AST.IdentifierType;
import AST.If;
import AST.Instanceof;
import AST.IntArrayType;
import AST.IntegerLiteral;
import AST.IntegerType;
import AST.LessThan;
import AST.LessThanEqual;
import AST.MainClass;
import AST.MethodDecl;
import AST.MethodDeclList;
import AST.Minus;
import AST.NewArray;
import AST.NewObject;
import AST.Not;
import AST.NotEqual;
import AST.Null;
import AST.Plus;
import AST.Print;
import AST.Program;
import AST.This;
import AST.Times;
import AST.True;
import AST.Type;
import AST.VarDecl;
import AST.VarDeclList;
import AST.While;
import Semantic.ClassNode;
import Semantic.ClassWithParentNode;
import Semantic.MethodNode;
import Semantic.Node;
import Semantic.NodeType;


public class TypeVisitor implements Visitor {
    private Map<String, ClassNode> classes;
    private ClassNode currentClass;
    private MethodNode currentMethod;

    public TypeVisitor() {
        super();

        classes = new HashMap<String, ClassNode>();
        currentClass = null;
        currentMethod = null;
    }

    public Map<String, ClassNode> getClasses() {
        return classes;
    }

    public void printTypes() {
        for (Map.Entry<String, ClassNode> entry : classes.entrySet()) {
            ClassNode klass = entry.getValue();
            if (klass instanceof ClassWithParentNode) {
                System.out.println(
                        entry.getKey() +
                        " extends " +
                        ((ClassWithParentNode)klass).getParent()
                );
            } else {
                System.out.println(entry.getKey());
            }

            for (Map.Entry<String, Node> inner :
                    klass.getMembers().entrySet()) {
                Node value = inner.getValue();

                System.out.println(
                        "    " +
                        inner.getKey() +
                        ": " +
                        value.getType().toString()
                );

                if (value instanceof MethodNode) {
                    MethodNode mvalue = (MethodNode)value;
                    List<Node> parameterTypes = mvalue.getParameters();
                    Map<Integer, String> parameterNames =
                        mvalue.getParametersPositions();

                    System.out.println("        Parameters:");
                    for (Map.Entry<Integer, String> param :
                            parameterNames.entrySet()) {
                        System.out.println(
                                "            " +
                                param.getValue() +
                                ": " +
                                parameterTypes
                                    .get(param.getKey())
                                    .getType()
                                    .toString()
                        );
                    }

                    System.out.println("        Local Variables:");
                    for (Map.Entry<String, Node> var :
                            mvalue.getLocalVariables().entrySet()) {
                        System.out.println(
                                "            " +
                                var.getKey() +
                                ": " +
                                var.getValue().getType()
                        );
                    }
                }
            }
        }
    }

    public void visit(Program n) {
        ClassDeclList classDeclarations = n.cl;
        int size = classDeclarations.size();
        for (int i = 0; i < size; ++i) {
            classDeclarations.elementAt(i).accept(this);
        }
    }

    public void visit(MainClass n) { }

    public void visit(ClassDeclSimple n) {
        ClassNode node = new ClassNode(n.i.s);
        currentClass = node;
        classes.put(n.i.s, node);

        VarDeclList variables = n.vl;
        int variablesCount = variables.size();
        for (int i = 0; i < variablesCount; ++i) {
            variables.elementAt(i).accept(this);
        }

        MethodDeclList methods = n.ml;
        int methodsCount = methods.size();
        for (int i = 0; i < methodsCount; ++i) {
            methods.elementAt(i).accept(this);
        }

        currentClass = null;
    }

    public void visit(ClassDeclExtends n) {
        ClassWithParentNode node = new ClassWithParentNode(n.i.s, n.j.s);
        currentClass = node;
        classes.put(n.i.s, node);

        VarDeclList variables = n.vl;
        int variablesCount = variables.size();
        for (int i = 0; i < variablesCount; ++i) {
            variables.elementAt(i).accept(this);
        }

        MethodDeclList methods = n.ml;
        int methodsCount = methods.size();
        for (int i = 0; i < methodsCount; ++i) {
            methods.elementAt(i).accept(this);
        }

        currentClass = null;
    }

    public void visit(VarDecl n) {
        Node node = new Node(nodeTypeOf(n.t));
        if(n.t instanceof IdentifierType) {
            node.iam = ((IdentifierType)n.t).s;
        }

        if (currentMethod != null) {
            currentMethod.getLocalVariables().put(n.i.s, node);
        } else {
            currentClass.getMembers().put(n.i.s, node);
        }
    }

    public void visit(MethodDecl n) {
        MethodNode node =
            new MethodNode(new Semantic.Node(nodeTypeOf(n.t)));
        currentMethod = node;
        currentClass.getMembers().put(n.i.s, node);

        FormalList parameters = n.fl;
        int parametersCount = parameters.size();
        for (int i = 0; i < parametersCount; ++i) {
            parameters.elementAt(i).accept(this);
        }

        VarDeclList variables = n.vl;
        int variablesCount = variables.size();
        for (int i = 0; i < variablesCount; ++i) {
            variables.elementAt(i).accept(this);
        }

        currentMethod = null;
    }

    public void visit(Formal n) {
        List<Node> parameters = currentMethod.getParameters();

        Semantic.Node node = new Semantic.Node(nodeTypeOf(n.t));
        if(n.t instanceof IdentifierType) {
            node.iam = ((IdentifierType)n.t).s;
        }


        parameters.add(node);
        currentMethod
            .getParametersPositions()
            .put(parameters.size() - 1, n.i.s);
    }

    public void visit(IntArrayType n) { }
    public void visit(BooleanType n) { }
    public void visit(IntegerType n) { }
    public void visit(IdentifierType n) { }
    public void visit(Block n) { }
    public void visit(If n) { }
    public void visit(While n) { }
    public void visit(Print n) { }
    public void visit(Assign n) { }
    public void visit(ArrayAssign n) { }
    public void visit(And n) { }
    public void visit(LessThan n) { }
    public void visit(Plus n) { }
    public void visit(Minus n) { }
    public void visit(Times n) { }
    public void visit(ArrayLookup n) { }
    public void visit(ArrayLength n) { }
    public void visit(Call n) { }
    public void visit(IntegerLiteral n) { }
    public void visit(True n) { }
    public void visit(False n) { }
    public void visit(IdentifierExp n) { }
    public void visit(This n) { }
    public void visit(NewArray n) { }
    public void visit(NewObject n) { }
    public void visit(Not n) { }
    public void visit(Identifier n) { }
    public void visit(FloatLiteral n) { }
    public void visit(FloatType n) { }
    public void visit(GreatThan n) { }
	public void visit(GreatThanEqual n) { }
	public void visit(LessThanEqual n) { }
	public void visit(DoubleLiteral n) { }
	public void visit(DoubleType n) { }
	public void visit(EqualEqual n) { }
	public void visit(NotEqual n) { }
	public void visit(Instanceof n) { }
	public void visit(Null n) { }
	
    private static NodeType nodeTypeOf(Type t) {
        if (t instanceof BooleanType) {
            return NodeType.BOOLEAN;
        } else if (t instanceof FloatType) {
            return NodeType.FLOAT;
        } else if (t instanceof DoubleType) {
            return NodeType.DOUBLE;
        } else if (t instanceof IntegerType) {
            return NodeType.INT;
        } else if (t instanceof IntArrayType) {
            return NodeType.INTARRAY;
        } else if (t instanceof IdentifierType) {
            return NodeType.CLASS;
        } else {
            return NodeType.UNKNOWN;
        }
    }

	
}