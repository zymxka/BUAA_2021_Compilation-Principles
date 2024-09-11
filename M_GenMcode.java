import java.io.IOException;
import java.util.ArrayList;
import java.util.Stack;

public class M_GenMcode {
    private IT_IdentTable table;//符号表
    private M_Midcodes midcodes; //中间代码
    private N_MidItem compunit; //ast
    private int level = 0; //Block层数
    //exp的计算结果
    private String expans0d = "";
    //初始化一维数组时exp的结果
    private ArrayList<String> expans1d = new ArrayList<>();
    private ArrayList<ArrayList<String>> expans2d = new ArrayList<>();
    //初始化const和全局变量时的计算结果
    private int conans0d = 0;
    private ArrayList<Integer> conans1d = new ArrayList<>();
    private ArrayList<ArrayList<Integer>> conans2d = new ArrayList<>();
    //tmpvar的命名标签
    private int tmpvarindex = 0;
    //Exp的递归保存
    private Stack<String> storestr = new Stack<>();
    private Stack<Integer> storeint = new Stack<>();
    //是否在全局变量/const的初始化中
    private boolean inconstinit = false;
    private boolean inglobalinit = false;

    //循环的标签
    private int jlableindex = 0;
    //当前的最外层循环的开始和结束lable的stack
    private Stack<String> storebelable = new Stack<>();
    private Stack<String> storeenlable = new Stack<>();

    public M_GenMcode(N_MidItem compunit){
        this.compunit = compunit;
        this.table = new IT_IdentTable();
        this.midcodes = new M_Midcodes(this.table);
        MidCompUnint(this.compunit);
    }

    public IT_IdentTable getTable() {
        return this.table;
    }

    public void printmidcode() throws IOException {
        this.midcodes.printmid();
    }

    public ArrayList<M_Quaternary> getmidcode() {
        return this.midcodes.getQuas();
    }

    //产生lable的名字
    private String gen_jlable() {
        String lablename = "jlable" + jlableindex;
        jlableindex++;
        return lablename;
    }

    //产生中间变量的名字
    private String gentmpvar() {
        String varname = "@__tmpvar" + tmpvarindex;
        tmpvarindex++;
        return varname;
    }

    //将字符串转化为int
    private int StrToInt(String exp) {
        int ans = 0;
        for (int i=0;i<exp.length();i++){
            int x = exp.charAt(i) - '0';
            ans = ans * 10 + x;
        }
        return ans;
    }

    //计算一堆element的维数
    private int cal_dim(ArrayList<N_Treenode> elements) {
        int dim = 0;
        for(N_Treenode nodes:elements) {
            if (nodes instanceof N_SymItem) {
                N_SymItem tmp = (N_SymItem) nodes;
                if (tmp.getType()== T_Typename.LBRACK) {
                    dim++;
                }
            }
        }
        return dim;
    }

    //Exp递归时的维护
    private void pushstore() {
        this.storestr.push(this.expans0d);
        this.storestr.push(this.tmpaddexp);
        this.storestr.push(this.tmpmulexp);
        this.storeint.push(this.conans0d);
        this.storeint.push(this.tmpaddans);
        this.storeint.push(this.tmpmulans);
    }

    private void popstore() {
        this.tmpmulans = this.storeint.pop();
        this.tmpaddans = this.storeint.pop();
        this.conans0d = this.storeint.pop();
        this.tmpmulexp = this.storestr.pop();
        this.tmpaddexp = this.storestr.pop();
        this.expans0d = this.storestr.pop();
    }

    //生成中间代码
    //CompUint -> {Decl} {FuncDef} MainFuncDef
    private void MidCompUnint(N_MidItem Compunit) {
        ArrayList<N_Treenode> nodes = Compunit.getNodes();
        for (N_Treenode son : nodes) {
            if (son instanceof N_MidItem) {
                String type = ((N_MidItem) son).getType();
                if (type.equals("ConstDecl")) {
                    this.inconstinit = true;
                    MidConstDecl((N_MidItem) son);
                    this.inconstinit = false;
                } else if (type.equals("VarDecl")) {
                    this.inglobalinit = true;
                    MidVarDecl((N_MidItem) son);
                    this.inglobalinit = false;
                } else if (type.equals("FuncDef")) {
                    midcodes.AddMark("BFuncDef");
                    MidFuncDef((N_MidItem) son);
                    midcodes.AddMark("EFuncDef");
                } else if (type.equals("MainFuncDef")) {
                    midcodes.AddMark("BMainFunc");
                    MidMainFuncDef((N_MidItem) son);
                    midcodes.AddMark("EMainFunc");
                }
            }
        }
    }

    //ConstDecl -> 'const' BType ConstDef { ',' ConstDef } ';'
    private void MidConstDecl(N_MidItem ConstDecl) {
        this.inconstinit = true;
        ArrayList<N_Treenode> nodes = ConstDecl.getNodes();
        for (N_Treenode son : nodes) {
            if (son instanceof N_MidItem &&
                    ((N_MidItem) son).getType().equals("ConstDef")) {
                midcodes.AddMark("BConstDecl");
                MidConstDef((N_MidItem) son);
                midcodes.AddMark("EConstDecl");
            }
        }
        this.inconstinit = false;
    }

    //ConstDef -> Ident { '[' ConstExp ']' } '=' ConstInitVal
    private void MidConstDef(N_MidItem ConstDef) {
        ArrayList<N_Treenode> nodes = ConstDef.getNodes();
        N_SymItem ident = (N_SymItem) nodes.get(0);
        N_MidItem ConstInitVal = (N_MidItem) nodes.get(nodes.size()-1);
        int dim = cal_dim(nodes);
        if (dim == 0) {
            table.additem(ident.getName(),1,1,level);
            IT_IdentItem const0d = table.getvar(ident.getName(),level);
            const0d.setDim1len(1);
            //InitValue
            MidConstInitVal(ConstInitVal,0);
            const0d.setConstvalue(conans0d);
            midcodes.AddConst(ident.getName(), conans0d);
        } else if (dim == 1) {
            table.additem(ident.getName(),3,1,level);
            IT_IdentItem const1d = table.getvar(ident.getName(),level);
            //1dlen
            N_MidItem constexp1 = (N_MidItem) nodes.get(2);
            MidConstExp(constexp1);
            int len1 = conans0d;
            const1d.setDim1len(len1);
            //InitValue
            MidConstInitVal(ConstInitVal,1);
            const1d.setConstdim1value(conans1d);
            midcodes.AddConst1d(ident.getName(), conans1d,len1);
        } else if (dim == 2) {
            table.additem(ident.getName(),4,1,level);
            IT_IdentItem const2d = table.getvar(ident.getName(),level);
            //1dlen
            N_MidItem constexp1 = (N_MidItem) nodes.get(2);
            MidConstExp(constexp1);
            int len1 = conans0d;
            const2d.setDim1len(len1);
            //2dlen
            N_MidItem constexp2 = (N_MidItem) nodes.get(5);
            MidConstExp(constexp2);
            int len2 = conans0d;
            const2d.setDim2len(len2);
            //InitValue
            MidConstInitVal(ConstInitVal,2);
            const2d.setConstdim2value(conans2d);
            midcodes.AddConst2d(ident.getName(), conans2d,len1,len2);
        }
    }

    //ConstInitVal -> ConstExp|'{' [ ConstInitVal { ',' ConstInitVal } ] '}'
    private void MidConstInitVal(N_MidItem ConstInitVal, int dim) {
        ArrayList<N_Treenode> nodes = ConstInitVal.getNodes();
        if (dim == 0) {
            this.conans0d = 0;
            N_MidItem constexp = (N_MidItem) nodes.get(0);
            MidConstExp(constexp);
        } else if (dim == 1) {
            this.conans1d = new ArrayList<>();
            for(N_Treenode node:nodes) {
                if (node instanceof N_MidItem) {
                    MidConstInitVal((N_MidItem) node,0);
                    this.conans1d.add(this.conans0d);
                }
            }
        } else if (dim == 2) {
            this.conans2d = new ArrayList<>();
            for(N_Treenode node:nodes) {
                if (node instanceof N_MidItem) {
                    MidConstInitVal((N_MidItem) node,1);
                    this.conans2d.add(this.conans1d);
                }
            }
        }
    }

    //VarDecl -> BType VarDef { ',' VarDef } ';'
    private void MidVarDecl(N_MidItem VarDecl) {
        ArrayList<N_Treenode> nodes = VarDecl.getNodes();
        for (N_Treenode node:nodes) {
            if (node instanceof N_MidItem && ((N_MidItem) node).getType().equals("VarDef")) {
                if (inglobalinit) {
                    midcodes.AddMark("BVarDecl");
                }
                MidVarDef((N_MidItem) node);
                if (inglobalinit) {
                    midcodes.AddMark("EVarDecl");
                }
            }
        }
    }

    //VarDef -> Ident { '[' ConstExp ']' }|Ident { '[' ConstExp ']' } '=' InitVal
    private void MidVarDef(N_MidItem VarDef) {
        ArrayList<N_Treenode> nodes = VarDef.getNodes();
        N_SymItem ident = (N_SymItem) nodes.get(0);
        int dim = cal_dim(nodes);
        if (dim == 0) {
            table.additem(ident.getName(),1,2,level);
        } else if (dim == 1) {
            table.additem(ident.getName(),3,2,level);
        } else if (dim == 2) {
            table.additem(ident.getName(),4,2,level);
        }
        IT_IdentItem var = table.getvar(ident.getName(),level);
        int len1=0,len2=0;
        if (dim == 0) {
            var.setDim1len(1);
        } else if (dim == 1) {
            N_MidItem constexp1 = (N_MidItem) nodes.get(2);
            MidConstExp(constexp1);
            len1 = conans0d;
            var.setDim1len(len1);
        } else if (dim == 2) {
            //1dlen
            N_MidItem constexp1 = (N_MidItem) nodes.get(2);
            MidConstExp(constexp1);
            len1 = conans0d;
            var.setDim1len(len1);
            //2dlen
            N_MidItem constexp2 = (N_MidItem) nodes.get(5);
            MidConstExp(constexp2);
            len2 = conans0d;
            var.setDim2len(len2);
        }
        if (nodes.get(nodes.size()-1) instanceof N_SymItem) {
            //NoValue
            if (dim == 0) {
                midcodes.Addint(ident.getName());
            } else if (dim == 1) {
                midcodes.Addint1d(ident.getName(),len1);
            } else if (dim == 2) {
                midcodes.Addint2d(ident.getName(),len1,len2); }
        } else if (nodes.get(nodes.size()-1) instanceof N_MidItem && inglobalinit){
            //WithValue
            N_MidItem InitVal = (N_MidItem) nodes.get(nodes.size()-1);
            if (dim == 0) {
                MidInitVal(InitVal,0);
                IT_IdentItem const0d = table.getvar(ident.getName(),level);
                const0d.setConstvalue(conans0d);
                midcodes.Addintval(ident.getName(),conans0d);
            } else if (dim == 1) {
                MidInitVal(InitVal,1);
                IT_IdentItem const1d = table.getvar(ident.getName(),level);
                const1d.setConstdim1value(conans1d);
                midcodes.Addintval1d(ident.getName(),conans1d,len1);
            } else if (dim == 2) {
                MidInitVal(InitVal,2);
                IT_IdentItem const2d = table.getvar(ident.getName(),level);
                const2d.setConstdim2value(conans2d);
                midcodes.Addinival2d(ident.getName(),conans2d,len1,len2);
            }
        } else {
            N_MidItem InitVal = (N_MidItem) nodes.get(nodes.size()-1);
            if (dim == 0) {
                MidInitVal(InitVal,0);
                midcodes.Addlocalval(ident.getName(),expans0d);
            } else if (dim == 1) {
                MidInitVal(InitVal,1);
                midcodes.Addlocalval1d(ident.getName(),expans1d,len1);
            } else if (dim == 2) {
                MidInitVal(InitVal,2);
                midcodes.Addlocalval2d(ident.getName(),expans2d,len1,len2);
            }
        }
    }

    //InitVal -> Exp | '{' [ InitVal { ',' InitVal } ] '}'
    private void MidInitVal(N_MidItem InitVal, int dim) {
        ArrayList<N_Treenode> nodes = InitVal.getNodes();
        if (dim == 0) {
            this.expans0d = "";
            this.conans0d = 0;
            N_MidItem exp = (N_MidItem) nodes.get(0);
            MidExp(exp);
        } else if (dim == 1) {
            this.expans1d = new ArrayList<>();
            this.conans1d = new ArrayList<>();
            for (N_Treenode node:nodes) {
                if (node instanceof N_MidItem) {
                    MidInitVal((N_MidItem) node,0);
                    this.expans1d.add(this.expans0d);
                    this.conans1d.add(this.conans0d);
                }
            }
        } else if (dim == 2) {
            this.expans2d = new ArrayList<>();
            this.conans2d = new ArrayList<>();
            for (N_Treenode node:nodes) {
                if (node instanceof N_MidItem) {
                    MidInitVal((N_MidItem) node,1);
                    this.expans2d.add(this.expans1d);
                    this.conans2d.add(this.conans1d);
                }
            }
        }
    }

    //FuncDef -> FuncType Ident '(' [FuncFParams] ')' Block
    private void MidFuncDef(N_MidItem FuncDef) {
        ArrayList<N_Treenode> nodes = FuncDef.getNodes();
        //SymTable
        N_SymItem FType = (N_SymItem) ((N_MidItem)nodes.get(0)).getfirst();
        String functype = "void";
        if (FType.getType() == T_Typename.INTTK) {
            functype = "int";
        } else if (FType.getType() == T_Typename.VOIDTK) {
            functype = "void";
        }
        //FuncName
        N_SymItem FName = (N_SymItem) nodes.get(1);
        if (functype.equals("int")) {
            table.additem(FName.getName(),1,4,level);
        } else {
            table.additem(FName.getName(),2,4,level);
        }
        //FuncMidCode
        midcodes.AddFuncName(FName.getName(),functype);

        //FuncParaMidCode
        if (nodes.get(3) instanceof N_MidItem) {
            N_MidItem FuncFParams = (N_MidItem) nodes.get(3);
            MidFuncFParams(FuncFParams);
        }

        //BlockMidCode
        N_MidItem Block = (N_MidItem) nodes.get(nodes.size()-1);
        MidBlock(Block);
    }

    //MainFuncDef -> 'int' 'main' '(' ')' Block
    private void MidMainFuncDef(N_MidItem MainFuncDef) {
        ArrayList<N_Treenode> nodes = MainFuncDef.getNodes();
        //SymTable
        table.additem("main",1,4,level);
        //FuncMidCode
        midcodes.AddFuncName("main","int");
        //BlockMidCode
        N_MidItem Block = (N_MidItem) nodes.get(nodes.size()-1);
        MidBlock(Block);
    }

    //FuncFParams -> FuncFParam { ',' FuncFParam }
    private void MidFuncFParams(N_MidItem FuncFParams) {
        ArrayList<N_Treenode> nodes = FuncFParams.getNodes();
        for (N_Treenode node:nodes) {
            if (node instanceof N_MidItem) {
                MidFuncFParam((N_MidItem) node);
            }
        }
    }

    //FuncFParam -> BType Ident ['[' ']' { '[' ConstExp ']' }]
    private void MidFuncFParam(N_MidItem FuncFParam) {
        ArrayList<N_Treenode> nodes = FuncFParam.getNodes();
        N_SymItem ident = (N_SymItem) nodes.get(1);
        int dim = cal_dim(nodes);

        //SymTable
        //MidCode
        if (dim == 0) {
            table.additem(ident.getName(),1,3,level+1);
            midcodes.AddFuncPara0d(ident.getName());
        } else if (dim == 1) {
            table.additem(ident.getName(),3,3,level+1);
            midcodes.AddFuncPara1d(ident.getName());
        } else if (dim == 2) {
            table.additem(ident.getName(),4,3,level+1);
            IT_IdentItem tmppara = table.getvar(ident.getName(),level+1);
            MidConstExp((N_MidItem) nodes.get(nodes.size()-2));
            tmppara.setDim2len(this.conans0d);
            midcodes.AddFuncPara2d(ident.getName(),this.conans0d);
        }
    }

    //ConstExp → AddExp
    private void MidConstExp(N_MidItem ConstExp) {
        this.inconstinit = true;
        this.conans0d = 0;
        this.expans0d = "";
        N_MidItem AddExp = (N_MidItem) ConstExp.getfirst();
        MidAddExp(AddExp);
        this.inconstinit = false;
    }

    private String condexpans = "";
    //Cond -> LOrExp
    private void MidCond(N_MidItem cond) {
        midcodes.AddMark("BMidCond");
        N_MidItem lorexp = (N_MidItem) cond.getfirst();
        this.condexpans = "";
        MidLOrExp(lorexp);
        midcodes.AddMark("EMidCond");
    }

    //LOrExp -> LAndExp | LOrExp '||' LAndExp
    private void MidLOrExp(N_MidItem lorexp) {
        ArrayList<N_Treenode> nodes = lorexp.getNodes();
        String labletrue = gen_jlable();
        String lableend = gen_jlable();
        this.condexpans = gentmpvar();
        midcodes.Addint(condexpans);
        for(int i=0;i<nodes.size();i++){
            if (i%2 == 0) {
                landexpans = "";
                midcodes.AddMark(labletrue);
                MidLAndExp((N_MidItem) nodes.get(i));
                midcodes.AddLogic("BNE", landexpans,"0",labletrue);
            }
        }
        table.additem(condexpans,1,2,level);
        midcodes.AddAssign0d(this.condexpans,"0");
        midcodes.AddJump(lableend);
        midcodes.AddLable(labletrue);
        midcodes.AddAssign0d(this.condexpans,"1");
        midcodes.AddLable(lableend);
    }

    private String landexpans = "";
    //LAndExp -> EqExp | LAndExp '&&' EqExp
    private void MidLAndExp(N_MidItem landexp) {
        ArrayList<N_Treenode> nodes = landexp.getNodes();
        String lablefalse = gen_jlable();
        String lableend = gen_jlable();
        this.landexpans = gentmpvar();
        midcodes.Addint(landexpans);
        for(int i=0;i<nodes.size();i++){
            if (i%2 == 0) {
                eqexpans = "";
                MidEqExp((N_MidItem) nodes.get(i));
                midcodes.AddLogic("BEQ", eqexpans,"0",lablefalse);
            }
        }
        table.additem(landexpans,1,2,level);
        midcodes.AddAssign0d(this.landexpans,"1");
        midcodes.AddJump(lableend);
        midcodes.AddLable(lablefalse);
        midcodes.AddAssign0d(this.landexpans,"0");
        midcodes.AddLable(lableend);
    }

    private String eqexpans = "";
    //EqExp -> RelExp | EqExp ('==' | '!=') RelExp
    private void MidEqExp(N_MidItem eqexp) {
        ArrayList<N_Treenode> nodes = eqexp.getNodes();
        String op="";
        String tmpvar = gentmpvar();
        for(int i=0;i<nodes.size();i++){
            if(i%2 == 0) {
                relexpans = "";
                MidRelExp((N_MidItem) nodes.get(i));
                if(i!=0) {
                    midcodes.Addint(tmpvar);
                    midcodes.AddLogic(op,eqexpans,relexpans,tmpvar);
                    table.additem(tmpvar,1,2,level);
                    eqexpans = tmpvar;
                    tmpvar = gentmpvar();
                } else {
                    eqexpans = relexpans;
                }
            } else {
                op = ((N_SymItem) nodes.get(i)).getName();
            }
        }
    }

    private String relexpans = "";
    //RelExp -> AddExp | RelExp ('<' | '>' | '<=' | '>=') AddExp
    private void MidRelExp(N_MidItem relexp) {
        ArrayList<N_Treenode> nodes = relexp.getNodes();
        String op = "";
        String tmpvar = gentmpvar();
        for(int i=0;i<nodes.size();i++){
            if(i%2 == 0) {
                this.expans0d = "";
                MidAddExp((N_MidItem) nodes.get(i));
                if(i!=0) {
                    midcodes.Addint(tmpvar);
                    midcodes.AddLogic(op,relexpans,expans0d,tmpvar);
                    table.additem(tmpvar,1,2,level);
                    relexpans = tmpvar;
                    tmpvar = gentmpvar();
                } else {
                    relexpans = expans0d;
                }
            } else {
                op = ((N_SymItem) nodes.get(i)).getName();
            }
        }
    }

    //Exp -> AddExp
    private void MidExp(N_MidItem Exp) {
        this.conans0d = 0;
        this.expans0d = "";
        N_MidItem AddExp = (N_MidItem) Exp.getfirst();
        MidAddExp(AddExp);
    }

    private int tmpaddans = 0;
    private String tmpaddexp = "";//MulExp修改的
    //AddExp -> MulExp | AddExp ('+' | '−') MulExp
    //AddExp -> MulExp {('+'|'-') MulExp}
    private void MidAddExp(N_MidItem AddExp) {
        ArrayList<N_Treenode> nodes = AddExp.getNodes();
        String tmpop = "";
        String tmpvar = gentmpvar();
        for (int i=0;i<nodes.size();i++) {
            if (i%2 == 0) {
                tmpaddans = 0;
                tmpaddexp = "";
                MidMulExp((N_MidItem) nodes.get(i));
                if (i != 0) {
                    //MidCode
                    if (!inconstinit && !inglobalinit) {
                        midcodes.Addint(tmpvar);
                        midcodes.AddAddExp(tmpop,expans0d,tmpaddexp,tmpvar);
                        table.additem(tmpvar,1,2,level);
                    }
                    expans0d = tmpvar;
                    tmpvar = gentmpvar();
                    //ConstNum
                    if (tmpop.equals("-")) {
                        conans0d = conans0d - tmpaddans;
                    } else {
                        conans0d = conans0d + tmpaddans;
                    }
                } else {
                    expans0d = tmpaddexp;
                    conans0d = tmpaddans;
                }
            } else {
                tmpop = ((N_SymItem) nodes.get(i)).getName();
            }
        }
    }

    private int tmpmulans = 0;
    private String tmpmulexp = ""; //UnaryExp修改的值
    //MulExp -> UnaryExp | MulExp ('*' | '/' | '%') UnaryExp
    //MulExp -> UnaryExp {('*'|'/'|'%') UnaryExp}
    private void MidMulExp(N_MidItem MulExp) {
        ArrayList<N_Treenode> nodes = MulExp.getNodes();
        String tmpop = "";
        String tmpvar = gentmpvar();
        for (int i=0;i<nodes.size();i++) {
            if (i % 2 == 0) {
                tmpmulans = 0;
                tmpmulexp = "";
                MidUnaryExp((N_MidItem) nodes.get(i));
                if (i != 0) {
                    //MidCode
                    if (!inconstinit && !inglobalinit){
                        midcodes.Addint(tmpvar);
                        midcodes.AddMulExp(tmpop,tmpaddexp,tmpmulexp,tmpvar);
                        table.additem(tmpvar,1,2,level);
                    }
                    tmpaddexp = tmpvar;
                    tmpvar = gentmpvar();
                    //ConstNum
                    if (tmpop.equals("*")) {
                        tmpaddans = tmpaddans * tmpmulans;
                    } else if (tmpop.equals("/") && tmpmulans != 0) {
                        tmpaddans = tmpaddans / tmpmulans;
                    } else if (tmpop.equals("%") && tmpmulans != 0) {
                        tmpaddans = tmpaddans % tmpmulans;
                    }
                } else {
                    tmpaddexp = tmpmulexp;
                    tmpaddans = tmpmulans;
                }
            } else {
                tmpop = ((N_SymItem) nodes.get(i)).getName();
            }
        }
    }

    //LVal -> Ident {'[' Exp ']'}
    public void MidLVal(N_MidItem LVal) {
        ArrayList<N_Treenode> nodes = LVal.getNodes();
        N_SymItem ident = (N_SymItem) nodes.get(0);
        IT_IdentItem var = table.getvar(ident.getName(),level);
        int dim = cal_dim(nodes);
        if ((inconstinit || inglobalinit)) {
            if (dim == 0) {
                this.tmpmulans = var.getConstvalue();
                this.tmpmulexp = tmpmulans+"";
            } else if (dim == 1) {
                pushstore();
                MidExp((N_MidItem) nodes.get(2));
                int len = this.conans0d;
                popstore();
                this.tmpmulans = var.getConstvalue1d(len);
                this.tmpmulexp = tmpmulans+"";
            } else if (dim == 2) {
                pushstore();
                MidExp((N_MidItem) nodes.get(2));
                int len1 = this.conans0d;
                popstore();
                pushstore();
                MidExp((N_MidItem) nodes.get(5));
                int len2 = this.conans0d;
                popstore();
                this.tmpmulans = var.getConstvalue2d(len1,len2);
                this.tmpmulexp = tmpmulans+"";
            }
        } else {
            int vardim = var.getClas();
            if (dim == 0 && vardim == 1) {
                this.tmpmulans = 0;
                this.tmpmulexp = ident.getName();
            } else if (dim == 0 && (vardim == 3 || vardim == 4)) {
                this.tmpmulans = 0;
                this.tmpmulexp = gentmpvar();
                midcodes.Addint(this.tmpmulexp);
                table.additem(this.tmpmulexp,1,2,level);
                midcodes.AddArrAddr1d(ident.getName(),tmpmulexp);
            }else if (dim == 1 && vardim == 3) {
                pushstore();
                MidExp((N_MidItem) nodes.get(2));
                String index = this.expans0d;
                popstore();
                this.tmpmulans = 0;
                this.tmpmulexp = gentmpvar();
                midcodes.Addint(this.tmpmulexp);
                table.additem(this.tmpmulexp,1,2,level);
                midcodes.AddgetArr1d(ident.getName(),index,this.tmpmulexp);
            } else if (dim == 1 && vardim == 4) {
                pushstore();
                MidExp((N_MidItem) nodes.get(2));
                String index1 = this.expans0d;
                popstore();
                this.tmpmulans = 0;
                this.tmpmulexp = gentmpvar();
                String len = var.getDim2len() +"";
                midcodes.Addint(tmpmulexp);
                table.additem(this.tmpmulexp,1,2,level);
                midcodes.AddArrAddr2d(ident.getName(), index1,
                        len,this.tmpmulexp,this.level);
            } else if (dim == 2) {
                pushstore();
                MidExp((N_MidItem) nodes.get(2));
                String index1 = this.expans0d;
                popstore();
                pushstore();
                MidExp((N_MidItem) nodes.get(5));
                String index2 = this.expans0d;
                popstore();
                this.tmpmulans = 0;
                this.tmpmulexp = gentmpvar();
                midcodes.Addint(this.tmpmulexp);
                table.additem(this.tmpmulexp,1,2,level);
                String len = var.getDim2len() +"";
                midcodes.AddgetArr2d(ident.getName(),
                        index1,index2,len,this.tmpmulexp,this.level);
            }
        }
    }

    //PrimaryExp -> '(' Exp ')' | LVal | Number
    private void MidPrimaryExp(N_MidItem PrimaryExp) {
        ArrayList<N_Treenode> nodes = PrimaryExp.getNodes();
        if (nodes.get(0) instanceof N_MidItem) {
            N_MidItem midItem = (N_MidItem) nodes.get(0);
            if (midItem.getType().equals("LVal")) {
                MidLVal(midItem);
            } else {
                tmpmulexp = ((N_SymItem) midItem.getfirst()).getName();
                tmpmulans = StrToInt(tmpmulexp);
            }
        } else {
            pushstore();
            MidExp((N_MidItem) nodes.get(1));
            int tmpans = this.conans0d;
            String tmpvar = this.expans0d;
            popstore();
            this.tmpmulexp = tmpvar;
            this.tmpmulans = tmpans;
        }
    }

    //UnaryExp -> PrimaryExp | Ident '(' [FuncRParams] ')'| UnaryOp UnaryExp
    //UnaryOp → '+' | '−' | '!'
    private void MidUnaryExp(N_MidItem UnaryExp) {
        ArrayList<N_Treenode> nodes = UnaryExp.getNodes();
        if (nodes.get(0) instanceof N_MidItem) {
            if (nodes.size() == 1) {
                MidPrimaryExp((N_MidItem) nodes.get(0));
            } else {
                N_MidItem op = ((N_MidItem) nodes.get(0));
                String sign = ((N_SymItem)op.getfirst()).getName();
                //System.out.println(sign);
                MidUnaryExp((N_MidItem) nodes.get(1));
                if (sign.equals("-")) {
                    String tmpunaryvar = gentmpvar();
                    if (!inconstinit && !inglobalinit){
                        midcodes.Addint(tmpunaryvar);
                        midcodes.AddUnaryop(sign,tmpunaryvar,tmpmulexp);
                        table.additem(tmpunaryvar,1,2,level);
                    }
                    tmpmulexp = tmpunaryvar;
                    tmpmulans = -tmpmulans;
                } else if (sign.equals("!")) {
                    String tmpunaryvar = gentmpvar();
                    if (!inconstinit && !inglobalinit) {
                        midcodes.Addint(tmpunaryvar);
                        midcodes.AddUnaryop(sign,tmpunaryvar,tmpmulexp);
                        table.additem(tmpunaryvar,1,2,level);
                    }
                    tmpmulexp = tmpunaryvar;
                    if (tmpmulans != 0) {
                        tmpmulans = 0;
                    } else {
                        tmpmulans = 1;
                    }
                }
            }
        } else {
            //CallFunc
            N_SymItem ident = (N_SymItem) nodes.get(0);
            if (nodes.get(2) instanceof N_MidItem) {
                MidFuncRParams((N_MidItem) nodes.get(2));
            }
            /*if (!inconstinit && !inglobalinit) {
                midcodes.AddFuncCall(ident.getName());
            }*/
            midcodes.AddFuncCall(ident.getName());
            tmpmulans = 0;
            tmpmulexp = gentmpvar();
            midcodes.Addint(tmpmulexp);
            midcodes.AddFuncCallRet(tmpmulexp);
            table.additem(tmpmulexp,1,2,level);
        }
    }

    //FuncRParams -> Exp { ',' Exp }
    private void MidFuncRParams(N_MidItem FuncRParams) {
        ArrayList<N_Treenode> nodes = FuncRParams.getNodes();
        ArrayList<String> pushrequest = new ArrayList<>();
        for (N_Treenode node : nodes) {
            if (node instanceof N_MidItem) {
                pushstore();
                MidExp((N_MidItem) node);
                pushrequest.add(this.expans0d);
                popstore();
            }
        }
        for (String push :pushrequest) {
            midcodes.AddFuncRP(push);
        }
    }

    private void MidBlock(N_MidItem Block) {
        this.level++;
        midcodes.AddMark("BBLOCK");
        ArrayList<N_Treenode> nodes = Block.getNodes();
        for (N_Treenode node:nodes) {
            if (node instanceof N_MidItem) {
                String type = ((N_MidItem)((N_MidItem) node).getfirst()).getType();
                if (type.equals("ConstDecl")) {
                    MidConstDecl((N_MidItem)((N_MidItem) node).getfirst());
                } else if (type.equals("VarDecl")) {
                    MidVarDecl((N_MidItem)((N_MidItem) node).getfirst());
                } else if (type.equals("Stmt")) {
                    MidStmt((N_MidItem)((N_MidItem) node).getfirst());
                }
            }
        }
        midcodes.AddMark("EBLOCK");
        table.poplevel(level);
        this.level--;
    }

    private void MidStmt(N_MidItem Stmt) {
        ArrayList<N_Treenode> nodes = Stmt.getNodes();
        N_Treenode symbol = nodes.get(0);
        if (symbol instanceof N_SymItem) {
            N_SymItem sym = (N_SymItem) symbol;
            if (sym.getType().equals(T_Typename.RETURNTK)) {
                MidRet(nodes);
            } else if (sym.getType().equals(T_Typename.PRINTFTK)) {
                MidPrint(nodes);
            } else if (sym.getType().equals(T_Typename.IFTK)) {
                MidIf(nodes);
            } else if (sym.getType().equals(T_Typename.WHILETK)) {
                MidWhile(nodes);
            } else if (sym.getType().equals(T_Typename.BREAKTK)) {
                MidBreak();
            } else if (sym.getType().equals(T_Typename.CONTINUETK)) {
                MidContinue();
            }
        } else if (symbol instanceof N_MidItem) {
            N_MidItem mid = (N_MidItem) symbol;
            if (mid.getType().equals("LVal")) {
                int dim = cal_dim(mid.getNodes());
                ArrayList<N_Treenode> midnodes = mid.getNodes();
                String lvalname = ((N_SymItem)mid.getfirst()).getName();
                String fexp = "";
                if (nodes.get(2) instanceof N_MidItem) {
                    MidExp((N_MidItem) nodes.get(2));
                    fexp = this.expans0d;
                } else {
                    fexp = "getint()";
                }
                if (dim == 0) {
                    midcodes.AddAssign0d(lvalname,fexp);
                } else if (dim == 1) {
                    MidExp((N_MidItem) midnodes.get(2));
                    String index1 = this.expans0d;
                    midcodes.AddAssign1d(lvalname,fexp,index1);
                } else if (dim == 2) {
                    MidExp((N_MidItem) midnodes.get(2));
                    String index1 = this.expans0d;
                    MidExp((N_MidItem) midnodes.get(5));
                    String index2 = this.expans0d;
                    int len = table.getvar(lvalname,level).getDim2len();
                    midcodes.AddAssign2d(lvalname,fexp,index1,
                            index2,len,this.level);
                }
            } else if (mid.getType().equals("Block")) {
                MidBlock(mid);
            } else if (mid.getType().equals("Exp")) {
                MidExp(mid);
            }
        }
    }

    //'return' [Exp] ';'
    private void MidRet(ArrayList<N_Treenode> nodes) {
        if (nodes.get(1) instanceof N_MidItem) {
            N_MidItem exp = (N_MidItem) nodes.get(1);
            MidExp(exp);
            midcodes.AddRetVal(this.expans0d);
        } else {
            midcodes.AddRetVal("");
        }
    }

    //'printf''('FormatString{','Exp}')'';'
    private void MidPrint(ArrayList<N_Treenode> nodes) {
        String fstr = ((N_SymItem)nodes.get(2)).getName();
        ArrayList<String> exps = new ArrayList<>();
        for (N_Treenode node:nodes) {
            if (node instanceof N_MidItem) {
                MidExp((N_MidItem) node);
                exps.add(this.expans0d);
            }
        }
        midcodes.AddPrintf(fstr,exps);
    }

    private String condendlable = "";
    //'if' '(' Cond ')' Stmt [ 'else' Stmt ]
    private void MidIf(ArrayList<N_Treenode> nodes) {
        String lable1 = gen_jlable();
        this.condendlable = lable1;
        N_MidItem cond = (N_MidItem) nodes.get(2);
        MidCond(cond);
        String cond_ans = this.condexpans;
        if (nodes.size() == 5) {
            //no else
            midcodes.AddLogic("BEQ",cond_ans,"0",lable1);
            MidStmt((N_MidItem) nodes.get(4));
            midcodes.AddLable(lable1);
        } else {
            //with else
            String lable2 = gen_jlable();
            midcodes.AddLogic("BEQ",cond_ans,"0",lable1);
            MidStmt((N_MidItem) nodes.get(4));
            midcodes.AddJump(lable2);
            midcodes.AddLable(lable1);
            MidStmt((N_MidItem) nodes.get(6));
            midcodes.AddLable(lable2);
        }
        this.condendlable = "";
    }

    //'while' '(' Cond ')' Stmt
    private void MidWhile(ArrayList<N_Treenode> nodes) {
        String lablebegin = gen_jlable();
        String lableend = gen_jlable();
        this.condendlable = lableend;
        this.storebelable.push(lablebegin);
        this.storeenlable.push(lableend);
        midcodes.AddLable(lablebegin);
        N_MidItem cond = (N_MidItem) nodes.get(2);
        MidCond(cond);
        String cond_ans = this.condexpans;
        midcodes.AddLogic("BEQ",cond_ans,"0",lableend);
        N_MidItem stmt = (N_MidItem) nodes.get(4);
        MidStmt(stmt);
        midcodes.AddJump(lablebegin);
        midcodes.AddLable(lableend);
        this.storebelable.pop();
        this.storeenlable.pop();
        this.condendlable = "";
    }

    private void MidBreak() {
        String toend = this.storeenlable.peek();
        midcodes.AddJump(toend);
    }

    private void MidContinue() {
        String tobegin = this.storebelable.peek();
        midcodes.AddJump(tobegin);
    }

}
