import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;

import AST.Program;
import AST.Visitor.*;
import Parser.parser;
import Scanner.scanner;
import CodeGenerator.*;
import IntermediateCode.IntermediateCode;

public class TestCodeGenerator {
    public static void main(String[] args) {
        try {
        	parser p = new parser(new scanner(new BufferedReader(new InputStreamReader(System.in))));
            Program prog = (Program)(p.parse().value);

            TypeVisitor typeInfo = new TypeVisitor();
            prog.accept(typeInfo);

//            VerifyTypeVisitor verifyTypeInfo = new VerifyTypeVisitor(typeInfo);
//            prog.accept(verifyTypeInfo);
//
//            int returnValue = verifyTypeInfo.getReturnValue();
//            if (returnValue != 0) {
//                System.exit(returnValue);
//            }
            IntermediateCode intermediateCode = new IntermediateCode(typeInfo);
            prog.accept(intermediateCode);
            
            CodeGenerator codeGenerator = new CodeGenerator(typeInfo, intermediateCode);
            prog.accept(codeGenerator);
            
             PrintStream out;
             if (args.length >= 1) {
                 out = new PrintStream(new File(args[0]));
             } else {
                 out = System.out;
             }
            for (String line : codeGenerator.getCode()) {
                System.out.println(line);
                 out.println(line);
            }
             out.close();

        } catch (Exception e) {
            // yuck: some kind of error in the compiler implementation
            // that we're not expecting (a bug!)
            System.err.println("Unexpected internal compiler error: " + 
                               e.toString());
            // print out a stack dump
            e.printStackTrace();
        }
    }
}
