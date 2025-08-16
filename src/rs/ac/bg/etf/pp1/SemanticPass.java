package rs.ac.bg.etf.pp1;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;

public class SemanticPass extends VisitorAdaptor {
	
	Struct currentType = null;
	Obj currentMethod = null;
	Struct currentMethodType = null;
	boolean returnFound = false;
	boolean returnVoidFound = false;
	boolean errorDetected;
	int currentFpPos = 1;
	int nVars;
	
	Logger log = Logger.getLogger(getClass());
	
	public void report_error(String message, SyntaxNode info) {
		errorDetected = true;
		
		StringBuilder msg = new StringBuilder(message);
		int line = (info == null) ? 0 : info.getLine();
		
		if (line != 0)
			msg.append(" na liniji ").append(line);
		
		log.error(msg.toString());
	}
	
	public void report_info(String message, SyntaxNode info) {
		StringBuilder msg = new StringBuilder(message);
		int line = (info == null) ? 0 : info.getLine();
		
		if (line != 0)
			msg.append(" na liniji ").append(line);
		
		log.info(msg.toString());
	}
	
	public void visit(ProgName progName) {
		progName.obj = SymbolTable.insert(Obj.Prog, progName.getProgName(), SymbolTable.noType);
		SymbolTable.openScope();
	}
	
	public void visit(Program program) {
		Obj mainMethod = SymbolTable.currentScope.findSymbol("main");
		
		if(mainMethod == null) {
			report_error("Greska: Program mora imati metodu 'main' povratnog tipa 'void' bez parametara!", null);
		}
		
		nVars = SymbolTable.currentScope.getnVars();
		SymbolTable.chainLocalSymbols(program.getProgName().obj);
		SymbolTable.closeScope();
	}
	
	public void visit(Type type) {
    	int typeLine = type.getLine();
    	String typeName = type.getTypeName();
    	Obj typeNode = SymbolTable.find(type.getTypeName());
    	
    	if(typeNode == SymbolTable.noObj) {
    		report_error("Greska na liniji " + typeLine + ": Nije pronadjen tip '" + typeName + "' u tabeli simbola!", null);
    		type.struct = SymbolTable.noType;
    	}
    	else {
    		if(typeNode.getKind() == Obj.Type) {
    			type.struct = typeNode.getType();
    		}
    		else {
    			report_error("Greska na liniji " + typeLine + ": Ime '" + typeName + "' ne predstavlja tip!", type);
    			type.struct = SymbolTable.noType;
    		}
    	}
    	
    	currentType = typeNode.getType();
    }
	
	public void visit(Constant constant) {
		int constLine = constant.getLine();
		String constName = constant.getConstName();
		Struct constType = currentType;
		ConstValue constValue = constant.getConstValue();
		int value = 0;
		
		if(constValue instanceof NumConst) {
			constType = new Struct(Struct.Int);
			value = ((NumConst)constValue).getNumConst();
		}
		else if(constValue instanceof CharConst) {
			constType = new Struct(Struct.Char);
			value = ((CharConst)constValue).getCharConst();
		}
		else {
			constType = new Struct(Struct.Bool);
			value = ((BoolConst)constValue).getBoolConst() ? 1 : 0;
		}
		
		String constTypeName = SymbolTable.getTypeName(constType);
		Obj existingConstNode = SymbolTable.currentScope.findSymbol(constName);
		
		if(existingConstNode != null) {
			report_error("Greska na liniji " + constLine + ": Simbolicka konstanta '" + constName + "' je vec definisana!", null);
		}
		else {
			if(currentType.getKind() != constType.getKind()) {
				report_error("Greska na liniji " + constLine + ": Tip i vrednost simbolicke konstante '" + constName + "' se ne poklapaju!", null);
			}
			else {
				constant.obj = SymbolTable.insert(Obj.Con, constName, constType);
				constant.obj.setAdr(value);
				report_info("Definisana simbolicka konstanta '" + constName + "' tipa '" + constTypeName + "'", constant);
			}
		}
	}
	
	public void visit(GlobalVariable globalVarible) {
		int varLine = globalVarible.getLine();
		String varName = globalVarible.getGlobalVarName();
		Struct varType = currentType;
		
		if(globalVarible.getBrackets() instanceof SquareBrackets) {
			varType = new Struct(Struct.Array, currentType);
		}

		String varTypeName = SymbolTable.getTypeName(varType);
		Obj existingVarNode = SymbolTable.currentScope.findSymbol(varName);
		
		if(existingVarNode != null) {
			report_error("Greska na liniji " + varLine + ": Globalna promenljiva '" + varName + "' je vec deklarisana!", null);
		}
		else {
			globalVarible.obj = SymbolTable.insert(Obj.Var, varName, varType);
			report_info("Deklarisana globalna promenljiva '" + varName + "' tipa '" + varTypeName + "'", globalVarible);
		}
	}
	
	public void visit(LocalVariable localVariable) {
		int varLine = localVariable.getLine();
		String varName = localVariable.getLocalVarName();
		Struct varType = currentType;
		String methodName = currentMethod.getName();
		
		if(localVariable.getBrackets() instanceof SquareBrackets) {
			varType = new Struct(Struct.Array, currentType);
		}
		
		String varTypeName = SymbolTable.getTypeName(currentType);
		Obj existingVarNode = SymbolTable.currentScope.findSymbol(varName);
		
		if(existingVarNode != null) {
			report_error("Greska na liniji " + varLine + ": Lokalna promenljiva '" + varName + "' je vec deklarisana u metodi '" + methodName + "'!", null);
		}
		else {
			localVariable.obj = SymbolTable.insert(Obj.Var, varName, varType);
			report_info("Deklarisana lokalna promenljiva '" + varName + "' tipa '" + varTypeName + "' u metodi '" + methodName + "'", localVariable);
		}
	}
	
	public void visit(MethodTypeName methodTypeName) {
		int methodLine = methodTypeName.getLine();
		String methodName = methodTypeName.getMethodName();
		
		if(methodTypeName.getTypeOrVoid() instanceof VoidType) {
			currentMethodType = new Struct(Struct.None);
			methodTypeName.getTypeOrVoid().struct = SymbolTable.noType;
		}
		else {
			currentMethodType = currentType;
		}
		
		String methodType = SymbolTable.getTypeName(currentMethodType);
		Obj existingMethodNode = SymbolTable.currentScope.findSymbol(methodName);
		
		if(existingMethodNode != null) {
			report_error("Greska na liniji " + methodLine + ": Metoda '" + methodName + "' je vec deklarisana!", null);
		}
		else {
			currentMethod = SymbolTable.insert(Obj.Meth, methodName, currentMethodType);
			methodTypeName.obj = currentMethod;
			SymbolTable.openScope();
			report_info("Obradjuje se funkcija '" + methodName + "' povratnog tipa '" + methodType + "'", methodTypeName);
		}
	}
	
	public void visit(FormParam formParam) {
		int paramLine = formParam.getLine();
		String paramName = formParam.getFormParamName();
		Struct paramType = currentType;
		String methodName = currentMethod.getName();
		
		if(formParam.getBrackets() instanceof SquareBrackets) {
			paramType = new Struct(Struct.Array, currentType);
		}
		
		String paramTypeName = SymbolTable.getTypeName(paramType);
		Obj existingParamNode = SymbolTable.currentScope.findSymbol(paramName);
		
		if(existingParamNode != null) {
			report_error("Greska na liniji " + paramLine + ": Formalni parametar '" + paramName + "' je vec definisan u metodi '" + methodName + "'!", null);
		}
		else {
			formParam.obj = SymbolTable.insert(Obj.Var, paramName, currentType);
			formParam.obj.setFpPos(currentFpPos++);
			
			report_info("Deklarisan formalni parametar '" + paramName + "' tipa '" + paramTypeName + "' u metodi '" + methodName + "'", formParam);
		}
	}
	
	public void visit(MethodDeclaration methodDeclaration) {
		int methodLine = methodDeclaration.getLine();
		String methodName = methodDeclaration.getMethodSignature().getMethodTypeName().getMethodName();
		
		// ako metoda main ima povratni tip
		if(methodName.equals("main") && currentMethodType.getKind() != Struct.None) {
			report_error("Greska na liniji " + methodLine + ": Metoda 'main' mora biti povratnog tipa 'void'!", null);
			return;
		}
		
		// ako metoda main ima formalne parametre
		if(methodName.equals("main") && methodDeclaration.getMethodSignature().getFormPars() instanceof FormalParameters) {
			report_error("Greska na liniji " + methodLine + ": Metoda 'main' ne sme imati formalne argumente!", null);
			return;
		}
		
		// ako metoda ima povratni tip i nema return <...>
		if(!returnFound && currentMethodType.getKind() != Struct.None) {
			report_error("Greska na liniji " + methodLine + ": Metoda " + methodName + " mora imati tipiziranu return naredbu!", null);
			return;
		}
		
		// ako metoda nema povratni tip i ima return <...>
		if(returnFound && currentMethodType.getKind() == Struct.None) {
			report_error("Greska na liniji " + methodLine + ": Metoda " + methodName + " ne moze imati tipiziranu return naredbu!", null);
			return;
		}
		
		if(currentMethod != null) {
			SymbolTable.chainLocalSymbols(currentMethod);
			SymbolTable.closeScope();
		}
		
		currentMethod = null;
		currentMethodType = null;
		currentFpPos = 1;
		returnFound = false;
		returnVoidFound = false;
	}
	
	public void visit(ReturnExprStmt returnExpr) {
		returnFound = true;
		
		int returnLine = returnExpr.getLine();
		String methodName = currentMethod.getName();
		
		if(!currentMethodType.compatibleWith(returnExpr.getExpr().struct) && returnExpr.getExpr().struct != null) {
			report_error("Greska na liniji " + returnLine + ": Povratna vrednost funkcije '" + methodName + "' se ne slaze sa tipom povratne vrednosti!", null);
		}
	}
	
	public void visit(ReturnNoExprStmt returnNoExpr) {
		returnVoidFound = true;
	}
	
	public void visit(NegativeExpr negativeExpr) {
		Struct term = negativeExpr.getTerm().struct;
		int lineNumber = negativeExpr.getLine();
		
		if(term == SymbolTable.intType) {
			negativeExpr.struct = term;
		}
		else {
			report_error("Greska na liniji " + lineNumber + ": Nekompatibilni tip za negaciju!", null);
			negativeExpr.struct = SymbolTable.noType;
		}
	}
	
	public void visit(PositiveExpr positiveExpr) {
		positiveExpr.struct = positiveExpr.getTerm().struct;
	}
	
	public void visit(ExprList exprList) {
		Struct expr = exprList.getExpr().struct;
		Struct term = exprList.getTerm().struct;
		int lineNumber = exprList.getLine();
		
		if(expr.getKind() == SymbolTable.intType.getKind() && term.getKind() == SymbolTable.intType.getKind()) {
			report_info("Sabiranje/oduzimanje simbola", exprList);
			exprList.struct = SymbolTable.intType;
		}
		else {
			report_error("Greska na liniji " + lineNumber + ": Nekompatibilni tipovi za operacije sabiranja i oduzimanja!", null);
			exprList.struct = SymbolTable.noType;
		}
	}
	
	public void visit(DesignatorExpr designatorExpr) {
		Obj leftDesignator = designatorExpr.getDesignator().obj;
		Obj rightDesignator = designatorExpr.getDesignator1().obj;
		Struct rightDesignatorType = rightDesignator.getType();
		int lineNumber = designatorExpr.getLine();
		
		if(leftDesignator.getKind() != Obj.Meth) {
			report_error("Greska na liniji " + lineNumber + ": Levi operator map naredbe mora biti funkcija!", null);
			designatorExpr.struct = SymbolTable.noType;
			return;
		}
		
		List<Obj> formalParams = new ArrayList<>();
	    for (Obj localObj : leftDesignator.getLocalSymbols()) {
	        if (localObj.getFpPos() > 0) {
	            formalParams.add(localObj);
	        }
	    }
	    
	    if (formalParams.size() != 1) {
	        report_error("Greska na liniji " + lineNumber + ": Levi operator map naredbe mora biti funkcija sa jednim formalnim parametrom!", null);
	        designatorExpr.struct = SymbolTable.noType;
	        return;
	    }
	    
	    if (formalParams.get(0).getType() != SymbolTable.intType) {
	        report_error("Greska na liniji " + lineNumber + ": Levi operator map naredbe mora biti funkcija sa jednim formalnim parametrom tipa 'int'!", null);
	        designatorExpr.struct = SymbolTable.noType;
	        return;
	    }
	    
	    if (leftDesignator.getType() != SymbolTable.intType) {
	        report_error("Greska na liniji " + lineNumber + ": Levi operator map naredbe mora biti funkcija povratne vrednosti tipa 'int'!!", null);
	        designatorExpr.struct = SymbolTable.noType;
	        return;
	    }
		
		if(rightDesignatorType.getKind() != Struct.Array) {
			report_error("Greska na liniji " + lineNumber + ": Desni operator map naredbe mora biti tipa 'niz'!", null);
			designatorExpr.struct = SymbolTable.noType;
			return;
		}
		
		if(rightDesignatorType.getElemType().getKind() != SymbolTable.intType.getKind()) {
			report_error("Greska na liniji " + lineNumber + ": Desni operator map naredbe mora biti tipa 'Arr of int'!", null);
			designatorExpr.struct = SymbolTable.noType;
			return;
		}
		
		report_info("Map naredba", designatorExpr);
		designatorExpr.struct = SymbolTable.intType;
	}
	
	public void visit(MulopTerm mulopTerm) {
		Struct term = mulopTerm.getTerm().struct;
		Struct factor = mulopTerm.getFactor().struct;
		int lineNumber = mulopTerm.getLine();
		
		if(term.getKind() == SymbolTable.intType.getKind() && factor.getKind() == SymbolTable.intType.getKind()) {
			report_info("Mnozenje/deljenje simbola", mulopTerm);
			mulopTerm.struct = SymbolTable.intType;
		}
		else {
			report_error("Greska na liniji " + lineNumber + ": Nekompatibilni tipovi za operacije mnozenja i deljenja!", null);
			mulopTerm.struct = SymbolTable.noType;
		}
	}
	
	public void visit(NoMulopTerm noMulopTerm) {
		noMulopTerm.struct = noMulopTerm.getFactor().struct;
	}
	
	public void visit(DesignatorIdentOnly designatorIdentOnly) {
		String designatorName = designatorIdentOnly.getIdent();
		int lineNumber = designatorIdentOnly.getLine();
		
		Obj obj = SymbolTable.find(designatorName);
		
		if (obj == SymbolTable.noObj) {
			report_error("Greska na liniji " + lineNumber + ": Simbol '" + designatorName + "' mora biti prethodno deklarisan ili definisan!", null);
			designatorIdentOnly.obj = SymbolTable.noObj;
		}
		else {
			report_info("Koriscenje simbola '" + designatorName + "'", designatorIdentOnly);
			designatorIdentOnly.obj = obj;
		}
	}
	
	public void visit(DesignatorWithExpr designatorWithExpr) {
		Obj baseDesignator = designatorWithExpr.getDesignator().obj;
		int lineNumber = designatorWithExpr.getLine();
		
		if (baseDesignator == SymbolTable.noObj || baseDesignator.getType() == SymbolTable.noType) {
			designatorWithExpr.obj = SymbolTable.noObj;
			return;
		}
		
		if (baseDesignator.getType().getKind() != Struct.Array) {
	        report_error("Greska na liniji " + lineNumber + ": Simbol '" + baseDesignator.getName() + "' mora biti niz da bi se indeksirao!", null);
	        designatorWithExpr.obj = SymbolTable.noObj;
			return;
		}
		
		Struct expr = designatorWithExpr.getExpr().struct;
		
		if (expr.getKind() != SymbolTable.intType.getKind()) {
			report_error("Greska na liniji " + lineNumber + ": Indeks niza mora biti tipa 'int'!", null);
			designatorWithExpr.obj = SymbolTable.noObj;
			return;
		}
		
		designatorWithExpr.obj = new Obj(Obj.Elem, "element niza", baseDesignator.getType().getElemType());
		report_info("Pristup elementu niza '" + baseDesignator.getName() + "'", designatorWithExpr);
	}
	
	public void visit(IfStmt ifStmt) {
		Struct conditionStruct = ifStmt.getCondition().struct;
		int lineNumber = ifStmt.getLine();
		
		if(conditionStruct.getKind() != SymbolTable.boolType.getKind()) {
			report_error("Greska na liniji " + lineNumber + ": Uslov if naredbe mora biti tipa 'bool'!", null);
			return;
		}

		report_info("If naredba", ifStmt);
	}
	
	public void visit(ElseStatement elseStatement) {
		report_info("Else grana", elseStatement);
	}
	
	public void visit(CondFactExpr condFactExpr) {
		Struct exprType = condFactExpr.getExpr().struct;
		int lineNumber = condFactExpr.getLine();
		
		if (exprType.getKind() != SymbolTable.boolType.getKind()) {
			report_error("Greska na liniji " + lineNumber + ": Uslovni izraz mora biti tipa 'bool'!", null);
			condFactExpr.struct = SymbolTable.noType;
			return;
		}
		
		condFactExpr.struct = SymbolTable.boolType;
	}
	
	public void visit(CondFactRelop condFactRelop) {
		Struct leftExprType = condFactRelop.getExpr().struct;
	    Struct rightExprType = condFactRelop.getExpr1().struct;
	    int lineNumber = condFactRelop.getLine();
	    
	    if (!leftExprType.compatibleWith(rightExprType) && !rightExprType.compatibleWith(leftExprType)) {
	        report_error("Greska na liniji " + lineNumber + ": Tipovi izraza u uslovu nisu kompatibilni!", null);
	        condFactRelop.struct = SymbolTable.noType;
	        return;
	    }
	    
	    if (leftExprType.getKind() == Struct.Array || rightExprType.getKind() == Struct.Array) {
	        if (!(condFactRelop.getRelop() instanceof Equal) && !(condFactRelop.getRelop() instanceof NotEqual)) {
	            report_error("Greska na liniji " + lineNumber + ": Za tip 'Arr' mogu se koristiti samo operatori '==' i '!='!", null);
	            condFactRelop.struct = SymbolTable.noType;
	            return;
	        }
	    }
	    
	    condFactRelop.struct = SymbolTable.boolType;
	}
	
	public void visit(NoCondTermList noCondTermList) {
		noCondTermList.struct = noCondTermList.getCondTerm().struct;
	}
	
	public void visit(CondTermList condTermList) {
	    Struct conditionType = condTermList.getCondition().struct;
	    Struct condTermType = condTermList.getCondTerm().struct;
	    int lineNumber = condTermList.getLine();
	    
	    if (conditionType != SymbolTable.boolType || condTermType != SymbolTable.boolType) {
	        report_error("Greska na liniji " + lineNumber + ": Argumenti OR operacije moraju biti tipa 'bool'!", null);
	        condTermList.struct = SymbolTable.noType;
	    } else {
	        condTermList.struct = SymbolTable.boolType;
	    }
	}
	
	public void visit(NoCondFactList noCondFactList) {
	    noCondFactList.struct = noCondFactList.getCondFact().struct;
	}
	
	public void visit(CondFactList condFactList) {
	    Struct condTermType = condFactList.getCondTerm().struct;
	    Struct condFactType = condFactList.getCondFact().struct;
	    int lineNumber = condFactList.getLine();
	    
	    if (condTermType != SymbolTable.boolType || condFactType != SymbolTable.boolType) {
	        report_error("Greska na liniji " + lineNumber + ": Argumenti AND operacije moraju biti tipa 'bool'!", null);
	        condFactList.struct = SymbolTable.noType;
	    } else {
	        condFactList.struct = SymbolTable.boolType;
	    }
	}
	
	public void visit(AssignDesignatorStmt assignDesignatorStmt) {
		String designatorName = "";
		Designator designator = assignDesignatorStmt.getDesignator();
		
		if (designator instanceof DesignatorIdentOnly) {
			designatorName = ((DesignatorIdentOnly)designator).getIdent();
		}
		
		Struct designatorType = assignDesignatorStmt.getDesignator().obj.getType();
		Struct expr = assignDesignatorStmt.getExpr().struct;
		String exprTypeName = SymbolTable.getTypeName(expr);
		int lineNumber = assignDesignatorStmt.getLine();
		
		if(designator.obj.getKind() == Obj.Con) {
			report_error("Greska na liniji " + lineNumber + ": Simbol '" + designatorName + "' je konstanta!", null);
		}
		else if(expr.getKind() != designatorType.getKind()) {
			report_error("Greska na liniji " + lineNumber + ": Tip simbola '" + designatorName + "' i tip vrednosti '" + exprTypeName + "' se ne poklapaju!", null);
		}
		else {
			report_info("Dodela vrednosti simbolu '" + designatorName + "'", assignDesignatorStmt);
		}
	}
	
	public void visit(IncrDesignatorStmt incrDesignatorStmt) {
		String designatorName = "";
		Designator designator = incrDesignatorStmt.getDesignator();
		
		if (designator instanceof DesignatorIdentOnly) {
			designatorName = ((DesignatorIdentOnly)designator).getIdent();
		}
		
		Struct designatorType = designator.obj.getType();
		String designatorTypeName = SymbolTable.getTypeName(designatorType);
		int lineNumber = incrDesignatorStmt.getLine();
		
		if(designator.obj.getKind() != Obj.Var) {
			report_error("Greska na liniji " + lineNumber + ": Simbol '" + designatorName + "' tipa '" + designatorTypeName + "' nije promenljiva!", null);
		}
		else if(designatorType.getKind() != SymbolTable.intType.getKind()) {
			report_error("Greska na liniji " + lineNumber + ": Simbol '" + designatorName + "' tipa '" + designatorTypeName + "' se ne moze inkrementirati!", null);
		}
		else {
			report_info("Inkrementiranje vrednosti simbola '" + designatorName + "'", incrDesignatorStmt);
		}
	}
	
	public void visit(DecrDesignatorStmt decrDesignatorStmt) {
		String designatorName = "";
		Designator designator = decrDesignatorStmt.getDesignator();
		
		if (designator instanceof DesignatorIdentOnly) {
			designatorName = ((DesignatorIdentOnly)designator).getIdent();
		}
		
		Struct designatorType = decrDesignatorStmt.getDesignator().obj.getType();
		String designatorTypeName = SymbolTable.getTypeName(designatorType);
		int lineNumber = decrDesignatorStmt.getLine();
		
		if(designatorType != SymbolTable.intType) {
			report_error("Greska na liniji " + lineNumber + ": Simbol '" + designatorName + "' tipa '" + designatorTypeName + "' se ne moze dekrementirati!", null);
		}
		else {
			report_info("Dekrementiranje vrednosti simbola '" + designatorName + "'", decrDesignatorStmt);
		}
	}
	
	public void visit(ReadStmt readStmt) {
		Designator designator = readStmt.getDesignator();
		Struct designatorType = designator.obj.getType();
		String designatorName = designator.obj.getName();
		int lineNumber = readStmt.getLine();
		
		if(designator.obj == SymbolTable.noObj) {
			return;
		}
		
		if(designator.obj.getKind() != Obj.Var && designator.obj.getKind() != Obj.Elem) {
	        report_error("Greska na liniji " + lineNumber + ": Simbol '" + designatorName + "' mora biti promenljiva ili element niza!", null);
	        return;
		}
		
		if (designatorType != SymbolTable.intType
			&& designatorType != SymbolTable.charType
			&& designatorType != SymbolTable.boolType) {
		        report_error("Greska na liniji " + lineNumber + ": Simbol '" + designatorName + "' mora biti tipa 'int', 'char' ili 'bool'!", null);
		        return;
		    }
		
	    report_info("Read naredba i upis u simbol '" + designatorName + "'", readStmt);
	}
	
	public void visit(SetDesignatorStmt setDesignatorStmt) {
		Designator resultDesignator = setDesignatorStmt.getDesignator();
		Designator leftDesignator = setDesignatorStmt.getDesignator1();
		Designator rightDesignator = setDesignatorStmt.getDesignator2();
		int lineNumber = setDesignatorStmt.getLine();
		
		if (resultDesignator.obj == SymbolTable.noObj
			|| leftDesignator.obj == SymbolTable.noObj
			|| rightDesignator.obj == SymbolTable.noObj) {
			return;
		}
		
		Struct resultType = resultDesignator.obj.getType();
		Struct leftType = leftDesignator.obj.getType();
		Struct rightType = rightDesignator.obj.getType();
		
		String resultName = resultDesignator.obj.getName();
		String leftName = leftDesignator.obj.getName();
		String rightName = rightDesignator.obj.getName();
		
		if (resultType.getKind() != SymbolTable.setType.getKind()) {
			report_error("Greska na liniji " + lineNumber + ": Simbol '" + resultName + "' mora biti tipa 'set'!", null);
			return;
		}
		if (leftType.getKind() != SymbolTable.setType.getKind()) {
			report_error("Greska na liniji " + lineNumber + ": Simbol '" + leftName + "' mora biti tipa 'set'!", null);
			return;
		}
		if (rightType.getKind() != SymbolTable.setType.getKind()) {
			report_error("Greska na liniji " + lineNumber + ": Simbol '" + rightName + "' mora biti tipa 'set'!", null);
			return;
		}
		
		report_info("Unija setova " + leftName + " i " + rightName, setDesignatorStmt);
	}
	
	public void visit(PrintNoNumStmt printNoNumStmt) {
		Struct exprType = printNoNumStmt.getExpr().struct;
		int lineNumber = printNoNumStmt.getLine();
		
		if(exprType.getKind() == SymbolTable.noType.getKind()) {
			return;
		}
		
		if(exprType.getKind() != SymbolTable.intType.getKind()
			&& exprType.getKind() != SymbolTable.charType.getKind()
			&& exprType.getKind() != SymbolTable.boolType.getKind()
			&& exprType.getKind() != SymbolTable.setType.getKind()) {
			report_error("Greska na liniji " + lineNumber + ": Argument print metode nije odgovarajuceg tipa!", null);
			return;
		}
		
		report_info("Ispis", printNoNumStmt);
		
	}
	
	public void visit(ContinueStmt continueStmt) {
		SyntaxNode currNode = continueStmt.getParent();
		boolean doWhileLoop = false;
		
		while(currNode != null) {
			if(currNode instanceof DoWhileStmt) {
				doWhileLoop = true;
				break;
			}
			currNode = currNode.getParent();
		}
		
		if (!doWhileLoop) {
			report_error("Greska: Continue naredba se moze koristiti samo unutar Do While petlje!", continueStmt);
			return;
		}
		
		report_info("Continue naredba", continueStmt);
	}
	
	public void visit(BreakStmt breakStmt) {
		SyntaxNode currNode = breakStmt.getParent();
		boolean doWhileLoop = false;
		
		while(currNode != null) {
			if(currNode instanceof DoWhileStmt) {
				doWhileLoop = true;
				break;
			}
			currNode = currNode.getParent();
		}
		
		if (!doWhileLoop) {
			report_error("Greska: Break naredba se moze koristiti samo unutar Do While petlje!", breakStmt);
			return;
		}
		
		report_info("Break naredba", breakStmt);
	}
	
	public void visit(DoWhileStmt doWhileStmt) {
		report_info("Do while pocetak", doWhileStmt);
	}
	
	public void visit(DoWhileCond doWhileCond) {
		report_info("Do while kraj sa uslovom", doWhileCond);
	}
	
	public void visit(DoWhileCondDesignator doWhileCondDesignator) {
		report_info("Do while kraj sa uslovom i iskazom", doWhileCondDesignator);
	}
	
	public void visit(NoDoWhileBody noDoWhileBody) {
		report_info("Do while kraj bez uslova", noDoWhileBody);
	}
	
	public void visit(NumConst numConst) {
		numConst.struct = SymbolTable.intType;
	}
	
	public void visit(CharConst charConst) {
		charConst.struct = SymbolTable.charType;
	}
	
	public void visit(BoolConst boolConst) {
		boolConst.struct = SymbolTable.boolType;
	}
	
	public void visit(FactorNoActPars factorActNoPars) {
		factorActNoPars.struct = factorActNoPars.getDesignator().obj.getType();
	}
	
	public void visit(ActParsDesignatorStmt actParsDesignatorStmt) {
		Obj designator = actParsDesignatorStmt.getDesignator().obj;
		String designatorName = designator.getName();
		int lineNumber = actParsDesignatorStmt.getLine();
		
		if (designator == SymbolTable.noObj) {
			return;
		}
		
		if (designator.getKind() != Obj.Meth) {
			report_error("Greska na liniji " + lineNumber + ": Simbol '" + designatorName + "' mora biti metoda!", null);
			return;
		}

		List<Obj> formalParams = new ArrayList<>();
		
		for(Obj localObj : designator.getLocalSymbols()) {
			if(localObj.getKind() == Obj.Var && localObj.getFpPos() > 0) {
				formalParams.add(localObj);
			}
		}
		
		List<Expr> actualExprs = new ArrayList<>();
		ActPars actualParametersNode = actParsDesignatorStmt.getActPars();
		
		if (actualParametersNode instanceof ActualParameters) {
			ActualParameters actualParameters = (ActualParameters)actualParametersNode;
            ActualPars currActualPars = actualParameters.getActualPars();
            
            while (currActualPars != null) {
                if (currActualPars instanceof NoActParsList) {
                    actualExprs.add(((NoActParsList)currActualPars).getExpr());
                    break;
                } 
                else if (currActualPars instanceof ActParsList) {
                    ActParsList actParsList = (ActParsList)currActualPars;
                    actualExprs.add(actParsList.getExpr());
                    currActualPars = actParsList.getActualPars();
                } 
                else break;
            }
            
            Collections.reverse(actualExprs);
        }
		
		if (designatorName.equals("chr")) {
           if (actualExprs.size() != 1 || actualExprs.get(0).struct != SymbolTable.intType) {
               report_error("Greska na liniji " + lineNumber + ": Metoda 'chr' mora imati jedan stvaran argument tipa 'int'!", null);
               return;
           }
           
           report_info("Poziv metode 'chr'", actParsDesignatorStmt);
           return;
		}
		
		if (designatorName.equals("ord")) {
			if (actualExprs.size() != 1 || actualExprs.get(0).struct != SymbolTable.charType) {
                report_error("Greska na liniji " + lineNumber + ": Metoda 'ord' mora imati jedan stvaran argument tipa 'char'!", null);
                return;
            }
			
            report_info("Poziv metode 'ord'", actParsDesignatorStmt);
            return;
		}
		
		if (designatorName.equals("len")) {
			if (actualExprs.size() != 1 || actualExprs.get(0).struct.getKind() != Struct.Array) {
				report_error("Greska na liniji " + lineNumber + ": Metoda 'len' mora imati jedan stvaran argument tipa 'Arr' ili 'Arr of char'!", null);
	    	 	return;
	        }
            
            report_info("Poziv metode 'len'", actParsDesignatorStmt);
            return;
        }
		
		if (designatorName.equals("add")) {
			if (actualExprs.size() != 2
				|| actualExprs.get(0).struct.getKind() != SymbolTable.setType.getKind()
				|| actualExprs.get(1).struct.getKind() != SymbolTable.intType.getKind()) {
				report_error("Greska na liniji " + lineNumber + ": Metoda 'add' mora imati tacno dva stvarna argumenta tipa 'set' i 'int'!", null);
	            return;
			}
			
	        report_info("Poziv metode 'add'", actParsDesignatorStmt);
	        return;
		}
		
		if (designatorName.equals("addAll")) {
			if (actualExprs.size() != 2
				|| actualExprs.get(0).struct.getKind() != SymbolTable.setType.getKind()
				|| actualExprs.get(1).struct.getKind() != Struct.Array
				|| actualExprs.get(1).struct.getElemType() != SymbolTable.intType) {
				report_error("Greska na liniji " + lineNumber + ": Metoda 'addAll' mora imati tacno dva stvarna argumenta tipa 'set' i 'Arr of int'!", null);
	            return;
			}
			
	        report_info("Poziv metode 'addAll'", actParsDesignatorStmt);
	        return;
		}
		
		if (formalParams.size() != actualExprs.size()) { 
            report_error("Greska na liniji " + lineNumber + ": Broj stvarnih argumenata funkcije '" + designatorName + "' se ne poklapa sa brojem formalnih argumenata!", null);
            return;
        } 
		
        for (int i = 0; i < formalParams.size(); i++) {
        	Obj formalParam = formalParams.get(i);
            Expr actualArgExpr = actualExprs.get(i);
            
            if (actualArgExpr.struct.getKind() != formalParam.getType().getKind()) {
                report_error("Greska na liniji " + lineNumber + ": Tip stvarnog argumenta funkcije '" + designatorName + "' se ne poklapa sa tipom formalnog argumenta!", null);
                return;
            }
        }
		
		
        report_info("Poziv metode '" + designatorName + "'", actParsDesignatorStmt);
        return;
	}
	
	public void visit(FactorActPars factorActPars) {
		Obj designator = factorActPars.getDesignator().obj;
		String designatorName = designator.getName();
		int lineNumber = factorActPars.getLine();
		
		if (designator == SymbolTable.noObj) {
			factorActPars.struct = SymbolTable.noType;
			return;
		}
		
		if (designator.getKind() != Obj.Meth) {
			report_error("Greska na liniji " + lineNumber + ": Simbol '" + designatorName + "' mora biti metoda!", null);
			factorActPars.struct = SymbolTable.noType;
			return;
		}
		
		List<Obj> formalParams = new ArrayList<>();
		
		for(Obj localObj : designator.getLocalSymbols()) {
			if(localObj.getKind() == Obj.Var && localObj.getFpPos() > 0) {
				formalParams.add(localObj);
			}
		}
		
		List<Expr> actualExprs = new ArrayList<>();
		ActualParameters actualParametersNode = null;
		
		if (factorActPars.getActPars() instanceof ActualParameters) {
            actualParametersNode = (ActualParameters) factorActPars.getActPars();
        }
		
		if (actualParametersNode != null) {
            ActualPars currActualPars = actualParametersNode.getActualPars();
            
            while (currActualPars != null) {
                if (currActualPars instanceof NoActParsList) {
                    actualExprs.add(((NoActParsList)currActualPars).getExpr());
                    break;
                } 
                else if (currActualPars instanceof ActParsList) {
                    ActParsList actParsList = (ActParsList)currActualPars;
                    actualExprs.add(actParsList.getExpr());
                    currActualPars = actParsList.getActualPars();
                } 
                else break;
            }
            
            Collections.reverse(actualExprs);
        }

		if (designatorName.equals("chr")) {
            if (actualExprs.size() != 1 || actualExprs.get(0).struct != SymbolTable.intType) {
               report_error("Greska na liniji " + lineNumber + ": Metoda 'chr' mora imati jedan stvaran argument tipa 'int'!", null);
               factorActPars.struct = SymbolTable.noType;
               return;
           }
           
           factorActPars.struct = SymbolTable.charType;
           report_info("Poziv metode 'chr'", factorActPars);
           return;
		}
		
		if (designatorName.equals("ord")) {
			if (actualExprs.size() != 1 || actualExprs.get(0).struct != SymbolTable.charType) {
                report_error("Greska na liniji " + lineNumber + ": Metoda 'ord' mora imati jedan stvaran argument tipa 'char'!", null);
                factorActPars.struct = SymbolTable.noType;
                return;
            }
			
            factorActPars.struct = SymbolTable.intType;
            report_info("Poziv metode 'ord'", factorActPars);
            return;
		}
		
		if (designatorName.equals("len")) {
			if (actualExprs.size() != 1 || actualExprs.get(0).struct.getKind() != Struct.Array) {
				report_error("Greska na liniji " + lineNumber + ": Metoda 'len' mora imati jedan stvaran argument tipa 'Arr' ili 'Arr of char'!", null);
				factorActPars.struct = SymbolTable.noType;
	    	 	return;
	        }
             
            factorActPars.struct = SymbolTable.intType;
            report_info("Poziv metode 'len'", factorActPars);
            return;
        }
		
		if (designatorName.equals("add")) {
			if (actualExprs.size() != 2
				|| actualExprs.get(0).struct.getKind() != SymbolTable.setType.getKind()
				|| actualExprs.get(1).struct.getKind() != SymbolTable.intType.getKind()) {
				report_error("Greska na liniji " + lineNumber + ": Metoda 'add' mora imati tacno dva stvarna argumenta tipa 'set' i 'int'!", null);
	            factorActPars.struct = SymbolTable.noType;
	            return;
			}
			
			factorActPars.struct = SymbolTable.noType;
	        report_info("Poziv metode 'add'", factorActPars);
	        return;
		}
		
		if (designatorName.equals("addAll")) {
			if (actualExprs.size() != 2
				|| actualExprs.get(0).struct.getKind() != SymbolTable.setType.getKind()
				|| actualExprs.get(1).struct.getKind() != Struct.Array
				|| actualExprs.get(1).struct.getElemType() != SymbolTable.intType) {
				report_error("Greska na liniji " + lineNumber + ": Metoda 'addAll' mora imati tacno dva stvarna argumenta tipa 'set' i 'Arr of int'!", null);
	            factorActPars.struct = SymbolTable.noType;
	            return;
			}
			
			factorActPars.struct = SymbolTable.noType;
	        report_info("Poziv metode 'addAll'", factorActPars);
	        return;
		}
		
		if (formalParams.size() != actualExprs.size()) { 
            report_error("Greska na liniji " + lineNumber + ": Broj stvarnih argumenata funkcije '" + designatorName + "' se ne poklapa sa brojem formalnih argumenata!", null);
            factorActPars.struct = SymbolTable.noType;
            return;
        } 
		
        for (int i = 0; i < formalParams.size(); i++) {
        	Obj formalParam = formalParams.get(i);
        	Expr actualArgExpr = actualExprs.get(i);
            
            if (actualArgExpr.struct.getKind() != formalParam.getType().getKind()) {
                report_error("Greska na liniji " + lineNumber + ": Tip stvarnog argumenta funkcije '" + designatorName + "' se ne poklapa sa tipom formalnog argumenta!", null);
        		factorActPars.struct = SymbolTable.noType;
                return;
            }
        }
		
		factorActPars.struct = designator.getType();
        report_info("Poziv metode '" + designatorName + "'", factorActPars);
	}
	
	public void visit(FactorNum factorNum) {
		factorNum.struct = SymbolTable.intType;
	}
	
	public void visit(FactorChar factorChar) {
		factorChar.struct = SymbolTable.charType;
	}
	
	public void visit(FactorBool factorBool) {
		factorBool.struct = SymbolTable.boolType;
	}
	
	public void visit(FactorNewExpr factorNewExpr) {
		Struct expr = factorNewExpr.getExpr().struct;
		Struct type = factorNewExpr.getType().struct;
		String typeName = SymbolTable.getTypeName(type);
		int lineNumber = factorNewExpr.getLine();
		
		if (expr.getKind() != SymbolTable.intType.getKind()) {
			report_error("Greska na liniji " + lineNumber + ": Velicina niza ili skupa mora biti tipa 'int'!", null);
			factorNewExpr.struct = SymbolTable.noType;
		}
		
		if (type.getKind() == SymbolTable.setType.getKind()) {
			report_info("Kreiranje skupa", factorNewExpr);
			factorNewExpr.struct = SymbolTable.setType;
		}
		else {
			report_info("Kreiranje niza tipa '" + typeName + "'", factorNewExpr);
			factorNewExpr.struct = new Struct(Struct.Array, type);
		}
	}
	
	public void visit(FactorNewActPars factorNewActExpr) {
		factorNewActExpr.struct = SymbolTable.noType;
	}
	
	public void visit(FactorExpr factorExpr) {
		factorExpr.struct = factorExpr.getExpr().struct;
	}
	
	public boolean passed() {
    	return !errorDetected;
    }
}
