import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

public class Optim_Midcodes {
    private ArrayList<M_Quaternary> old;
    private ArrayList<M_Quaternary> optim;
    private HashSet<String> delmidvar;
    private int index;

    public Optim_Midcodes(ArrayList<M_Quaternary> old) {
        this.old = old;
        this.optim = new ArrayList<>();
        this.delmidvar = new HashSet<>();
        this.index = 0;
        Optim();
    }

    public void printmid() throws IOException {
        BufferedWriter bout = new BufferedWriter(new FileWriter("midcode.txt"));
        for (M_Quaternary i:old) {
            bout.write(i.toString());
            bout.newLine();
        }
        bout.close();
    }

    public ArrayList<M_Quaternary> getOptim() {
        return this.old;
    }

    private int StrToInt(String exp) {
        int ans = 0;
        if (exp.charAt(0)!='-') {
            for (int i=0;i<exp.length();i++){
                int x = exp.charAt(i) - '0';
                ans = ans * 10 + x;
            }
        } else {
            for (int i=1;i<exp.length();i++){
                int x = exp.charAt(i) - '0';
                ans = ans * 10 + x;
            }
            ans = -ans;
        }
        return ans;
    }

    private boolean isnum(String str) {
        boolean ans = true;
        for (int i=0;i<str.length();i++) {
            char tmp = str.charAt(i);
            if(tmp>='0' && tmp<='9') {
                ans = true;
            } else if (tmp == '-' && i == 0) {
                ans = true;
            } else {
                ans = false;
                break;
            }
        }
        return ans;
    }

    private void Optim() {
        Optim_CondExp_Branch();
        Optim_LAndExp_Jump();
        Optim_LOrExp_Jump();
        Optim_Cond_Jump();
        Optim_Cal_Const();
        Optim_CondExp_Del();
    }

    private void Optim_CondExp_Branch() {
        this.index = 0;
        for(;index<old.size()-1;index++) {
            M_Quaternary q = old.get(index);
            M_Quaternary nextq = old.get(index+1);
            if ((q.getOp().equals("GEQ") || q.getOp().equals("GRE") || q.getOp().equals("NEQ") ||
                    q.getOp().equals("LEQ") || q.getOp().equals("LSS") || q.getOp().equals("EQL")) &&
                nextq.getOp().equals("BEQ") && q.getOperant3().equals(nextq.getOperant1())) {
                delmidvar.add(q.getOperant3());
                if (q.getOp().equals("GEQ")) {
                    M_Quaternary o = new M_Quaternary("BLT",q.getOperant1(), q.getOperant2(), nextq.getOperant3());
                    optim.add(o);
                } else if (q.getOp().equals("GRE")) {
                    M_Quaternary o = new M_Quaternary("BLE",q.getOperant1(), q.getOperant2(), nextq.getOperant3());
                    optim.add(o);
                } else if (q.getOp().equals("LEQ")) {
                    M_Quaternary o = new M_Quaternary("BGT",q.getOperant1(), q.getOperant2(), nextq.getOperant3());
                    optim.add(o);
                } else if (q.getOp().equals("LSS")) {
                    M_Quaternary o = new M_Quaternary("BGE",q.getOperant1(), q.getOperant2(), nextq.getOperant3());
                    optim.add(o);
                } else if (q.getOp().equals("EQL")) {
                    M_Quaternary o = new M_Quaternary("BNE",q.getOperant1(), q.getOperant2(), nextq.getOperant3());
                    optim.add(o);
                } else if (q.getOp().equals("NEQ")) {
                    M_Quaternary o = new M_Quaternary("BEQ",q.getOperant1(), q.getOperant2(), nextq.getOperant3());
                    optim.add(o);
                }
                index++;
            } else {
                optim.add(q);
            }
        }
        optim.add(old.get(old.size()-1));
        this.old = this.optim;
        this.optim = new ArrayList<>();
    }

    private void Optim_LOrExp_Jump() {
        this.index = 0;
        for(;index<old.size();index++) {
            M_Quaternary q = old.get(index);
            if (q.getOp().equals("BLOrExpDef")) {
                while (!old.get(index).getOp().equals("ELOrExpDef")) {
                    index++;
                }
            } else if (q.getOp().equals("BLOrExpTail")) {
                index++;
                String lableend = old.get(index).getOp();
                index++;
                String lableadd = old.get(index).getOp();
                M_Quaternary jend = new M_Quaternary("JUMP",lableend,"","");
                M_Quaternary addl = new M_Quaternary("ADDLABLE",lableadd,"","");
                optim.add(jend);
                optim.add(addl);
                while (!old.get(index).getOp().equals("ELOrExpTail")) {
                    if(old.get(index).getOp().equals("EMidCond")) {
                        optim.add(old.get(index));
                    }
                    index++;
                }
            } else {
                this.optim.add(q);
            }
        }
        this.old = this.optim;
        this.optim = new ArrayList<>();
    }

    private void Optim_LAndExp_Jump() {
        this.index = 0;
        for(;index<old.size();index++) {
            M_Quaternary q = old.get(index);
            if(q.getOp().equals("BMidLAndExp")) {
                Reduce_LAndExp_Jump();
            } else {
                optim.add(q);
            }
        }
        this.old = this.optim;
        this.optim = new ArrayList<>();
    }

    private void Reduce_LAndExp_Jump() {
        index++;
        String finallable = old.get(index).getOp();
        index++;
        String nextlable = old.get(index).getOp();
        index++;
        delmidvar.add(old.get(index).getOperant2());
        for(;!old.get(index).getOp().equals("EMidLAndBody");index++) {
            M_Quaternary q = old.get(index);
            if (q.getOp().equals("BLT") || q.getOp().equals("BLE") || q.getOp().equals("BGT")
                || q.getOp().equals("BGE") || q.getOp().equals("BNE") || q.getOp().equals("BEQ")) {
                q.setOperant3(nextlable);
            }
            optim.add(q);
        }
        M_Quaternary jfinal = new M_Quaternary("JUMP",finallable,"","");
        optim.add(jfinal);
        M_Quaternary jnext = new M_Quaternary("ADDLABLE",nextlable,"","");
        optim.add(jnext);
        while(!old.get(index).getOp().equals("EMidLAndExp")) {
            index++;
        }
    }

    private void Optim_Cond_Jump() {
        this.index = 0;
        for(;index<old.size();index++) {
            M_Quaternary q = this.old.get(index);
            if(q.getOp().equals("BMidCond")) {
                int start = index;
                while(!old.get(start).getOp().equals("EMidCond")) {
                    start++;
                }
                //System.out.println(old.get(start-4).toString());
                String replable = old.get(start-2).getOperant1();
                String dellable = old.get(start-3).getOperant1();
                old.remove(start-4);
                while (!old.get(index).getOp().equals("EMidCond")) {
                    M_Quaternary tmpq = old.get(index);
                    if (tmpq.getOperant3().equals(dellable)) {
                        tmpq.setOperant3(replable);
                        optim.add(tmpq);
                    } else if (tmpq.getOp().equals("ADDLABLE") &&
                            tmpq.getOperant1().equals(dellable)) {

                    } else if (tmpq.getOp().equals("JUMP")
                            && tmpq.getOperant1().equals(replable)) {

                    } else {
                        optim.add(tmpq);
                    }
                    index++;
                }
                optim.add(old.get(index));
            } else {
                this.optim.add(q);
            }
        }
        this.old = this.optim;
        this.optim = new ArrayList<>();
    }

    private void Optim_CondExp_Del() {
        this.index = 0;
        for(;index<old.size();index++){
            M_Quaternary q = this.old.get(index);
            if (q.getOp().equals("VAR") && delmidvar.contains(q.getOperant2())) {
            } else {
                optim.add(q);
            }
        }
        this.old = this.optim;
        this.optim = new ArrayList<>();
    }

    private void Optim_Cal_Const() {
        this.index = 0;
        for(;index<old.size();index++) {
            M_Quaternary q = this.old.get(index);
            String op = q.getOp();
            if (op.equals("MUL")||op.equals("DIV")||op.equals("ADD")
                    ||op.equals("SUB")||op.equals("MOD")) {
                if (isnum(q.getOperant2()) && isnum(q.getOperant1())) {
                    String target = q.getOperant3();
                    int op1 = StrToInt(q.getOperant1());
                    int op2 = StrToInt(q.getOperant2());
                    int ans = 0;
                    if (op.equals("MUL")) {
                        ans = op1*op2;
                    } else if(op.equals("DIV")) {
                        ans = op1/op2;
                    } else if(op.equals("ADD")) {
                        ans = op1+op2;
                    } else if(op.equals("SUB")) {
                        ans = op1-op2;
                    } else if(op.equals("MOD")) {
                        ans = op1%op2;
                    }
                    if (target.charAt(0)=='@') {
                        //System.out.println(target);
                        delmidvar.add(target);
                        int start = index+1;
                        while (true) {
                            M_Quaternary tmpq = old.get(start);
                            if (start==old.size()-1) {
                                break;
                            }
                            if (tmpq.getOperant1().equals(target)) {
                                tmpq.setOperant1(ans+"");
                                break;
                            } else if (tmpq.getOperant2().equals(target)) {
                                tmpq.setOperant2(ans+"");
                                break;
                            } else if (tmpq.getOperant3().equals(target)) {
                                tmpq.setOperant3(ans+"");
                            }
                            start++;
                        }
                    } else {
                        M_Quaternary newq = new M_Quaternary("ASSIGN",target,ans+"","");
                        optim.add(newq);
                    }
                } else {
                    optim.add(q);
                }
            } else {
                optim.add(q);
            }
        }
        this.old = this.optim;
        this.optim = new ArrayList<>();
    }
}
