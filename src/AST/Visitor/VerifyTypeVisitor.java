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
import AST.ExpList;
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
import AST.StatementList;
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

public class VerifyTypeVisitor implements Visitor {
    private TypeVisitor declaredTypes;

    private Node previousNode;
    private Node currentNode;
    private NodeType rValueType;
    private String lastId;

    private int returnValue;

    public VerifyTypeVisitor(TypeVisitor declaredTypes) {
        super();
        this.declaredTypes = declaredTypes;
        this.lastId = "";
        returnValue = 0;
    }

    public int getReturnValue() {
        return returnValue;
    }

    public void visit(Program n) {
        ClassDeclList classes = n.cl;
        int size = classes.size();
        for (int i = 0; i < size; ++i) {
            classes.elementAt(i).accept(this);
        }
    }

    public void visit(MainClass n) { }

    public void visit(ClassDeclSimple n) {
        Map classes = this.declaredTypes.getClasses();
        ClassNode myNode = (ClassNode)classes.get(n.i.s);

        if(myNode == null) {
            returnValue = 1;
            System.out.print("[CLASS ERROR]: " + n.i.s);
            System.out.println(" not in symbol table");
            Node unknownClass = new Node(NodeType.UNKNOWN);
            classes.put(n.i.s, unknownClass);
            return;
        }

        NodeType typeDeclared = myNode.getType();
        if(typeDeclared != NodeType.CLASS && typeDeclared != NodeType.UNKNOWN) {
            returnValue = 1;
            System.out.print("[CLASS ERROR]: " + n.i.s);
            System.out.println(" declared as " + typeDeclared);
        }

        VarDeclList vars = n.vl;
        int varsize = vars.size();
        for (int i = 0; i < varsize; ++i) {
            this.currentNode = myNode;
            vars.elementAt(i).accept(this);
        }

        MethodDeclList methods = n.ml;
        int methodsize = methods.size();
        for (int i = 0; i < methodsize; ++i) {
            this.currentNode = myNode;
            methods.elementAt(i).accept(this);
        }
    }

    public void visit(ClassDeclExtends n) {
        Map classes = this.declaredTypes.getClasses();
        ClassWithParentNode myNode = (ClassWithParentNode)classes
                                            .get(n.i.s);

        if(myNode == null) {
            returnValue = 1;
            System.out.print("[CLASS ERROR]: " + n.i.s);
            System.out.println(" not in symbol table");
            Node unknownClass = new Node(NodeType.UNKNOWN);
            classes.put(n.i.s, unknownClass);
            return;
        }

        NodeType typeDeclared = myNode.getType();
        if(typeDeclared != NodeType.CLASS && typeDeclared != NodeType.UNKNOWN) {
            returnValue = 1;
            System.out.print("[CLASS ERROR]: " + n.i.s);
            System.out.println(" declared as " + typeDeclared);
        }

        VarDeclList vars = n.vl;
        int varsize = vars.size();
        for (int i = 0; i < varsize; ++i) {
            this.currentNode = myNode;
            vars.elementAt(i).accept(this);
        }
       
        MethodDeclList methods = n.ml;
        int methodsize = methods.size();
        for (int i = 0; i < methodsize; ++i) {
            this.currentNode = myNode;
            methods.elementAt(i).accept(this);
        }
    }

    public void visit(VarDecl n) { }

    public void visit(MethodDecl n) {
        ClassNode currentNode = (ClassNode)this.currentNode;

        MethodNode declMethod = (MethodNode)(currentNode.getMembers()
                                            .get(n.i.s));

        if(declMethod == null) {
            returnValue = 1;
            System.out.print("[METHOD ERROR]: \tline " + n.getLine());
            System.out.println(": " + n.i.s + " not found!");
            return;
        }

        NodeType expecRetType = declMethod.getReturnType().getType();
        NodeType foundRetType = this.nodeTypeOf(n.t);
        if(!expecRetType.equals(foundRetType)) {
            returnValue = 1;
            System.out.print("[METHOD ERROR]: \tline" + n.getLine());
            System.out.print(": " + n.i + " returnType mismatch, ");
            System.out.print("Exected type: " + expecRetType);
            System.out.println(" Found type: " + foundRetType);
        }


        List<Node> params = declMethod.getParameters();
        FormalList formals = n.fl;
        for(int i = 0; i < formals.size(); i++) {
            Formal f = formals.elementAt(i);
            NodeType expecParamType = params.get(i).getType();
            NodeType foundParamType = this.nodeTypeOf(f.t);
            if(!expecParamType.equals(foundParamType)) {
                returnValue = 1;
                System.out.print("[METHOD ERROR]:\tline " + n.getLine());
                System.out.print(": Exected param type=" + expecParamType);
                System.out.println(" Found param type=" + foundParamType);
            }
        }

        this.previousNode = currentNode;
        this.currentNode = declMethod;

        Map expecMethodVars = declMethod.getLocalVariables();
        VarDeclList vars = n.vl;
        for (int i = 0; i < vars.size(); ++i) {
            vars.elementAt(i).accept(this);
        }

        StatementList stmts = n.sl;
        for (int i = 0; i < stmts.size(); ++i) {
            stmts.elementAt(i).accept(this);
        }

        n.e.accept(this);
        if (!declMethod.getReturnType().getType().equals(rValueType)) {
            returnValue = 1;
            System.err.println("[METHOD ERROR]: Invalid return type!");
            System.err.println("    Got " + rValueType);
            System.err.println(
                    " Wanted " + declMethod.getReturnType().getType());
        }
    }

    public void visit(Formal n) {
        Map expecMethodVars = ((MethodNode)this.currentNode).getLocalVariables();
        NodeType nt = nodeTypeOf(n.t);
        String iden = n.i.s;
        expecMethodVars.put(iden, nt);
    }

    public void visit(IntArrayType n) {
        this.rValueType = nodeTypeOf(n);
    }

    public void visit(BooleanType n) {
        this.rValueType = nodeTypeOf(n);
    }

    public void visit(IntegerType n) {
        this.rValueType = nodeTypeOf(n);
    }

    public void visit(IdentifierType n) {
        this.rValueType = nodeTypeOf(n);
    }

    public void visit(Block n) {
        StatementList sl = n.sl;
        int size = sl.size();

        for (int i = 0; i < size; ++i) {
            sl.elementAt(i).accept(this);
        }
    }

    public void visit(If n) {
        n.e.accept(this);
        if(!this.rValueType.equals(NodeType.BOOLEAN)) {
            returnValue = 1;
            System.out.print("[IF ERROR]: \tline " + n.getLine() + ":");
            System.out.print(" non-boolean condition type="+this.rValueType);
            System.out.println(" from expression=" + n.e);
        }
        n.s1.accept(this);
        n.s2.accept(this);
        
    }

    public void visit(While n) {
        n.e.accept(this);
        if(!this.rValueType.equals(NodeType.BOOLEAN)) {
            returnValue = 1;
            System.out.print("[WHILE ERROR]: \tline " + n.getLine());
            System.out.println(": CONDITION NOT OF BOOLEAN TYPE!");
        }
        n.s.accept(this);
    }

    public void visit(Print n) {
        n.e.accept(this);
        if (!this.rValueType.equals(NodeType.INT)) {
            returnValue = 1;
            System.out.print("[PRINT ERROR]: \tline " + n.getLine());
            System.out.println(": WE CAN ONLY PRINT INTEGERS");
        }
    }

    public void visit(Assign n) {
        Node myNode = this.currentNode;
        Map scope;
        if(myNode instanceof ClassNode) {
            scope = new HashMap(((ClassNode)myNode).getMembers());
        } else {
            scope = new HashMap(((MethodNode)myNode).getLocalVariables());
            List<Node> params = ((MethodNode)myNode).getParameters();
            Map<Integer, String> paramPos;
            paramPos = ((MethodNode)myNode).getParametersPositions();

            int index = 0;
            for(Node p: params) {
                String id = paramPos.get(index);
                scope.put(id, p);
            }
            
        }
        Node node = (Node)scope.get(n.i.s);

        if(node == null) {
            Node prevNode = this.previousNode;

            System.out.println(((ClassNode)prevNode).getName());
            if(prevNode instanceof ClassWithParentNode)
            System.out.println(((ClassWithParentNode)prevNode).getParent());

            while(node == null && prevNode instanceof ClassWithParentNode) {
                scope = ((ClassNode)prevNode).getMembers();
                node = (Node)scope.get(n.i.s);
                prevNode = this.declaredTypes.getClasses()
                               .get(((ClassWithParentNode)prevNode)
                               .getParent());
            }
            if(node == null) {
                scope = ((ClassNode)prevNode).getMembers();
                node = (Node)scope.get(n.i.s);
            }

            if(node == null) {
                return;
            }
        }

        NodeType expecType = node.getType(); 

        n.e.accept(this);
        NodeType foundType = this.rValueType;
        if(!expecType.equals(foundType)) {
            returnValue = 1;
            System.out.print("[ASSIGN ERROR]: \tline " + n.getLine());
            System.out.print(": " + n.i + " expecs " + expecType);
            System.out.println(" found " + foundType);
        }
    }

    public void visit(ArrayAssign n) {
        n.e1.accept(this);
        if(!this.rValueType.equals(NodeType.INT)) {
            returnValue = 1;
            System.out.print("[ARRAYASSIGN ERROR]: \tline " + n.getLine());
            System.out.println(": INDEX NOT INT TYPE!");
        }
    }

    public void visit(And n) {
        n.e1.accept(this);
        if(!this.rValueType.equals(NodeType.BOOLEAN)) {
            returnValue = 1;
            System.out.print("[AND ERROR]: line "+n.getLine());
            System.out.println(": ARG1 NOT NUMBER");
        }
        n.e2.accept(this);
        if(!this.rValueType.equals(NodeType.BOOLEAN)) {
            returnValue = 1;
            System.out.print("[AND ERROR]: line "+n.getLine());
            System.out.println(": ARG2 NOT NUMBER");
        }
        this.rValueType = NodeType.BOOLEAN;
    }

    public void visit(LessThan n) {
        n.e1.accept(this);
        if(!this.rValueType.equals(NodeType.INT) &&
           !this.rValueType.equals(NodeType.FLOAT) &&
           !this.rValueType.equals(NodeType.DOUBLE)) {
            returnValue = 1;
            System.out.print("[LESSTHAN ERROR]: line "+n.getLine());
            System.out.println(": ARG1 NOT NUMBER");
        }
        n.e2.accept(this);
        if(!this.rValueType.equals(NodeType.INT) &&
           !this.rValueType.equals(NodeType.FLOAT) &&
           !this.rValueType.equals(NodeType.DOUBLE)) {
            returnValue = 1;
            System.out.print("[LESSTHAN ERROR]: line "+n.getLine());
            System.out.println(": ARG2 NOT NUMBER");
        }
        this.rValueType = NodeType.BOOLEAN;
    }

    public void visit(Plus n) {
        NodeType arg1, arg2;

        n.e1.accept(this);
        arg1 = this.rValueType;
        if(!this.rValueType.equals(NodeType.INT) &&
           !this.rValueType.equals(NodeType.FLOAT) &&
           !this.rValueType.equals(NodeType.DOUBLE)) {
            returnValue = 1;
            System.out.print("[PLUS ERROR]: line "+n.getLine());
            System.out.println(": ARG1 NOT NUMBER");
        }

        n.e2.accept(this);
        arg2 = this.rValueType;
        if(!this.rValueType.equals(NodeType.INT) &&
           !this.rValueType.equals(NodeType.FLOAT) &&
           !this.rValueType.equals(NodeType.DOUBLE)) {
            returnValue = 1;
            System.out.print("[PLUS ERROR]: line "+n.getLine());
            System.out.println(": ARG2 NOT NUMBER");
        }

        if(arg1.equals(NodeType.FLOAT) || arg2.equals(NodeType.FLOAT)) {
            this.rValueType = NodeType.FLOAT;
        } else {
            this.rValueType = NodeType.INT;
        }
    }

    public void visit(Minus n) {
        NodeType arg1, arg2;

        n.e1.accept(this);
        arg1 = this.rValueType;
        if(!this.rValueType.equals(NodeType.INT) &&
           !this.rValueType.equals(NodeType.FLOAT) &&
           !this.rValueType.equals(NodeType.DOUBLE)) {
            returnValue = 1;
            System.out.print("[MINUS ERROR]: line "+n.getLine());
            System.out.println(": ARG1 NOT NUMBER");
        }

        n.e2.accept(this);
        arg2 = this.rValueType;
        if(!this.rValueType.equals(NodeType.INT) &&
           !this.rValueType.equals(NodeType.FLOAT) &&
           !this.rValueType.equals(NodeType.DOUBLE)) {
            returnValue = 1;
            System.out.print("[MINUS ERROR]: line "+n.getLine());
            System.out.println(": ARG2 NOT NUMBER");
        }

        if(arg1.equals(NodeType.FLOAT) || arg2.equals(NodeType.FLOAT)) {
            this.rValueType = NodeType.FLOAT;
        } else {
            this.rValueType = NodeType.INT;
        }
    }

    public void visit(Times n) {
        NodeType arg1, arg2;

        n.e1.accept(this);
        arg1 = this.rValueType;
        if(!this.rValueType.equals(NodeType.INT) &&
           !this.rValueType.equals(NodeType.FLOAT) &&
           !this.rValueType.equals(NodeType.DOUBLE)) {
            returnValue = 1;
            System.out.print("[TIMES ERROR]: \tline "+n.getLine());
            System.out.println(": ARG1 NOT NUMBER");
        }

        n.e2.accept(this);
        arg2 = this.rValueType;
        if(!this.rValueType.equals(NodeType.INT) &&
           !this.rValueType.equals(NodeType.FLOAT) &&
           !this.rValueType.equals(NodeType.DOUBLE)) {
            returnValue = 1;
            System.out.print("[TIMES ERROR]: \tline "+n.getLine());
            System.out.println(": ARG2 NOT NUMBER");
        }

        if(arg1.equals(NodeType.FLOAT) || arg2.equals(NodeType.FLOAT)) {
            this.rValueType = NodeType.FLOAT;
        } else {
            this.rValueType = NodeType.INT;
        }
    }

    public void visit(ArrayLookup n) {
        n.e1.accept(this);
        if (!this.rValueType.equals(NodeType.INTARRAY)) {
            returnValue = 1;
            System.out.print("[ARRAYLOOKUP ERROR]: \tline " + n.getLine());
            System.out.println(": CAN ONLY DEREFERENCE ARRAYS");
        }

        n.e2.accept(this);
        if (!this.rValueType.equals(NodeType.INT)) {
            returnValue = 1;
            System.out.print("[ARRAYLOOKUP ERROR]: \tline " + n.getLine());
            System.out.println(": ARRAY INDEX MUST BE INTEGER");
        }
    }

    public void visit(ArrayLength n) {
        n.e.accept(this);
        if(!this.rValueType.equals(NodeType.INTARRAY)) {
            returnValue = 1;
            System.out.print("[ARRAYLENGTH ERROR]: \tline " + n.getLine());
            System.out.println(": INDEX NOT INT TYPE!");
        }

        rValueType = NodeType.INT;
    }

    public void visit(Call n) {
        n.e.accept(this);

        String callId = this.lastId;                            
        MethodNode methScope = (MethodNode)this.currentNode;    
        ClassNode clsScope = (ClassNode)this.previousNode;      

        
        Map<String, Node> mScope = methScope.getLocalVariables();
        Map<Integer, String> index = methScope.getParametersPositions();
        int ctr = 0;
        for(Node myn: methScope.getParameters()) {
            mScope.put(index.get(ctr++), myn);
        }

        
        Map<String, Node> cScope = clsScope.getMembers();

        
        if(mScope.containsKey(callId)) { 
            Node myn = mScope.get(callId);
            if(myn.getType().equals(NodeType.CLASS)) {
                this.previousNode = this.declaredTypes.getClasses()
                                        .get(myn.getIam());
            }
        } else if(cScope.containsKey(callId)) {
            Node myn = cScope.get(callId);
            if(myn.getType().equals(NodeType.CLASS)) {
                this.previousNode = this.declaredTypes.getClasses()
                                        .get(myn.getIam());
            }
        } else {

        }

        Node caller = this.previousNode;
        Map scope = ((ClassNode)caller).getMembers();
        
        Node myNode = (Node)scope.get(n.i.s);

        ExpList exprs = n.el;
        int size = exprs.size();
        for (int i = 0; i < size; ++i) {
            exprs.elementAt(i).accept(this);
        }

        if(myNode != null) {
            this.rValueType = ((MethodNode)myNode).getReturnType().getType();
        } else if(caller instanceof ClassWithParentNode) { 
       
            Node prevNode = caller;
            while(myNode == null && prevNode instanceof ClassWithParentNode) {
                scope = ((ClassNode)prevNode).getMembers();
                myNode = (Node)scope.get(n.i.s);
                prevNode = this.declaredTypes.getClasses().get(
                                ((ClassWithParentNode)prevNode).getParent()
                                                              );
            }
            if(myNode == null) { 
                scope = ((ClassNode)prevNode).getMembers();
                myNode = (Node)scope.get(n.i.s);
            }

            if(myNode == null) {
                returnValue = 1;
                System.out.print("[LOOKUP ERROR]: \tline " + n.getLine());
                System.out.print(": couldn't find " + n.i + "in");
                System.out.println(" scope containing" + scope.keySet());
                return;
            }
            this.rValueType = ((MethodNode)myNode).getReturnType().getType();
        } else {
            returnValue = 1;
            System.out.print("[CALL ERROR]: \tline " + n.getLine());
            System.out.print(": did not find " + n.i.s + " in ");
            System.out.println("the scope of "+caller);
        }
    }

    public void visit(IntegerLiteral n) {
        this.rValueType = NodeType.INT;
    }

    public void visit(True n) {
        this.rValueType = NodeType.BOOLEAN;
    }

    public void visit(False n) {
        this.rValueType = NodeType.BOOLEAN;
    }

    public void visit(IdentifierExp n) {
        Map scope;
        Node currNode = this.currentNode;
        this.lastId = n.s;

        scope = ((MethodNode)currNode).getLocalVariables();
        Node myNode = (Node)scope.get(n.s);

        if(myNode == null) {
            if(currNode instanceof MethodNode) { 
                List paramList = ((MethodNode)currNode).getParameters();
                Map<Integer, String> paramPos = ((MethodNode)currNode)
                                                .getParametersPositions();

                for(int i = 0; i < paramList.size(); i++) { 
                    String param = paramPos.get(i);
                    if(param.equals(n.s)) {
                        this.rValueType = ((Node)paramList.get(i)).getType();
                        return;
                    }
                }

                myNode = ((MethodNode)currNode).getLocalVariables().get(n.s);
                if(myNode != null) {
                    this.rValueType = myNode.getType();
                    return;
                }
            }
            if(this.previousNode instanceof ClassNode) {
                scope = ((ClassNode)this.previousNode).getMembers();
                myNode = (Node)scope.get(n.s);
                if(myNode != null) {
                    if(!(myNode  instanceof MethodNode)) {
                        this.rValueType = myNode.getType();
                        return;
                    } else {
                        this.rValueType = ((MethodNode)myNode).getReturnType()
                                                              .getType();
                        return;
                    }
                }
            }
            returnValue = 1;
            System.out.print("[IDENTIFIER_EXP ERROR]: \tline " + n.getLine());
            System.out.print(": " + n.s + " not found! in KeySet=");
            System.out.println(((MethodNode)currNode).getLocalVariables());
            return;
        }
        this.rValueType = myNode.getType();
    }

    public void visit(This n) {
        this.rValueType = NodeType.CLASS;
    }

    public void visit(NewArray n) {
        n.e.accept(this);
        if(!this.rValueType.equals(NodeType.INT) &&
                !this.rValueType.equals(NodeType.FLOAT) &&
                !this.rValueType.equals(NodeType.DOUBLE)) {
            returnValue = 1;
            System.out.print("[NEW_ARRAY ERROR]: \tline " + n.getLine());
            System.out.println(": SIZE NOT A NUMBER TYPE");
        }

        rValueType = NodeType.INTARRAY;
    }

    public void visit(NewObject n) {
        if (!declaredTypes.getClasses().containsKey(n.i.s)) {
            returnValue = 1;
            System.out.print("[NEWOBJECT ERROR: \tline " + n.getLine());
            System.out.println(": UNDEFINED CLASS " + n.i.s);
        }
        this.rValueType = NodeType.CLASS;
    }

    public void visit(Not n) {
        n.e.accept(this);

        if(!this.rValueType.equals(NodeType.BOOLEAN)) {
            returnValue = 1;
            System.out.print("[NOT ERROR]: \tline " + n.getLine());
            System.out.print(": Exp " + n.e + " is not boolean");
            System.out.println("\tFound rValue="+this.rValueType);
        }
        this.rValueType = NodeType.BOOLEAN;
    }

    public void visit(Identifier n) {
        Map scope;
        Node currNode = this.currentNode;
        if(this.previousNode instanceof ClassNode) { // look in the class scope
            scope = ((ClassNode)this.previousNode).getMembers();
        } else {
            scope = ((MethodNode)currNode).getLocalVariables();
        }
        Node myNode = (Node)scope.get(n.s);
        if(myNode == null) {
            if(currNode instanceof MethodNode) { 
                List paramList = ((MethodNode)currNode).getParameters();
                Map<Integer, String> paramPos = ((MethodNode)currNode)
                                                .getParametersPositions();

                for(int i = 0; i < paramList.size(); i++) {
                    String param = paramPos.get(i);
                    if(param.equals(n.s)) {
                        this.rValueType = ((Node)paramList.get(i)).getType();
                        return;
                    }
                }
            }
            returnValue = 1;
            System.out.print("[IDENTIFIER ERROR]: line " + n.getLine());
            System.out.print(": " + n.s + " not found in scope containing");
            System.out.println("Keys=" + scope.keySet());
            return;
        }
        this.rValueType = myNode.getType();
    }

    public void visit(FloatLiteral n) {
        this.rValueType = NodeType.FLOAT;
    }

    public void visit(FloatType n) {
        this.rValueType = nodeTypeOf(n);
    }
 
    public void visit(GreatThan n) {
		n.e1.accept(this);
        if(!this.rValueType.equals(NodeType.INT) &&
           !this.rValueType.equals(NodeType.FLOAT) &&
           !this.rValueType.equals(NodeType.DOUBLE)) {
            returnValue = 1;
            System.out.print("[GREATTHAN ERROR]: line "+n.getLine());
            System.out.println(": ARG1 NOT NUMBER");
        }
        n.e2.accept(this);
        if(!this.rValueType.equals(NodeType.INT) &&
           !this.rValueType.equals(NodeType.FLOAT) &&
           !this.rValueType.equals(NodeType.DOUBLE)) {
            returnValue = 1;
            System.out.print("[GREATTHAN ERROR]: line "+n.getLine());
            System.out.println(": ARG2 NOT NUMBER");
        }
        this.rValueType = NodeType.BOOLEAN;
	}

	public void visit(GreatThanEqual n) {
		n.e1.accept(this);
        if(!this.rValueType.equals(NodeType.INT) &&
           !this.rValueType.equals(NodeType.FLOAT) &&
           !this.rValueType.equals(NodeType.DOUBLE)) {
            returnValue = 1;
            System.out.print("[GREATTHANEQUAL ERROR]: line "+n.getLine());
            System.out.println(": ARG1 NOT NUMBER");
        }
        n.e2.accept(this);
        if(!this.rValueType.equals(NodeType.INT) &&
           !this.rValueType.equals(NodeType.FLOAT) &&
           !this.rValueType.equals(NodeType.DOUBLE)) {
            returnValue = 1;
            System.out.print("[GREATTHANEQUAL ERROR]: line "+n.getLine());
            System.out.println(": ARG2 NOT NUMBER");
        }
        this.rValueType = NodeType.BOOLEAN;
	}

	public void visit(LessThanEqual n) {
		n.e1.accept(this);
        if(!this.rValueType.equals(NodeType.INT) &&
           !this.rValueType.equals(NodeType.FLOAT) &&
           !this.rValueType.equals(NodeType.DOUBLE)) {
            returnValue = 1;
            System.out.print("[LESSTHANEQUAL ERROR]: line "+n.getLine());
            System.out.println(": ARG1 NOT NUMBER");
        }
        n.e2.accept(this);
        if(!this.rValueType.equals(NodeType.INT) &&
           !this.rValueType.equals(NodeType.FLOAT) &&
           !this.rValueType.equals(NodeType.DOUBLE)) {
            returnValue = 1;
            System.out.print("[LESSTHANEQUAL ERROR]: line "+n.getLine());
            System.out.println(": ARG2 NOT NUMBER");
        }
        this.rValueType = NodeType.BOOLEAN;		
	}

	public void visit(DoubleLiteral n) {
		this.rValueType = NodeType.DOUBLE;
	}

	public void visit(DoubleType n) {
		this.rValueType = nodeTypeOf(n);
	}

	public void visit(EqualEqual n) {
		n.e1.accept(this);
        if(!this.rValueType.equals(NodeType.INT) &&
           !this.rValueType.equals(NodeType.FLOAT) &&
           !this.rValueType.equals(NodeType.DOUBLE)) {
            returnValue = 1;
            System.out.print("[EQUALEQUAL ERROR]: line "+n.getLine());
            System.out.println(": ARG1 NOT NUMBER");
        }
        n.e2.accept(this);
        if(!this.rValueType.equals(NodeType.INT) &&
           !this.rValueType.equals(NodeType.FLOAT) &&
           !this.rValueType.equals(NodeType.DOUBLE)) {
            returnValue = 1;
            System.out.print("[EQUALEQUAL ERROR]: line "+n.getLine());
            System.out.println(": ARG2 NOT NUMBER");
        }
        this.rValueType = NodeType.BOOLEAN;			
	}

	public void visit(NotEqual n) {
		n.e1.accept(this);
        if(!this.rValueType.equals(NodeType.INT) &&
           !this.rValueType.equals(NodeType.FLOAT) &&
           !this.rValueType.equals(NodeType.DOUBLE)) {
            returnValue = 1;
            System.out.print("[NOTEQUAL ERROR]: line "+n.getLine());
            System.out.println(": ARG1 NOT NUMBER");
        }
        n.e2.accept(this);
        if(!this.rValueType.equals(NodeType.INT) &&
           !this.rValueType.equals(NodeType.FLOAT) &&
           !this.rValueType.equals(NodeType.DOUBLE)) {
            returnValue = 1;
            System.out.print("[NOTEQUAL ERROR]: line "+n.getLine());
            System.out.println(": ARG2 NOT NUMBER");
        }
        this.rValueType = NodeType.BOOLEAN;		
	}

	public void visit(Instanceof n) {
		this.rValueType = NodeType.BOOLEAN;
	}

	public void visit(Null n) {
		this.rValueType = NodeType.UNKNOWN;
	}
    
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
