import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class M_UniqueName {
    private ArrayList<M_Quaternary> old_quas;
    private IT_Uninametable unitable;
    private ArrayList<M_Quaternary> new_quas;
    private int constindex = 0;
    private int level = 0;

    public M_UniqueName(ArrayList<M_Quaternary> old_quas) {
        this.old_quas = old_quas;
        this.unitable = new IT_Uninametable();
        this.new_quas = new ArrayList<>();
        Gen_Newquas();
    }

    public ArrayList<M_Quaternary> getNew_quas() {
        return this.new_quas;
    }

    public void printmid() throws IOException {
        BufferedWriter bout = new BufferedWriter(new FileWriter("midcode_old.txt"));
        for (M_Quaternary i:new_quas) {
            bout.write(i.toString());
            bout.newLine();
        }
        bout.close();
    }

    private void Gen_Newquas() {
        for(int i=0;i<old_quas.size();i++) {
            M_Quaternary pro = old_quas.get(i);
            String op = pro.getOp();
            String operant1 = pro.getOperant1();
            String operant2 = pro.getOperant2();
            String operant3 = pro.getOperant3();
            if (op.equals("BBLOCK")) {
                this.level++;
                AddQuater(op,operant1,operant2,operant3);
            } else if (op.equals("EBLOCK")) {
                unitable.poplevel(level);
                this.level--;
                AddQuater(op,operant1,operant2,operant3);
            } else if (op.equals("CONST") || op.equals("CONSTARR1")
                    || op.equals("CONSTARR2")) {
                Addconst(op,operant1,operant2,operant3);
            } else if (op.equals("VAR") || op.equals("VARARR1")
                    || op.equals("VARARR2")) {
                Addvar(op,operant1,operant2,operant3);
            } else if (op.equals("PARAM") || op.equals("PARAMARR1")
                    || op.equals("PARAMARR2")) {
                Addparam(op,operant1,operant2,operant3);
            } else if (op.equals("FUNC")) {
                Addfunc(op,operant1,operant2,operant3);
            } else if (op.equals("ASSIGN")) {
                Addassign(op,operant1,operant2,operant3);
            } else if (op.equals("ASSIGNARR")) {
                Addassignarr(op,operant1,operant2,operant3);
            } else if (op.equals("PRINTF")) {
                Addprintf(op,operant1,operant2,operant3);
            } else if (op.equals("ASSIGNRET") || op.equals("RETEXP")
                    || op.equals("PUSH")) {
                Addop1(op,operant1,operant2,operant3);
            } else if (op.equals("ADD") || op.equals("SUB") || op.equals("MUL")
                    || op.equals("DIV") || op.equals("MOD") || op.equals("GETARR1")
                    || op.equals("GETARR2") || op.equals("GETARRADDR2")
                    || op.equals("EQL") || op.equals("NEQ") || op.equals("LSS")
                    || op.equals("GRE") || op.equals("LEQ") || op.equals("GEQ")) {
                Addop1_2_3(op,operant1,operant2,operant3);
            } else if (op.equals("NEG") || op.equals("NOT") || op.equals("GETARRADDR1")) {
                Addop1_3(op,operant1,operant2,operant3);
            } else if (op.equals("BEQ") || op.equals("BNE")) {
                Addop1_2(op,operant1,operant2,operant3);
            } else {
                AddQuater(op,operant1,operant2,operant3);
            }
        }
    }

    private boolean isnum(String str) {
        boolean ans = true;
        for (int i=0;i<str.length();i++) {
            char tmp = str.charAt(i);
            if(tmp>='0' && tmp<='9') {
                ans = true;
            } else if (i==0 && tmp=='-') {
                ans = true;
            } else {
                ans = false;
                break;
            }
        }
        return ans;
    }

    private String gennewname(String name) {
        String ret = name + "_" + constindex;
        constindex++;
        return ret;
    }

    private void AddQuater(String op,String op1,String op2,String op3) {
        M_Quaternary newqua = new M_Quaternary(op,op1,op2,op3);
        new_quas.add(newqua);
    }

    private void Addconst(String op,String op1,String op2,String op3) {
        String genname = gennewname(op2);
        int clas = 0;
        if (op.equals("CONST")) {
            clas = 1;
        } else if (op.equals("CONSTARR1")) {
            clas = 3;
        } else if (op.equals("CONSTARR2")) {
            clas = 4;
        }
        unitable.adduniquename(op2,clas,1,level,genname);
        op2 = genname;
        AddQuater(op,op1,op2,op3);
    }

    private void Addvar(String op,String op1,String op2,String op3) {
        int clas = 0;
        if (op.equals("VAR")) {
            clas = 1;
        } else if (op.equals("CONSTARR1")) {
            clas = 3;
        } else if (op.equals("VARARR2")) {
            clas = 4;
        }
        unitable.adduniquename(op2,clas,2,level,op2);
        AddQuater(op,op1,op2,op3);
    }

    private void Addparam(String op,String op1,String op2,String op3) {
        int clas = 0;
        if (op.equals("PARAM")) {
            clas = 1;
        } else if (op.equals("PARAMARR")) {
            clas = 3;
        } else if (op.equals("PARAMARR2")) {
            clas = 4;
        }
        unitable.adduniquename(op2,clas,3,level+1,op2);
        AddQuater(op,op1,op2,op3);
    }

    private void Addfunc(String op,String op1,String op2,String op3) {
        int clas = 0;
        if (op2.equals("int")) {
            clas = 1;
        } else if (op2.equals("void")) {
            clas = 2;
        }
        unitable.adduniquename(op1,clas,4,level,op1);
        AddQuater(op,op1,op2,op3);
    }

    private void Addassign(String op,String op1,String op2,String op3) {
        if(!isnum(op1)) {
            op1 = unitable.getuniname(op1,level);
        }
        if(!isnum(op2) && !op2.equals("getint()")) {
            op2 = unitable.getuniname(op2,level);
        }
        AddQuater(op,op1,op2,op3);
    }

    private void Addassignarr(String op,String op1,String op2,String op3) {
        if(!isnum(op1)) {
            op1 = unitable.getuniname(op1,level);
        }
        if(!isnum(op2)) {
            op2 = unitable.getuniname(op2,level);
        }
        if(!isnum(op3) && !op3.equals("getint()")) {
            op3 = unitable.getuniname(op3,level);
        }
        AddQuater(op,op1,op2,op3);
    }

    private void Addprintf(String op,String op1,String op2,String op3) {
        if (op1.equals("")) {
            if(!isnum(op2)) {
                op2 = unitable.getuniname(op2,level);
            }
        }
        AddQuater(op,op1,op2,op3);
    }

    private void Addop1(String op,String op1,String op2,String op3) {
        if (!isnum(op1)) {
            op1 = unitable.getuniname(op1,level);
        }
        AddQuater(op,op1,op2,op3);
    }

    private void Addop1_2_3(String op,String op1,String op2,String op3) {
        if (!isnum(op1)) {
            op1 = unitable.getuniname(op1,level);
        }
        if (!isnum(op2)) {
            op2 = unitable.getuniname(op2,level);
        }
        if (!isnum(op3)) {
            op3 = unitable.getuniname(op3,level);
        }
        AddQuater(op,op1,op2,op3);
    }

    private void Addop1_3(String op,String op1,String op2,String op3) {
        if (!isnum(op1)) {
            op1 = unitable.getuniname(op1,level);
        }
        if (!isnum(op3)) {
            op3 = unitable.getuniname(op3,level);
        }
        AddQuater(op,op1,op2,op3);
    }

    private void Addop1_2(String op,String op1,String op2,String op3) {
        if (!isnum(op1)) {
            op1 = unitable.getuniname(op1,level);
        }
        if (!isnum(op2)) {
            op2 = unitable.getuniname(op2,level);
        }
        AddQuater(op,op1,op2,op3);
    }
}
