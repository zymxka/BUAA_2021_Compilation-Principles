import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;

//计算所有非中间变量的寿命(起点index&终点index)
public class Optim_Varlife {
    private ArrayList<M_Quaternary> codes;
    private int index;
    private int level;
    //表中只包含每个函数的非中间变量以及参数
    private IT_IdentTable funcvarlife;
    private HashMap<String,IT_IdentTable> funcvartable;
    private int inwhile;
    private HashSet<String> whilevar;
    private Stack<Integer> inwhilestack;
    private Stack<HashSet<String>> whilevarstack;

    public Optim_Varlife(ArrayList<M_Quaternary> codes) {
        this.codes = codes;
        this.index = 0;
        this.level = 0;
        this.funcvarlife = new IT_IdentTable();
        this.funcvartable = new HashMap<>();
        this.inwhile = 0;
        this.whilevar = new HashSet<>();
        this.inwhilestack = new Stack<>();
        this.whilevarstack = new Stack<>();
        getVarlife();
    }

    public HashMap<String,IT_IdentTable> getFuncvartable() {
        return this.funcvartable;
    }

    public IT_IdentTable getFuncvarlife() {
        return this.funcvarlife;
    }

    private void getVarlife() {
        for(;!codes.get(index).getOp().equals("BFuncDef") &&
                !codes.get(index).getOp().equals("BMainFunc");index++) {
            //System.out.println(index);
        }
        for(;!codes.get(index).getOp().equals("BMainFunc");index++) {
            String funcname = codes.get(index+1).getOperant1();
            //System.out.println(funcname);
            this.funcvarlife = new IT_IdentTable();
            get_FuncVarlife("EFuncDef");
            funcvartable.put(funcname,funcvarlife);
        }

        String funcname = codes.get(index+1).getOperant1();
        this.funcvarlife = new IT_IdentTable();
        get_FuncVarlife("EMainFunc");
        funcvartable.put(funcname,funcvarlife);
    }

    private void get_FuncVarlife(String end) {
        for(;!codes.get(index).getOp().equals(end);index++) {
            M_Quaternary tmpq = codes.get(index);
            String type = codes.get(index).getOp();
            if (type.equals("BConstDecl")) {
                for(;!codes.get(index).getOp().equals("EConstDecl");index++) {
                    String tmp = codes.get(index).getOp();
                    if (tmp.equals("CONST")||tmp.equals("CONSTARR1")
                            ||tmp.equals("CONSTARR2")) {
                        Add_BirthtimeConst(tmp);
                    }
                }
            } else if (type.equals("BBLOCK")) {
                this.level++;
            } else if (type.equals("EBLOCK")) {
                funcvarlife.popnoconst(level);
                this.level--;
            } else if (type.equals("VAR")) {
                Add_BirthtimeVar();
            } else if (type.equals("VARARR1")
                    || type.equals("VARARR2")) {
                Add_BirthtimeArr(type);
            } else if (type.equals("PARAM") ||
                    type.equals("PARAMARR1") || type.equals("PARAMARR2")) {
                Add_BirthtimeParam(type);
            } else if (type.equals("ASSIGN")) {
                Update_Deatime(tmpq.getOperant1());
                Update_Deatime(tmpq.getOperant2());
            } else if (type.equals("ASSIGNARR")) {
                Update_Deatime(tmpq.getOperant1());
                Update_Deatime(tmpq.getOperant2());
                Update_Deatime(tmpq.getOperant3());
            } else if (type.equals("ASSIGNRET")||
                    type.equals("RETEXP")||type.equals("PUSH")) {
                Update_Deatime(tmpq.getOperant1());
            } else if (type.equals("PRINTF")){
                Update_Deatime(tmpq.getOperant2());
            } else if (type.equals("ADD") || type.equals("SUB")
                    ||type.equals("MUL")||type.equals("DIV")||type.equals("MOD")) {
                Update_Deatime(tmpq.getOperant1());
                Update_Deatime(tmpq.getOperant2());
                Update_Deatime(tmpq.getOperant3());
            } else if (type.equals("NEG") || type.equals("NOT")) {
                Update_Deatime(tmpq.getOperant1());
                Update_Deatime(tmpq.getOperant3());
            } else if (type.equals("GETARR1")
                    || type.equals("GETARR2")) {
                Update_Deatime(tmpq.getOperant1());
                Update_Deatime(tmpq.getOperant2());
                Update_Deatime(tmpq.getOperant3());
            } else if (type.equals("GETARRADDR1")) {
                Update_Deatime(tmpq.getOperant1());
                Update_Deatime(tmpq.getOperant3());
            } else if (type.equals("GETARRADDR2")) {
                Update_Deatime(tmpq.getOperant1());
                Update_Deatime(tmpq.getOperant2());
                Update_Deatime(tmpq.getOperant3());
            } else if (type.equals("EQL") || type.equals("NEQ") || type.equals("LSS")
                    ||type.equals("GRE") || type.equals("LEQ") || type.equals("GEQ")) {
                Update_Deatime(tmpq.getOperant1());
                Update_Deatime(tmpq.getOperant2());
                Update_Deatime(tmpq.getOperant3());
            } else if (type.equals("BLT") || type.equals("BLE") || type.equals("BGT")
                    ||type.equals("BGE") || type.equals("BNE") || type.equals("BEQ")) {
                Update_Deatime(tmpq.getOperant1());
                Update_Deatime(tmpq.getOperant2());
            } else if (type.equals("BWHILE")) {
                //System.out.println("begin");
                if (this.inwhile != 0) {
                    this.inwhilestack.push(this.inwhile);
                    this.whilevarstack.push(this.whilevar);
                    this.whilevar = new HashSet<>();
                }
                this.inwhile = index;
            } else if (type.equals("EWHILE")) {
                //System.out.println("end");
                Update_WhileDeadtime();
                if (!inwhilestack.empty()) {
                    this.inwhile = this.inwhilestack.pop();
                    this.whilevar = this.whilevarstack.pop();
                } else {
                    this.inwhile = 0;
                    this.whilevar = new HashSet<>();
                }
            }
        }
    }

    private void Update_WhileDeadtime() {
        for (String name:this.whilevar) {
            IT_IdentItem var = this.funcvarlife.getvar(name,level+1);
            var.setDeadtime(index);
        }
    }

    private void Add_BirthtimeVar() {
        //additem并且设置birthtime
        String varname = codes.get(index).getOperant2();
        if (varname.charAt(0) != '@') {
            funcvarlife.additem(varname,1,2,level);
            IT_IdentItem var = funcvarlife.getvar(varname,level);
            var.setBirthtime(index);
        }
    }

    private void Add_BirthtimeParam(String type) {
        //additem并且设置birthtime
        String varname = codes.get(index).getOperant2();
        if (type.equals("PARAM")) {
            funcvarlife.additem(varname,1,3,level+1);
        }else if (type.equals("PARAMARR1")) {
            funcvarlife.additem(varname,3,3,level+1);
        } else {
            funcvarlife.additem(varname,4,3,level+1);
        }
        IT_IdentItem var = funcvarlife.getvar(varname,level+1);
        var.setBirthtime(index);
    }

    private void Add_BirthtimeArr(String type) {
        //additem并且设置birthtime
        //数组元素没有deadtime
        String arrname = codes.get(index).getOperant2();
        if (type.equals("VARARR1")) {
            funcvarlife.additem(arrname,3,2,level);
        } else {
            funcvarlife.additem(arrname,4,2,level);
        }
        IT_IdentItem var = funcvarlife.getvar(arrname,level);
        var.setBirthtime(index);
    }

    private void Add_BirthtimeConst(String type) {
        //additem并且设置birthtime
        //常量元素没有deadtime
        String constname = codes.get(index).getOperant2();
        if (type.equals("CONSTARR1")) {
            funcvarlife.additem(constname,3,1,level);
        } else if (type.equals("CONSTARR2")) {
            funcvarlife.additem(constname,4,1,level);
        } else {
            funcvarlife.additem(constname,1,1,level);
        }
        IT_IdentItem var = funcvarlife.getvar(constname,level);
        var.setBirthtime(index);
    }

    private void Update_Deatime(String name) {
        if (name.equals("")) {
            return;
        }
        //System.out.println(name);
        //注意不要给数组元素弄deadtime!!!!
        IT_IdentItem var = funcvarlife.getvar(name,level);
        if (var != null) {
            int type = var.getType();
            if (type != 1) {
                int clas = var.getClas();
                if (type==3 || (type==2 && clas==1)) {
                    var.setDeadtime(index);
                    if (this.inwhile != 0 && this.inwhile> var.getBirthtime() && !this.whilevar.contains(name)) {
                        this.whilevar.add(name);
                    }
                }
            }
        }
    }

}
