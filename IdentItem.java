public class IdentItem {
    private String name;
    private int clas;//1:int 2:void 3:1d-array 4:2d-array
    private int type;//1:const 2:var 3:param 4:func
    private int level;
    private int addr;

    public IdentItem(String name,int clas,int type,int level,int addr) {
        this.name = name;
        this.clas = clas;
        this.type = type;
        this.level = level;
        this.addr = addr;
    }

    public String getName() {
        return this.name;
    }

    public int getClas() {
        return this.clas;
    }

    public int getType() {
        return this.type;
    }

    public int getLevel() {
        return this.level;
    }

    public int getAddr() {
        return this.addr;
    }

}
