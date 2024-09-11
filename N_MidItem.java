import java.util.ArrayList;

public class N_MidItem extends N_Treenode {
    private String type;
    private ArrayList<N_Treenode> nodes;

    public N_MidItem(String type) {
        this.type = type;
        nodes = new ArrayList<>();
    }

    public ArrayList<N_Treenode> getNodes() {
        return this.nodes;
    }

    public String getType() { return this.type; }

    public void addnode(N_Treenode n) {
        nodes.add(n);
    }

    public N_Treenode getlast() {
        return nodes.get(nodes.size()-1);
    }

    public N_Treenode getselast() {
        return nodes.get(nodes.size()-2);
    }

    public N_Treenode getfirst() { return nodes.get(0); }

    @Override
    public String toString() {
        String str = "";
        if (this.type.equals("LOrExp") || this.type.equals("LAndExp") ||
            this.type.equals("EqExp") || this.type.equals("RelExp") ||
            this.type.equals("AddExp") || this.type.equals("MulExp") ) {
            for(int i=0;i<nodes.size();i++) {
                N_Treenode treenode = nodes.get(i);
                str += treenode.toString()+"\n";
                if (i%2==0){
                    str += "<" + type + ">\n";
                }
            }
            if (str.charAt(str.length()-1)=='\n') {
                str = str.substring(0,str.length()-1);
            }
            return str;
        }
        for(N_Treenode treenode:nodes) {
            str += treenode.toString()+"\n";
        }
        if (!this.type.equals("BlockItem")) {
            str += "<" + type + ">";
        }
        if (str.charAt(str.length()-1)=='\n') {
            str = str.substring(0,str.length()-1);
        }
        return str;
    }
}
