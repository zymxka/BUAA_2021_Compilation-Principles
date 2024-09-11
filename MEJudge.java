import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class MEJudge {
    private MidItem compunit;
    private int cirdepth;
    private int level;
    private String functypenow = "";
    private String rettypenow = "";
    private IdentTable table;
    private boolean funcfind = true;

    public MEJudge(MidItem compunit) {
        this.compunit = compunit;
        this.cirdepth = 0;
        this.level = 0;
        this.table = new IdentTable();
        search_error(compunit);
    }

    public void print_comp() throws IOException {
        FileWriter writer = new FileWriter("output.txt");
        writer.write(compunit.toString());
        writer.flush();
        writer.close();
    }

    public void search_error(Treenode now) {
        if (now instanceof MidItem) {
            MidItem midnow = ((MidItem) now);
            boolean incir = false;
            boolean inblk = false;
            ArrayList<Treenode> sons = midnow.getNodes();

            if (midnow.getType().equals("Stmt")) {
                if (sons.get(0) instanceof SymItem) {
                    SymItem son0 = (SymItem) sons.get(0);
                    if (son0.getType().equals(Typename.PRINTFTK)) {
                        ejudge_i(sons);
                    } else if (son0.getType().equals(Typename.WHILETK)) {
                        this.cirdepth++;
                        incir = true;
                    } else if (son0.getType().equals(Typename.RETURNTK)) {
                        ejudge_fg(sons);
                    }
                } else if (sons.get(0) instanceof MidItem) {
                    MidItem sonm0 = (MidItem) sons.get(0);
                    if (sonm0.getType().equals("LVal")) {
                        if (sons.get(1) instanceof SymItem
                       && ((SymItem) sons.get(1)).getType().equals(Typename.ASSIGN)) {
                            ejudge_h(sons.get(0));
                        }
                    }
                }
            } else if (midnow.getType().equals("FuncDef")) {
                in_func(sons.get(0));
                ejudge_b_f(sons.get(1));
            } else if (midnow.getType().equals("MainFuncDef")) {
                in_main_func(sons.get(0));
                ejudge_b_f(sons.get(1));
            } else if (midnow.getType().equals("Block")) {
                this.level++;
                inblk = true;
            } else if (midnow.getType().equals("ConstDef")) {
                ejudge_b_c(sons);
            } else if (midnow.getType().equals("VarDef")) {
                ejudge_b_v(sons);
            } else if (midnow.getType().equals("FuncFParam")) {
                ejudge_b_fp(sons);
            } else if (midnow.getType().equals("UnaryExp")) {
                if (sons.get(0) instanceof SymItem) {
                    this.funcfind = true;
                    ejudge_cde_u(sons);
                    if (this.funcfind) {
                        ejudge_de(sons);
                    }
                }
            } else if (midnow.getType().equals("LVal")) {
                ejudge_c_l(sons);
            }

            for(Treenode son:sons) {
                search_error(son);
            }

            if (incir) { this.cirdepth--; }
            if (inblk) { table.poplevel(level);this.level--;}
            if (midnow.getType().equals("FuncDef")
               || midnow.getType().equals("MainFuncDef")) {
                if (this.functypenow.equals("int")) {
                    MidItem blk = (MidItem) midnow.getlast();
                    ArrayList<Treenode> blkitems = blk.getNodes();
                    this.rettypenow = "";
                    Treenode items = blkitems.get(blkitems.size()-2);
                        if (items instanceof MidItem) {
                            MidItem retsen = (MidItem) items;
                            MidItem stmt = (MidItem) retsen.getfirst();
                            ArrayList<Treenode> eles = stmt.getNodes();
                            if (eles.get(0) instanceof SymItem
                                    && ((SymItem) eles.get(0)).getType().equals(Typename.RETURNTK) ) {
                                this.rettypenow = "int";
                            }
                        }

                }
                int retline = ((SymItem)((MidItem) midnow.getlast()).getlast()).getLine();
                out_func(retline);
            }
        } else if (now instanceof SymItem) {
            SymItem symnow = (SymItem) now;
            if (symnow.getType() == Typename.BREAKTK ||
                symnow.getType() == Typename.CONTINUETK) {
                ejudge_m(symnow.getLine());
            }
        }
    }

    private void in_func(Treenode type) {
        SymItem ft = (SymItem) ((MidItem) type).getfirst();
        if (ft.getType() == Typename.INTTK) {
            this.functypenow = "int";
        } else if (ft.getType() == Typename.VOIDTK) {
            this.functypenow = "void";
        }
        this.rettypenow = "";
    }

    private void in_main_func(Treenode type) {
        this.functypenow = "int";
        this.rettypenow = "";
    }

    private void out_func(int line) {
        if (this.rettypenow.equals("") && this.functypenow.equals("int")) {
            Error.adderror("g",line);
        }
        this.rettypenow = "";
        this.functypenow = "";
    }

    private void ejudge_i(ArrayList<Treenode> elements) {
        int index = 2;
        SymItem fstr = (SymItem) elements.get(index);
        int fnum = fstr.getformatc();
        int pnum = 0;
        index++;
        SymItem next = (SymItem) elements.get(index);
        if (next.getType() == Typename.COMMA) {
            while(next.getType() == Typename.COMMA) {
                pnum++;
                index += 2;
                next = (SymItem) elements.get(index);
            }
        }
        if (fnum != pnum) {
            Error.adderror("l",next.getLine());
        }
    }

    private void ejudge_m(int line) {
        if (this.cirdepth == 0) {
            Error.adderror("m",line);
        }
    }

    private void ejudge_fg(ArrayList<Treenode> elements) {
        SymItem rets = (SymItem) elements.get(0);
        Treenode last = elements.get(elements.size()-1);
        if (last instanceof SymItem && ((SymItem) last).getType().equals(Typename.SEMICN)) {
            if (elements.size() == 2) {
                this.rettypenow = "void";
                if (!this.rettypenow.equals(this.functypenow)) {
                    Error.adderror("g",rets.getLine());
                }
            } else {
                this.rettypenow = "int";
                if (!this.rettypenow.equals(this.functypenow)) {
                    Error.adderror("f",rets.getLine());
                }
            }
        } else {
            if (elements.size() == 1) {
                this.rettypenow = "void";
                if (!this.rettypenow.equals(this.functypenow)) {
                    Error.adderror("g",rets.getLine());
                }
            } else {
                this.rettypenow = "int";
                if (!this.rettypenow.equals(this.functypenow)) {
                    Error.adderror("f",rets.getLine());
                }
            }
        }
    }

    private int cal_dim(ArrayList<Treenode> elements) {
        int dim = 0;
        for(Treenode nodes:elements) {
            if (nodes instanceof SymItem) {
                SymItem tmp = (SymItem) nodes;
                if (tmp.getType()==Typename.LBRACK) {
                    dim++;
                }
            }
        }
        return dim;
    }

    private void ejudge_b_c(ArrayList<Treenode> elements) {
        SymItem ident = (SymItem) elements.get(0);
        int dim = cal_dim(elements);
        if (table.mulvar(ident.getName(), level) != -1) {
            Error.adderror("b", ident.getLine());
        }
        if (dim == 0) {
            table.additem(ident.getName(),1,1,level);
        } else if (dim == 1) {
            table.additem(ident.getName(),3,1,level);
        } else if (dim == 2) {
            table.additem(ident.getName(),4,1,level);
        }
    }

    private void ejudge_b_v(ArrayList<Treenode> elements) {
        SymItem ident = (SymItem) elements.get(0);
        int dim = cal_dim(elements);
        if (table.mulvar(ident.getName(), level) != -1) {
            Error.adderror("b", ident.getLine());
        }
        if (dim == 0) {
            table.additem(ident.getName(),1,2,level);
        } else if (dim == 1) {
            table.additem(ident.getName(),3,2,level);
        } else if (dim == 2) {
            table.additem(ident.getName(),4,2,level);
        }
    }

    private void ejudge_b_f(Treenode ident) {
        SymItem name = (SymItem) ident;
        if (table.mulvar(name.getName(),level) != -1) {
            Error.adderror("b", name.getLine());
        }
        if (this.functypenow.equals("int")) {
            table.additem(name.getName(),1,4,level);
        } else {
            table.additem(name.getName(),2,4,level);
        }
    }

    private void ejudge_b_fp(ArrayList<Treenode> elements) {
        SymItem ident = (SymItem) elements.get(1);
        int dim = cal_dim(elements);
        if (table.mulvar(ident.getName(), level + 1) != -1) {
            Error.adderror("b", ident.getLine());
        }
        if (dim == 0) {
            table.additem(ident.getName(),1,3,level+1);
        } else if (dim == 1) {
            table.additem(ident.getName(),3,3,level+1);
        } else if (dim == 2) {
            table.additem(ident.getName(),4,3,level+1);
        }
    }

    private void ejudge_cde_u(ArrayList<Treenode> elements) {
        SymItem funcnow = (SymItem) elements.get(0);
        if (table.findfunc(funcnow.getName())==-1) {
            Error.adderror("c",funcnow.getLine());
            this.funcfind = false;
        }
    }

    private void ejudge_c_l(ArrayList<Treenode> elements) {
        SymItem ident = (SymItem) elements.get(0);
        if (table.findvar(ident.getName(),level)==-1) {
            Error.adderror("c",ident.getLine());
        }
    }

    private void ejudge_h(Treenode ident) {
        MidItem lval = (MidItem) ident;
        SymItem tmpsym = (SymItem) lval.getfirst();
        if (table.isconst(tmpsym.getName(),level)){
            Error.adderror("h",tmpsym.getLine());
        }
    }

    private void ejudge_de(ArrayList<Treenode> elements) {
        SymItem funcnow = (SymItem) elements.get(0);
        ArrayList<Integer> typern = new ArrayList<>();
        ArrayList<Integer> typerr = table.funcrtype(funcnow.getName());
        if (elements.size() >= 3 && elements.get(2) instanceof MidItem) {
            MidItem funcrps = (MidItem) elements.get(2);
            //System.out.println(funcrps.getType());
            calrp(funcrps,typern);
        }
        if (typern.size() != typerr.size()) {
            Error.adderror("d",funcnow.getLine());
        } else {
            for(int i=0;i<typerr.size();i++) {
                if(!typern.get(i).equals(typerr.get(i))) {
                    Error.adderror("e",funcnow.getLine());
                    break;
                }
            }
        }
    }

    private void calrp(MidItem funcrps,ArrayList<Integer> typern) {
        ArrayList<Treenode> exps = funcrps.getNodes();
        for (Treenode exp: exps) {
            if (exp instanceof MidItem) {
                int type = caltp_exp((MidItem) exp);
                typern.add(type);
            }
        }
    }

    private int caltp_exp(MidItem exp) {
        MidItem addexp = (MidItem) exp.getfirst();
        int ans = caltp_addexp(addexp);
        return ans;
    }

    private int judgeadd(int typen,int typeo){
        if (typeo == typen) {
            return typeo;
        } else {
            if (typeo == 1) {
                if (typen == 4) {
                    return 4;
                } else if (typen == 2) {
                    return 2;
                } else if(typen == 3){
                    return 3;
                } else {
                    return 1;
                }
            } else if (typeo == 2) {
                return typen;
            } else if (typeo == 3) {
                if (typen == 1) {
                    return 3;
                } else if (typen == 2) {
                    return 2;
                } else {
                    return 3; }
            } else {
                if (typen == 1) {
                    return 4;
                } else if (typen == 2) {
                    return 2;
                } else {
                    return 4; }
            }
        }
    }

    private int caltp_addexp(MidItem addexp) {
        int ans = 2;
        ArrayList<Treenode> mulexps = addexp.getNodes();
        for (Treenode mulexp : mulexps) {
            if (mulexp instanceof MidItem) {
                MidItem mulexpn = (MidItem) mulexp;
                int tmptype = caltp_mulexp(mulexpn);
                ans = judgeadd(tmptype,ans);
            }
        }
        return ans;
    }

    private int judgemul(int typen,int typeo) {
        if (typeo == typen) {
            return typeo;
        } else {
            if (typeo == 1) {
                if (typen == 2) {
                    return 2;
                }
                return 1;
            } else if (typeo == 2) {
                return typen;
            } else {
                if (typen == 2) {
                    return 2;
                }
                return 1;
            }
        }
    }

    private int caltp_mulexp(MidItem mulexp) {
        int ans = 2;
        ArrayList<Treenode> uaexps = mulexp.getNodes();
        for (Treenode uaexp : uaexps) {
            if (uaexp instanceof MidItem) {
                MidItem uaexpn = (MidItem) uaexp;
                int tmptype = caltp_uaexp(uaexpn);
                ans = judgemul(tmptype,ans);
            }
        }
        return ans;
    }

    private int caltp_uaexp(MidItem uaexp) {
        int ans;
        ArrayList<Treenode> nodes = uaexp.getNodes();
        if (nodes.get(0) instanceof SymItem) {
            SymItem sym = (SymItem) nodes.get(0);
            ans = table.findfunctype(sym.getName());
        } else {
            MidItem mid = (MidItem) nodes.get(0);
            if (nodes.size()==1) {
                ans = caltp_priexp(mid);
            } else {
                ans = caltp_uaexp((MidItem) nodes.get(1));
            }
        }
        return ans;
    }

    private int caltp_priexp(MidItem priexp) {
        int ans = 2;
        ArrayList<Treenode> nodes = priexp.getNodes();
        if (nodes.size()==1) {
            MidItem tmp = (MidItem) nodes.get(0);
            if (tmp.getType().equals("Number")) {
                ans = 1;
            } else if (tmp.getType().equals("LVal")) {
                ans = caltp_lval(tmp);
            }
        } else {
            MidItem exp = (MidItem) nodes.get(1);
            ans = caltp_exp(exp);
        }
        return ans;
    }

    private int caltp_lval(MidItem lval) {
        int ans = 2;
        ArrayList<Treenode> nodes = lval.getNodes();
        SymItem ident = (SymItem) nodes.get(0);
        int typeid = table.findvartype(ident.getName());
        int dim = cal_dim(nodes);
        if (typeid == 1) {
            ans = 1;
        } else if (typeid == 3) {
            if (dim==0) {
                ans = 3;
            } else {
                ans = 1; }
        } else if (typeid == 4) {
            if (dim == 2) {
                ans = 1;
            } else if (dim == 1) {
                ans = 3;
            } else {
                ans = 4; } }
        return ans;
    }
}
