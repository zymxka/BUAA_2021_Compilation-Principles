import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class M_Midcodes {
    private ArrayList<M_Quaternary> quas;
    private ArrayList<String> mymid;
    private IT_IdentTable table;
    private int var_index = 0;

    public M_Midcodes(IT_IdentTable table) {
        this.quas = new ArrayList<>();
        this.mymid = new ArrayList<>();
        this.table = table;
    }

    public ArrayList<String> getMymid() {
        return this.mymid;
    }

    public ArrayList<M_Quaternary> getQuas() {
        return this.quas;
    }

    private String gen_var() {
        String name = "@__midvar" + var_index;
        var_index++;
        return name;
    }

    public void printmid() throws IOException {
        BufferedWriter bout = new BufferedWriter(new FileWriter("midcode_old.txt"));
        for (String i : mymid) {
            bout.write(i);
            bout.newLine();
        }
        bout.close();
    }

    private void AssArrEle(String op, String operant1, String operant2, String operant3) {
        M_Quaternary tmparri1d = new M_Quaternary(op,operant1,operant2,operant3);
        quas.add(tmparri1d);
        String arri1dprint = op + " " + operant1 + "[" + operant2 + "]" + "=" + operant3;
        mymid.add(arri1dprint);
    }

    private void AddArr(String op,String operant1,String operant2,String operant3) {
        M_Quaternary var = new M_Quaternary(op,operant1,operant2,operant3);
        quas.add(var);
        String var_print = op + " " + operant1 + " " + operant2 + "[" + operant3 + "]";
        mymid.add(var_print);
    }

    public void AddMark(String lable) {
        M_Quaternary lab = new M_Quaternary(lable,"","","");
        quas.add(lab);
        mymid.add(lable);
    }

    //const int
    //const int x = a
    public void AddConst(String name,int value) {
        String op = "CONST";
        String operant1 = "int";
        String operant2 = name;
        String operant3 = value+"";
        //System.out.println(name);
        M_Quaternary tmpconst = new M_Quaternary(op,operant1,operant2,operant3);
        quas.add(tmpconst);

        //Print
        String toprint = op + " " + operant1 + " " + operant2 + "=" + operant3;
        mymid.add(toprint);
    }

    //const int[]
    //constarr1 int x[n]
    //x[0] = a1....x[n-1]=an
    public void AddConst1d(String name,ArrayList<Integer> value1d,int len1) {
        String op = "CONSTARR1";
        String operant1 = "int";
        String operant2 = name;
        String operant3 = len1 + "";
        AddArr(op,operant1,operant2,operant3);

        int size = value1d.size();
        for (int i=0;i<len1;i++) {
            op = "ASSIGNARR";
            operant1 = name;
            operant2 = i+"";
            if (i < size) {
                operant3 = value1d.get(i)+"";
            } else {
                operant3 = "0";
            }
            AssArrEle(op,operant1,operant2,operant3);
        }
    }

    //const int[][]
    //constarr2 int x[m][n]
    //x[0][1]=a11...x[m-1][n-1]=amn
    public void AddConst2d(String name,ArrayList<ArrayList<Integer>> value2d,
                           int dim2len,int dim1len) {
        String op = "CONSTARR2";
        String operant1 = "int";
        String operant2 = name;
        String operant3 = dim2len*dim1len + "";
        AddArr(op,operant1,operant2,operant3);

        int dim2size = value2d.size();
        for (int i=0;i<dim2len;i++) {
            if (i < dim2size) {
                ArrayList<Integer> tmpvalue1d = value2d.get(i);
                int dim1size = tmpvalue1d.size();
                for (int j=0;j<dim1len;j++) {
                    op = "ASSIGNARR";
                    operant1 = name;
                    int idx = i * dim1len + j;
                    operant2 = idx + "";
                    if (j < dim1size) {
                        operant3 = tmpvalue1d.get(j)+"";
                    } else {
                        operant3 = "0";
                    }
                    AssArrEle(op,operant1,operant2,operant3);
                }
            } else {
                for (int j=0;j<dim1len;j++) {
                    op = "ASSIGNARR";
                    operant1 = name;
                    int idx = i * dim1len + j;
                    operant2 = idx+"";
                    operant3 = "0";
                    AssArrEle(op,operant1,operant2,operant3);
                }
            }
        }
    }

    //int
    //int a
    public void Addint(String name) {
        String op = "VAR";
        String operant1 = "int";
        String operant2 = name;
        String operant3 = "";
        M_Quaternary var = new M_Quaternary(op,operant1,operant2,operant3);
        quas.add(var);
        String var_print = op + " " + operant1 + " " + operant2;
        mymid.add(var_print);
    }

    //int[]
    public void Addint1d(String name,int len1) {
        String op = "VARARR1";
        String operant1 = "int";
        String operant2 = name;
        String operant3 = len1+"";
        AddArr(op,operant1,operant2,operant3);
    }

    //int[][]
    public void Addint2d(String name,int dim2len,int dim1len) {
        String op = "VARARR2";
        String operant1 = "int";
        String operant2 = name;
        String operant3 = dim2len*dim1len + "";
        AddArr(op,operant1,operant2,operant3);
    }

    public void Addlocalval(String name,String num) {
        String op = "VAR";
        String operant1 = "int";
        String operant2 = name;
        String operant3 = "";
        M_Quaternary var = new M_Quaternary(op,operant1,operant2,operant3);
        quas.add(var);
        String var_print = op + " " + operant1 + " " + operant2;
        mymid.add(var_print);
        op = "ASSIGN";
        operant1 = name;
        operant2 = num+"";
        var = new M_Quaternary(op,operant1,operant2,"");
        quas.add(var);
        var_print = op + " " + operant1 + " = " + operant2;
        mymid.add(var_print);
    }

    public void Addlocalval1d(String name,ArrayList<String> exp1d,int len1) {
        String op = "VARARR1";
        String operant1 = "int";
        String operant2 = name;
        String operant3 = len1+"";
        AddArr(op,operant1,operant2,operant3);
        int size = exp1d.size();
        for (int i=0;i<len1;i++) {
            op = "ASSIGNARR";
            operant1 = name;
            operant2 = i+"";
            if (i < size) {
                operant3 = exp1d.get(i)+"";
            } else {
                operant3 = "0";
            }
            AssArrEle(op,operant1,operant2,operant3);
        }
    }

    public void Addlocalval2d(String name,ArrayList<ArrayList<String>> exp2d,int dim2len,int dim1len) {
        String op = "VARARR2";
        String operant1 = "int";
        String operant2 = name;
        String operant3 = dim2len*dim1len + "";
        AddArr(op,operant1,operant2,operant3);
        int dim2size = exp2d.size();
        for (int i=0;i<dim2len;i++) {
            if (i < dim2size) {
                ArrayList<String> tmpvalue1d = exp2d.get(i);
                int dim1size = tmpvalue1d.size();
                for (int j=0;j<dim1len;j++) {
                    op = "ASSIGNARR";
                    operant1 = name;
                    int idx = i * dim1len + j;
                    operant2 = idx + "";
                    if (j < dim1size) {
                        operant3 = tmpvalue1d.get(j)+"";
                    } else {
                        operant3 = "0";
                    }
                    AssArrEle(op,operant1,operant2,operant3);
                }
            } else {
                for (int j=0;j<dim1len;j++) {
                    op = "ASSIGNARR";
                    operant1 = name;
                    int idx = i * dim1len + j;
                    operant2 = idx+"";
                    operant3 = "0";
                    AssArrEle(op,operant1,operant2,operant3);
                }
            }
        }
    }

    //int =
    public void Addintval(String name,int num) {
        String op = "VAR";
        String operant1 = "int";
        String operant2 = name;
        String operant3 = "";
        M_Quaternary var = new M_Quaternary(op,operant1,operant2,operant3);
        quas.add(var);
        String var_print = op + " " + operant1 + " " + operant2;
        mymid.add(var_print);
        op = "ASSIGN";
        operant1 = name;
        operant2 = num+"";
        var = new M_Quaternary(op,operant1,operant2,"");
        quas.add(var);
        var_print = op + " " + operant1 + " = " + operant2;
        mymid.add(var_print);
    }

    //int[] =
    public void Addintval1d(String name,ArrayList<Integer> exp1d,int len1) {
        String op = "VARARR1";
        String operant1 = "int";
        String operant2 = name;
        String operant3 = len1+"";
        AddArr(op,operant1,operant2,operant3);
        int size = exp1d.size();
        for (int i=0;i<len1;i++) {
            op = "ASSIGNARR";
            operant1 = name;
            operant2 = i+"";
            if (i < size) {
                operant3 = exp1d.get(i)+"";
            } else {
                operant3 = "0";
            }
            AssArrEle(op,operant1,operant2,operant3);
        }
    }

    //int[][] =
    public void Addinival2d(String name,ArrayList<ArrayList<Integer>> exp2d,int dim2len,int dim1len) {
        String op = "VARARR2";
        String operant1 = "int";
        String operant2 = name;
        String operant3 = dim2len*dim1len + "";
        AddArr(op,operant1,operant2,operant3);
        int dim2size = exp2d.size();
        for (int i=0;i<dim2len;i++) {
            if (i < dim2size) {
                ArrayList<Integer> tmpvalue1d = exp2d.get(i);
                int dim1size = tmpvalue1d.size();
                for (int j=0;j<dim1len;j++) {
                    op = "ASSIGNARR";
                    operant1 = name;
                    int idx = i * dim1len + j;
                    operant2 = idx + "";
                    if (j < dim1size) {
                        operant3 = tmpvalue1d.get(j)+"";
                    } else {
                        operant3 = "0";
                    }
                    AssArrEle(op,operant1,operant2,operant3);
                }
            } else {
                for (int j=0;j<dim1len;j++) {
                    op = "ASSIGNARR";
                    operant1 = name;
                    int idx = i * dim1len + j;
                    operant2 = idx+"";
                    operant3 = "0";
                    AssArrEle(op,operant1,operant2,operant3);
                }
            }
        }
    }

    //funcname
    public void AddFuncName(String name,String type) {
        String op = "FUNC";
        String operant1 = name;
        String operant2 = type;
        M_Quaternary tmpfunc = new M_Quaternary(op,operant1,operant2,"");
        quas.add(tmpfunc);
        String func_print = op + " " + operant2 + " " + operant1 + " ()";
        mymid.add(func_print);
    }

    //funcpara0d
    public void AddFuncPara0d(String name) {
        String op = "PARAM";
        String operant1 = "int";
        String operant2 = name;
        M_Quaternary tmppara = new M_Quaternary(op,operant1,operant2,"");
        quas.add(tmppara);
        String para_print = op + " " + operant1 + " " + operant2;
        mymid.add(para_print);
    }

    //funcpara1d
    public void AddFuncPara1d(String name) {
        String op = "PARAMARR1";
        String operant1 = "int";
        String operant2 = name;
        M_Quaternary tmppara = new M_Quaternary(op,operant1,operant2,"");
        quas.add(tmppara);
        String para_print = op + " " + operant1 + " " + operant2 + "[]";
        mymid.add(para_print);
    }

    //funcpara2d
    public void AddFuncPara2d(String name,int len) {
        String op = "PARAMARR2";
        String operant1 = "int";
        String operant2 = name;
        String operant3 = len+"";
        M_Quaternary tmppara = new M_Quaternary(op,operant1,operant2,operant3);
        quas.add(tmppara);
        String para_print = op + " " + operant1 + " " + operant2 + "[][" + operant3 +"]";
        mymid.add(para_print);
    }

    //printf
    public void AddPrintf(String fstr,ArrayList<String> exps) {
        String op = "PRINTF";
        int lastindex = 0;
        int expindex = 0;
        String opstr = fstr.substring(1,fstr.length()-1);
        for (int i=0;i<opstr.length();i++) {
            char tmp = opstr.charAt(i);
            if (tmp == '%') {
                if (i != lastindex) {
                    String str = opstr.substring(lastindex,i);
                    M_Quaternary tmpprint = new M_Quaternary(op,str,"","");
                    quas.add(tmpprint);
                    String print_str = op + " " + str;
                    mymid.add(print_str);
                }
                i++;
                lastindex = i+1;
                M_Quaternary tmpexp = new M_Quaternary(op,"",exps.get(expindex),"");
                quas.add(tmpexp);
                String print_exp = op + " " + exps.get(expindex);
                mymid.add(print_exp);
                expindex++;
            }
        }
        if (lastindex != opstr.length()) {
            String str = opstr.substring(lastindex);
            M_Quaternary tmpprint = new M_Quaternary(op,str,"","");
            quas.add(tmpprint);
            String print_str = op + " " + str;
            mymid.add(print_str);
        }
    }

    //return val
    public void AddRetVal(String final_exp) {
        String op = "RETEXP";
        String operant1 = final_exp;
        M_Quaternary tmpret = new M_Quaternary(op,operant1,"","");
        quas.add(tmpret);
        String ret_print = op + " " + operant1;
        mymid.add(ret_print);
    }

    //x = exp
    public void AddAssign0d(String name,String fexp) {
        String op = "ASSIGN";
        String operant1 = name;
        String operant2 = fexp;
        M_Quaternary assignvar = new M_Quaternary(op,operant1,operant2,"");
        quas.add(assignvar);
        String assignvar_print = op + " " + operant1 + " = " + operant2;
        mymid.add(assignvar_print);
    }

    //x[i] = exp
    public void AddAssign1d(String name,String fexp,String index1) {
        String op = "ASSIGNARR";
        String operant1 = name;
        String operant2 = index1;
        String operant3 = fexp;
        AssArrEle(op,operant1,operant2,operant3);
    }

    //x[i][j] = exp
    public void AddAssign2d(String name,String fexp,String index1,
                            String index2,int len2,int level) {
        String findex2d = gen_var();
        Addint(findex2d);
        table.additem(findex2d,1,2,level);
        AddMulExp("*",index1,len2+"",findex2d);
        String findex1d = gen_var();
        Addint(findex1d);
        table.additem(findex1d,1,2,level);
        AddAddExp("+",findex2d,index2,findex1d);
        String op = "ASSIGNARR";
        String operant1 = name;
        String operant2 = findex1d;
        String operant3 = fexp;
        AssArrEle(op,operant1,operant2,operant3);
    }

    // x= y +/- z
    public void AddAddExp(String op,String num1,String num2,String num3) {
        String operant1 = num1;
        String operant2 = num2;
        String operant3 = num3;
        if (op.equals("+")) {
            String opr = "ADD";
            M_Quaternary addexp = new M_Quaternary(opr,operant1,operant2,operant3);
            quas.add(addexp);
            String addexp_print = opr + " " + operant3 + " = " + operant1 + " + " +operant2;
            mymid.add(addexp_print);
        } else if (op.equals("-")) {
            String opr = "SUB";
            M_Quaternary subexp = new M_Quaternary(opr,operant1,operant2,operant3);
            quas.add(subexp);
            String subexp_print = opr + " " + operant3 + " = " + operant1 + " - " +operant2;
            mymid.add(subexp_print);
        }
    }

    // x = y */% z
    public void AddMulExp(String op,String num1,String num2,String num3) {
        String operant1 = num1;
        String operant2 = num2;
        String operant3 = num3;
        if (op.equals("*")) {
            String opr = "MUL";
            M_Quaternary mulexp = new M_Quaternary(opr,operant1,operant2,operant3);
            quas.add(mulexp);
            String mulexp_print = opr + " " + operant3 + " = " + operant1 + " * " +operant2;
            mymid.add(mulexp_print);
        } else if (op.equals("/")) {
            String opr = "DIV";
            M_Quaternary divexp = new M_Quaternary(opr,operant1,operant2,operant3);
            quas.add(divexp);
            String divexp_print = opr + " " + operant3 + " = " + operant1 + " / " +operant2;
            mymid.add(divexp_print);
        } else if (op.equals("%")) {
            String opr = "MOD";
            M_Quaternary modexp = new M_Quaternary(opr,operant1,operant2,operant3);
            quas.add(modexp);
            String modexp_print = opr + " " + operant3 + " = " + operant1 + " % " +operant2;
            mymid.add(modexp_print);
        }
    }

    //func call
    public void AddFuncCall(String name) {
        M_Quaternary tmpfunccall = new M_Quaternary("CALL",name,"","");
        quas.add(tmpfunccall);
        String fc_print = "CALL " + name;
        mymid.add(fc_print);
    }

    //func call ret
    public void AddFuncCallRet(String name) {
        String op = "ASSIGNRET";
        String operant1 = name;
        String operant2 = "RET";
        String operant3 = "";
        M_Quaternary funret = new M_Quaternary(op,operant1,operant2,operant3);
        quas.add(funret);
        String funret_print = op + " " + operant1 + " = " + operant2;
        mymid.add(funret_print);
    }

    //f(x,y)
    public void AddFuncRP(String name) {
        String op = "PUSH";
        String operant1 = name;
        String operant2 = "";
        String operant3 = "";
        M_Quaternary pushpara = new M_Quaternary(op,operant1,operant2,operant3);
        quas.add(pushpara);
        String pushpara_print = op + " " + operant1;
        mymid.add(pushpara_print);
    }

    //- ! x
    public void AddUnaryop(String op,String name1,String name2) {
        String operant1 = name2;
        String operant3 = name1;
        if (op.equals("-")) {
            String opr = "NEG";
            M_Quaternary negexp = new M_Quaternary(opr,operant1,"",operant3);
            quas.add(negexp);
            String negexp_print = opr + " " + operant3 + " = -" + operant1;
            mymid.add(negexp_print);
        } else if (op.equals("!")) {
            String opr = "NOT";
            M_Quaternary notexp = new M_Quaternary(opr,operant1,"",operant3);
            quas.add(notexp);
            String notexp_print = opr + " " + operant3 + " = !" + operant1;
            mymid.add(notexp_print);
        }
    }

    //a[]
    public void AddgetArr1d(String arrname,String index1,String target) {
        String op = "GETARR1";
        String operant1 = arrname;
        String operant2 = index1;
        String operant3 = target;
        M_Quaternary getarr1 = new M_Quaternary(op,operant1,operant2,operant3);
        quas.add(getarr1);
        String getarr1_print =op + " " + operant3 + " = " + operant1 + "[" + operant2 + "]";
        mymid.add(getarr1_print);
    }

    //a[][]
    public void AddgetArr2d(String arrname,String index1,String index2,
                            String len,String target,int level) {
        String findex2d = gen_var();
        Addint(findex2d);
        table.additem(findex2d,1,2,level);
        AddMulExp("*",index1,len,findex2d);
        String findex1d = gen_var();
        Addint(findex1d);
        table.additem(findex1d,1,2,level);
        AddAddExp("+",findex2d,index2,findex1d);
        String op = "GETARR2";
        String operant1 = arrname;
        String operant2 = findex1d;
        String operant3 = target;
        M_Quaternary getarr2 = new M_Quaternary(op,operant1,operant2,operant3);
        quas.add(getarr2);
        String getarr2_print =op + " " + operant3 + " = " + operant1 + "[" + operant2 + "]";
        mymid.add(getarr2_print);
    }

    //a[] -> a
    //a[][] -> a
    public void AddArrAddr1d(String arrname,String target) {
        String op = "GETARRADDR1";
        String operant1 = arrname;
        String operant3 = target;
        M_Quaternary getarr2 = new M_Quaternary(op,operant1,"",operant3);
        quas.add(getarr2);
        String getarr2_print =op + " " + operant3 + " = " + operant1;
        mymid.add(getarr2_print);
    }

    //a[][]  -> a[]
    public void AddArrAddr2d(String arrname,String index1,
                             String len,String target,int level) {
        String findex2d = gen_var();
        Addint(findex2d);
        table.additem(findex2d,1,2,level);
        AddMulExp("*",index1,len,findex2d);
        String op = "GETARRADDR2";
        String operant1 = arrname;
        String operant2 = findex2d;
        String operant3 = target;
        M_Quaternary getarr2 = new M_Quaternary(op,operant1,operant2,operant3);
        quas.add(getarr2);
        String getarr2_print =op + " " + operant3 + " = " + operant1 + "[" + operant2 + "]";
        mymid.add(getarr2_print);
    }


    //第二次作业新加的
    //lable:
    public void AddLable(String lablename){
        String op = "ADDLABLE";
        String operant1 = lablename;
        M_Quaternary labledef = new M_Quaternary(op,operant1,"","");
        quas.add(labledef);
        String labledef_print = op + " " + operant1;
        mymid.add(labledef_print);
    }

    //beq/bne/ a,b,lable
    public void AddLogic(String calop,String var1,String var2,String lablename){
        String op = "";
        if (calop.equals("BEQ") || calop.equals("BNE")) {
            op = calop;
        } else if (calop.equals("==")) {
            op = "EQL";
        } else if (calop.equals("!=")) {
            op = "NEQ";
        } else if (calop.equals("<")) {
            op = "LSS";
        } else if (calop.equals(">")) {
            op = "GRE";
        } else if (calop.equals("<=")) {
            op = "LEQ";
        } else if (calop.equals(">=")) {
            op = "GEQ";
        }
        String operant1 = var1;
        String operant2 = var2;
        String operant3 = lablename;
        M_Quaternary logic = new M_Quaternary(op,operant1,operant2,operant3);
        quas.add(logic);
        String logic_print = op + " " + operant1 + " " + operant2 + " " + operant3;
        mymid.add(logic_print);
    }

    //jump lable
    public void AddJump(String lablename) {
        String op = "JUMP";
        String operant1 = lablename;
        M_Quaternary jumplable = new M_Quaternary(op,operant1,"","");
        quas.add(jumplable);
        String jumplable_print = op + " " + operant1;
        mymid.add(jumplable_print);
    }

}
