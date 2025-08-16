// generated with ast extension for cup
// version 0.8
// 12/7/2025 0:35:58


package rs.ac.bg.etf.pp1.ast;

public class LocalVarsDeclList extends LocalVarDeclList {

    private LocalVarDeclList LocalVarDeclList;
    private LocalVarDecl LocalVarDecl;

    public LocalVarsDeclList (LocalVarDeclList LocalVarDeclList, LocalVarDecl LocalVarDecl) {
        this.LocalVarDeclList=LocalVarDeclList;
        if(LocalVarDeclList!=null) LocalVarDeclList.setParent(this);
        this.LocalVarDecl=LocalVarDecl;
        if(LocalVarDecl!=null) LocalVarDecl.setParent(this);
    }

    public LocalVarDeclList getLocalVarDeclList() {
        return LocalVarDeclList;
    }

    public void setLocalVarDeclList(LocalVarDeclList LocalVarDeclList) {
        this.LocalVarDeclList=LocalVarDeclList;
    }

    public LocalVarDecl getLocalVarDecl() {
        return LocalVarDecl;
    }

    public void setLocalVarDecl(LocalVarDecl LocalVarDecl) {
        this.LocalVarDecl=LocalVarDecl;
    }

    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    public void childrenAccept(Visitor visitor) {
        if(LocalVarDeclList!=null) LocalVarDeclList.accept(visitor);
        if(LocalVarDecl!=null) LocalVarDecl.accept(visitor);
    }

    public void traverseTopDown(Visitor visitor) {
        accept(visitor);
        if(LocalVarDeclList!=null) LocalVarDeclList.traverseTopDown(visitor);
        if(LocalVarDecl!=null) LocalVarDecl.traverseTopDown(visitor);
    }

    public void traverseBottomUp(Visitor visitor) {
        if(LocalVarDeclList!=null) LocalVarDeclList.traverseBottomUp(visitor);
        if(LocalVarDecl!=null) LocalVarDecl.traverseBottomUp(visitor);
        accept(visitor);
    }

    public String toString(String tab) {
        StringBuffer buffer=new StringBuffer();
        buffer.append(tab);
        buffer.append("LocalVarsDeclList(\n");

        if(LocalVarDeclList!=null)
            buffer.append(LocalVarDeclList.toString("  "+tab));
        else
            buffer.append(tab+"  null");
        buffer.append("\n");

        if(LocalVarDecl!=null)
            buffer.append(LocalVarDecl.toString("  "+tab));
        else
            buffer.append(tab+"  null");
        buffer.append("\n");

        buffer.append(tab);
        buffer.append(") [LocalVarsDeclList]");
        return buffer.toString();
    }
}
