package rs.ac.bg.etf.pp1;

import java.util.Stack;

import rs.ac.bg.etf.pp1.CounterVisitor.FormParamCounter;
import rs.ac.bg.etf.pp1.CounterVisitor.VarCounter;
import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.mj.runtime.Code;
import rs.etf.pp1.symboltable.concepts.Obj;

public class CodeGenerator extends VisitorAdaptor {

	private int mainPc;
	private Stack<Integer> fixupStack = new Stack<>();
	private int doWhileStart;
	private int doWhileEnd;
	private boolean doWhileBreaked = false;
	
	public int getMainPc() {
		return mainPc;
	}
	
	public void visit(ProgName progName) {
		generateChrFunction();
		generateOrdFunction();
		generateLenFunction();
		generateAddFunction();
		generateAddAllFunction();
	}
	
	public void visit(PrintNoNumStmt printNoNumStmt) {
		if (printNoNumStmt.getExpr().struct.getKind() == SymbolTable.intType.getKind()) {
			Code.loadConst(5);
			Code.put(Code.print);
		}
		else if (printNoNumStmt.getExpr().struct.getKind() == SymbolTable.boolType.getKind()) {
			Code.loadConst(1);
			Code.put(Code.print);
		}
		else if (printNoNumStmt.getExpr().struct.getKind() == SymbolTable.setType.getKind()) {
			printSet();
		}
		else {
			Code.loadConst(1);
			Code.put(Code.bprint);
		}
	}
	
	public void visit(FactorNum factorNum) {
		Obj cnst = new Obj(Obj.Con, "$", SymbolTable.intType);
		cnst.setLevel(0);
		cnst.setAdr(factorNum.getNumConst());
		
		Code.load(cnst);
	}
	
	public void visit(FactorChar factorChar) {
		Obj cnst = new Obj(Obj.Con, "$", SymbolTable.charType);
		cnst.setLevel(0);
		cnst.setAdr(factorChar.getCharConst());
		
		Code.load(cnst);
	}
	
	public void visit(FactorBool factorBool) {
		Obj cnst = new Obj(Obj.Con, "$", SymbolTable.boolType);
		cnst.setLevel(0);
		cnst.setAdr(factorBool.getBoolConst() ? 1 : 0);
		
		Code.load(cnst);
	}
	
	public void visit(FactorNewExpr factorNewExpr) {
		Code.put(Code.newarray);
		
		if (factorNewExpr.struct.getKind() == SymbolTable.setType.getKind()) {
			Code.put(SymbolTable.intType.getKind());
		}
		else {
			Code.put(factorNewExpr.struct.getElemType().getKind());
		}
	}
	
	public void visit(MethodTypeName methodTypeName) {
		if (methodTypeName.getMethodName().equals("main")) {
			mainPc = Code.pc;
		}
		methodTypeName.obj.setAdr(Code.pc);
		
		SyntaxNode methodSignature = methodTypeName.getParent();
		SyntaxNode methodDeclaration = methodSignature.getParent();
		
		VarCounter varCounter = new VarCounter();
		methodDeclaration.traverseTopDown(varCounter);
		
		FormParamCounter fpCnt = new FormParamCounter();
		methodSignature.traverseTopDown(fpCnt);
		
		Code.put(Code.enter);
		Code.put(fpCnt.getCount());
		Code.put(fpCnt.getCount() + varCounter.getCount());
	}
	
	public void visit(MethodDeclaration methodDeclaration) {
		Code.put(Code.exit);
		Code.put(Code.return_);
	}
	
	public void visit(AssignDesignatorStmt assignDesignatorStmt) {
		Designator designator = assignDesignatorStmt.getDesignator();
		
		if (designator instanceof DesignatorIdentOnly) {
			Code.store(assignDesignatorStmt.getDesignator().obj);
		}
		else if (designator instanceof DesignatorWithExpr) {
			if(((DesignatorWithExpr)(designator)).obj.getType().getKind() == SymbolTable.charType.getKind()) {
				Code.put(Code.bastore);
			}
			else {
				Code.put(Code.astore);
			}
		}
	}
	
	public void visit(IncrDesignatorStmt incrDesignatorStmt) {
		Obj obj = incrDesignatorStmt.getDesignator().obj;
		
		Code.loadConst(1);
		Code.put(Code.add);
		Code.store(obj);
	}
	
	public void visit(DecrDesignatorStmt decrDesignatorStmt) {
		Obj obj = decrDesignatorStmt.getDesignator().obj;
		
		Code.loadConst(1);
		Code.put(Code.sub);
		Code.store(obj);
	}
	
	public void visit(DesignatorIdentOnly designatorIdentOnly) {
		SyntaxNode parent = designatorIdentOnly.getParent();
			
		if (parent.getClass() != AssignDesignatorStmt.class 
				&& parent.getClass() != ActParsDesignatorStmt.class
				&& parent.getClass() != ReadStmt.class) {
			Code.load(designatorIdentOnly.obj);
		}
	}
	
	public void visit(DesignatorWithExpr designatorWithExpr) {
		SyntaxNode parent = designatorWithExpr.getParent();
		
		if (parent.getClass() != AssignDesignatorStmt.class) {
			if (designatorWithExpr.obj.getType().getKind() == SymbolTable.charType.getKind()) {
				Code.put(Code.baload);
			}
			else {
				Code.put(Code.aload);
			}
		}
	}
	
	public void visit(NegativeExpr negativeExpr) {
		Code.put(Code.neg);
	}
	
	public void visit(ExprList exprList) {
		Addop addop = exprList.getAddop();
		
		if (addop instanceof Plus) {
			Code.put(Code.add);
		}
		else {
			Code.put(Code.sub);
		}
	}
	
	public void visit(IfStmt ifStmt) {
		Code.fixup(fixupStack.pop());
	}
	
	public void visit(IfStart ifStart) {
		Code.loadConst(0);
		int skip = Code.pc + 1;
		Code.putFalseJump(Code.ne, 0);
		fixupStack.push(skip);
	}
	
	public void visit(ElseStart elseStart) {
		int skip = Code.pc + 1;
		Code.putJump(0);
		Code.fixup(fixupStack.pop());
		fixupStack.add(skip);
	}
	
	public void visit(CondTermList condTermList) {
		Code.put(Code.add);
	}
	
	public void visit(CondFactList condFactList) {
		Code.put(Code.mul);
	}
	
	public void visit(CondFactExpr condFactExpr) {
		Code.loadConst(0);
		
		int notEqual = Code.pc + 1;
		Code.putFalseJump(Code.ne, 0);
		Code.loadConst(1);
		int end = Code.pc + 1;
		Code.putJump(0);
		Code.fixup(notEqual);
		Code.loadConst(0);
		Code.fixup(end);
	}
	
	public void visit(CondFactRelop condFactRelop) {
		Relop relop = condFactRelop.getRelop();
		
		int falseLabel = Code.pc + 1;
		
		if (relop instanceof Equal) {
			Code.putFalseJump(Code.eq, 0);
		}
		else if (relop instanceof NotEqual) {
			Code.putFalseJump(Code.ne, 0);
		}
		else if (relop instanceof Greater) {
			Code.putFalseJump(Code.gt, 0);
		}
		else if (relop instanceof GreaterEqual) {
			Code.putFalseJump(Code.ge, 0);
		}
		else if (relop instanceof Less) {
			Code.putFalseJump(Code.lt, 0);
		}
		else {
			Code.putFalseJump(Code.le, 0);
		}
		
		Code.loadConst(1);
		int end = Code.pc + 1;
		Code.putJump(0);
		Code.fixup(falseLabel);
		Code.loadConst(0);
		Code.fixup(end);
	}
	
	public void visit(MulopTerm mulopTerm) {
		Mulop mulop = mulopTerm.getMulop();
		
		if (mulop instanceof Multiply) {
			Code.put(Code.mul);
		}
		else if (mulop instanceof Divide) {
			Code.put(Code.div);
		}
		else {
			Code.put(Code.rem);
		}
	}
	
	public void visit(ReadStmt readStmt) {
		Designator designator = readStmt.getDesignator();
		
		if (designator instanceof DesignatorWithExpr) {
			DesignatorWithExpr arrayAccess = (DesignatorWithExpr)designator;
	        arrayAccess.getDesignator().traverseBottomUp(this);
	        arrayAccess.getExpr().traverseBottomUp(this);
		}
		
		if (designator.obj.getType().getKind() == SymbolTable.charType.getKind()) {
			Code.put(Code.bread);
		}
		else {
			Code.put(Code.read);
		}
		
		Obj obj = designator.obj;
		Code.store(obj);

		if (designator instanceof DesignatorWithExpr) {
			Code.put(Code.pop);
		}
	}
	
	public void visit(ActParsDesignatorStmt actParsDesignatorStmt) {
		Obj functionObj = actParsDesignatorStmt.getDesignator().obj;
		int offset = functionObj.getAdr() - Code.pc;
		Code.put(Code.call);
		Code.put2(offset);
		
		if(functionObj.getType().getKind() != SymbolTable.noType.getKind()) {
			Code.put(Code.pop);
		}
	}
	
	public void visit(FactorActPars factorActPars) {
		Obj functionObj = factorActPars.getDesignator().obj;
		int offset = functionObj.getAdr() - Code.pc;
		Code.put(Code.call);
		Code.put2(offset);
	}
	
	public void visit(ReturnExprStmt returnExprStmt) {
		Code.put(Code.exit);
		Code.put(Code.return_);
	}
	
	public void visit(ReturnNoExprStmt returnNoExprStmt) {
		Code.put(Code.exit);
		Code.put(Code.return_);
	}
	
	public void visit(SetDesignatorStmt setDesignatorStmt) {
		union();
	}
	
	public void visit(DoStart doStart) {
		this.doWhileStart = Code.pc;
	}
	
	public void visit(ContinueStmt continueStmt) {
		Code.put(Code.jmp);
		Code.put2(this.doWhileStart - Code.pc + 1);
	}
	
	public void visit(BreakStmt breakStmt) {
		this.doWhileBreaked = true;
		this.doWhileEnd = Code.pc + 1;
		Code.putJump(0);
	}
	
	public void visit(DoWhileStmt doWhileStmt) {
		if(this.doWhileBreaked) {
			Code.fixup(this.doWhileEnd);
			this.doWhileBreaked = false;
		}
	}
	
	public void visit(DoWhileCond doWhileCond) {
		Code.loadConst(0);
		int skip = Code.pc + 1;
		Code.putFalseJump(Code.ne, 0);
		Code.put(Code.jmp);
		Code.put2(this.doWhileStart - Code.pc + 1);
		Code.fixup(skip);
	}
	
	public void visit(DoWhileCondDesignator doWhileCondDesignator) {
		doWhileCondDesignator.getCondition().traverseBottomUp(this);
		Code.loadConst(0);
		int skip = Code.pc + 1;
	    Code.putFalseJump(Code.ne, 0);
	    Code.put(Code.pop);
	    Code.put(Code.jmp);
	    Code.put2(this.doWhileStart - Code.pc + 1);
	    Code.fixup(skip);
	    Code.put(Code.pop);
	}
	
	public void visit(NoDoWhileBody noDoWhileBody) {
		Code.put(Code.jmp);
		Code.put2(this.doWhileStart - Code.pc + 1);
	}
	
	public void visit(DesignatorExpr designatorExpr) {
		Obj function = designatorExpr.getDesignator().obj;
		Obj array = designatorExpr.getDesignator1().obj;
		
		map(function, array);
	}
	
	private void printSet() {
		// set address
		Obj set = new Obj(Obj.Var, "set", SymbolTable.intType);
		set.setAdr(100);
		Code.store(set);
		
		// counter for iterating through set, 0 at start
		Obj counter = new Obj(Obj.Var, "counter", SymbolTable.intType);
		counter.setAdr(101);
		Code.loadConst(0);
		Code.store(counter);
		
		// currentItem = set[counter]
		Obj currentItem = new Obj(Obj.Var, "currentItem", SymbolTable.intType);
		currentItem.setAdr(102);
		
		// loop start
		int loop = Code.pc;
		
		// fetch set[counter] and store it in currentItem
		Code.load(set);
		Code.load(counter);
		Code.put(Code.aload);
		Code.store(currentItem);
		
		// break if currentItem is 0
		Code.load(currentItem);
		Code.loadConst(0);
		int exit = Code.pc + 1;
		Code.putFalseJump(Code.ne, 0);
		
		// print currentItem
		Code.load(currentItem);
		Code.loadConst(5);
		Code.put(Code.print);
		
		// increment counter
		Code.load(counter);
		Code.loadConst(1);
		Code.put(Code.add);
		Code.store(counter);
		
		// loop again if counter < setLength
		Code.load(counter);
		Code.load(set);
		Code.put(Code.arraylength);
		Code.put(Code.jcc + Code.ne);
		Code.put2(loop - Code.pc + 1);
		
		// loop end
		Code.fixup(exit);
	}
	
	private void union() {
		// set1 address
		Obj set1 = new Obj(Obj.Var, "set1", SymbolTable.intType);
		set1.setAdr(100);
		
		// set2 address
		Obj set2 = new Obj(Obj.Var, "set2", SymbolTable.intType);
		set2.setAdr(101);
		
		// destination set address
		Obj destSet = new Obj(Obj.Var, "destSet", SymbolTable.intType);
		destSet.setAdr(102);
		
		Code.store(set2);
		Code.store(set1);
		Code.store(destSet);
		
		// position for inserting in destination set, 0 at start
		Obj insertPos = new Obj(Obj.Var, "insertPos", SymbolTable.intType);
		insertPos.setAdr(103);
		Code.loadConst(0);
		Code.store(insertPos);
		
		// counter for iterating through set1 and set2, 0 at start
		Obj counter = new Obj(Obj.Var, "counter", SymbolTable.intType);
		counter.setAdr(104);
		Code.loadConst(0);
		Code.store(counter);
		
		// counter for searching if set1[counter] or set2[counter] is already in destination set
		Obj searchCounter = new Obj(Obj.Var, "searchCounter", SymbolTable.intType);
		searchCounter.setAdr(105);
		
		// currentItem = set1[counter] or set2[counter]
		Obj currentItem = new Obj(Obj.Var, "currentItem", SymbolTable.intType);
		currentItem.setAdr(106);
		
		// loop start for adding whole set1 to destination set
		int loop1 = Code.pc;
		
		// fetch set1[counter] and store it in currentItem
		Code.load(set1);
		Code.load(counter);
		Code.put(Code.aload);
		Code.store(currentItem);
		
		// break if set1[counter] == 0
		Code.load(currentItem);
		Code.loadConst(0);
		int exit1 = Code.pc + 1;
		Code.putFalseJump(Code.ne, 0);
		
		// store currentItem to destSet[insertPos]
		Code.load(destSet);
		Code.load(insertPos);
		Code.load(currentItem);
		Code.put(Code.astore);
		
		// increment counter, go to next item in set1
		Code.load(counter);
		Code.loadConst(1);
		Code.put(Code.add);
		Code.store(counter);
		
		// increment insertPos
		Code.load(insertPos);
		Code.loadConst(1);
		Code.put(Code.add);
		Code.store(insertPos);
		
		// loop again if counter < set1 length
		Code.load(counter);
		Code.load(set1);
		Code.put(Code.arraylength);
		Code.put(Code.jcc + Code.ne);
		Code.put2(loop1 - Code.pc + 1);
		
		// loop end for adding whole set1 to destination set
		Code.fixup(exit1);
		
		// add unique items from set2 to destination set, set counter to -1
		Code.loadConst(-1);
		Code.store(counter);
		
		// loop for adding unique items from set2 to destination set
		int loop2 = Code.pc;
		
		// increment counter for current iteration
		Code.load(counter);
		Code.loadConst(1);
		Code.put(Code.add);
		Code.store(counter);
		
		// fetch set2[counter] and store it in currentItem
		Code.load(set2);
		Code.load(counter);
		Code.put(Code.aload);
		Code.store(currentItem);
		
		// break if currentItem == 0
		Code.load(currentItem);
		Code.loadConst(0);
		int exit2 = Code.pc + 1;
		Code.putFalseJump(Code.ne, 0);
		
		//break if currentItem is already between in destSet, start searching from destSet[0]
		Code.loadConst(0);
		Code.store(searchCounter);
		
		// loop start for searching if currentItem is between destSet[0] and destSet[insertPos]
		int searchLoopStart = Code.pc;
		
		// currentItem found in destSet, skip it and go to next item from set2
		Code.load(destSet);
		Code.load(searchCounter);
		Code.put(Code.aload);
		Code.load(currentItem);
		Code.put(Code.jcc + Code.eq);
		Code.put2(loop2 - Code.pc + 1);
		
		// increment searchCounter and continue searching
		Code.load(searchCounter);
		Code.loadConst(1);
		Code.put(Code.add);
		Code.store(searchCounter);
		
		// loop again if searchCounter < insertPos
		Code.load(searchCounter);
		Code.load(insertPos);
		Code.put(Code.jcc + Code.ne);
		Code.put2(searchLoopStart - Code.pc + 1); 
		
		// loop end for searching if currentItem is already present in destSet
		
		// store currentItem to destSet[insertPos]
		Code.load(destSet);
		Code.load(insertPos);
		Code.load(currentItem);
		Code.put(Code.astore);
		
		// increment insertPos
		Code.load(insertPos);
		Code.loadConst(1);
		Code.put(Code.add);
		Code.store(insertPos);
		
		// loop again if counter < set2 length
		Code.load(counter);
		Code.load(set2);
		Code.put(Code.arraylength);
		Code.put(Code.jcc + Code.ne);
		Code.put2(loop2 - Code.pc + 1);
		
		// end loop for adding items from set2 to destination set
		Code.fixup(exit2);
	}
	
	private void map(Obj function, Obj array) {
		Obj counter = new Obj(Obj.Var, "counter", SymbolTable.intType);
		counter.setAdr(100);
		Code.loadConst(0);
		Code.store(counter);
		
		Obj currentItem = new Obj(Obj.Var, "currentItem", SymbolTable.intType);
		currentItem.setAdr(101);
		
		Obj result = new Obj(Obj.Var, "result", SymbolTable.intType);
		result.setAdr(102);
		Code.loadConst(0);
		Code.store(result);
		
		// loop start
		int loop = Code.pc;
		
		// fetch set[counter] and store it in currentItem
		Code.load(array);
		Code.load(counter);
		Code.put(Code.aload);
		Code.store(currentItem);
		
		// call function(currentItem)
		Code.load(currentItem);
		int offset = function.getAdr() - Code.pc;
		Code.put(Code.call);
		Code.put2(offset);
		
		// add returned value to result
		Code.load(result);
		Code.put(Code.add);
		Code.store(result);
		
		// increment counter
		Code.load(counter);
		Code.loadConst(1);
		Code.put(Code.add);
		Code.store(counter);
		
		// loop again if counter < arrayLength
		Code.load(counter);
		Code.load(array);
		Code.put(Code.arraylength);
		Code.put(Code.jcc + Code.ne);
		Code.put2(loop - Code.pc + 1);
		
		Code.put(Code.pop);
		Code.load(result);
	}
	
	private void generateChrFunction() {
		Obj chrObj = SymbolTable.find("chr");
		if (chrObj != SymbolTable.noObj) {
			chrObj.setAdr(Code.pc);
		}
		
		Code.put(Code.enter);
		Code.put(1);
		Code.put(1);
		
		Code.load(new Obj(Obj.Var, "int", SymbolTable.intType, 0, 1));
		
		Code.put(Code.exit);
		Code.put(Code.return_);
	}
	
	private void generateOrdFunction() {
		Obj ordObj = SymbolTable.find("ord");
		if (ordObj != SymbolTable.noObj) {
			ordObj.setAdr(Code.pc);
		}
		
		Code.put(Code.enter);
		Code.put(1);
		Code.put(1);
		
		Code.load(new Obj(Obj.Var, "char", SymbolTable.intType, 0, 1));
		
		Code.put(Code.exit);
		Code.put(Code.return_);
	}
	
	private void generateLenFunction() {
		Obj lenObj = SymbolTable.find("len");
		if (lenObj != SymbolTable.noObj) {
			lenObj.setAdr(Code.pc);
		}
		
		Code.put(Code.enter);
		Code.put(1);
		Code.put(1);
		
		Code.load(new Obj(Obj.Var, "array", SymbolTable.intType, 0, 1));
		Code.put(Code.arraylength);
		
		Code.put(Code.exit);
		Code.put(Code.return_);
	}
	
	private void generateAddFunction() {
		Obj addObj = SymbolTable.find("add");
		if (addObj != SymbolTable.noObj) {
			addObj.setAdr(Code.pc);
		}
		
		Code.put(Code.enter);
		Code.put(2);
		Code.put(2);
		
		// set address
		Obj set = new Obj(Obj.Var, "set", SymbolTable.setType, 0, 1);
		
		// value to be added to the set
		Obj value = new Obj(Obj.Var, "value", SymbolTable.intType, 1, 1);
		
		Code.load(value);
		Code.store(value);
		Code.load(set);
		Code.store(set);
		
		// counter for iterating through set, -1 at start, incrementing at the beginning of each loop iteration
		Obj counter = new Obj(Obj.Var, "counter", SymbolTable.intType);
		counter.setAdr(102);
		Code.loadConst(-1);	
		Code.store(counter);
		
		// currentItem = set[counter]
		Obj currentItem = new Obj(Obj.Var, "currentItem", SymbolTable.intType);
		currentItem.setAdr(103);
		
		// loop start
		int loop = Code.pc;
		
		// increment counter for iteration
		Code.load(counter);	
		Code.loadConst(1);	
		Code.put(Code.add);	
		Code.store(counter);
		
		// fetch set[counter] and store it in currentItem
		Code.load(set);
		Code.load(counter);	
		Code.put(Code.aload);
		Code.store(currentItem);
		
		// break if value is already in set
		Code.load(currentItem);
		Code.load(value);
		int exit = Code.pc + 1;
		Code.putFalseJump(Code.ne, 0);
		
		// if currentItem is not 0 (place is occupied) loop again
		Code.load(currentItem);
		Code.loadConst(0);
		Code.put(Code.jcc + Code.ne);
		Code.put2(loop - Code.pc + 1);
		
		// store value at first available place (set[counter])
		Code.load(set);	
		Code.load(counter);	
		Code.load(value);
		Code.put(Code.astore);
		
		// loop end
		Code.fixup(exit);
		
		Code.put(Code.exit);
		Code.put(Code.return_);
	}
	
	private void generateAddAllFunction() {
		Obj addAllObj = SymbolTable.find("addAll");
		if (addAllObj != SymbolTable.noObj) {
			addAllObj.setAdr(Code.pc);
		}
		
		Code.put(Code.enter);
		Code.put(2);
		Code.put(2);
		
		// set address
		Obj set = new Obj(Obj.Var, "set", SymbolTable.setType, 0, 1);
		
		// array address
		Obj array = new Obj(Obj.Var, "array", SymbolTable.intType, 1, 1);
		
		Code.load(array);
		Code.store(array);
		Code.load(set);
		Code.store(set);
		
		// position for inserting in the set, -1 at the start
		Obj insertPos = new Obj(Obj.Var, "insertPos", SymbolTable.intType);
		insertPos.setAdr(102);
		Code.loadConst(-1);
		Code.store(insertPos);
		
		// counter for iterating through array, -1 at start
		Obj counter = new Obj(Obj.Var, "counter", SymbolTable.intType);
		counter.setAdr(103);
		Code.loadConst(-1);
		Code.store(counter);
		
		// counter for iterating through set
		Obj searchCounter = new Obj(Obj.Var, "searchCounter", SymbolTable.intType);
		searchCounter.setAdr(104);
		
		// currentItem = array[counter]
		Obj currentItem = new Obj(Obj.Var, "currentItem", SymbolTable.intType);
		currentItem.setAdr(105);
		
		// loop start for finding first available position in the set
		int loop1 = Code.pc;
		
		// increment insertPos
		Code.load(insertPos);
		Code.loadConst(1);
		Code.put(Code.add);
		Code.store(insertPos);
		
		// fetch set[insertPos]
		Code.load(set);
		Code.load(insertPos);
		Code.put(Code.aload);
		
		// loop again if set[insertPos] != 0
		Code.loadConst(0);
		Code.put(Code.jcc + Code.ne);
		Code.put2(loop1 - Code.pc + 1);
		
		// loop end for finding first available position in the set
		
		// loop start for inserting array to the set from given position
		int loop2 = Code.pc;
		
		// increment counter for iterating through array
		Code.load(counter);
		Code.loadConst(1);
		Code.put(Code.add);
		Code.store(counter);
		
		// break if counter is equal to array length
		Code.load(counter);
		Code.load(array);
		Code.put(Code.arraylength);
		int exit = Code.pc + 1;
		Code.putFalseJump(Code.ne, 0);
		
		// fetch array[counter] and store it in currentItem
		Code.load(array);
		Code.load(counter);
		Code.put(Code.aload);
		Code.store(currentItem);
		
		// break if currentItem is already present in set, start search from set[0]
		Code.loadConst(0);
		Code.store(searchCounter);
		
		// loop start for searching if currentItem is already between set[0] and set[insertPos]
		int searchLoop = Code.pc;
		
		// if set[searchCounter] == currentItem skip currentItem and go to next item in array 
		Code.load(set);
		Code.load(searchCounter);
		Code.put(Code.aload);
		Code.load(currentItem);
		Code.put(Code.jcc + Code.eq);
		Code.put2(loop2 - Code.pc + 1);
		
		// increment searchCounter
		Code.load(searchCounter);
		Code.loadConst(1);
		Code.put(Code.add);
		Code.store(searchCounter);
		
		// loop again if searchCounter < insertPos
		Code.load(searchCounter);
		Code.load(insertPos);
		Code.put(Code.jcc + Code.lt);
		Code.put2(searchLoop - Code.pc + 1); 
		
		// loop end for searching
		
		// store currentItem to set[insertPos]
		Code.load(set);
		Code.load(insertPos);
		Code.load(currentItem);
		Code.put(Code.astore);
		
		// increment insertPos
		Code.load(insertPos);
		Code.loadConst(1);
		Code.put(Code.add);
		Code.store(insertPos);
		
		// loop again if counter < arrayLength
		Code.load(counter);
		Code.load(array);
		Code.put(Code.arraylength);
		Code.put(Code.jcc + Code.ne);
		Code.put2(loop2 - Code.pc + 1);
		
		// loop end for inserting array
		Code.fixup(exit);
		
		Code.put(Code.exit);
		Code.put(Code.return_);
	}
}
