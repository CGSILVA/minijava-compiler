package IntermediateCode;

import java.util.*;
import AST.*;
import AST.Visitor.*;
import Semantic.*;

public class IntermediateCode implements Visitor {
    private List<String> code;
    private String currentClass;
    private String currentMethod;
    private TypeVisitor declaredTypes;
    private Map<String, Integer> currentMethodParameters;
    private Map<String, Integer> currentMethodVariables;
    private int lastLabel;
    private Map<String, Map> vTable;
    private String lastSeenType;

    public IntermediateCode(TypeVisitor declaredTypes) {
        super();
        this.code = new ArrayList<String>();
        this.currentClass = null;
        this.currentMethod = null;
        this.currentMethodParameters = null;
        this.declaredTypes = declaredTypes;
        this.lastLabel = 0;
        this.vTable = null;
        this.lastSeenType = null;
    }

    public List<String> getCode() {
        return code;
    }

    private String getLabel() {
        String rv = "L" + lastLabel;
        ++lastLabel;
        return rv;
    }

    /*
     * vTable: key=class name, value=(method name=>slot number)
     */
    private void createVTables() {
        this.vTable = new HashMap<String, Map>();

        Map classes = this.declaredTypes.getClasses();
        Iterator cls_it = classes.entrySet().iterator();
        while(cls_it.hasNext()) {
            Map.Entry clsMapEntry = (Map.Entry)cls_it.next();

            String clsName = (String)clsMapEntry.getKey();
            ClassNode clsNode = (ClassNode)clsMapEntry.getValue();

            Map methods = collectVtableMethods(clsName, clsNode, classes);

            List<ClassNode> clsRel = createClsRelList(clsNode, classes);
            HashMap<String, Integer> clsVTable = constructTableEntry(clsName,
                                                                     methods,
                                                                     clsRel,
                                                                     clsNode);
            this.vTable.put(clsName, clsVTable);
        }
    }

    /*
     * clsRel is used to start formatting vtable entry based on older classes
     *
     *      VTable Representation:
     *          Foo$$:
     *              .long 0 # null parent   [slot 0]
     *              .long Foo$MethodA       [slot 1]
     *              .long Foo$MethodB       [slot 2]
     *          Bar$$
     *              .long Foo$$             [slot 0]
     *              .long Foo$MethodA       [slot 1]
     *              .long Bar$MethodB       [slot 2]
     *
     * @return: HashMap, key=method, value=slot #
     */
    private HashMap<String, Integer> constructTableEntry(String clsName,
                                     Map<String, String> methods,
                                     List<ClassNode> clsRel,
                                     ClassNode clsNode) {
        HashMap<String, Integer> clsVTable = new HashMap<String, Integer>();
        int slotNumber = 1;

        String clsParent = "";
        if(clsNode instanceof ClassWithParentNode)
            clsParent = ((ClassWithParentNode)clsNode).getParent() + "$$";
        else
            clsParent = "0";
        code.add(clsName + ":");

        HashSet<String> recordedMethods = new HashSet<String>();

        List<ClassNode> clsRelList = clsRel;
        Iterator clsRel_it = clsRelList.iterator();
        while(clsRel_it.hasNext()) {
            ClassNode currCls = (ClassNode)clsRel_it.next();
            Map<String, MethodNode> currMeth = getMethods(currCls);
            Iterator currMeth_it = currMeth.keySet().iterator();
            while(currMeth_it.hasNext()) {
                String meth = (String)currMeth_it.next();
                if(!recordedMethods.contains(meth)) {
                    String methEntry = methods.get(meth);
                    code.add("    " + methEntry);
                    clsVTable.put(meth, new Integer(slotNumber++));
                    recordedMethods.add(meth);
                }
            }
        }
        return clsVTable;
    }

    /*
     * Collects all the methods visible by a class, as well as the code
     *      source of each method
     */
    private Map<String, String> collectVtableMethods(String clsName,
                                                     ClassNode clsNode,
                                                     Map classes) {
        Map<String, String> methods = new HashMap<String, String>();

        String currClsName = clsName;
        ClassNode currCls = clsNode;

        while(currCls instanceof ClassWithParentNode) {
            Map<String, MethodNode> currMethods = getMethods(currCls);
            Iterator method_it = currMethods.keySet().iterator();
            while(method_it.hasNext()) {
                String methodName = (String)method_it.next();
                if(!methods.containsKey((String)methodName)) {
                    methods.put(methodName, currClsName + "$" + methodName);
                }
            }

            currClsName = ((ClassWithParentNode)currCls).getParent();
            currCls = (ClassNode)classes.get(currClsName);
        }

        // finish adding methods from base class...
        Map<String, MethodNode> currMethods = getMethods(currCls);
        Iterator method_it = currMethods.keySet().iterator();
        while(method_it.hasNext()) {
            String methodName = (String)method_it.next();
            if(!methods.containsKey(methodName)) {
                methods.put(methodName, currClsName + "$" + methodName);
            }
        }
        return methods;
    }

    /*
     * Get all methods associated with a class
     */
    private Map<String, MethodNode> getMethods(ClassNode cls) {
        Map<String, MethodNode> methods = new HashMap<String, MethodNode>();

        Map<String, Node> allAttrs = cls.getMembers();
        Iterator allAttrs_it = allAttrs.entrySet().iterator();
        while(allAttrs_it.hasNext()) {
            Map.Entry attr_entry = (Map.Entry)allAttrs_it.next();
            if(attr_entry.getValue() instanceof MethodNode) {
                String methodName = (String)attr_entry.getKey();
                MethodNode method = (MethodNode)attr_entry.getValue();
                methods.put(methodName, method);
            }
        }
        return methods;
    }

    /*
     * Construct List: This is used to backtrack to make sure we vtable
     *      entries are aligned uniformly according the the ancestors.
     *
     *      [GreatGrandpa] -> [Grandpa] -> [pa] -> [me]
     *          ^
     *        HEAD
     */
    private List<ClassNode> createClsRelList(ClassNode clsNode, Map classes) {
        ClassNode currClsNode = clsNode;
        Map<String, ClassNode> classMap = classes;

        List<ClassNode> clsRel = new LinkedList<ClassNode>();
        clsRel.add(clsNode);

        while(currClsNode instanceof ClassWithParentNode) {
            ClassNode nextClsNode = classMap.get(((ClassWithParentNode)
                                                currClsNode).getParent());
            clsRel.add(0, nextClsNode);
            currClsNode = nextClsNode;
        }
        return clsRel;
    }

    public Map<String, Integer> getMethodParameterOffsets(
            String className,
            String methodName) {
        Map<Integer, String> parameterPositions =
            ((MethodNode)(
                declaredTypes
                .getClasses()
                .get(className)
                .getMembers()
                .get(methodName)))
            .getParametersPositions();
        Map<String, Integer> rv = new HashMap<String, Integer>();

        for (Map.Entry<Integer, String> entry :
                parameterPositions.entrySet()) {
            rv.put(entry.getValue(), entry.getKey());
        }

        return rv;
    }

    public Map<String, Integer> getMethodVariableOffsets(
            String className,
            String methodName) {
        String[] localVariables =
            ((MethodNode)(
                declaredTypes
                .getClasses()
                .get(className)
                .getMembers()
                .get(methodName)))
            .getLocalVariables()
            .keySet()
            .toArray(new String[0]);
        Arrays.sort(localVariables);

        Map<String, Integer> rv = new HashMap<String, Integer>();
        int i = 0;
        for (String localvar : localVariables) {
            rv.put(localVariables[i], i);
            ++i;
        }

        return rv;
    }

    public Map<String, Integer> getInstanceVariableOffsets(
            String className) {
        Map<String, ClassNode> classes = declaredTypes.getClasses();

        Map<String, Integer> rv = new HashMap<String, Integer>();
        int currentPosition = 1; // 0 has the vtable pointer
        ClassNode klass = classes.get(className);

        while (true) {
            for (Map.Entry<String, Node> entry :
                    klass.getMembers().entrySet()) {
                if (!(entry.getValue() instanceof MethodNode)) {
                    rv.put(entry.getKey(), currentPosition++);
                }
            }

            if (klass instanceof ClassWithParentNode) {
                klass = classes.get(((ClassWithParentNode)klass).getParent());
            } else {
                break;
            }
        }

        return rv;
    }

    public void visit(Program n) {
        createVTables();

        n.m.accept(this);

        ClassDeclList classDeclarations = n.cl;
        int size = classDeclarations.size();
        for (int i = 0; i < size; ++i) {
            classDeclarations.elementAt(i).accept(this);
        }
    }

    public void visit(MainClass n) {
    	code.add("main: ");
    	code.add("    call " + statementToString(n.s));
    }

    public void visit(ClassDeclSimple n) {
        currentClass = n.i.s;

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
        currentClass = n.i.s;

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

    public void visit(VarDecl n) { }

    public void visit(MethodDecl n) {
        currentMethod = n.i.s;

        code.add(currentClass + "$" + currentMethod + ":");

        currentMethodParameters = new HashMap<String, Integer>();
        FormalList params = n.fl;
        int paramsCount = params.size();
        for (int i = 0; i < paramsCount; ++i) {
            currentMethodParameters.put(params.elementAt(i).i.s, i);
        }

        currentMethodVariables = new HashMap<String, Integer>();
        VarDeclList localVariables = n.vl;
        int variablesCount = localVariables.size();
        for (int i = 0; i < variablesCount; ++i) {
            currentMethodVariables.put(localVariables.elementAt(i).i.s, i);
        }

        StatementList stmts = n.sl;
        int stmtsCount = stmts.size();
        for (int i = 0; i < stmtsCount; ++i) {
            stmts.elementAt(i).accept(this);
        }

        code.add("return " + expToValue(n.e));

        currentMethodParameters = null;
        currentMethod = null;
    }

    public void visit(Formal n) { }

    public void visit(IntArrayType n) { }

    public void visit(BooleanType n) { }

    public void visit(IntegerType n) { }

    public void visit(IdentifierType n) { }

    public void visit(Block n) {
        StatementList stmts = n.sl;
        int size = stmts.size();
        for (int i = 0; i < size; ++i) {
            stmts.elementAt(i).accept(this);
        }
    }
    
    private String expToValue(Exp exp) {
    	String value = "";
    	if (exp instanceof IdentifierExp) {
    		value = ((IdentifierExp) exp).s;
		} else if (exp instanceof IntegerLiteral) {
    		value =  ((IntegerLiteral) exp).i + "";
		} else if (exp instanceof FloatLiteral) {
    		value =  ((FloatLiteral) exp).f + "";
		} else if (exp instanceof True) {
    		value =   "true";
		} else if (exp instanceof False) {
    		value =   "false";
		} else if (exp instanceof Plus) {    		
    		value = expToValue(((Plus) exp).e1) + " + " + expToValue(((Plus) exp).e2);
		} else if (exp instanceof Minus) {    		
    		value = expToValue(((Minus) exp).e1) + " - " + expToValue(((Minus) exp).e2);
		} else if (exp instanceof Times) {    		
    		value = expToValue(((Times) exp).e1) + " * " + expToValue(((Times) exp).e2);
		} 

    	return value;
    }
    
    private String expListToValue(ExpList expList) {
    	String value = "";
		for (int i = 0; i < expList.size(); i++) {
			value = value + expToValue(expList.elementAt(i));
			if (i < expList.size() - 1) {
				value = value + "\n        ";
			}
		}
		
		return value;
    }
    
    private String expToCondition(Exp exp) {
    	String codition = "";
    	
    	if (exp instanceof LessThan) {
    		codition = expToValue(((LessThan) exp).e1) + " < " + expToValue(((LessThan) exp).e2);
    	} else if (exp instanceof LessThanEqual) {
    		codition = expToValue(((LessThanEqual) exp).e1) + " <= " + expToValue(((LessThanEqual) exp).e2);
    	} else if (exp instanceof GreatThan) {
    		codition = expToValue(((GreatThan) exp).e1) + " > " + expToValue(((GreatThan) exp).e2);
    	} else if (exp instanceof GreatThanEqual) {
    		codition = expToValue(((GreatThanEqual) exp).e1) + " >= " + expToValue(((GreatThanEqual) exp).e2);
    	} else if (exp instanceof EqualEqual) {
    		codition = expToValue(((EqualEqual) exp).e1) + " == " + expToValue(((EqualEqual) exp).e2);
    	} else if (exp instanceof NotEqual) {
    		codition = expToValue(((NotEqual) exp).e1) + " != " + expToValue(((NotEqual) exp).e2);
    	} else if (exp instanceof And) {
    		codition = expToCondition(((And) exp).e1) + " && " + expToCondition(((And) exp).e2);
    	} else if (exp instanceof Call) {
    		codition = expToValue(((Call) exp).e) + ((Call) exp).i + " " + expListToValue(((Call) exp).el) ;
    	}

    	return codition;
    }
    
    private String statementToString(Statement stm) {
    	String value = "";
    	
    	if (stm instanceof Assign) {
    		value = ((Assign) stm).i.s + " := " + expToValue(((Assign) stm).e);
    	} else if (stm instanceof Block) {
    		StatementList stmList = ((Block) stm).sl;
    		for (int i = 0; i < stmList.size(); i++) {
				value = value + statementToString(stmList.elementAt(i)) + ";";
				if (i < stmList.size() - 1) {
					value = value + "\n        ";
				}
			}
    	} else if (stm instanceof Print) {    		
    		value = expToCondition(((Print) stm).e);
		}
    	
    	return value;
    }
    
    public void visit(If n) {
    	code.add("    if "+expToCondition(n.e) + " goto ifTrue");
    	code.add("        " + statementToString(n.s2) + ";");
    	code.add("        goto endElse");
    	code.add("    ifTrue:");
    	code.add("        " + statementToString(n.s1) + ";");
    	code.add("    endElse:");
        
    }

    public void visit(While n) {

        code.add("    while: if " + expToCondition(n.e) + " goto endWhile");
        code.add("        " + statementToString(n.s) + "");
        code.add("        goto while:");
        code.add("   endWhile:");
    }

    public void visit(Print n) {
        code.add("    print " + expToValue(n.e));
    }

    public void visit(Assign n) {
        n.e.accept(this);

        String nid = n.i.s;

        code.add("    " + nid);
    }

    public void visit(ArrayAssign n) {
        n.e2.accept(this);
        code.add("    " + n.e1);

    }

    public void visit(And n) {

        code.add("    " + expToCondition(n.e1) + " && " + expToCondition(n.e2));

    }

    public void visit(LessThan n) {
    	code.add(expToValue(n.e1) + " < " + expToValue(n.e2));
    }

    public void visit(Plus n) {
        code.add(expToValue(n.e1) + " + " + expToValue(n.e2));

    }

    public void visit(Minus n) {
        code.add(expToValue(n.e1) + " - " + expToValue(n.e2));
    }

    public void visit(Times n) {
    	code.add(expToValue(n.e1) + " * " + expToValue(n.e2));
    }

    public void visit(ArrayLookup n) {
        code.add("    " + n.e1);
        code.add("    " + n.e2);
    }

    public void visit(ArrayLength n) {
        code.add("    " + n.e);
    }

    public void visit(Call n) {
        code.add("    call " + n.i.s);
    }

    public void visit(IntegerLiteral n) {
        code.add("    " + n.i);
    }

    public void visit(True n) {
        code.add("    true");
    }

    public void visit(False n) {
        code.add("    false");
    }

    public void visit(IdentifierExp n) {
    	code.add("    " + n.s);
    }

    public void visit(This n) {
        code.add("    " + n);

        lastSeenType = currentClass;
    }

    public void visit(NewArray n) {
        n.e.accept(this);
        code.add("    " + n);
    }

    public void visit(NewObject n) {
        int objectSize = 4; // Space for vtable pointer
        
        code.add("    " + n.i.s);

        lastSeenType = n.i.s;
    }

    public void visit(Not n) {
        String labelTrue = getLabel();
        String labelEnd = getLabel();

        n.e.accept(this);
        code.add("    " + n.e);
    }

	@Override
	public void visit(GreatThan n) {
      String labelTrue = getLabel();
      String labelEnd = getLabel();

      n.e1.accept(this);
      code.add("    " + n.e1);
      n.e2.accept(this);
      code.add("    > " + n.e2);
		
	}

	@Override
	public void visit(GreatThanEqual n) {
	      String labelTrue = getLabel();
	      String labelEnd = getLabel();

	      n.e1.accept(this);
	      code.add("    " + n.e1);
	      n.e2.accept(this);
	      code.add("    >= " + n.e2);
		
	}

	@Override
	public void visit(LessThanEqual n) {
	      String labelTrue = getLabel();
	      String labelEnd = getLabel();

	      n.e1.accept(this);
	      code.add("    " + n.e1);
	      n.e2.accept(this);
	      code.add("    <= " + n.e2);
		
	}

	@Override
	public void visit(EqualEqual n) {
	      String labelTrue = getLabel();
	      String labelEnd = getLabel();

	      n.e1.accept(this);
	      code.add("    " + n.e1);
	      n.e2.accept(this);
	      code.add("    == " + n.e2);
		
	}

	@Override
	public void visit(NotEqual n) {
	      String labelTrue = getLabel();
	      String labelEnd = getLabel();

	      n.e1.accept(this);
	      code.add("    " + n.e1);
	      n.e2.accept(this);
	      code.add("    != " + n.e2);
		
	}
	
    public void visit(Identifier n) {
    }

    public void visit(FloatLiteral n) {
    }

    public void visit(FloatType n) {
    }
    
	@Override
	public void visit(DoubleLiteral n) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(DoubleType n) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(Instanceof n) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(Null n) {
		// TODO Auto-generated method stub
		
	}
}

