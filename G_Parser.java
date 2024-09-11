import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class G_Parser {
    private ArrayList<N_SymItem> words = new ArrayList<>();
    private ArrayList<String> anawords = new ArrayList<>();
    private int index;
    private String FuncTypeNow = null;
    private int level;
    private N_MidItem compuint;
    private N_SymItem tmp = null;

    public G_Parser(ArrayList<N_SymItem> gene) {
        this.words.addAll(gene);
        this.index = 0;
        this.level = 0; //用来判断重名
        this.compuint = new N_MidItem("CompUnit");
    }

    public N_MidItem getCompuint() {
        return this.compuint;
    }

    public void print_words() throws IOException {
        BufferedWriter bout = new BufferedWriter(new FileWriter("output.txt"));
        for(int i=0;i<anawords.size();i++){
            bout.write(anawords.get(i));
            bout.newLine();
        }
        bout.close();
    }

    public void print_comp() throws IOException {
        FileWriter writer = new FileWriter("output.txt");
        writer.write(compuint.toString());
        writer.flush();
        writer.close();
    }

    private N_SymItem getword() {
        N_SymItem ans = this.words.get(this.index);
        this.index++;
        return ans;
    }

    private void addword() {
        anawords.add(tmp.getType().toString()+" "+tmp.getName());
        //System.out.println(tmp.getType().toString()+" "+tmp.getName());
    }

    //CompUint -> {Decl} {FuncDef} MainFuncDef
    //Addnode
    public void CompUnit() {
        this.tmp = getword();
        //{Decl}
        while ((this.tmp.getType() == T_Typename.CONSTTK)
                || (this.tmp.getType() == T_Typename.INTTK
                && this.words.get(this.index+1).getType() != T_Typename.LPARENT)) {
            if (this.tmp.getType() == T_Typename.CONSTTK) {
                this.index--;
                N_MidItem constdecl = new N_MidItem("ConstDecl");
                ConstDecl(constdecl);
                this.compuint.addnode(constdecl);
            } else {
                this.index--;
                N_MidItem vardecl = new N_MidItem("VarDecl");
                VarDecl(vardecl);
                this.compuint.addnode(vardecl);
            }
            tmp = getword();
        }

        //{FuncDef}
        while ((this.tmp.getType() == T_Typename.INTTK || this.tmp.getType() == T_Typename.VOIDTK)
                && (this.words.get(this.index).getType() != T_Typename.MAINTK)
                && (this.words.get(this.index+1).getType() == T_Typename.LPARENT)) {
            this.index--;
            N_MidItem funcdef = new N_MidItem("FuncDef");
            FuncDef(funcdef);
            this.compuint.addnode(funcdef);
            tmp = getword();
        }

        //MainFuncDef
        if ((this.tmp.getType() == T_Typename.INTTK)
                && (this.words.get(this.index).getType() == T_Typename.MAINTK)
                && (this.words.get(this.index+1).getType() == T_Typename.LPARENT)) {
            this.index--;
            N_MidItem mainfuncdef = new N_MidItem("MainFuncDef");
            MainFuncDef(mainfuncdef);
            this.compuint.addnode(mainfuncdef);
        }

        anawords.add("<CompUnit>");
    }

    //ConstDecl -> 'const' 'int' ConstDef {',' ConstDef} ;
    private void ConstDecl(N_MidItem constdecl) {
        // const
        tmp = getword();
        constdecl.addnode(tmp);
        addword();

        // int
        tmp = getword();
        constdecl.addnode(tmp);
        addword();

        //ConstDef {',' ConstDef}
        do {
            N_MidItem constdef = new N_MidItem("ConstDef");
            ConstDef(constdef);
            constdecl.addnode(constdef);
            tmp = getword();
            if (tmp.getType() == T_Typename.COMMA) {
                constdecl.addnode(tmp);
                addword();
            }
        } while (tmp.getType() == T_Typename.COMMA);

        // ;
        if (tmp.getType() != T_Typename.SEMICN) {
            E_Error.adderror("i",words.get(index-2).getLine());
            index--;
        } else{
            constdecl.addnode(tmp);
            addword();
        }
        anawords.add("<ConstDecl>");
    }

    //ConstDef -> Ident {'[' ConstExp ']'} '=' ConstInitVal
    private void ConstDef(N_MidItem constdef) {
        //Ident
        tmp = getword();
        constdef.addnode(tmp);
        addword();

        tmp = getword();
        // {'[' ConstExp ']'}
        while (tmp.getType() == T_Typename.LBRACK) {
            constdef.addnode(tmp);//[
            addword();
            N_MidItem constExp = new N_MidItem("ConstExp");
            ConstExp(constExp);
            constdef.addnode(constExp);//ConstExp
            tmp = getword();
            if (tmp.getType() == T_Typename.RBRACK) {
                constdef.addnode(tmp); //]
                addword();
            }else {
                E_Error.adderror("k",this.words.get(index-2).getLine());
                index--;
            }
            tmp = getword();
        }

        // '='
        if (tmp.getType() == T_Typename.ASSIGN) {
            constdef.addnode(tmp);
            addword();
        }

        //ConstInitVal
        N_MidItem constinitval = new N_MidItem("ConstInitVal");
        ConstInitVal(constinitval);
        constdef.addnode(constinitval);

        anawords.add("<ConstDef>");
    }

    //ConstExp -> AddExp
    private void ConstExp(N_MidItem constexp) {
        N_MidItem addexp = new N_MidItem("AddExp");
        AddExp(addexp);
        constexp.addnode(addexp);
        anawords.add("<ConstExp>");
    }

    //ConstInitVal -> ConstExp
    // | '{' [ConstInitVal {',' ConstInitVal}] '}'
    private void ConstInitVal(N_MidItem constinitval) {
        tmp = getword();
        if (tmp.getType() != T_Typename.LBRACE) {
            this.index--;
            N_MidItem constexp = new N_MidItem("ConstExp");
            ConstExp(constexp);
            constinitval.addnode(constexp);
        } else {
            constinitval.addnode(tmp);
            addword();
            tmp = getword();
            if (tmp.getType() != T_Typename.RBRACE) {
                this.index--;
                N_MidItem constinitval1 = new N_MidItem("ConstInitVal");
                ConstInitVal(constinitval1);
                constinitval.addnode(constinitval1);
                tmp = getword();
                while (tmp.getType() == T_Typename.COMMA) {
                    constinitval.addnode(tmp);
                    addword();
                    N_MidItem constinitval2 = new N_MidItem("ConstInitVal");
                    ConstInitVal(constinitval2);
                    constinitval.addnode(constinitval2);
                    tmp = getword();
                }
            }
            if (tmp.getType() == T_Typename.RBRACE) {
                constinitval.addnode(tmp);
                addword();
            }
        }
        anawords.add("<ConstInitVal>");
    }

    //VarDecl -> BType VarDef {',' VarDef} ';'
    private void VarDecl(N_MidItem vardecl) {
        // int
        tmp = getword();
        vardecl.addnode(tmp);
        addword();

        //VarDef {',' VarDef}
        do {
            N_MidItem vardef = new N_MidItem("VarDef");
            VarDef(vardef);
            vardecl.addnode(vardef);
            tmp = getword();
            if (tmp.getType() == T_Typename.COMMA) {
                vardecl.addnode(tmp);
                addword();
            }
        } while (tmp.getType() == T_Typename.COMMA);

        // ;
        if (tmp.getType() != T_Typename.SEMICN) {
            E_Error.adderror("i",words.get(index-2).getLine());
            index--;
        } else{
            vardecl.addnode(tmp);
            addword();
        }

        anawords.add("<VarDecl>");
    }

    //VarDef -> Ident { '[' ConstExp ']' }
    // | Ident { '[' ConstExp ']' } '=' InitVal
    private void VarDef(N_MidItem vardef) {
        //Ident
        tmp = getword();
        vardef.addnode(tmp);
        addword();

        tmp = getword();
        //{'[' ConstExp ']'}
        while (tmp.getType() == T_Typename.LBRACK) {
            vardef.addnode(tmp); //[
            addword();
            N_MidItem constexp = new N_MidItem("ConstExp");
            ConstExp(constexp);
            vardef.addnode(constexp);
            tmp = getword();
            if (tmp.getType() == T_Typename.RBRACK) {
                vardef.addnode(tmp); //]
                addword();
            }else {
                E_Error.adderror("k",this.words.get(index-2).getLine());
                index--;
            }
            tmp = getword();
        }

        // '=' InitVal
        if (tmp.getType() == T_Typename.ASSIGN) {
            vardef.addnode(tmp);
            addword();
            N_MidItem initval = new N_MidItem("InitVal");
            InitVal(initval);
            vardef.addnode(initval);
        } else {
            this.index--;
        }

        anawords.add("<VarDef>");
    }

    //InitVal → Exp | '{' [ InitVal { ',' InitVal } ] '}'
    private void InitVal(N_MidItem initval) {
        tmp = getword();
        if (tmp.getType() != T_Typename.LBRACE) {
            this.index--;
            N_MidItem exp = new N_MidItem("Exp");
            Exp(exp);
            initval.addnode(exp);
        } else {
            initval.addnode(tmp);
            addword();
            tmp = getword();
            if (tmp.getType() != T_Typename.RBRACE) {
                this.index--;
                N_MidItem initval1 = new N_MidItem("InitVal");
                InitVal(initval1);
                initval.addnode(initval1);
                tmp = getword();
                while (tmp.getType() == T_Typename.COMMA) {
                    initval.addnode(tmp);
                    addword();
                    N_MidItem initval2 = new N_MidItem("InitVal");
                    InitVal(initval2);
                    initval.addnode(initval2);
                    tmp = getword();
                }
            }
            if (tmp.getType() == T_Typename.RBRACE) {
                initval.addnode(tmp);
                addword();
            }
         }
        anawords.add("<InitVal>");
    }

    //Exp -> AddExp
    private void Exp(N_MidItem exp) {
        N_MidItem addexp = new N_MidItem("AddExp");
        AddExp(addexp);
        exp.addnode(addexp);
        anawords.add("<Exp>");
    }

    //FuncDef -> FuncType Ident '(' [FuncFParams] ')' Block
    private void FuncDef(N_MidItem funcdef) {
        tmp = getword();
        N_MidItem functype = new N_MidItem("FuncType");
        functype.addnode(tmp);
        funcdef.addnode(functype);
        addword();
        if (tmp.getType() == T_Typename.INTTK) {
            anawords.add("<FuncType>");
            this.FuncTypeNow = "int";
        } else if (tmp.getType() == T_Typename.VOIDTK) {
            anawords.add("<FuncType>");
            this.FuncTypeNow = "void";
        }

        //Ident
        tmp = getword();
        funcdef.addnode(tmp);
        addword();

        //'(' [FuncFParams] ')'
        tmp = getword();
        funcdef.addnode(tmp); //(
        addword();
        tmp = getword();
        if (tmp.getType() != T_Typename.RPARENT && tmp.getType() != T_Typename.LBRACE) {
            this.index--;
            N_MidItem funcfparams = new N_MidItem("FuncFParams");
            FuncFParams(funcfparams);
            funcdef.addnode(funcfparams);
            tmp = getword();
        }
        if (tmp.getType() == T_Typename.RPARENT) {
            funcdef.addnode(tmp);//)
            addword();
        } else {
            E_Error.adderror("j",words.get(index-2).getLine());
            index--;
        }

        //Block
        N_MidItem block = new N_MidItem("Block");
        Block(block);
        funcdef.addnode(block);

        this.FuncTypeNow = null;
        anawords.add("<FuncDef>");
    }

    //FuncFParams -> FuncFParam { ',' FuncFParam }
    private void FuncFParams(N_MidItem funcfparams) {
        do {
            N_MidItem funcfparam = new N_MidItem("FuncFParam");
            FuncFParam(funcfparam);
            funcfparams.addnode(funcfparam);
            tmp = getword();
            if (tmp.getType() == T_Typename.COMMA) {
                funcfparams.addnode(tmp);
                addword();
            }
        } while (tmp.getType() == T_Typename.COMMA);
        index--;
        anawords.add("<FuncFParams>");
    }

    //FuncFParam → BType Ident ['[' ']' { '[' ConstExp ']' }]
    private void FuncFParam(N_MidItem funcfparam) {
        //BType
        tmp = getword();
        funcfparam.addnode(tmp);
        addword();

        //Ident
        tmp = getword();
        funcfparam.addnode(tmp);
        addword();

        //['[' ']' { '[' ConstExp ']' }]
        tmp = getword();
        if (tmp.getType() == T_Typename.LBRACK) {
            funcfparam.addnode(tmp);
            addword();
            //']'
            tmp = getword();
            if (tmp.getType() == T_Typename.RBRACK) {
                funcfparam.addnode(tmp); //]
                addword();
            }else {
                E_Error.adderror("k",this.words.get(index-2).getLine());
                index--;
            }
            //{ '[' ConstExp ']' }
            tmp = getword();
            if (tmp.getType() == T_Typename.LBRACK) {
                while (tmp.getType() == T_Typename.LBRACK) {
                    funcfparam.addnode(tmp);
                    addword();
                    N_MidItem constexp = new N_MidItem("ConstExp");
                    ConstExp(constexp);
                    funcfparam.addnode(constexp);
                    //']'
                    tmp = getword();
                    if (tmp.getType() == T_Typename.RBRACK) {
                        funcfparam.addnode(tmp); //]
                        addword();
                    }else {
                        E_Error.adderror("k",this.words.get(index-2).getLine());
                        index--;
                    }
                    tmp = getword();
                }
            }
        }
        index--;

        anawords.add("<FuncFParam>");
    }

    //MainFuncDef -> 'int' 'main' '(' ')' Block
    private void MainFuncDef(N_MidItem mainfuncdef) {
        //'int'
        tmp = getword();
        mainfuncdef.addnode(tmp);
        addword();
        this.FuncTypeNow = "int";

        //'main'
        tmp = getword();
        mainfuncdef.addnode(tmp);
        addword();

        //'('
        tmp = getword();
        mainfuncdef.addnode(tmp);
        addword();

        //')'
        tmp = getword();
        if (tmp.getType() == T_Typename.RPARENT) {
            mainfuncdef.addnode(tmp); //)
            addword();
        }else {
            E_Error.adderror("j",this.words.get(index-2).getLine());
            index--;
        }

        N_MidItem block = new N_MidItem("Block");
        Block(block);
        mainfuncdef.addnode(block);

        this.FuncTypeNow = null;
        anawords.add("<MainFuncDef>");
    }

    //Block -> '{' {BlockItem} '}'
    private void Block(N_MidItem block) {
        //'{'
        tmp = getword();
        block.addnode(tmp);
        addword();

        tmp = getword();
        while (tmp.getType() != T_Typename.RBRACE) {
            index--;
            N_MidItem blockitem = new N_MidItem("BlockItem");
            BlockItem(blockitem);
            block.addnode(blockitem);
            tmp = getword();
        }

        //'}'
        if (tmp.getType() == T_Typename.RBRACE) {
            block.addnode(tmp);
            addword();
        }

        anawords.add("<Block>");
    }

    //BlockItem -> ConstDecl | VarDecl | Stmt
    private void BlockItem(N_MidItem blockitem) {
        tmp = getword();
        if (tmp.getType() == T_Typename.CONSTTK) {
            this.index--;
            N_MidItem constdecl = new N_MidItem("ConstDecl");
            ConstDecl(constdecl);
            blockitem.addnode(constdecl);
        }else if (tmp.getType() == T_Typename.INTTK
                && this.words.get(this.index+1).getType() != T_Typename.LPARENT) {
            this.index--;
            N_MidItem vardecl = new N_MidItem("VarDecl");
            VarDecl(vardecl);
            blockitem.addnode(vardecl);
        } else {
            this.index--;
            N_MidItem stmt = new N_MidItem("Stmt");
            Stmt(stmt);
            blockitem.addnode(stmt);
        }
    }

    private void Cond(N_MidItem cond) {
        N_MidItem lorexp = new N_MidItem("LOrExp");
        LOrExp(lorexp);
        cond.addnode(lorexp);
        anawords.add("<Cond>");
    }

    //LOrExp -> LAndExp | LOrExp '||' LAndExp
    //LOrExp -> LAndExp {'||' LAndExp}
    private void LOrExp(N_MidItem lorexp) {
        do {
            N_MidItem landexp = new N_MidItem("LAndExp");
            LAndExp(landexp);
            lorexp.addnode(landexp);
            anawords.add("<LOrExp>");
            tmp = getword();
            if (tmp.getType() == T_Typename.OR) {
                lorexp.addnode(tmp);
                addword();
            }
        } while (tmp.getType() == T_Typename.OR);
        this.index--;
    }

    //LAndExp -> EqExp | LAndExp '&&' EqExp
    //LAndExp -> EqExp {'&&' EqExp}
    private void LAndExp(N_MidItem landexp) {
        do {
            N_MidItem eqexp = new N_MidItem("EqExp");
            EqExp(eqexp);
            landexp.addnode(eqexp);
            anawords.add("<LAndExp>");
            tmp = getword();
            if (tmp.getType() == T_Typename.AND) {
                landexp.addnode(tmp);
                addword();
            }
        } while (tmp.getType() == T_Typename.AND);
        this.index--;
    }

    //EqExp -> RelExp | EqExp ('==' | '!=') RelExp
    //EqExp -> RelExp {('=='|'!=') RelExp}
    private void EqExp(N_MidItem eqexp) {
        do {
            N_MidItem relexp = new N_MidItem("RelExp");
            RelExp(relexp);
            eqexp.addnode(relexp);
            anawords.add("<EqExp>");
            tmp = getword();
            if (tmp.getType() == T_Typename.EQL ||
                tmp.getType() == T_Typename.NEQ) {
                eqexp.addnode(tmp);
                addword();
            }
        } while (tmp.getType() == T_Typename.EQL ||
                tmp.getType() == T_Typename.NEQ);
        this.index--;
    }

    //RelExp -> AddExp | RelExp ('<' | '>' | '<=' | '>=') AddExp
    //RelExp -> AddExp {('<'|'>'|'<='|'>=') AddExp}
    private void RelExp(N_MidItem relexp) {
        do {
            N_MidItem addexp = new N_MidItem("AddExp");
            AddExp(addexp);
            relexp.addnode(addexp);
            anawords.add("<RelExp>");
            tmp = getword();
            if (tmp.getType() == T_Typename.LSS ||
                tmp.getType() == T_Typename.LEQ ||
                tmp.getType() == T_Typename.GRE ||
                tmp.getType() == T_Typename.GEQ) {
                relexp.addnode(tmp);
                addword();
            }
        } while (tmp.getType() == T_Typename.LSS ||
                tmp.getType() == T_Typename.LEQ ||
                tmp.getType() == T_Typename.GRE ||
                tmp.getType() == T_Typename.GEQ);
        this.index--;
    }

    //AddExp -> MulExp | AddExp ('+'|'-') MulExp
    //AddExp -> MulExp {('+'|'-') MulExp}
    private void AddExp(N_MidItem addexp) {
        do {
            N_MidItem mulexp = new N_MidItem("MulExp");
            MulExp(mulexp);
            addexp.addnode(mulexp);
            anawords.add("<AddExp>");
            tmp = getword();
            if (tmp.getType() == T_Typename.PLUS ||
            tmp.getType() == T_Typename.MINU) {
                addexp.addnode(tmp);
                addword();
            }
        } while (tmp.getType() == T_Typename.PLUS ||
                tmp.getType() == T_Typename.MINU);
        this.index--;
    }

    //MulExp -> UnaryExp | MulExp ('*' | '/' | '%') UnaryExp
    //MulExp -> UnaryExp {('*'|'/'|'%') UnaryExp}
    private void MulExp(N_MidItem mulexp) {
        do {
            N_MidItem unaryexp = new N_MidItem("UnaryExp");
            UnaryExp(unaryexp);
            mulexp.addnode(unaryexp);
            anawords.add("<MulExp>");
            tmp = getword();
            if (tmp.getType() == T_Typename.MULT||
                tmp.getType() == T_Typename.DIV||
                tmp.getType() == T_Typename.MOD) {
                mulexp.addnode(tmp);
                addword();
            }
        } while (tmp.getType() == T_Typename.MULT||
                tmp.getType() == T_Typename.DIV||
                tmp.getType() == T_Typename.MOD);
        this.index--;
    }

    //UnaryExp ->  PrimaryExp
    // | Ident '(' [FuncRParams] ')'
    // | UnaryOp UnaryExp
    private void UnaryExp(N_MidItem unaryexp) {
        tmp = getword();
        //int times = 1;
        /*
        while (tmp.getType() == Typename.PLUS||
            tmp.getType() == Typename.MINU||
            tmp.getType() == Typename.NOT) {
            addword();
            anawords.add("<UnaryOp>");
            times++;
            tmp = getword();
        }*/
        if (tmp.getType() == T_Typename.PLUS||
                tmp.getType() == T_Typename.MINU||
                tmp.getType() == T_Typename.NOT) {
            N_MidItem unaryop = new N_MidItem("UnaryOp");
            unaryop.addnode(tmp);
            unaryexp.addnode(unaryop);
            addword();

            N_MidItem unaryexp1 = new N_MidItem("UnaryExp");
            UnaryExp(unaryexp1);
            unaryexp.addnode(unaryexp1);
            return;
        }

        if (tmp.getType() == T_Typename.IDENFR &&
            this.words.get(this.index).getType() == T_Typename.LPARENT) {
            //Ident '(' [FuncRParams] ')'
            unaryexp.addnode(tmp);
            addword();
            //'('
            tmp = getword();
            unaryexp.addnode(tmp);
            addword();

            tmp = getword();
            if (tmp.getType() != T_Typename.RPARENT && tmp.getType() != T_Typename.SEMICN) {
                index--;
                N_MidItem funcrparams = new N_MidItem("FuncRParams");
                FuncRParams(funcrparams);
                unaryexp.addnode(funcrparams);
                tmp = getword();
            }

            if (tmp.getType() == T_Typename.RPARENT) {
                unaryexp.addnode(tmp);
                addword();
            } else {
                E_Error.adderror("j",words.get(index-2).getLine());
                index--;
            }
        } else {
            //PrimaryExp
            index--;
            N_MidItem primaryexp = new N_MidItem("PrimaryExp");
            PrimaryExp(primaryexp);
            unaryexp.addnode(primaryexp);
        }

        /*for(int i=0;i<times;i++){
            anawords.add("<UnaryExp>");
        }*/
        anawords.add("<UnaryExp>");
    }

    //PrimaryExp -> '(' Exp ')' | LVal | Number
    //Number -> IntConst
    private void PrimaryExp(N_MidItem primaryexp) {
        tmp = getword();
        //System.out.println(tmp.getType()+" "+tmp.getName());
        if (tmp.getType() == T_Typename.LPARENT) {
            primaryexp.addnode(tmp);
            addword();
            N_MidItem exp = new N_MidItem("Exp");
            Exp(exp);
            primaryexp.addnode(exp);
            //')'
            tmp = getword();
            if (tmp.getType() == T_Typename.RPARENT) {
                primaryexp.addnode(tmp);
                addword();
            }else {
                E_Error.adderror("j",words.get(index-2).getLine());
                index--;
            }
        } else if (tmp.getType() == T_Typename.INTCON) {
            N_MidItem number = new N_MidItem("Number");
            number.addnode(tmp);
            primaryexp.addnode(number);
            addword();
            anawords.add("<Number>");
        } else if (tmp.getType() == T_Typename.IDENFR) {
            index--;
            N_MidItem lval = new N_MidItem("LVal");
            LVal(lval);
            primaryexp.addnode(lval);
        }
        anawords.add("<PrimaryExp>");
    }

    //FuncRParams -> Exp {',' Exp}
    private void FuncRParams(N_MidItem funcr) {
        do {
            N_MidItem exp = new N_MidItem("Exp");
            Exp(exp);
            funcr.addnode(exp);
            tmp = getword();
            if (tmp.getType() == T_Typename.COMMA) {
                funcr.addnode(tmp);
                addword();
            }
        } while (tmp.getType() == T_Typename.COMMA);
        index--;
        anawords.add("<FuncRParams>");
    }

    //LVal -> Ident {'[' Exp ']'}
    private void LVal(N_MidItem lval) {
        //Ident
        tmp = getword();
        lval.addnode(tmp);
        addword();
        //{'[' Exp ']'}
        tmp = getword();
        if (tmp.getType() == T_Typename.LBRACK) {
            while (tmp.getType() == T_Typename.LBRACK) {
                lval.addnode(tmp);//[
                addword();
                N_MidItem exp = new N_MidItem("Exp");
                Exp(exp);
                lval.addnode(exp);
                //']'
                tmp = getword();
                if (tmp.getType() == T_Typename.RBRACK) {
                    lval.addnode(tmp); //]
                    addword();
                }else {
                    E_Error.adderror("k",this.words.get(index-2).getLine());
                    index--;
                }
                tmp = getword();
            }
        }
        index--;
        anawords.add("<LVal>");
    }

    //Stmt -> Block | IfStmt | 'break' ';'
    //|'continue' ';'|'return' [Exp] ';'|WhileStmt
    //|LVal '=' Exp ';'|LVal '=' 'getint''('')'';'
    private void Stmt(N_MidItem stmt) {
        tmp = getword();
        if (tmp.getType() == T_Typename.LBRACE) {
            index--;
            N_MidItem block = new N_MidItem("Block");
            Block(block);
            stmt.addnode(block);
        } else if (tmp.getType() == T_Typename.IFTK) {
            index--;
            IfStmt(stmt);
        } else if (tmp.getType() == T_Typename.BREAKTK) {
            stmt.addnode(tmp);
            addword();
            int line = tmp.getLine();
            tmp = getword();
            if (tmp.getType() != T_Typename.SEMICN) {
                E_Error.adderror("i",line);
                index--;
            } else{
                stmt.addnode(tmp);
                addword();
            }
        } else if (tmp.getType() == T_Typename.CONTINUETK) {
            stmt.addnode(tmp);
            addword();
            int line = tmp.getLine();
            tmp = getword();
            if (tmp.getType() != T_Typename.SEMICN) {
                E_Error.adderror("i",line);
                index--;
            } else{
                stmt.addnode(tmp);
                addword();
            }
        } else if (tmp.getType() == T_Typename.RETURNTK) {
            stmt.addnode(tmp);
            addword();
            tmp = getword();
            if (tmp.getType() != T_Typename.SEMICN) {
                index--;
                N_MidItem exp = new N_MidItem("Exp");
                Exp(exp);
                stmt.addnode(exp);
                tmp = getword();
            }
            if (tmp.getType() != T_Typename.SEMICN) {
                E_Error.adderror("i",this.words.get(index-2).getLine());
                //System.out.println(line);
                index--;
            } else{
                stmt.addnode(tmp);
                addword();
            }
        } else if (tmp.getType() == T_Typename.WHILETK) {
            index--;
            WhileStmt(stmt);
        } else if (tmp.getType() == T_Typename.PRINTFTK) {
            index--;
            PrintStmt(stmt);
        } else if (tmp.getType() == T_Typename.IDENFR) {
            index--;
            int ori_index = this.index;
            boolean lval = false;
            //System.out.println(tmp.getType()+ " " + tmp.getName());
            while (this.words.get(index).getType() != T_Typename.SEMICN) {
                if (this.words.get(index).getType() == T_Typename.ASSIGN) {
                    lval = true;
                    break;
                }
                index++;
            }
            index = ori_index;
            if (lval) {
                N_MidItem lvalm = new N_MidItem("LVal");
                LVal(lvalm);
                stmt.addnode(lvalm);
                tmp = getword();
                stmt.addnode(tmp);
                addword();

                tmp = getword();
                if (tmp.getType() == T_Typename.GETINTTK) {
                    stmt.addnode(tmp);
                    addword();
                    //'('
                    tmp = getword();
                    stmt.addnode(tmp);
                    addword();
                    //')'
                    tmp = getword();
                    if (tmp.getType() != T_Typename.RPARENT) {
                        E_Error.adderror("j",words.get(index-2).getLine());
                        index--;
                    } else {
                        stmt.addnode(tmp);
                        addword();
                    }
                } else {
                    index--;
                    N_MidItem exp = new N_MidItem("Exp");
                    Exp(exp);
                    stmt.addnode(exp);
                }
            } else {
                N_MidItem exp = new N_MidItem("Exp");
                Exp(exp);
                stmt.addnode(exp);
            }
            tmp = getword();
            if (tmp.getType() != T_Typename.SEMICN) {
                E_Error.adderror("i",words.get(index-2).getLine());
                index--;
            } else{
                stmt.addnode(tmp);
                addword();
            }
            //System.out.println(tmp.getType()+ " " + tmp.getName());
        } else if (tmp.getType() != T_Typename.SEMICN) {
            index--;
            N_MidItem exp = new N_MidItem("Exp");
            Exp(exp);
            stmt.addnode(exp);
            //';'
            tmp = getword();
            if (tmp.getType() != T_Typename.SEMICN) {
                E_Error.adderror("i",words.get(index-2).getLine());
                index--;
            } else{
                stmt.addnode(tmp);
                addword();
            }
        } else {
            stmt.addnode(tmp);
            addword();
        }
        anawords.add("<Stmt>");
    }

    //IfStmt -> 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
    private void IfStmt(N_MidItem stmt) {
        //'if'
        tmp = getword();
        stmt.addnode(tmp);
        addword();
        //'('
        tmp = getword();
        stmt.addnode(tmp);
        addword();
        //Cond
        N_MidItem cond = new N_MidItem("Cond");
        Cond(cond);
        stmt.addnode(cond);
        //')'
        tmp = getword();
        if (tmp.getType() != T_Typename.RPARENT) {
            E_Error.adderror("j",words.get(index-2).getLine());
            index--;
        } else {
            stmt.addnode(tmp);
            addword();
        }

        N_MidItem stmt1 = new N_MidItem("Stmt");
        Stmt(stmt1);
        stmt.addnode(stmt1);
        //[ 'else' Stmt ]
        tmp = getword();
        if (tmp.getType() == T_Typename.ELSETK) {
            stmt.addnode(tmp);
            addword();
            N_MidItem stmt2 = new N_MidItem("Stmt");
            Stmt(stmt2);
            stmt.addnode(stmt2);
        } else {
            this.index--;
        }
    }

    //WhileStmt -> 'while' '(' Cond ')' Stmt
    private void WhileStmt(N_MidItem stmt) {
        //'while'
        tmp = getword();
        stmt.addnode(tmp);
        addword();
        //'('
        tmp = getword();
        stmt.addnode(tmp);
        addword();
        N_MidItem cond = new N_MidItem("Cond");
        Cond(cond);
        stmt.addnode(cond);
        //')'
        tmp = getword();
        if (tmp.getType() != T_Typename.RPARENT) {
            E_Error.adderror("j",words.get(index-2).getLine());
            index--;
        } else {
            stmt.addnode(tmp);
            addword();
        }
        N_MidItem stmt1 = new N_MidItem("Stmt");
        Stmt(stmt1);
        stmt.addnode(stmt1);
    }

    //PrintStmt -> 'printf''('FormatString {','Exp}')'';'
    private void PrintStmt(N_MidItem stmt) {
        //'printf'
        tmp = getword();
        stmt.addnode(tmp);
        addword();
        //'('
        tmp = getword();
        stmt.addnode(tmp);
        addword();
        //FormatString
        tmp = getword();
        stmt.addnode(tmp);
        addword();

        tmp = getword();
        if (tmp.getType() == T_Typename.COMMA) {
            while(tmp.getType() == T_Typename.COMMA) {
                stmt.addnode(tmp);
                addword();
                N_MidItem exp = new N_MidItem("Exp");
                Exp(exp);
                stmt.addnode(exp);
                tmp = getword();
            }
        }

        if (tmp.getType() != T_Typename.RPARENT) {
            E_Error.adderror("j",words.get(index-2).getLine());
            index--;
        } else {
            stmt.addnode(tmp);
            addword();
        }

        tmp = getword();

        if (tmp.getType() != T_Typename.SEMICN) {
            E_Error.adderror("i",words.get(index-2).getLine());
            index--;
        } else{
            stmt.addnode(tmp);
            addword();
        }
    }

}
