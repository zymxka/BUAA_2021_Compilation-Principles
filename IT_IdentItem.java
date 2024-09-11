import java.util.ArrayList;

public class IT_IdentItem {
    private String name;
    private int clas;//1:int 2:void 3:1d-array 4:2d-array
    private int type;//1:const 2:var 3:param 4:func
    private int level;
    private int addr;
    private int dim1len = 0;
    private int dim2len = 0;
    private int constvalue = 0;
    private String lablename = "";
    private int localvaroff = 0;
    private ArrayList<Integer> constdim1value = new ArrayList<>();
    private ArrayList<ArrayList<Integer>> constdim2value = new ArrayList<>();

    //在中间代码转换成uniquename时会用到
    private String uniquename;

    //优化的时候计算了一个变量的定义位置和最后一个使用位置
    //只对函数中定义的变量(非中间变量&非数组&非常量)和参数定义~
    private int birthtime=0;
    private int deadtime=0;

    public IT_IdentItem(String name, int clas, int type, int level, int addr) {
        this.name = name;
        this.clas = clas;
        this.type = type;
        this.level = level;
        this.addr = addr;
        this.uniquename = "";
    }

    public IT_IdentItem(String name, int clas, int type, int level, int addr,String uniquename) {
        this.name = name;
        this.clas = clas;
        this.type = type;
        this.level = level;
        this.addr = addr;
        this.uniquename = uniquename;
    }

    public void setBirthtime(int birthtime) {
        this.birthtime = birthtime;
    }

    public void setDeadtime(int deadtime) {
        this.deadtime = deadtime;
    }

    public int getBirthtime() {
        return this.birthtime;
    }

    public int getDeadtime() {
        return this.deadtime;
    }

    public String getuniname() {
        return this.uniquename;
    }

    public void setLocalvaroff(int off) {
        this.localvaroff = off;
    }

    public int getLocalvaroff() {
        return this.localvaroff;
    }

    public void setLablename(String name) {
        this.lablename = name;
    }

    public String getLablename() {
        return this.lablename;
    }

    public int getConstvalue() {
        return this.constvalue;
    }

    public int getConstvalue1d(int index) {
        if (index < this.constdim1value.size()) {
            return this.constdim1value.get(index);
        } else {
            return 0;
        }
    }

    public int getConstvalue2d(int index1,int index2) {
        if (index1 < this.constdim2value.size()) {
            ArrayList<Integer> nums = this.constdim2value.get(index1);
            if (index2 < nums.size()) {
                return this.constdim2value.get(index1).get(index2);
            } else {
                return 0;
            }
        } else {
            return 0;
        }
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

    public void setAddr(int addr) {
        this.addr = addr;
    }

    public void setDim1len(int dim1len){
        this.dim1len = dim1len;
    }

    public int getDim1len() {
        return this.dim1len;
    }

    public void setDim2len(int dim2len){
        this.dim2len = dim2len;
    }

    public int getDim2len() {
        return this.dim2len;
    }

    public void setConstvalue(int constvalue){
        this.constvalue = constvalue;
    }

    public void setConstdim1value(ArrayList<Integer> const1dvalue) {
        this.constdim1value = const1dvalue;
    }

    public void setConstdim2value(ArrayList<ArrayList<Integer>> const2dvalue) {
        this.constdim2value = const2dvalue;
    }
}
