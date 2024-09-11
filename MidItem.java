import java.util.ArrayList;

public class MidItem extends Treenode{
    private String type;
    private ArrayList<Treenode> nodes;

    public MidItem(String type) {
        this.type = type;
        nodes = new ArrayList<>();
    }

    public ArrayList<Treenode> getNodes() {
        return this.nodes;
    }

    public String getType() { return this.type; }

    public void addnode(Treenode n) {
        nodes.add(n);
    }

    public Treenode getlast() {
        return nodes.get(nodes.size()-1);
    }

    public Treenode getselast() {
        return nodes.get(nodes.size()-2);
    }

    public Treenode getfirst() { return nodes.get(0); }

    @Override
    public String toString() {
        String str = "";
        if (this.type.equals("LOrExp") || this.type.equals("LAndExp") ||
            this.type.equals("EqExp") || this.type.equals("RelExp") ||
            this.type.equals("AddExp") || this.type.equals("MulExp") ) {
            for(int i=0;i<nodes.size();i++) {
                Treenode treenode = nodes.get(i);
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
        for(Treenode treenode:nodes) {
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
