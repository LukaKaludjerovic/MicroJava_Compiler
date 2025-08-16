// generated with ast extension for cup
// version 0.8
// 12/7/2025 0:35:58


package rs.ac.bg.etf.pp1.ast;

public class GlobalVarsList extends GlobalVarList {

    private GlobalVarList GlobalVarList;
    private GlobalVar GlobalVar;

    public GlobalVarsList (GlobalVarList GlobalVarList, GlobalVar GlobalVar) {
        this.GlobalVarList=GlobalVarList;
        if(GlobalVarList!=null) GlobalVarList.setParent(this);
        this.GlobalVar=GlobalVar;
        if(GlobalVar!=null) GlobalVar.setParent(this);
    }

    public GlobalVarList getGlobalVarList() {
        return GlobalVarList;
    }

    public void setGlobalVarList(GlobalVarList GlobalVarList) {
        this.GlobalVarList=GlobalVarList;
    }

    public GlobalVar getGlobalVar() {
        return GlobalVar;
    }

    public void setGlobalVar(GlobalVar GlobalVar) {
        this.GlobalVar=GlobalVar;
    }

    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    public void childrenAccept(Visitor visitor) {
        if(GlobalVarList!=null) GlobalVarList.accept(visitor);
        if(GlobalVar!=null) GlobalVar.accept(visitor);
    }

    public void traverseTopDown(Visitor visitor) {
        accept(visitor);
        if(GlobalVarList!=null) GlobalVarList.traverseTopDown(visitor);
        if(GlobalVar!=null) GlobalVar.traverseTopDown(visitor);
    }

    public void traverseBottomUp(Visitor visitor) {
        if(GlobalVarList!=null) GlobalVarList.traverseBottomUp(visitor);
        if(GlobalVar!=null) GlobalVar.traverseBottomUp(visitor);
        accept(visitor);
    }

    public String toString(String tab) {
        StringBuffer buffer=new StringBuffer();
        buffer.append(tab);
        buffer.append("GlobalVarsList(\n");

        if(GlobalVarList!=null)
            buffer.append(GlobalVarList.toString("  "+tab));
        else
            buffer.append(tab+"  null");
        buffer.append("\n");

        if(GlobalVar!=null)
            buffer.append(GlobalVar.toString("  "+tab));
        else
            buffer.append(tab+"  null");
        buffer.append("\n");

        buffer.append(tab);
        buffer.append(") [GlobalVarsList]");
        return buffer.toString();
    }
}
