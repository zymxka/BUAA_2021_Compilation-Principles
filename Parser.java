import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class Parser {
    private ArrayList<SymItem> words = new ArrayList<>();
    private ArrayList<String> anawords = new ArrayList<>();
    private int index;
    private String FuncTypeNow = null;
    private int level;
    private MidItem compuint;
    private SymItem tmp = null;

    public Parser(ArrayList<SymItem> gene) {
        this.words.addAll(gene);
        this.index = 0;
        this.level = 0; //用来判断重名
        this.compuint = new MidItem("CompUnit");
    }

    public MidItem getCompuint() {
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

    private SymItem getword() {
        SymItem ans = this.words.get(this.index);
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
        while ((this.tmp.getType() == Typename.CONSTTK)
                || (this.tmp.getType() == Typename.INTTK
                && this.words.get(this.index+1).getType() != Typename.LPARENT)) {
            if (this.tmp.getType() == Typename.CONSTTK) {
                this.index--;
                MidItem constdecl = new MidItem("ConstDecl");
                ConstDecl(constdecl);
                this.compuint.addnode(constdecl);
            } else {
                this.index--;
                MidItem vardecl = new MidItem("VarDecl");
                VarDecl(vardecl);
                this.compuint.addnode(vardecl);
            }
            tmp = getword();
        }

        //{FuncDef}
        while ((this.tmp.getType() == Typename.INTTK || this.tmp.getType() == Typename.VOIDTK)
                && (this.words.get(this.index).getType() != Typename.MAINTK)
                && (this.words.get(this.index+1).getType() == Typename.LPARENT)) {
            this.index--;
            MidItem funcdef = new MidItem("FuncDef");
            FuncDef(funcdef);
            this.compuint.addnode(funcdef);
            tmp = getword();
        }

        //MainFuncDef
        if ((this.tmp.getType() == Typename.INTTK)
                && (this.words.get(this.index).getType() == Typename.MAINTK)
                && (this.words.get(this.index+1).getType() == Typename.LPARENT)) {
            this.index--;
            MidItem mainfuncdef = new MidItem("MainFuncDef");
            MainFuncDef(mainfuncdef);
            this.compuint.addnode(mainfuncdef);
        }

        anawords.add("<CompUnit>");
    }

    //ConstDecl -> 'const' 'int' ConstDef {',' ConstDef} ;
    private void ConstDecl(MidItem constdecl) {
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
            MidItem constdef = new MidItem("ConstDef");
            ConstDef(constdef);
            constdecl.addnode(constdef);
            tmp = getword();
            if (tmp.getType() == Typename.COMMA) {
                constdecl.addnode(tmp);
                addword();
            }
        } while (tmp.getType() == Typename.COMMA);

        // ;
        if (tmp.getType() != Typename.SEMICN) {
            Error.adderror("i",words.get(index-2).getLine());
            index--;
        } else{
            constdecl.addnode(tmp);
            addword();
        }
        anawords.add("<ConstDecl>");
    }

    //ConstDef -> Ident {'[' ConstExp ']'} '=' ConstInitVal
    private void ConstDef(MidItem constdef) {
        //Ident
        tmp = getword();
        constdef.addnode(tmp);
        addword();

        tmp = getword();
        // {'[' ConstExp ']'}
        while (tmp.getType() == Typename.LBRACK) {
            constdef.addnode(tmp);//[
            addword();
            MidItem constExp = new MidItem("ConstExp");
            ConstExp(constExp);
            constdef.addnode(constExp);//ConstExp
            tmp = getword();
            if (tmp.getType() == Typename.RBRACK) {
                constdef.addnode(tmp); //]
                addword();
            }else {
                Error.adderror("k",this.words.get(index-2).getLine());
                index--;
            }
            tmp = getword();
        }

        // '='
        if (tmp.getType() == Typename.ASSIGN) {
            constdef.addnode(tmp);
            addword();
        }

        //ConstInitVal
        MidItem constinitval = new MidItem("ConstInitVal");
        ConstInitVal(constinitval);
        constdef.addnode(constinitval);

        anawords.add("<ConstDef>");
    }

    //ConstExp -> AddExp
    private void ConstExp(MidItem constexp) {
        MidItem addexp = new MidItem("AddExp");
        AddExp(addexp);
        constexp.addnode(addexp);
        anawords.add("<ConstExp>");
    }

    //ConstInitVal -> ConstExp
    // | '{' [ConstInitVal {',' ConstInitVal}] '}'
    private void ConstInitVal(MidItem constinitval) {
        tmp = getword();
        if (tmp.getType() != Typename.LBRACE) {
            this.index--;
            MidItem constexp = new MidItem("ConstExp");
            ConstExp(constexp);
            constinitval.addnode(constexp);
        } else {
            constinitval.addnode(tmp);
            addword();
            tmp = getword();
            if (tmp.getType() != Typename.RBRACE) {
                this.index--;
                MidItem constinitval1 = new MidItem("ConstInitVal");
                ConstInitVal(constinitval1);
                constinitval.addnode(constinitval1);
                tmp = getword();
                while (tmp.getType() == Typename.COMMA) {
                    constinitval.addnode(tmp);
                    addword();
                    MidItem constinitval2 = new MidItem("ConstInitVal");
                    ConstInitVal(constinitval2);
                    constinitval.addnode(constinitval2);
                    tmp = getword();
                }
            }
            if (tmp.getType() == Typename.RBRACE) {
                constinitval.addnode(tmp);
                addword();
            }
        }
        anawords.add("<ConstInitVal>");
    }

    //VarDecl -> BType VarDef {',' VarDef} ';'
    private void VarDecl(MidItem vardecl) {
        // int
        tmp = getword();
        vardecl.addnode(tmp);
        addword();

        //VarDef {',' VarDef}
        do {
            MidItem vardef = new MidItem("VarDef");
            VarDef(vardef);
            vardecl.addnode(vardef);
            tmp = getword();
            if (tmp.getType() == Typename.COMMA) {
                vardecl.addnode(tmp);
                addword();
            }
        } while (tmp.getType() == Typename.COMMA);

        // ;
        if (tmp.getType() != Typename.SEMICN) {
            Error.adderror("i",words.get(index-2).getLine());
            index--;
        } else{
            vardecl.addnode(tmp);
            addword();
        }

        anawords.add("<VarDecl>");
    }

    //VarDef -> Ident { '[' ConstExp ']' }
    // | Ident { '[' ConstExp ']' } '=' InitVal
    private void VarDef(MidItem vardef) {
        //Ident
        tmp = getword();
        vardef.addnode(tmp);
        addword();

        tmp = getword();
        //{'[' ConstExp ']'}
        while (tmp.getType() == Typename.LBRACK) {
            vardef.addnode(tmp); //[
            addword();
            MidItem constexp = new MidItem("ConstExp");
            ConstExp(constexp);
            vardef.addnode(constexp);
            tmp = getword();
            if (tmp.getType() == Typename.RBRACK) {
                vardef.addnode(tmp); //]
                addword();
            }else {
                Error.adderror("k",this.words.get(index-2).getLine());
                index--;
            }
            tmp = getword();
        }

        // '=' InitVal
        if (tmp.getType() == Typename.ASSIGN) {
            vardef.addnode(tmp);
            addword();
            MidItem initval = new MidItem("InitVal");
            InitVal(initval);
            vardef.addnode(initval);
        } else {
            this.index--;
        }

        anawords.add("<VarDef>");
    }

    //InitVal → Exp | '{' [ InitVal { ',' InitVal } ] '}'
    private void InitVal(MidItem initval) {
        tmp = getword();
        if (tmp.getType() != Typename.LBRACE) {
            this.index--;
            MidItem exp = new MidItem("Exp");
            Exp(exp);
            initval.addnode(exp);
        } else {
            initval.addnode(tmp);
            addword();
            tmp = getword();
            if (tmp.getType() != Typename.RBRACE) {
                this.index--;
                MidItem initval1 = new MidItem("InitVal");
                InitVal(initval1);
                initval.addnode(initval1);
                tmp = getword();
                while (tmp.getType() == Typename.COMMA) {
                    initval.addnode(tmp);
                    addword();
                    MidItem initval2 = new MidItem("InitVal");
                    InitVal(initval2);
                    initval.addnode(initval2);
                    tmp = getword();
                }
            }
            if (tmp.getType() == Typename.RBRACE) {
                initval.addnode(tmp);
                addword();
            }
         }
        anawords.add("<InitVal>");
    }

    //Exp -> AddExp
    private void Exp(MidItem exp) {
        MidItem addexp = new MidItem("AddExp");
        AddExp(addexp);
        exp.addnode(addexp);
        anawords.add("<Exp>");
    }

    //FuncDef -> FuncType Ident '(' [FuncFParams] ')' Block
    private void FuncDef(MidItem funcdef) {
        tmp = getword();
        MidItem functype = new MidItem("FuncType");
        functype.addnode(tmp);
        funcdef.addnode(functype);
        addword();
        if (tmp.getType() == Typename.INTTK) {
            anawords.add("<FuncType>");
            this.FuncTypeNow = "int";
        } else if (tmp.getType() == Typename.VOIDTK) {
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
        if (tmp.getType() != Typename.RPARENT && tmp.getType() != Typename.LBRACE) {
            this.index--;
            MidItem funcfparams = new MidItem("FuncFParams");
            FuncFParams(funcfparams);
            funcdef.addnode(funcfparams);
            tmp = getword();
        }
        if (tmp.getType() == Typename.RPARENT) {
            funcdef.addnode(tmp);//)
            addword();
        } else {
            Error.adderror("j",words.get(index-2).getLine());
            index--;
        }

        //Block
        MidItem block = new MidItem("Block");
        Block(block);
        funcdef.addnode(block);

        this.FuncTypeNow = null;
        anawords.add("<FuncDef>");
    }

    //FuncFParams -> FuncFParam { ',' FuncFParam }
    private void FuncFParams(MidItem funcfparams) {
        do {
            MidItem funcfparam = new MidItem("FuncFParam");
            FuncFParam(funcfparam);
            funcfparams.addnode(funcfparam);
            tmp = getword();
            if (tmp.getType() == Typename.COMMA) {
                funcfparams.addnode(tmp);
                addword();
            }
        } while (tmp.getType() == Typename.COMMA);
        index--;
        anawords.add("<FuncFParams>");
    }

    //FuncFParam → BType Ident ['[' ']' { '[' ConstExp ']' }]
    private void FuncFParam(MidItem funcfparam) {
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
        if (tmp.getType() == Typename.LBRACK) {
            funcfparam.addnode(tmp);
            addword();
            //']'
            tmp = getword();
            if (tmp.getType() == Typename.RBRACK) {
                funcfparam.addnode(tmp); //]
                addword();
            }else {
                Error.adderror("k",this.words.get(index-2).getLine());
                index--;
            }
            //{ '[' ConstExp ']' }
            tmp = getword();
            if (tmp.getType() == Typename.LBRACK) {
                while (tmp.getType() == Typename.LBRACK) {
                    funcfparam.addnode(tmp);
                    addword();
                    MidItem constexp = new MidItem("ConstExp");
                    ConstExp(constexp);
                    funcfparam.addnode(constexp);
                    //']'
                    tmp = getword();
                    if (tmp.getType() == Typename.RBRACK) {
                        funcfparam.addnode(tmp); //]
                        addword();
                    }else {
                        Error.adderror("k",this.words.get(index-2).getLine());
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
    private void MainFuncDef(MidItem mainfuncdef) {
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
        if (tmp.getType() == Typename.RPARENT) {
            mainfuncdef.addnode(tmp); //)
            addword();
        }else {
            Error.adderror("j",this.words.get(index-2).getLine());
            index--;
        }

        MidItem block = new MidItem("Block");
        Block(block);
        mainfuncdef.addnode(block);

        this.FuncTypeNow = null;
        anawords.add("<MainFuncDef>");
    }

    //Block -> '{' {BlockItem} '}'
    private void Block(MidItem block) {
        //'{'
        tmp = getword();
        block.addnode(tmp);
        addword();

        tmp = getword();
        while (tmp.getType() != Typename.RBRACE) {
            index--;
            MidItem blockitem = new MidItem("BlockItem");
            BlockItem(blockitem);
            block.addnode(blockitem);
            tmp = getword();
        }

        //'}'
        if (tmp.getType() == Typename.RBRACE) {
            block.addnode(tmp);
            addword();
        }

        anawords.add("<Block>");
    }

    //BlockItem -> ConstDecl | VarDecl | Stmt
    private void BlockItem(MidItem blockitem) {
        tmp = getword();
        if (tmp.getType() == Typename.CONSTTK) {
            this.index--;
            MidItem constdecl = new MidItem("ConstDecl");
            ConstDecl(constdecl);
            blockitem.addnode(constdecl);
        }else if (tmp.getType() == Typename.INTTK
                && this.words.get(this.index+1).getType() != Typename.LPARENT) {
            this.index--;
            MidItem vardecl = new MidItem("VarDecl");
            VarDecl(vardecl);
            blockitem.addnode(vardecl);
        } else {
            this.index--;
            MidItem stmt = new MidItem("Stmt");
            Stmt(stmt);
            blockitem.addnode(stmt);
        }
    }

    private void Cond(MidItem cond) {
        MidItem lorexp = new MidItem("LOrExp");
        LOrExp(lorexp);
        cond.addnode(lorexp);
        anawords.add("<Cond>");
    }

    //LOrExp -> LAndExp | LOrExp '||' LAndExp
    //LOrExp -> LAndExp {'||' LAndExp}
    private void LOrExp(MidItem lorexp) {
        do {
            MidItem landexp = new MidItem("LAndExp");
            LAndExp(landexp);
            lorexp.addnode(landexp);
            anawords.add("<LOrExp>");
            tmp = getword();
            if (tmp.getType() == Typename.OR) {
                lorexp.addnode(tmp);
                addword();
            }
        } while (tmp.getType() == Typename.OR);
        this.index--;
    }

    //LAndExp -> EqExp | LAndExp '&&' EqExp
    //LAndExp -> EqExp {'&&' EqExp}
    private void LAndExp(MidItem landexp) {
        do {
            MidItem eqexp = new MidItem("EqExp");
            EqExp(eqexp);
            landexp.addnode(eqexp);
            anawords.add("<LAndExp>");
            tmp = getword();
            if (tmp.getType() == Typename.AND) {
                landexp.addnode(tmp);
                addword();
            }
        } while (tmp.getType() == Typename.AND);
        this.index--;
    }

    //EqExp -> RelExp | EqExp ('==' | '!=') RelExp
    //EqExp -> RelExp {('=='|'!=') RelExp}
    private void EqExp(MidItem eqexp) {
        do {
            MidItem relexp = new MidItem("RelExp");
            RelExp(relexp);
            eqexp.addnode(relexp);
            anawords.add("<EqExp>");
            tmp = getword();
            if (tmp.getType() == Typename.EQL ||
                tmp.getType() == Typename.NEQ) {
                eqexp.addnode(tmp);
                addword();
            }
        } while (tmp.getType() == Typename.EQL ||
                tmp.getType() == Typename.NEQ);
        this.index--;
    }

    //RelExp -> AddExp | RelExp ('<' | '>' | '<=' | '>=') AddExp
    //RelExp -> AddExp {('<'|'>'|'<='|'>=') AddExp}
    private void RelExp(MidItem relexp) {
        do {
            MidItem addexp = new MidItem("AddExp");
            AddExp(addexp);
            relexp.addnode(addexp);
            anawords.add("<RelExp>");
            tmp = getword();
            if (tmp.getType() == Typename.LSS ||
                tmp.getType() == Typename.LEQ ||
                tmp.getType() == Typename.GRE ||
                tmp.getType() == Typename.GEQ) {
                relexp.addnode(tmp);
                addword();
            }
        } while (tmp.getType() == Typename.LSS ||
                tmp.getType() == Typename.LEQ ||
                tmp.getType() == Typename.GRE ||
                tmp.getType() == Typename.GEQ);
        this.index--;
    }

    //AddExp -> MulExp | AddExp ('+'|'-') MulExp
    //AddExp -> MulExp {('+'|'-') MulExp}
    private void AddExp(MidItem addexp) {
        do {
            MidItem mulexp = new MidItem("MulExp");
            MulExp(mulexp);
            addexp.addnode(mulexp);
            anawords.add("<AddExp>");
            tmp = getword();
            if (tmp.getType() == Typename.PLUS ||
            tmp.getType() == Typename.MINU) {
                addexp.addnode(tmp);
                addword();
            }
        } while (tmp.getType() == Typename.PLUS ||
                tmp.getType() == Typename.MINU);
        this.index--;
    }

    //MulExp -> UnaryExp | MulExp ('*' | '/' | '%') UnaryExp
    //MulExp -> UnaryExp {('*'|'/'|'%') UnaryExp}
    private void MulExp(MidItem mulexp) {
        do {
            MidItem unaryexp = new MidItem("UnaryExp");
            UnaryExp(unaryexp);
            mulexp.addnode(unaryexp);
            anawords.add("<MulExp>");
            tmp = getword();
            if (tmp.getType() == Typename.MULT||
                tmp.getType() == Typename.DIV||
                tmp.getType() == Typename.MOD) {
                mulexp.addnode(tmp);
                addword();
            }
        } while (tmp.getType() == Typename.MULT||
                tmp.getType() == Typename.DIV||
                tmp.getType() == Typename.MOD);
        this.index--;
    }

    //UnaryExp ->  PrimaryExp
    // | Ident '(' [FuncRParams] ')'
    // | UnaryOp UnaryExp
    private void UnaryExp(MidItem unaryexp) {
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
        if (tmp.getType() == Typename.PLUS||
                tmp.getType() == Typename.MINU||
                tmp.getType() == Typename.NOT) {
            MidItem unaryop = new MidItem("UnaryOp");
            unaryop.addnode(tmp);
            unaryexp.addnode(unaryop);
            addword();

            MidItem unaryexp1 = new MidItem("UnaryExp");
            UnaryExp(unaryexp1);
            unaryexp.addnode(unaryexp1);
            return;
        }

        if (tmp.getType() == Typename.IDENFR &&
            this.words.get(this.index).getType() == Typename.LPARENT) {
            //Ident '(' [FuncRParams] ')'
            unaryexp.addnode(tmp);
            addword();
            //'('
            tmp = getword();
            unaryexp.addnode(tmp);
            addword();

            tmp = getword();
            if (tmp.getType() != Typename.RPARENT && tmp.getType() != Typename.SEMICN) {
                index--;
                MidItem funcrparams = new MidItem("FuncRParams");
                FuncRParams(funcrparams);
                unaryexp.addnode(funcrparams);
                tmp = getword();
            }

            if (tmp.getType() == Typename.RPARENT) {
                unaryexp.addnode(tmp);
                addword();
            } else {
                Error.adderror("j",words.get(index-2).getLine());
                index--;
            }
        } else {
            //PrimaryExp
            index--;
            MidItem primaryexp = new MidItem("PrimaryExp");
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
    private void PrimaryExp(MidItem primaryexp) {
        tmp = getword();
        //System.out.println(tmp.getType()+" "+tmp.getName());
        if (tmp.getType() == Typename.LPARENT) {
            primaryexp.addnode(tmp);
            addword();
            MidItem exp = new MidItem("Exp");
            Exp(exp);
            primaryexp.addnode(exp);
            //')'
            tmp = getword();
            if (tmp.getType() == Typename.RPARENT) {
                primaryexp.addnode(tmp);
                addword();
            }else {
                Error.adderror("j",words.get(index-2).getLine());
                index--;
            }
        } else if (tmp.getType() == Typename.INTCON) {
            MidItem number = new MidItem("Number");
            number.addnode(tmp);
            primaryexp.addnode(number);
            addword();
            anawords.add("<Number>");
        } else if (tmp.getType() == Typename.IDENFR) {
            index--;
            MidItem lval = new MidItem("LVal");
            LVal(lval);
            primaryexp.addnode(lval);
        }
        anawords.add("<PrimaryExp>");
    }

    //FuncRParams -> Exp {',' Exp}
    private void FuncRParams(MidItem funcr) {
        do {
            MidItem exp = new MidItem("Exp");
            Exp(exp);
            funcr.addnode(exp);
            tmp = getword();
            if (tmp.getType() == Typename.COMMA) {
                funcr.addnode(tmp);
                addword();
            }
        } while (tmp.getType() == Typename.COMMA);
        index--;
        anawords.add("<FuncRParams>");
    }

    //LVal -> Ident {'[' Exp ']'}
    private void LVal(MidItem lval) {
        //Ident
        tmp = getword();
        lval.addnode(tmp);
        addword();
        //{'[' Exp ']'}
        tmp = getword();
        if (tmp.getType() == Typename.LBRACK) {
            while (tmp.getType() == Typename.LBRACK) {
                lval.addnode(tmp);//[
                addword();
                MidItem exp = new MidItem("Exp");
                Exp(exp);
                lval.addnode(exp);
                //']'
                tmp = getword();
                if (tmp.getType() == Typename.RBRACK) {
                    lval.addnode(tmp); //]
                    addword();
                }else {
                    Error.adderror("k",this.words.get(index-2).getLine());
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
    private void Stmt(MidItem stmt) {
        tmp = getword();
        if (tmp.getType() == Typename.LBRACE) {
            index--;
            MidItem block = new MidItem("Block");
            Block(block);
            stmt.addnode(block);
        } else if (tmp.getType() == Typename.IFTK) {
            index--;
            IfStmt(stmt);
        } else if (tmp.getType() == Typename.BREAKTK) {
            stmt.addnode(tmp);
            addword();
            int line = tmp.getLine();
            tmp = getword();
            if (tmp.getType() != Typename.SEMICN) {
                Error.adderror("i",line);
                index--;
            } else{
                stmt.addnode(tmp);
                addword();
            }
        } else if (tmp.getType() == Typename.CONTINUETK) {
            stmt.addnode(tmp);
            addword();
            int line = tmp.getLine();
            tmp = getword();
            if (tmp.getType() != Typename.SEMICN) {
                Error.adderror("i",line);
                index--;
            } else{
                stmt.addnode(tmp);
                addword();
            }
        } else if (tmp.getType() == Typename.RETURNTK) {
            stmt.addnode(tmp);
            addword();
            tmp = getword();
            if (tmp.getType() != Typename.SEMICN) {
                index--;
                MidItem exp = new MidItem("Exp");
                Exp(exp);
                stmt.addnode(exp);
                tmp = getword();
            }
            if (tmp.getType() != Typename.SEMICN) {
                Error.adderror("i",this.words.get(index-2).getLine());
                //System.out.println(line);
                index--;
            } else{
                stmt.addnode(tmp);
                addword();
            }
        } else if (tmp.getType() == Typename.WHILETK) {
            index--;
            WhileStmt(stmt);
        } else if (tmp.getType() == Typename.PRINTFTK) {
            index--;
            PrintStmt(stmt);
        } else if (tmp.getType() == Typename.IDENFR) {
            index--;
            int ori_index = this.index;
            boolean lval = false;
            //System.out.println(tmp.getType()+ " " + tmp.getName());
            while (this.words.get(index).getType() != Typename.SEMICN) {
                if (this.words.get(index).getType() == Typename.ASSIGN) {
                    lval = true;
                    break;
                }
                index++;
            }
            index = ori_index;
            if (lval) {
                MidItem lvalm = new MidItem("LVal");
                LVal(lvalm);
                stmt.addnode(lvalm);
                tmp = getword();
                stmt.addnode(tmp);
                addword();

                tmp = getword();
                if (tmp.getType() == Typename.GETINTTK) {
                    stmt.addnode(tmp);
                    addword();
                    //'('
                    tmp = getword();
                    stmt.addnode(tmp);
                    addword();
                    //')'
                    tmp = getword();
                    if (tmp.getType() != Typename.RPARENT) {
                        Error.adderror("j",words.get(index-2).getLine());
                        index--;
                    } else {
                        stmt.addnode(tmp);
                        addword();
                    }
                } else {
                    index--;
                    MidItem exp = new MidItem("Exp");
                    Exp(exp);
                    stmt.addnode(exp);
                }
            } else {
                MidItem exp = new MidItem("Exp");
                Exp(exp);
                stmt.addnode(exp);
            }
            tmp = getword();
            if (tmp.getType() != Typename.SEMICN) {
                Error.adderror("i",words.get(index-2).getLine());
                index--;
            } else{
                stmt.addnode(tmp);
                addword();
            }
            //System.out.println(tmp.getType()+ " " + tmp.getName());
        } else if (tmp.getType() != Typename.SEMICN) {
            index--;
            MidItem exp = new MidItem("Exp");
            Exp(exp);
            stmt.addnode(exp);
            //';'
            tmp = getword();
            if (tmp.getType() != Typename.SEMICN) {
                Error.adderror("i",words.get(index-2).getLine());
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
    private void IfStmt(MidItem stmt) {
        //'if'
        tmp = getword();
        stmt.addnode(tmp);
        addword();
        //'('
        tmp = getword();
        stmt.addnode(tmp);
        addword();
        //Cond
        MidItem cond = new MidItem("Cond");
        Cond(cond);
        stmt.addnode(cond);
        //')'
        tmp = getword();
        if (tmp.getType() != Typename.RPARENT) {
            Error.adderror("j",words.get(index-2).getLine());
            index--;
        } else {
            stmt.addnode(tmp);
            addword();
        }

        MidItem stmt1 = new MidItem("Stmt");
        Stmt(stmt1);
        stmt.addnode(stmt1);
        //[ 'else' Stmt ]
        tmp = getword();
        if (tmp.getType() == Typename.ELSETK) {
            stmt.addnode(tmp);
            addword();
            MidItem stmt2 = new MidItem("Stmt");
            Stmt(stmt2);
            stmt.addnode(stmt2);
        } else {
            this.index--;
        }
    }

    //WhileStmt -> 'while' '(' Cond ')' Stmt
    private void WhileStmt(MidItem stmt) {
        //'while'
        tmp = getword();
        stmt.addnode(tmp);
        addword();
        //'('
        tmp = getword();
        stmt.addnode(tmp);
        addword();
        MidItem cond = new MidItem("Cond");
        Cond(cond);
        stmt.addnode(cond);
        //')'
        tmp = getword();
        if (tmp.getType() != Typename.RPARENT) {
            Error.adderror("j",words.get(index-2).getLine());
            index--;
        } else {
            stmt.addnode(tmp);
            addword();
        }
        MidItem stmt1 = new MidItem("Stmt");
        Stmt(stmt1);
        stmt.addnode(stmt1);
    }

    //PrintStmt -> 'printf''('FormatString {','Exp}')'';'
    private void PrintStmt(MidItem stmt) {
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
        if (tmp.getType() == Typename.COMMA) {
            while(tmp.getType() == Typename.COMMA) {
                stmt.addnode(tmp);
                addword();
                MidItem exp = new MidItem("Exp");
                Exp(exp);
                stmt.addnode(exp);
                tmp = getword();
            }
        }

        if (tmp.getType() != Typename.RPARENT) {
            Error.adderror("j",words.get(index-2).getLine());
            index--;
        } else {
            stmt.addnode(tmp);
            addword();
        }

        tmp = getword();

        if (tmp.getType() != Typename.SEMICN) {
            Error.adderror("i",words.get(index-2).getLine());
            index--;
        } else{
            stmt.addnode(tmp);
            addword();
        }
    }

}
