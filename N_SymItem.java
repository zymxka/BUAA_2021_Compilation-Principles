public class N_SymItem extends N_Treenode {
    private T_Typename type;
    private String name = null;
    private int line = 0;

    public N_SymItem(T_Typename type, String name, int line){
        this.type = type;
        this.name = name;
        this.line = line;
    }

    public String getName(){
        return this.name;
    }

    public T_Typename getType(){
        return this.type;
    }

    public int getLine() {
        return this.line;
    }

    public int getformatc() {
        int ans=0;
        for(int i=0;i<name.length();i++){
            if(name.charAt(i)=='%') {
                ans++;
            }
        }
        return ans;
    }

    @Override
    public String toString() {
        return type.toString()+" "+name;
    }
}
