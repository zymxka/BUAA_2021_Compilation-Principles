public class TableItem {
    private Typename type = null;
    private String name = null;

    public TableItem(Typename type, String name){
        this.type = type;
        this.name = name;
    }

    public String getName(){
        return this.name;
    }

    public Typename getType(){
        return this.type;
    }
}
