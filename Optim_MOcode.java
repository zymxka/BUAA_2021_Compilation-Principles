import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class Optim_MOcode{
    private ArrayList<M_Quaternary> codes;
    private IT_IdentTable table;
    private IT_IdentTable midtable;
    private ArrayList<String> targetcode;
    private int index; //遍历codes
    private int fpra = 8;
    private int glovarindex;
    private int constindex;
    private int level = 0;
    private int localoffset = 0;
    private int strindex;

    //优化加的东西
    private Optim_Midvartable midreg;
    private HashMap<String,IT_IdentTable> lifetable;
    private IT_IdentTable funclifetable;
    private Optim_Norvartavle norreg;

    public Optim_MOcode(ArrayList<M_Quaternary> code,IT_IdentTable oldtable,
                        HashMap<String,IT_IdentTable> lifetable){
        this.codes = code;
        this.midtable = oldtable;
        this.table = new IT_IdentTable();
        this.index = 0;
        this.targetcode = new ArrayList<>();
        this.glovarindex = 0;
        this.constindex = 0;
        this.strindex = 0;
        this.midreg = new Optim_Midvartable();
        this.norreg = new Optim_Norvartavle();
        this.lifetable = lifetable;
        this.funclifetable = null;
        gen_target();
    }

    public ArrayList<String> getTargetcode() {
        return this.targetcode;
    }

    public void print_target() throws IOException {
        BufferedWriter bout = new BufferedWriter(new FileWriter("mips_old.txt"));
        for (String i : targetcode) {
            bout.write(i);
            bout.newLine();
        }
        bout.close();
    }

    //功能函数
    //将str转化成int
    private int StrToInt(String exp) {
        int ans = 0;
        for (int i=0;i<exp.length();i++){
            int x = exp.charAt(i) - '0';
            ans = ans * 10 + x;
        }
        return ans;
    }

    //判断一个字符串是不是数字
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

    //先处理Data段
    //ConstDecl/VarDecl + FuncDef + MainFuncDef
    private void gen_target() {
        targetcode.add(".data");
        //处理常量和全局变量定义
        for (int i=0;i<codes.size();i++) {
            M_Quaternary tmp = codes.get(i);
            if (tmp.getOp().equals("BVarDecl")) {
                gen_Glolable_int(i+1);
            } else if (tmp.getOp().equals("CONST")) {
                //gen_Glolable_const(tmp);
            } else if (tmp.getOp().equals("CONSTARR1")
                    || tmp.getOp().equals("CONSTARR2")) {
                gen_Glolable_constarr(tmp);
            } else if (tmp.getOp().equals("BBLOCK")) {
                this.level++;
            } else if (tmp.getOp().equals("EBLOCK")) {
                this.level--;
            } else if (tmp.getOp().equals("FUNC")) {
                this.nowfuncname = tmp.getOperant1();
            }
        }

        //处理字符串
        for (M_Quaternary qua:codes) {
            if (qua.getOp().equals("PRINTF")) {
                AddString(qua);
            }
        }

        //将常量的初始化放到.data段中
        targetcode.add(".text");
        for (int i=0;i<codes.size();i++) {
            M_Quaternary tmp = codes.get(i);
            if (tmp.getOp().equals("BConstDecl")) {
                gen_ConstDecl(i);
            } else if (tmp.getOp().equals("BVarDecl")) {
                gen_GolVarDecl(i);
            } else if (tmp.getOp().equals("BBLOCK")) {
                this.level++;
            } else if (tmp.getOp().equals("EBLOCK")) {
                this.level--;
            }
        }

        //函数定义部分
        targetcode.add("j main");
        for(;!codes.get(index).getOp().equals("BFuncDef") &&
                !codes.get(index).getOp().equals("BMainFunc");index++) {
            //System.out.println(index);
        }
        for(;!codes.get(index).getOp().equals("BMainFunc");index++) {
            gen_FuncDef();
        }

        gen_MainFuncDef();
    }

    //处理全局变量和printf的lable
    //生成全局变量(const或者int)对应的lable
    private void gen_GolLable(String name,int len) {
        String namelable = name + ":";
        String sizelable = "  .space " + len*4;
        //targetcode.add(namelable);
        targetcode.add(namelable+sizelable);
    }

    //生成全局变量中的int型的lable
    private void gen_Glolable_int(int i){
        for(int id = i;!codes.get(id).getOp().equals("EVarDecl");id++){
            M_Quaternary tmp = codes.get(id);
            if (tmp.getOp().equals("VAR")) {
                table.additem(tmp.getOperant2(),1,2,level);
                gen_GolLable(tmp.getOperant2(),1);
            } else if (tmp.getOp().equals("VARARR1")
                    || tmp.getOp().equals("VARARR2")) {
                if (tmp.getOp().equals("VARARR1")) {
                    table.additem(tmp.getOperant2(),3,2,level);
                } else {
                    table.additem(tmp.getOperant2(),4,2,level);
                }
                gen_GolLable(tmp.getOperant2(),StrToInt(tmp.getOperant3()));
            }
        }
    }

    //生成全局变量中的const型的lable
    private void gen_Glolable_const(M_Quaternary tmp) {
        table.additem(tmp.getOperant2(),1,1,level);
        IT_IdentItem tmpconst = table.getvar(tmp.getOperant2(),level);
        String lablename = tmpconst.getName()+"str"+strindex;
        strindex++;
        tmpconst.setLablename(lablename);
        gen_GolLable(lablename,1);
    }

    //生成全局变量中的constarr型的lable
    private void gen_Glolable_constarr(M_Quaternary tmp) {
        if (tmp.getOp().equals("CONSTARR1")) {
            table.additem(tmp.getOperant2(),3,1,level);
        } else {
            table.additem(tmp.getOperant2(),4,1,level);
        }
        IT_IdentItem tmpconst = table.getvar(tmp.getOperant2(),level);
        //System.out.println(tmp.getOperant2());
        String lablename = tmpconst.getName()+"str"+strindex;
        strindex++;
        //System.out.println(lablename);
        tmpconst.setLablename(lablename);
        gen_GolLable(lablename,StrToInt(tmp.getOperant3()));
    }

    //把printf的字符串加到.data段
    private void AddString(M_Quaternary qua) {
        if (!qua.getOperant1().equals("")) {
            String printlable = "printlablestr"+strindex;
            strindex++;
            targetcode.add(printlable+":.asciiz \""+qua.getOperant1()+"\"");
            qua.setOperant3(printlable);
        }
    }

    //初始化常量和全局int的函数
    //存储数组元素到指定地址的方法
    private void gen_GloValInit(String varname,int offset,String regname) {
        int staticoffset = offset*4;
        //System.out.println(varname+" "+table.getGloOffset(varname));
        String swins = "sw"+" "+regname+", "+staticoffset+"($s4)";
        targetcode.add(swins);
    }

    //初始化常量
    private void gen_ConstDecl(int start) {
        for(int i=start;!codes.get(i).getOp().equals("EConstDecl");i++) {
            M_Quaternary tmp = codes.get(i);
            if (tmp.getOp().equals("CONST")) {
                //gen_AssConstVar(i);
            } else if (tmp.getOp().equals("CONSTARR1")
                    || tmp.getOp().equals("CONSTARR2")) {
                gen_AssConstArr(i);
            }
        }
    }

    //单个常量的定义和赋值
    private void gen_AssConstVar(int start) {
        M_Quaternary initconst0d = codes.get(start);
        this.glovarindex++;
        String lablename = table.getvar(initconst0d.getOperant2(),level).getLablename();
        targetcode.add("la $s4,"+lablename);
        //value is calculated before
        String regname = "$v1";
        String initval = initconst0d.getOperant3();
        String li_init = "li"+" "+regname+", "+initval;
        targetcode.add(li_init);
        gen_GloValInit(initconst0d.getOperant2(),0,regname);
    }

    //常量数组的定义和赋值
    private void gen_AssConstArr(int start) {
        M_Quaternary initconst1d = codes.get(start);
        String arrname = initconst1d.getOperant2();
        int arrlen = StrToInt(initconst1d.getOperant3());
        String lablename = table.getvar(initconst1d.getOperant2(),level).getLablename();
        targetcode.add("la $s4,"+lablename);
        this.glovarindex += arrlen;
        for(int i=0;i<arrlen;i++) {
            start++;
            M_Quaternary arrelem = codes.get(start);
            String regname = "$v1";
            String initval = arrelem.getOperant3();
            String li_init = "li"+" "+regname+", "+initval;
            int offset = StrToInt(arrelem.getOperant2());
            targetcode.add(li_init);
            gen_GloValInit(arrname,offset,regname);
        }
    }

    //初始化全局变量/全局数组
    private void gen_GolVarDecl(int start) {
        for(int i=start;!codes.get(i).getOp().equals("EVarDecl");i++) {
            M_Quaternary tmp = codes.get(i);
            if (tmp.getOp().equals("VAR")) {
                gen_GloVar(i);
            } else if (tmp.getOp().equals("VARARR1")
                    || tmp.getOp().equals("VARARR2")) {
                gen_GloArr(i);
            }
        }
    }

    //全局变量的定义/定义和赋值
    private void gen_GloVar(int start) {
        M_Quaternary initvar0d = codes.get(start);
        this.glovarindex++;
        if (!codes.get(start+1).getOp().equals("EVarDecl")) {
            targetcode.add("la $s4,"+initvar0d.getOperant2());
            start++;
            M_Quaternary initvalue = codes.get(start);
            String regname = "$v1";
            String initval = initvalue.getOperant2();
            String li_init = "li"+" "+regname+", "+initval;
            targetcode.add(li_init);
            gen_GloValInit(initvar0d.getOperant2(),0,regname);
        }
    }

    //全局数组的定义/定义和赋值
    private void gen_GloArr(int start) {
        M_Quaternary initvar1d = codes.get(start);
        String arrname = initvar1d.getOperant2();
        int arrlen = StrToInt(initvar1d.getOperant3());
        this.glovarindex += arrlen;
        if (!codes.get(start+1).getOp().equals("EVarDecl")) {
            targetcode.add("la $s4," + arrname);
            for(int i=0;i<arrlen;i++) {
                start++;
                M_Quaternary arrelem = codes.get(start);
                String regname = "$v1";
                String initval = arrelem.getOperant3();
                String li_init = "li"+" "+regname+", "+initval;
                int offset = StrToInt(arrelem.getOperant2());
                targetcode.add(li_init);
                gen_GloValInit(arrname,offset,regname);
            }
        }
    }

    //函数
    //功能性方法

    //把变量名字对应的值加载到reg中
    //立即数:li 非数组变量:lw值 数组变量(参数):值就是地址 数组变量(非参数):
    private String gen_LoadtoReg(String varname,String regname) {
        String ans = "";
        //System.out.println(varname);
        if (isnum(varname)) {
            targetcode.add("li "+regname+", "+varname);
        } else {
            //System.out.println(varname);
            IT_IdentItem var = table.getvar(varname,level);
            //System.out.println(varname);
            int type= var.getType();
            int clas = var.getClas();
            int varlevel = var.getLevel();
            if (type == 1) {
                if (clas == 1) {
                    targetcode.add("la $a0, "+var.getLablename());
                    targetcode.add("lw "+regname+", 0($a0)");
                } else {
                    targetcode.add("la "+regname+", "+var.getLablename());
                }
            } else if (type == 2) {
                if (varlevel == 0) {
                    if (clas == 1) {
                        targetcode.add("la $a0, "+varname);
                        targetcode.add("lw "+regname+", 0($a0)");
                    } else {
                        targetcode.add("la "+regname+", "+varname);
                    }
                } else {
                    if (varname.charAt(0) == '@'){
                        int idx = midreg.FindMidvar(varname);
                        if (idx != -1) {
                            ans = "$t"+idx;
                            midreg.DelMidvar(varname);
                        } else {
                            int varofff = var.getLocalvaroff();
                            if (clas == 1) {
                                targetcode.add("lw "+regname+", -"+varofff+"($fp)");
                            } else {
                                targetcode.add("sub "+ regname+", $fp,"+varofff);
                            }
                        }
                    } else {
                        int idx = norreg.FindNorvar(varname,var.getClas()
                                        ,var.getType(),var.getLevel());
                        if (idx != -1) {
                            ans = "$s"+idx;
                            //targetcode.add("move "+regname+",$s"+idx);
                        } else {
                            int varofff = var.getLocalvaroff();
                            if (clas == 1) {
                                targetcode.add("lw "+regname+", -"+varofff+"($fp)");
                            } else {
                                targetcode.add("sub "+ regname+", $fp,"+varofff);
                            }
                        }
                    }
                }
            } else if (type == 3) {
                int idx = norreg.FindNorvar(varname,var.getClas()
                        ,var.getType(),var.getLevel());
                if (idx != -1) {
                    ans = "$s"+idx;
                    //targetcode.add("move "+regname+",$s"+idx);
                } else {
                    int paraoff = var.getLocalvaroff();
                    targetcode.add("lw "+regname+", -"+paraoff+"($fp)");
                }
            }
        }
        return ans;
    }

    //把reg中的值存到varname的地址中
    private void gen_StorefReg(String varname,String regname) {
        //System.out.println(varname);
        IT_IdentItem var = table.getvar(varname,level);
        int type= var.getType();
        int clas = var.getClas();
        int varlevel = var.getLevel();
        if (type == 2) {
            if (varlevel == 0) {
                if (clas == 1) {
                    targetcode.add("la $a0, "+varname);
                    targetcode.add("sw "+regname+", 0($a0)");
                }
            } else {
                if (varname.charAt(0)=='@') {
                    int idx = midreg.FindMidvar(varname);
                    if (idx != -1) {
                        targetcode.add("move $t"+idx+","+regname);
                    } else {
                        if (clas == 1) {
                            int varofff = var.getLocalvaroff();
                            targetcode.add("sw "+regname+", -"+varofff+"($fp)");
                        }
                    }
                } else {
                    int idx = norreg.FindNorvar(varname,var.getClas()
                            ,var.getType(),var.getLevel());
                    /*
                    targetcode.add(var.getClas()+" "+var.getType()+" "+var.getLevel());
                    targetcode.add("find "+varname+" "+idx);
                    targetcode.add(norreg.getRegs().get(3).getName()+" "+
                            norreg.getRegs().get(3).getClas()+" "+
                            norreg.getRegs().get(3).getType()+" "+
                            norreg.getRegs().get(3).getLevel());*/
                    if (idx != -1) {
                        targetcode.add("move $s"+idx+","+regname);
                    } else {
                        if (clas == 1) {
                            int varofff = var.getLocalvaroff();
                            targetcode.add("sw "+regname+", -"+varofff+"($fp)");
                        }
                    }
                }
            }
        } else if (type == 3) {
            int idx = norreg.FindNorvar(varname,var.getClas()
                    ,var.getType(),var.getLevel());
            if (idx != -1) {
                targetcode.add("move $s"+idx+","+regname);
            } else {
                if (clas == 1) {
                    int paraoff = var.getLocalvaroff();
                    targetcode.add("sw "+regname+", -"+paraoff+"($fp)");
                }
            }
        }
    }

    private String nowfuncname = "global";
    //函数定义的整体
    private void gen_FuncDef() {
        index++;
        M_Quaternary fundef = codes.get(index);
        String funcname = fundef.getOperant1();
        this.funclifetable = lifetable.get(funcname);
        this.norreg = new Optim_Norvartavle();
        this.nowfuncname = funcname;
        targetcode.add(funcname+":");
        index++;
        gen_FuncHead(funcname);
        gen_FuncBlock("EFuncDef");
        targetcode.add(funcname+"tail"+":");
        gen_FuncTail();
        this.nowfuncname = "global";
    }

    //函数头的目标代码
    //设置一些寄存器
    private void gen_FuncHead(String funcname) {
        int paracount = table.funcparanum(funcname);
        int fpoff = this.fpra;
        this.localoffset = this.fpra+68;
        int raoff = fpoff-4;
        String storefp = "sw $fp,"+fpoff+"($sp)";
        String addfp = "add $fp,$sp,"+fpoff;
        String storera = "sw $ra,"+raoff+"($sp)";
        targetcode.add(storefp);
        targetcode.add(addfp);
        targetcode.add(storera);
        targetcode.add("addi $sp,$sp,-68");
        targetcode.add("sw $s0,68($sp)");
        targetcode.add("sw $s1,64($sp)");
        targetcode.add("sw $s2,60($sp)");
        targetcode.add("sw $s3,56($sp)");
        targetcode.add("sw $s4,52($sp)");
        targetcode.add("sw $s5,48($sp)");
        targetcode.add("sw $s6,44($sp)");
        targetcode.add("sw $t0,40($sp)");
        targetcode.add("sw $t1,36($sp)");
        targetcode.add("sw $t2,32($sp)");
        targetcode.add("sw $t3,28($sp)");
        targetcode.add("sw $t4,24($sp)");
        targetcode.add("sw $t5,20($sp)");
        targetcode.add("sw $t6,16($sp)");
        targetcode.add("sw $t7,12($sp)");
        targetcode.add("sw $t8,8($sp)");
        targetcode.add("sw $t9,4($sp)");
        int offset = midtable.funclocallen(nowfuncname);
        targetcode.add("sub $sp,$sp,"+offset);
        //参数设置（call的时候弄）
    }

    //函数尾部的目标代码
    private void gen_FuncTail() {
        targetcode.add("lw $ra,-4($fp)");
        targetcode.add("lw $s0,-8($fp)");
        targetcode.add("lw $s1,-12($fp)");
        targetcode.add("lw $s2,-16($fp)");
        targetcode.add("lw $s3,-20($fp)");
        targetcode.add("lw $s4,-24($fp)");
        targetcode.add("lw $s5,-28($fp)");
        targetcode.add("lw $s6,-32($fp)");
        targetcode.add("lw $t0,-36($fp)");
        targetcode.add("lw $t1,-40($fp)");
        targetcode.add("lw $t2,-44($fp)");
        targetcode.add("lw $t3,-48($fp)");
        targetcode.add("lw $t4,-52($fp)");
        targetcode.add("lw $t5,-56($fp)");
        targetcode.add("lw $t6,-60($fp)");
        targetcode.add("lw $t7,-64($fp)");
        targetcode.add("lw $t8,-68($fp)");
        targetcode.add("lw $t9,-72($fp)");
        targetcode.add("move $sp, $fp");
        targetcode.add("lw $fp, ($sp)");
        targetcode.add("move $sp, $a3");
        targetcode.add("jr $ra");
    }

    //Main函数
    private void gen_MainFuncDef() {
        index++;
        M_Quaternary mainfundef = codes.get(index);
        String funcname = mainfundef.getOperant1();
        this.funclifetable = lifetable.get(funcname);
        this.norreg = new Optim_Norvartavle();
        this.nowfuncname = funcname;
        targetcode.add(funcname+":");
        gen_MainHead();
        gen_FuncBlock("EMainFunc");
        targetcode.add(funcname+"tail"+":");
        this.nowfuncname = "global";
    }

    //Main函数头的目标代码
    private void gen_MainHead() {
        targetcode.add("sw $fp, ($sp)");
        targetcode.add("move $fp, $sp");
        int offset = 8+midtable.funclocallen(nowfuncname);
        targetcode.add("sub $sp, $sp, "+offset);
        this.localoffset = this.fpra;
    }

    //函数体的目标代码
    private void gen_FuncBlock(String end) {
        int call_offset = this.fpra;
        for(;!codes.get(index).getOp().equals(end);index++){
            String type = codes.get(index).getOp();
            if (type.equals("BConstDecl")) {
                //const的定义部分可以跳过
                for(;!codes.get(index).getOp().equals("EConstDecl");index++) {
                }
            }else if (type.equals("VAR")){
                gen_Local_var();
            } else if (type.equals("VARARR1")
                    || type.equals("VARARR2")) {
                gen_Local_vararr(type);
            } else if (type.equals("PARAM") ||
                    type.equals("PARAMARR1") || type.equals("PARAMARR2")) {
                gen_Local_param(type);
            } else if (type.equals("ASSIGN")) {
                //targetcode.add("assign");
                gen_Local_Assign_var();
                //targetcode.add("eassign");
            } else if (type.equals("ASSIGNARR")) {
                gen_Local_Assign_arr();
            } else if (type.equals("ASSIGNRET")) {
                gen_Local_Assigb_ret();
            } else if (type.equals("PRINTF")){
                gen_Local_printf();
            } else if (type.equals("RETEXP")){
                gen_Local_ret();
            } else if (type.equals("ADD") || type.equals("SUB")
                    ||type.equals("MUL")) {
                gen_Local_Mathcal(type);
            } else if (type.equals("DIV")||type.equals("MOD")) {
                gen_Optim_Div(type);
            } else if (type.equals("CALL")) {
                gen_Local_Funccall();
                call_offset = this.fpra;
            } else if (type.equals("PUSH")) {
                gen_Local_Funcpush(call_offset);
                call_offset+=4;
            } else if (type.equals("NEG") || type.equals("NOT")) {
                gen_Local_Logic_two(type);
            } else if (type.equals("GETARR1")
                    || type.equals("GETARR2")) {
                gen_Local_Getarr();
            } else if (type.equals("BBLOCK")) {
                this.level++;
            } else if (type.equals("EBLOCK")) {
                table.popnoconst(level);
                this.level--;
            } else if (type.equals("GETARRADDR1")) {
                gen_Local_Getaddr();
            } else if (type.equals("GETARRADDR2")) {
                gen_Local_Getaddr2();
            } else if (type.equals("FUNC")) {
                gen_Func_Name();
            } else if (type.equals("JUMP")) {
                gen_Local_Jump_Nocon();
            } else if (type.equals("ADDLABLE")) {
                gen_Local_Addlable();
            }else if (type.equals("EQL") || type.equals("NEQ") || type.equals("LSS")
                    ||type.equals("GRE") || type.equals("LEQ") || type.equals("GEQ")) {
                gen_Local_logic_three(type);
            } else if (type.equals("BLT") || type.equals("BLE") || type.equals("BGT")
                    ||type.equals("BGE") || type.equals("BNE") || type.equals("BEQ")) {
                gen_Local_Jump_Con(type);
            }
        }
    }

    //增加无条件跳转语句
    private void gen_Local_Jump_Nocon() {
        String lablename = codes.get(index).getOperant1();
        targetcode.add("j "+lablename);
    }

    //增加标签
    private void gen_Local_Addlable() {
        String lablename = codes.get(index).getOperant1();
        targetcode.add(lablename+":");
    }

    //增加有条件跳转语句
    private void gen_Local_Jump_Con(String type) {
        String var1 = codes.get(index).getOperant1();
        String var2 = codes.get(index).getOperant2();
        String lable = codes.get(index).getOperant3();
        //Load var1
        String reg1 = gen_LoadtoReg(var1,"$a1");
        if (reg1.equals("")) {
            reg1 = "$a1";
        }
        //Load var2
        String reg2 = gen_LoadtoReg(var2,"$a2");
        if (reg2.equals("")) {
            reg2 = "$a2";
        }
        String bop = "";
        switch (type) {
            case "BEQ":
                bop = "beq";
                break;
            case "BNE":
                bop = "bne";
                break;
            case "BGE":
                bop = "bge";
                break;
            case "BGT":
                bop = "bgt";
                break;
            case "BLE":
                bop = "ble";
                break;
            case "BLT":
                bop = "blt";
                break;
        }
        targetcode.add(bop+" "+reg1+","+reg2+", "+lable);
    }

    //增加逻辑计算语句
    private void gen_Local_logic_three(String type) {
        String var1 = codes.get(index).getOperant1();
        String var2 = codes.get(index).getOperant2();
        String target = codes.get(index).getOperant3();
        //Load var1
        String reg1 = gen_LoadtoReg(var1,"$a1");
        if (reg1.equals("")) {
            reg1 = "$a1";
        }
        //Load var2
        String reg2 = gen_LoadtoReg(var2,"$a2");
        if (reg2.equals("")) {
            reg2 = "$a2";
        }
        String bop = "";
        if (type.equals("EQL")) {
            bop = "seq";
        } else if (type.equals("NEQ")) {
            bop = "sne";
        } else if (type.equals("LSS")) {
            bop = "slt";
        } else if (type.equals("GRE")) {
            bop = "sgt";
        } else if (type.equals("LEQ")) {
            bop = "sle";
        } else if (type.equals("GEQ")) {
            bop = "sge";
        }
        String treg = "$v1";
        if(target.charAt(0)=='@') {
            int idx = midreg.FindMidvar(target);
            if (idx != -1) {
                treg = "$t"+idx;
            }
        } else {
            IT_IdentItem var = table.getvar(target,level);
            int idx = norreg.FindNorvar(target,var.getClas()
                    ,var.getType(),var.getLevel());
            if (idx != -1) {
                treg = "$s"+idx;
            }
        }
        if (treg.equals("$v1")) {
            targetcode.add(bop+" "+"$v1"+","+reg1+","+reg2);
            //Store target
            gen_StorefReg(target,"$v1");
        } else {
            targetcode.add(bop+" "+treg+","+reg1+","+reg2);
        }
    }

    //函数声明
    private void gen_Func_Name() {
        if (codes.get(index).getOperant2().equals("int")) {
            table.additem(codes.get(index).getOperant1(),1,4,level);
        } else {
            table.additem(codes.get(index).getOperant1(),2,4,level);
        }
    }

    //局部变量声明
    private void gen_Local_var() {
        table.additem(codes.get(index).getOperant2(),1,2,level);
        String varname = codes.get(index).getOperant2();
        //System.out.println(varname);
        if (varname.charAt(0) == '@') {
            int idx = midreg.AddMidvar(varname);
            if (idx == -1) {
                IT_IdentItem var = table.getvar(varname, level);
                var.setLocalvaroff(this.localoffset);
                this.localoffset += 4;
                //targetcode.add("sub $sp, $sp, 4");
            }
        } else {
            IT_IdentItem var = table.getvar(varname, level);
            var.setLocalvaroff(this.localoffset);
            Add_To_Sreg(varname,level);
            this.localoffset += 4;
            //targetcode.add("sub $sp, $sp, 4");
        }
    }

    //局部变量数组声明
    private void gen_Local_vararr(String type) {
        int len = 0;
        len = StrToInt(codes.get(index).getOperant3());
        if (type.equals("VARARR1")) {
            table.additem(codes.get(index).getOperant2(),3,2,level);
        } else {
            table.additem(codes.get(index).getOperant2(),4,2,level);
        }
        String arrname = codes.get(index).getOperant2();
        IT_IdentItem arr = table.getvar(arrname,level);
        arr.setLocalvaroff(this.localoffset+4*len-4);
        int suboff = StrToInt(codes.get(index).getOperant3())*4;
        this.localoffset += suboff;
        //targetcode.add("sub $sp, $sp, "+suboff);
    }

    private void Add_To_Sreg(String varname,int nowlevel) {
        IT_IdentItem lifevar = funclifetable.getvar_search(varname,nowlevel);
        IT_IdentItem var = table.getvar(varname,nowlevel);
        int idx = norreg.JudgeNorvar(index);
        if (idx != -1) {
            Optim_Midvar ret = norreg.getRegs().get(idx);
            if (!ret.getName().equals("$null")) {
                String retreg = "$s"+idx;
                //System.out.println(ret.getName());
                int retoff = ret.getLocaloff();
                targetcode.add("sw "+retreg+",-"+retoff+"($fp)");
            }
            norreg.AddNorvar(varname,idx, lifevar.getDeadtime(),
                    var.getClas(),var.getType(),var.getLevel(),var.getLocalvaroff());
            if (nowlevel!=level) {
                int paraoff = table.getvar(varname,nowlevel).getLocalvaroff();
                targetcode.add("lw "+"$s"+idx+", -"+paraoff+"($fp)");
            }
        }
    }

    //函数参数声明
    private void gen_Local_param(String type) {
        if (type.equals("PARAM")) {
            table.additem(codes.get(index).getOperant2(),1,3,level+1);
        }else if (type.equals("PARAMARR1")) {
            table.additem(codes.get(index).getOperant2(),3,3,level+1);
        } else {
            table.additem(codes.get(index).getOperant2(),4,3,level+1);
        }
        String varname = codes.get(index).getOperant2();
        //System.out.println(varname);
        IT_IdentItem var = table.getvar(varname,level+1);
        var.setLocalvaroff(this.localoffset);
        Add_To_Sreg(varname,level+1);
        this.localoffset += 4;
        //targetcode.add("sub $sp, $sp, 4");
    }

    //普通变量赋值语句
    private void gen_Local_Assign_var() {
        String reg = "$a1";
        if (codes.get(index).getOperant2().equals("getint()")) {
            targetcode.add("li $v0, 5");
            targetcode.add("syscall");
            targetcode.add("move $a1, $v0");
        } else {
            //Load num
            reg = gen_LoadtoReg(codes.get(index).getOperant2(),"$a1");
            if (reg.equals("")) {
                reg="$a1";
            }
        }
        //Store
        gen_StorefReg(codes.get(index).getOperant1(),reg);
    }

    //数组变量赋值语句
    private void gen_Local_Assign_arr() {
        //Load name addr
        //Para直接是地址上的值,全局是lable,局部是栈的地址
        String reg1 = gen_LoadtoReg(codes.get(index).getOperant1(),"$v1");
        if (reg1.equals("")) {
            reg1 = "$v1";
        } else if (reg1.charAt(1)=='s') {
            targetcode.add("move $v1,"+reg1);
            reg1 = "$v1";
        }
        //Load index
        String reg2 = gen_LoadtoReg(codes.get(index).getOperant2(),"$a1");
        if (reg2.equals("")) {
            reg2 = "$a1";
        } else if (reg2.charAt(1)=='s') {
            targetcode.add("move $a1,"+reg2);
            reg2 = "$a1";
        }
        String reg3 = "$a2";
        if (codes.get(index).getOperant3().equals("getint()")) {
            targetcode.add("li $v0, 5");
            targetcode.add("syscall");
            targetcode.add("move $a2, $v0");
        } else {
            //Load num
            reg3 = gen_LoadtoReg(codes.get(index).getOperant3(),"$a2");
            if (reg3.equals("")) {
                reg3 = "$a2";
            }
        }
        //Store
        targetcode.add("sll "+reg2+", "+reg2+", 2");
        targetcode.add("add "+reg1+", "+reg1+", "+reg2);
        targetcode.add("sw "+reg3+", 0("+reg1+")");
    }

    //返回值赋值语句
    private void gen_Local_Assigb_ret() {
        //Store name
        gen_StorefReg(codes.get(index).getOperant1(),"$v0");
    }

    //输出语句
    private void gen_Local_printf() {
        M_Quaternary tmp = codes.get(index);
        if (codes.get(index).getOperant1().equals("")) {
            //load exp
            String reg = gen_LoadtoReg(codes.get(index).getOperant2(),"$a0");
            if (!reg.equals("")) {
                targetcode.add("move $a0, "+reg);
            }
            targetcode.add("li $v0, 1\n" + "syscall");
        } else {
            //str
            String printlable = codes.get(index).getOperant3();
            targetcode.add("la $a0," + printlable);
            targetcode.add("li $v0, 4\n" + "syscall");
        }
    }

    //返回语句
    private void gen_Local_ret() {
        //Load retexp
        if (!codes.get(index).getOperant1().equals("")) {
            String reg = gen_LoadtoReg(codes.get(index).getOperant1(),"$v0");
            if (!reg.equals("")) {
                targetcode.add("move $v0, "+reg);
            }
        }
        targetcode.add("j "+nowfuncname+"tail");
    }

    private int lableidx = 0;
    private void gen_Optim_Div(String type) {
        //Load op1
        String var1 = gen_LoadtoReg(codes.get(index).getOperant1(),"$a1");
        if (var1.equals("")) {
            var1 = "$a1";
        }
        //Load target
        String target = codes.get(index).getOperant3();
        String treg = "$v1";
        if(target.charAt(0)=='@') {
            int idx = midreg.FindMidvar(target);
            if (idx != -1) {
                treg = "$t"+idx;
            }
        } else {
            IT_IdentItem var = table.getvar(target,level);
            int idx = norreg.FindNorvar(target,var.getClas()
                    ,var.getType(),var.getLevel());
            if (idx != -1) {
                treg = "$s" +idx;
            }
        }

        if (isnum(codes.get(index).getOperant2())) {
            Optim_div odiv = new Optim_div(treg,var1,codes.get(index).getOperant2(),type,lableidx);
            lableidx++;
            targetcode.addAll(odiv.getOptim());
        } else {
            //Load op2
            String var2 = gen_LoadtoReg(codes.get(index).getOperant2(),"$a2");
            if (var2.equals("")) {
                var2 = "$a2";
            }
            //Calculate
            String op = "";
            if (type.equals("MOD")) {
                //targetcode.add("div $a1, $a2");
                targetcode.add("div "+var1+", "+var2);
                targetcode.add("mfhi "+treg);
            } else {
                op = "div";
                targetcode.add(op+" "+treg+", "+var1+", "+var2);
            }
            if (treg.equals("$v1")) {
                //Store target
                gen_StorefReg(target,"$v1");
            }
        }
    }

    //代数运算语句
    private void gen_Local_Mathcal(String type) {
        //Load op1
        String var1 = gen_LoadtoReg(codes.get(index).getOperant1(),"$a1");
        if (var1.equals("")) {
            var1 = "$a1";
        }
        //Load op2
        String var2 = gen_LoadtoReg(codes.get(index).getOperant2(),"$a2");
        if (var2.equals("")) {
            var2 = "$a2";
        }
        //Calculate
        String op = "";
        String target = codes.get(index).getOperant3();
        String treg = "$v1";
        if(target.charAt(0)=='@') {
            int idx = midreg.FindMidvar(target);
            if (idx != -1) {
                treg = "$t"+idx;
            }
        } else {
            IT_IdentItem var = table.getvar(target,level);
            int idx = norreg.FindNorvar(target,var.getClas()
                    ,var.getType(),var.getLevel());
            if (idx != -1) {
                treg = "$s" +idx;
            }
        }
        if (type.equals("ADD")) {
            op = "addu";
        } else if (type.equals("SUB")) {
            op = "subu";
        } else if (type.equals("MUL")) {
            op = "mul";
        }
            targetcode.add(op+" "+treg+", "+var1+", "+var2);
        if (treg.equals("$v1")) {
            //Store target
            gen_StorefReg(target,"$v1");
        }
    }

    //逻辑运算语句
    private void gen_Local_Logic_two(String type) {
        //Load op1
        String reg = gen_LoadtoReg(codes.get(index).getOperant1(),"$a1");
        if (reg.equals("")) {
            reg = "$a1";
        }
        //Calculate
        String target = codes.get(index).getOperant3();
        String treg = "$v1";
        if (target.charAt(0)=='@') {
            int idx = midreg.FindMidvar(target);
            if (idx != -1) {
                treg = "$t"+idx;
            }
        } else {
            IT_IdentItem var = table.getvar(target,level);
            int idx = norreg.FindNorvar(target,var.getClas()
                    ,var.getType(),var.getLevel());
            if (idx != -1) {
                treg = "$s"+idx;
            }
        }
        if (type.equals("NEG")) {
            targetcode.add("sub "+treg+", $0, "+reg);
        } else if (type.equals("NOT")) {
            //跳转还没弄
            targetcode.add("seq "+treg+", $0, "+reg);
        }
        //Store target
        if (treg.equals("$v1")) {
            gen_StorefReg(target,"$v1");
        }
    }

    //函数调用语句
    private void gen_Local_Funccall() {
        targetcode.add("move $a3, $sp");
        int newspoff = midtable.funclocallen(nowfuncname)+2*this.fpra+68;
        targetcode.add("sub $sp, $fp, "+newspoff);
        targetcode.add("jal "+codes.get(index).getOperant1());
    }

    //Push调用函数的参数
    private void gen_Local_Funcpush(int call_offset) {
        //单独识别一下数组元素
        int offset = midtable.funclocallen(nowfuncname) + this.fpra + 136 + call_offset;
        String reg = gen_LoadtoReg(codes.get(index).getOperant1(),"$v1");
        if (reg.equals("")) {
            reg = "$v1";
        }
        targetcode.add("sw "+reg+",-"+offset+"($fp)");
    }

    //获取函数调用时的参数地址
    private void gen_Local_Getaddr() {
        //Only in Para to Func
        //Load arr addr
        //System.out.println(codes.get(index).getOperant1()+"\n"+codes.get(index).getOperant3());
        String reg = gen_LoadtoReg(codes.get(index).getOperant1(),"$v1");
        if (reg.equals("")) {
            reg = "$v1";
        }
        gen_StorefReg(codes.get(index).getOperant3(),reg);
    }

    //获取函数调用时的参数地址(二维->一维)
    private void gen_Local_Getaddr2() {
        //Only in Para to Func
        //Load arr addr
        String var1 = gen_LoadtoReg(codes.get(index).getOperant1(),"$a1");
        if (var1.equals("")) {
            var1 = "$a1";
        }
        //Load arr offset
        String var2 = gen_LoadtoReg(codes.get(index).getOperant2(),"$a2");
        if (var2.equals("")) {
            var2 = "$a2";
        } else if (var2.charAt(1)=='s') {
            targetcode.add("move $a2,"+var2);
            var2="$a2";
        }
        targetcode.add("sll "+var2+", "+var2+", 2");
        targetcode.add("add $v1, "+var1+", "+var2);
        //Store
        gen_StorefReg(codes.get(index).getOperant3(),"$v1");
    }

    //获取数组元素
    private void gen_Local_Getarr() {
        //Para直接是地址上的值,全局是lable,局部是栈的地址
        //Load arrname addr
        //System.out.println(codes.get(index).toString());
        String var1 = gen_LoadtoReg(codes.get(index).getOperant1(),"$a1");
        if (var1.equals("")) {
            var1 = "$a1";
        } else if (var1.charAt(1)=='s') {
            targetcode.add("move $a1,"+var1);
            var1 = "$a1";
        }
        //Load len
        String var2 = gen_LoadtoReg(codes.get(index).getOperant2(),"$a2");
        if (var2.equals("")) {
            var2 = "$a2";
        } else if (var2.charAt(1)=='s') {
            targetcode.add("move $a2,"+var2);
            var2 = "$a2";
        }
        //Load ArrayNum
        targetcode.add("sll "+var2+", "+var2+", 2");
        targetcode.add("add "+var1+", "+var1+", "+var2);
        targetcode.add("lw $v1, 0("+var1+")");
        //Store num
        gen_StorefReg(codes.get(index).getOperant3(),"$v1");
    }

}

