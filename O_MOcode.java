import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Stack;

public class O_MOcode {
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


    public O_MOcode(ArrayList<M_Quaternary> code,IT_IdentTable oldtable){
        this.codes = code;
        this.midtable = oldtable;
        this.table = new IT_IdentTable();
        this.index = 0;
        this.targetcode = new ArrayList<>();
        this.glovarindex = 0;
        this.constindex = 0;
        this.strindex = 0;
        gen_target();
    }

    public void print_target() throws IOException {
        BufferedWriter bout = new BufferedWriter(new FileWriter("mips.txt"));
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
                gen_Glolable_const(tmp);
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
        String swins = "sw"+" "+regname+", "+staticoffset+"($s0)";
        targetcode.add(swins);
    }

    //初始化常量
    private void gen_ConstDecl(int start) {
        for(int i=start;!codes.get(i).getOp().equals("EConstDecl");i++) {
            M_Quaternary tmp = codes.get(i);
            if (tmp.getOp().equals("CONST")) {
                gen_AssConstVar(i);
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
        targetcode.add("la $s0,"+lablename);
        //value is calculated before
        String regname = "$t0";
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
        targetcode.add("la $s0,"+lablename);
        this.glovarindex += arrlen;
        for(int i=0;i<arrlen;i++) {
            start++;
            M_Quaternary arrelem = codes.get(start);
            String regname = "$t0";
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
        targetcode.add("la $s0,"+initvar0d.getOperant2());
        if (!codes.get(start+1).getOp().equals("EVarDecl")) {
            start++;
            M_Quaternary initvalue = codes.get(start);
            String regname = "$t0";
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
            targetcode.add("la $s0," + arrname);
            for(int i=0;i<arrlen;i++) {
                start++;
                M_Quaternary arrelem = codes.get(start);
                String regname = "$t0";
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
    private boolean gen_LoadtoReg(String varname,String regname) {
        boolean isglobal = false;
        //System.out.println(varname);
        if (isnum(varname)) {
            targetcode.add("li "+regname+", "+varname);
        } else {
            IT_IdentItem var = table.getvar(varname,level);
            int type= var.getType();
            int clas = var.getClas();
            int varlevel = var.getLevel();
            if (type == 1) {
                isglobal = true;
                if (clas == 1) {
                    targetcode.add("la $t3, "+var.getLablename());
                    targetcode.add("lw "+regname+", 0($t3)");
                } else {
                    targetcode.add("la "+regname+", "+var.getLablename());
                }
            } else if (type == 2) {
                if (varlevel == 0) {
                    isglobal = true;
                    if (clas == 1) {
                        targetcode.add("la $t3, "+varname);
                        targetcode.add("lw "+regname+", 0($t3)");
                    } else {
                        targetcode.add("la "+regname+", "+varname);
                    }
                } else {
                    int varofff = var.getLocalvaroff();
                    if (clas == 1) {
                        targetcode.add("sub $t3, $fp,"+varofff);
                        targetcode.add("lw "+regname+", 0($t3)");
                    } else {
                        targetcode.add("sub "+ regname+", $fp,"+varofff);
                    }
                }
            } else if (type == 3) {
                int paraoff = var.getLocalvaroff();
                targetcode.add("sub $t3, $fp,"+paraoff);
                targetcode.add("lw "+regname+", 0($t3)");
            }
        }
        return isglobal;
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
                    targetcode.add("la $t3, "+varname);
                    targetcode.add("sw "+regname+", 0($t3)");
                }
            } else {
                if (clas == 1) {
                    int varofff = var.getLocalvaroff();
                    targetcode.add("sub $t3, $fp,"+varofff);
                    targetcode.add("sw "+regname+", 0($t3)");
                }
            }
        } else if (type == 3) {
            if (clas == 1) {
                int paraoff = var.getLocalvaroff();
                targetcode.add("sub $t3, $fp,"+paraoff);
                targetcode.add("sw "+regname+", 0($t3)");
            }
        }
    }

    private String nowfuncname = "global";
    //函数定义的整体
    private void gen_FuncDef() {
        index++;
        M_Quaternary fundef = codes.get(index);
        String funcname = fundef.getOperant1();
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
        this.localoffset = this.fpra;
        int raoff = fpoff-4;
        String storefp = "sw $fp,"+fpoff+"($sp)";
        String addfp = "add $fp,$sp,"+fpoff;
        String storera = "sw $ra,"+raoff+"($sp)";
        targetcode.add(storefp);
        targetcode.add(addfp);
        targetcode.add(storera);
        //参数设置（call的时候弄）
    }

    //函数尾部的目标代码
    private void gen_FuncTail() {
        targetcode.add("lw $ra, -4($fp)");
        targetcode.add("move $sp, $fp");
        targetcode.add("lw $fp, ($sp)");
        targetcode.add("move $sp, $s7");
        targetcode.add("jr $ra");
    }

    //Main函数
    private void gen_MainFuncDef() {
        index++;
        M_Quaternary mainfundef = codes.get(index);
        String funcname = mainfundef.getOperant1();
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
        targetcode.add("sub $sp, $sp, 8");
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
            }else if (type.equals("VAR") ){
                gen_Local_var();
            } else if (type.equals("VARARR1")
                    || type.equals("VARARR2")) {
                gen_Local_vararr(type);
            } else if (type.equals("PARAM") ||
                    type.equals("PARAMARR1") || type.equals("PARAMARR2")) {
                gen_Local_param(type);
            } else if (type.equals("ASSIGN")) {
                gen_Local_Assign_var();
            } else if (type.equals("ASSIGNARR")) {
                gen_Local_Assign_arr();
            } else if (type.equals("ASSIGNRET")) {
                gen_Local_Assigb_ret();
            } else if (type.equals("PRINTF")){
                gen_Local_printf();
            } else if (type.equals("RETEXP")){
                gen_Local_ret();
            } else if (type.equals("ADD") || type.equals("SUB")
                    ||type.equals("MUL")||type.equals("DIV")||type.equals("MOD")) {
                gen_Local_Mathcal(type);
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
            } else if (type.equals("BEQ") || type.equals("BNE")) {
                gen_Local_Jump_Con(type);
            } else if (type.equals("EQL") || type.equals("NEQ") || type.equals("LSS")
                    ||type.equals("GRE") || type.equals("LEQ") || type.equals("GEQ")) {
                gen_Local_logic_three(type);
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
        gen_LoadtoReg(var1,"$t1");
        //Load var2
        gen_LoadtoReg(var2,"$t2");
        if (type.equals("BEQ")) {
            targetcode.add("beq $t1,$t2,"+lable);
        } else if (type.equals("BNE")) {
            targetcode.add("bne $t1,$t2,"+lable);
        }
    }

    //增加逻辑计算语句
    private void gen_Local_logic_three(String type) {
        String var1 = codes.get(index).getOperant1();
        String var2 = codes.get(index).getOperant2();
        String target = codes.get(index).getOperant3();
        //Load var1
        gen_LoadtoReg(var1,"$t1");
        //Load var2
        //System.out.println(type+" "+var1+" "+var2+" "+target);
        gen_LoadtoReg(var2,"$t2");
        if (type.equals("EQL")) {
            targetcode.add("seq $t0, $t1, $t2");
        } else if (type.equals("NEQ")) {
            targetcode.add("sne $t0, $t1, $t2");
        } else if (type.equals("LSS")) {
            targetcode.add("slt $t0, $t1, $t2");
        } else if (type.equals("GRE")) {
            targetcode.add("sgt $t0, $t1, $t2");
        } else if (type.equals("LEQ")) {
            targetcode.add("sle $t0, $t1, $t2");
        } else if (type.equals("GEQ")) {
            targetcode.add("sge $t0, $t1, $t2");
        }
        //Store target
        gen_StorefReg(target,"$t0");
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
        IT_IdentItem var = table.getvar(varname,level);
        var.setLocalvaroff(this.localoffset);
        this.localoffset += 4;
        targetcode.add("sub $sp, $sp, 4");
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
        targetcode.add("sub $sp, $sp, "+suboff);
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
        this.localoffset += 4;
        targetcode.add("sub $sp, $sp, 4");
    }

    //普通变量赋值语句
    private void gen_Local_Assign_var() {
        if (codes.get(index).getOperant2().equals("getint()")) {
            targetcode.add("li $v0, 5");
            targetcode.add("syscall");
            targetcode.add("move $t1, $v0");
        } else {
            //Load num
            gen_LoadtoReg(codes.get(index).getOperant2(),"$t1");
        }
        //Store
        gen_StorefReg(codes.get(index).getOperant1(),"$t1");
    }

    //数组变量赋值语句
    private void gen_Local_Assign_arr() {
        //Load name addr
        //Para直接是地址上的值,全局是lable,局部是栈的地址
        boolean isglobal = false;
        isglobal = gen_LoadtoReg(codes.get(index).getOperant1(),"$t0");
        //Load index
        gen_LoadtoReg(codes.get(index).getOperant2(),"$t1");
        if (codes.get(index).getOperant3().equals("getint()")) {
            targetcode.add("li $v0, 5");
            targetcode.add("syscall");
            targetcode.add("move $t2, $v0");
        } else {
            //Load num
            gen_LoadtoReg(codes.get(index).getOperant3(),"$t2");
        }
        //Store
        targetcode.add("sll $t1, $t1, 2");
        targetcode.add("add $t0, $t0, $t1");
        //targetcode.add("sub $t0, $t0, $t1");要改
        targetcode.add("sw $t2, 0($t0)");
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
            gen_LoadtoReg(codes.get(index).getOperant2(),"$a0");
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
            gen_LoadtoReg(codes.get(index).getOperant1(),"$v0");
        }
        targetcode.add("j "+nowfuncname+"tail");
    }

    //代数运算语句
    private void gen_Local_Mathcal(String type) {
        //Load op1
        gen_LoadtoReg(codes.get(index).getOperant1(),"$t1");
        //Load op2
        gen_LoadtoReg(codes.get(index).getOperant2(),"$t2");
        //Calculate
        if (type.equals("ADD")) {
            targetcode.add("addu $t0, $t1, $t2");
        } else if (type.equals("SUB")) {
            targetcode.add("subu $t0, $t1, $t2");
        } else if (type.equals("MUL")) {
            targetcode.add("mul $t0, $t1, $t2");
        } else if (type.equals("DIV")) {
            targetcode.add("div $t0, $t1, $t2");
        } else if (type.equals("MOD")) {
            targetcode.add("div $t1, $t2");
            targetcode.add("mfhi $t0");
        }
        //Store target
        gen_StorefReg(codes.get(index).getOperant3(),"$t0");
    }

    //逻辑运算语句
    private void gen_Local_Logic_two(String type) {
        //Load op1
        gen_LoadtoReg(codes.get(index).getOperant1(),"$t1");
        //Calculate
        if (type.equals("NEG")) {
            targetcode.add("sub $t0, $0, $t1");
        } else if (type.equals("NOT")) {
            //跳转还没弄
            targetcode.add("seq $t0, $0, $t1");
        }
        //Store target
        gen_StorefReg(codes.get(index).getOperant3(),"$t0");
    }

    //函数调用语句
    private Stack<Integer> call_offset_stack = new Stack<>();
    private void gen_Local_Funccall() {
        targetcode.add("move $s7, $sp");
        int newspoff = midtable.funclocallen(nowfuncname)+2*this.fpra;
        targetcode.add("sub $sp, $fp, "+newspoff);
        targetcode.add("jal "+codes.get(index).getOperant1());
    }

    //Push调用函数的参数
    private void gen_Local_Funcpush(int call_offset) {
        //单独识别一下数组元素
        int offset = midtable.funclocallen(nowfuncname) + this.fpra + call_offset;
        gen_LoadtoReg(codes.get(index).getOperant1(),"$t0");
        targetcode.add("sw $t0,-"+offset+"($fp)");
    }

    //获取函数调用时的参数地址
    private void gen_Local_Getaddr() {
        //Only in Para to Func
        //Load arr addr
        //System.out.println(codes.get(index).getOperant1()+"\n"+codes.get(index).getOperant3());
        gen_LoadtoReg(codes.get(index).getOperant1(),"$t0");
        gen_StorefReg(codes.get(index).getOperant3(),"$t0");
    }

    //获取函数调用时的参数地址(二维->一维)
    private void gen_Local_Getaddr2() {
        //Only in Para to Func
        //Load arr addr
        boolean isglobal = gen_LoadtoReg(codes.get(index).getOperant1(),"$t1");
        //Load arr offset
        gen_LoadtoReg(codes.get(index).getOperant2(),"$t2");
        targetcode.add("sll $t2, $t2, 2");
        targetcode.add("add $t0, $t1, $t2");
        //targetcode.add("sub $t0, $t1, $t2");要改
        //Store
        gen_StorefReg(codes.get(index).getOperant3(),"$t0");
    }

    //获取数组元素
    private void gen_Local_Getarr() {
        //Para直接是地址上的值,全局是lable,局部是栈的地址
        //Load arrname addr
        //System.out.println(codes.get(index).toString());
        boolean isglobal = gen_LoadtoReg(codes.get(index).getOperant1(),"$t1");
        //Load len
        gen_LoadtoReg(codes.get(index).getOperant2(),"$t2");
        //Load ArrayNum
        targetcode.add("sll $t2, $t2, 2");
        targetcode.add("add $t1, $t1, $t2");
        //targetcode.add("sub $t1, $t1, $t2");要改
        targetcode.add("lw $t0, 0($t1)");
        //Store num
        gen_StorefReg(codes.get(index).getOperant3(),"$t0");
    }

}
