import java.io.BufferedReader;
import java.io.InputStreamReader;

import AST.Visitor.TypeVisitor;

import AST.Program;
import AST.Visitor.VerifyTypeVisitor;
import Parser.parser;
import Scanner.scanner;

public class TestSemantic {
    public static void main(String[] args) {
        try {
            parser p = new parser(new scanner(new BufferedReader(new InputStreamReader(System.in))));
            Program prog = (Program)(p.parse().value);

            TypeVisitor typeInfo = new TypeVisitor();
            prog.accept(typeInfo);

            typeInfo.printTypes();

            VerifyTypeVisitor verifyTypeInfo = new VerifyTypeVisitor(typeInfo);
            prog.accept(verifyTypeInfo);

            int returnValue = verifyTypeInfo.getReturnValue();
            if (returnValue != 0) {
                System.exit(returnValue);
            }
        } catch (Exception e) {
        	
        	System.out.println("Deu Merda!!!");
            // yuck: some kind of error in the compiler implementation
            // that we're not expecting (a bug!)
            System.err.println("Unexpected internal compiler error: " + 
                               e.toString());
            // print out a stack dump
            //e.printStackTrace();
        }
    }
}