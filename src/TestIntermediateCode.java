import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;

import AST.Program;
import AST.Visitor.*;
import Parser.parser;
import Scanner.scanner;
import IntermediateCode.*;

public class TestIntermediateCode {
    public static void main(String[] args) {
        try {
        	parser p = new parser(new scanner(new BufferedReader(new InputStreamReader(System.in))));
            Program prog = (Program)(p.parse().value);

            TypeVisitor typeInfo = new TypeVisitor();
            prog.accept(typeInfo);


            IntermediateCode intermediateCode = new IntermediateCode(typeInfo);
            prog.accept(intermediateCode);
            
            PrintStream out;
            if (args.length >= 1) {
                out = new PrintStream(new File(args[0]));
            } else {
                out = System.out;
            }
            
            for (String line : intermediateCode.getCode()) {
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
