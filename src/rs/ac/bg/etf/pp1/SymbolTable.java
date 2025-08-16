package rs.ac.bg.etf.pp1;

import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Scope;
import rs.etf.pp1.symboltable.concepts.Struct;

public class SymbolTable extends Tab {
	
	public static final Struct setType = new Struct(8);
	public static final Struct boolType = new Struct(Struct.Bool);
	public static Obj addObj;
	public static Obj addAllObj;
	
	public static void init() {
		Tab.init();
		
		currentScope.addToLocals(new Obj(Obj.Type, "set", setType));
		currentScope.addToLocals(new Obj(Obj.Type, "bool", boolType));
		
		currentScope.addToLocals(addObj = new Obj(Obj.Meth, "add", noType, 0, 1));
		{
			openScope();
			currentScope.addToLocals(new Obj(Obj.Var, "a", setType, 0, 1));
			currentScope.addToLocals(new Obj(Obj.Var, "b", intType, 0, 1));
			addObj.setLocals(currentScope.getLocals());
			closeScope();
		}
		
		currentScope.addToLocals(addAllObj = new Obj(Obj.Meth, "addAll", noType, 0, 1));
		{
			openScope();
			currentScope.addToLocals(new Obj(Obj.Var, "a", setType, 0, 1));
			currentScope.addToLocals(new Obj(Obj.Var, "b", new Struct(Struct.Array, intType), 0, 1));
			addAllObj.setLocals(currentScope.getLocals());
			closeScope();
		}
	}
	
	public static void dump(SymbolTableDumpVisitor stv) {
		System.out.println("=====================ISPIS TABELE SIMBOLA=========================");
		
		if (stv == null)
			stv = new SymbolTableDumpVisitor();
		
		for (Scope s = currentScope; s != null; s = s.getOuter()) {
			s.accept(stv);
		}
		
		System.out.println(stv.getOutput());
	}
	
	public static void dump() {
		dump(null);
	}
	
	public static String getTypeName(Struct type) {
		switch(type.getKind()) {
			case 0:
				return "void";
			case 1: 
				return "int";
			case 2:
				return "char";
			case 3:
				return "array of " + getTypeName(type.getElemType());
			case 5:
				return "bool";
			case 8:
				return "set";
			default:
				return "";
		}
	}
}
