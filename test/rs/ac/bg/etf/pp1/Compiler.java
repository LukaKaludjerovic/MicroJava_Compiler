package rs.ac.bg.etf.pp1;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import java_cup.runtime.Symbol;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import rs.ac.bg.etf.pp1.ast.Program;
import rs.ac.bg.etf.pp1.util.Log4JUtils;
import rs.etf.pp1.mj.runtime.Code;

public class Compiler {
	static {
		DOMConfigurator.configure(Log4JUtils.instance().findLoggerConfigFile());
		Log4JUtils.instance().prepareLogFile(Logger.getRootLogger());
	}
	
	public static void main(String[] args) throws Exception {
		Logger log = Logger.getLogger(Compiler.class);
		Reader br = null;
		
		try {
			File sourceCode = new File("test/test302.mj");
			
			log.info("Compiling source file: " + sourceCode.getAbsolutePath());
			
			br = new BufferedReader(new FileReader(sourceCode));
			Yylex lexer = new Yylex(br);
			
			MJParser p = new MJParser(lexer);
			
			// ispis prepoznatih tokena
			log.info("=====================ISPIS PREPOZNATIH TOKENA=====================");
	        Symbol s = p.parse(); 
	        
	        Program prog = (Program)(s.value); 
	        SymbolTable.init();
	        
	        // ispis sintaksnog stabla
			log.info("=====================ISPIS SINTAKSNOG STABLA=====================");
			log.info(prog.toString(""));
			
			// ispis prepoznatih programskih konstrukcija
			log.info("=====================ISPIS PREPOZNATIH PROGRAMSKIH KONSTRUKCIJA=====================");
			SemanticPass v = new SemanticPass();
			prog.traverseBottomUp(v); 
			
			// ispis tabele simbola
			SymbolTable.dump();
			
			if(!p.errorDetected && v.passed()) {
				log.info("Parsiranje uspesno zavrseno!");
				
				File objFile = new File("test/program.obj");
				if(objFile.exists()) {
					objFile.delete();
				}
				
				CodeGenerator codeGenerator = new CodeGenerator();
				prog.traverseBottomUp(codeGenerator);
				Code.dataSize = v.nVars;
				Code.mainPc = codeGenerator.getMainPc();
				Code.write(new FileOutputStream(objFile));
				
				log.info("Generisanje koda uspesno zavrseno!");
			}
			else {
				log.info("Parsiranje nije uspesno zavrseno!");
			}
		} 
		finally {
			if (br != null) try { br.close(); } catch (IOException e1) { log.error(e1.getMessage(), e1); }
		}
	}
}
