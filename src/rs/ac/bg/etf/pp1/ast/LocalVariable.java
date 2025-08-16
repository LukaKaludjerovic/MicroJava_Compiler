// generated with ast extension for cup
// version 0.8
// 12/7/2025 0:35:58


package rs.ac.bg.etf.pp1.ast;

public class LocalVariable extends LocalVar {

    private String localVarName;
    private Brackets Brackets;

    public LocalVariable (String localVarName, Brackets Brackets) {
        this.localVarName=localVarName;
        this.Brackets=Brackets;
        if(Brackets!=null) Brackets.setParent(this);
    }

    public String getLocalVarName() {
        return localVarName;
    }

    public void setLocalVarName(String localVarName) {
        this.localVarName=localVarName;
    }

    public Brackets getBrackets() {
        return Brackets;
    }

    public void setBrackets(Brackets Brackets) {
        this.Brackets=Brackets;
    }

    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    public void childrenAccept(Visitor visitor) {
        if(Brackets!=null) Brackets.accept(visitor);
    }

    public void traverseTopDown(Visitor visitor) {
        accept(visitor);
        if(Brackets!=null) Brackets.traverseTopDown(visitor);
    }

    public void traverseBottomUp(Visitor visitor) {
        if(Brackets!=null) Brackets.traverseBottomUp(visitor);
        accept(visitor);
    }

    public String toString(String tab) {
        StringBuffer buffer=new StringBuffer();
        buffer.append(tab);
        buffer.append("LocalVariable(\n");

        buffer.append(" "+tab+localVarName);
        buffer.append("\n");

        if(Brackets!=null)
            buffer.append(Brackets.toString("  "+tab));
        else
            buffer.append(tab+"  null");
        buffer.append("\n");

        buffer.append(tab);
        buffer.append(") [LocalVariable]");
        return buffer.toString();
    }
}
