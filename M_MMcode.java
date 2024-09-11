import java.io.IOException;
import java.util.ArrayList;
import java.util.Stack;

public class M_MMcode {
    private IT_IdentTable table;
    private M_Midcodes midcodes;
    private N_MidItem compunit;
    private int level = 0;
    private String expans0d = "";
    private ArrayList<String> expans1d = new ArrayList<>();
    private ArrayList<ArrayList<String>> expans2d = new ArrayList<>();
    private int conans0d = 0;
    private ArrayList<Integer> conans1d = new ArrayList<>();
    private ArrayList<ArrayList<Integer>> conans2d = new ArrayList<>();
    private int tmpvarindex = 0;
    private Stack<String> storestr = new Stack<>();
    private Stack<Integer> storeint = new Stack<>();
    private boolean inconstinit = false;
    private boolean inglobalinit = false;

    public M_MMcode(N_MidItem compunit){
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

    private String gentmpvar() {
        String varname = "tmpvar" + tmpvarindex;
        tmpvarindex++;
        return varname;
    }

    //CompUint -> {Decl} {FuncDef} MainFuncDef
    private void MidCompUnint(N_MidItem Compunit) {
        ArrayList<N_Treenode> nodes = Compunit.getNodes();
        for (N_Treenode son : nodes) {
            if (son instanceof N_MidItem) {
                String type = ((N_MidItem) son).getType();
                if (type.equals("ConstDecl")) {
                    midcodes.AddLable("BConstDecl");
                    this.inconstinit = true;
                    MidConstDecl((N_MidItem) son);
                    this.inconstinit = false;
                    midcodes.AddLable("EConstDecl");
                } else if (type.equals("VarDecl")) {
                    midcodes.AddLable("BVarDecl");
                    this.inglobalinit = true;
                    MidVarDecl((N_MidItem) son);
                    this.inglobalinit = false;
                    midcodes.AddLable("EVarDecl");
                } else if (type.equals("FuncDef")) {
                    midcodes.AddLable("BFuncDef");
                    MidFuncDef((N_MidItem) son);
                    midcodes.AddLable("EFuncDef");
                } else if (type.equals("MainFuncDef")) {
                    midcodes.AddLable("BMainFunc");
                    MidMainFuncDef((N_MidItem) son);
                    midcodes.AddLable("EMainFunc");
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
                MidConstDef((N_MidItem) son);
            }
        }
        this.inconstinit = false;
    }

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
                MidVarDef((N_MidItem) node);
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

    private int StrToInt(String exp) {
        int ans = 0;
        for (int i=0;i<exp.length();i++){
            int x = exp.charAt(i) - '0';
            ans = ans * 10 + x;
        }
        return ans;
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
            if (!inconstinit && !inglobalinit) {
                midcodes.AddFuncCall(ident.getName());
            }
            tmpmulans = 0;
            tmpmulexp = gentmpvar();
            midcodes.Addint(tmpmulexp);
            midcodes.AddFuncCallRet(tmpmulexp);
            table.additem(tmpmulexp,1,2,level);
        }
    }

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
        midcodes.AddLable("BBLOCK");
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
        midcodes.AddLable("EBLOCK");
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
            } else if (sym.getType().equals(T_Typename.BREAKTK)
                    ||sym.getType().equals(T_Typename.CONTINUETK)) {

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

    private void MidIf(ArrayList<N_Treenode> nodes) {

    }

    private void MidWhile(ArrayList<N_Treenode> nodes) {

    }


}
