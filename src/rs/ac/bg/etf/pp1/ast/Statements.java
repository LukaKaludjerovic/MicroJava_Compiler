// generated with ast extension for cup
// version 0.8
// 12/7/2025 0:35:58


package rs.ac.bg.etf.pp1.ast;

public class Statements extends Statement {

    private StatementsList StatementsList;

    public Statements (StatementsList StatementsList) {
        this.StatementsList=StatementsList;
        if(StatementsList!=null) StatementsList.setParent(this);
    }

    public StatementsList getStatementsList() {
        return StatementsList;
    }

    public void setStatementsList(StatementsList StatementsList) {
        this.StatementsList=StatementsList;
    }

    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    public void childrenAccept(Visitor visitor) {
        if(StatementsList!=null) StatementsList.accept(visitor);
    }

    public void traverseTopDown(Visitor visitor) {
        accept(visitor);
        if(StatementsList!=null) StatementsList.traverseTopDown(visitor);
    }

    public void traverseBottomUp(Visitor visitor) {
        if(StatementsList!=null) StatementsList.traverseBottomUp(visitor);
        accept(visitor);
    }

    public String toString(String tab) {
        StringBuffer buffer=new StringBuffer();
        buffer.append(tab);
        buffer.append("Statements(\n");

        if(StatementsList!=null)
            buffer.append(StatementsList.toString("  "+tab));
        else
            buffer.append(tab+"  null");
        buffer.append("\n");

        buffer.append(tab);
        buffer.append(") [Statements]");
        return buffer.toString();
    }
}
