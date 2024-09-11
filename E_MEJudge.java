import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class E_MEJudge {
    private N_MidItem compunit;
    private int cirdepth;
    private int level;
    private String functypenow = "";
    private String rettypenow = "";
    private IT_IdentTable table;
    private boolean funcfind = true;

    public E_MEJudge(N_MidItem compunit) {
        this.compunit = compunit;
        this.cirdepth = 0;
        this.level = 0;
        this.table = new IT_IdentTable();
        search_error(compunit);
    }

    public void print_comp() throws IOException {
        FileWriter writer = new FileWriter("output.txt");
        writer.write(compunit.toString());
        writer.flush();
        writer.close();
    }

    public void search_error(N_Treenode now) {
        if (now instanceof N_MidItem) {
            N_MidItem midnow = ((N_MidItem) now);
            boolean incir = false;
            boolean inblk = false;
            ArrayList<N_Treenode> sons = midnow.getNodes();

            if (midnow.getType().equals("Stmt")) {
                if (sons.get(0) instanceof N_SymItem) {
                    N_SymItem son0 = (N_SymItem) sons.get(0);
                    if (son0.getType().equals(T_Typename.PRINTFTK)) {
                        ejudge_i(sons);
                    } else if (son0.getType().equals(T_Typename.WHILETK)) {
                        this.cirdepth++;
                        incir = true;
                    } else if (son0.getType().equals(T_Typename.RETURNTK)) {
                        ejudge_fg(sons);
                    }
                } else if (sons.get(0) instanceof N_MidItem) {
                    N_MidItem sonm0 = (N_MidItem) sons.get(0);
                    if (sonm0.getType().equals("LVal")) {
                        if (sons.get(1) instanceof N_SymItem
                       && ((N_SymItem) sons.get(1)).getType().equals(T_Typename.ASSIGN)) {
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
                if (sons.get(0) instanceof N_SymItem) {
                    this.funcfind = true;
                    ejudge_cde_u(sons);
                    if (this.funcfind) {
                        ejudge_de(sons);
                    }
                }
            } else if (midnow.getType().equals("LVal")) {
                ejudge_c_l(sons);
            }

            for(N_Treenode son:sons) {
                search_error(son);
            }

            if (incir) { this.cirdepth--; }
            if (inblk) { table.poplevel(level);this.level--;}
            if (midnow.getType().equals("FuncDef")
               || midnow.getType().equals("MainFuncDef")) {
                if (this.functypenow.equals("int")) {
                    N_MidItem blk = (N_MidItem) midnow.getlast();
                    ArrayList<N_Treenode> blkitems = blk.getNodes();
                    this.rettypenow = "";
                    N_Treenode items = blkitems.get(blkitems.size()-2);
                        if (items instanceof N_MidItem) {
                            N_MidItem retsen = (N_MidItem) items;
                            N_MidItem stmt = (N_MidItem) retsen.getfirst();
                            ArrayList<N_Treenode> eles = stmt.getNodes();
                            if (eles.get(0) instanceof N_SymItem
                                    && ((N_SymItem) eles.get(0)).getType().equals(T_Typename.RETURNTK) ) {
                                this.rettypenow = "int";
                            }
                        }

                }
                int retline = ((N_SymItem)((N_MidItem) midnow.getlast()).getlast()).getLine();
                out_func(retline);
            }
        } else if (now instanceof N_SymItem) {
            N_SymItem symnow = (N_SymItem) now;
            if (symnow.getType() == T_Typename.BREAKTK ||
                symnow.getType() == T_Typename.CONTINUETK) {
                ejudge_m(symnow.getLine());
            }
        }
    }

    private void in_func(N_Treenode type) {
        N_SymItem ft = (N_SymItem) ((N_MidItem) type).getfirst();
        if (ft.getType() == T_Typename.INTTK) {
            this.functypenow = "int";
        } else if (ft.getType() == T_Typename.VOIDTK) {
            this.functypenow = "void";
        }
        this.rettypenow = "";
    }

    private void in_main_func(N_Treenode type) {
        this.functypenow = "int";
        this.rettypenow = "";
    }

    private void out_func(int line) {
        if (this.rettypenow.equals("") && this.functypenow.equals("int")) {
            E_Error.adderror("g",line);
        }
        this.rettypenow = "";
        this.functypenow = "";
    }

    private void ejudge_i(ArrayList<N_Treenode> elements) {
        int index = 2;
        N_SymItem fstr = (N_SymItem) elements.get(index);
        int fnum = fstr.getformatc();
        int pnum = 0;
        index++;
        N_SymItem next = (N_SymItem) elements.get(index);
        if (next.getType() == T_Typename.COMMA) {
            while(next.getType() == T_Typename.COMMA) {
                pnum++;
                index += 2;
                next = (N_SymItem) elements.get(index);
            }
        }
        if (fnum != pnum) {
            E_Error.adderror("l",next.getLine());
        }
    }

    private void ejudge_m(int line) {
        if (this.cirdepth == 0) {
            E_Error.adderror("m",line);
        }
    }

    private void ejudge_fg(ArrayList<N_Treenode> elements) {
        N_SymItem rets = (N_SymItem) elements.get(0);
        N_Treenode last = elements.get(elements.size()-1);
        if (last instanceof N_SymItem && ((N_SymItem) last).getType().equals(T_Typename.SEMICN)) {
            if (elements.size() == 2) {
                this.rettypenow = "void";
                if (!this.rettypenow.equals(this.functypenow)) {
                    E_Error.adderror("g",rets.getLine());
                }
            } else {
                this.rettypenow = "int";
                if (!this.rettypenow.equals(this.functypenow)) {
                    E_Error.adderror("f",rets.getLine());
                }
            }
        } else {
            if (elements.size() == 1) {
                this.rettypenow = "void";
                if (!this.rettypenow.equals(this.functypenow)) {
                    E_Error.adderror("g",rets.getLine());
                }
            } else {
                this.rettypenow = "int";
                if (!this.rettypenow.equals(this.functypenow)) {
                    E_Error.adderror("f",rets.getLine());
                }
            }
        }
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

    private void ejudge_b_c(ArrayList<N_Treenode> elements) {
        N_SymItem ident = (N_SymItem) elements.get(0);
        int dim = cal_dim(elements);
        if (table.mulvar(ident.getName(), level) != -1) {
            E_Error.adderror("b", ident.getLine());
        }
        if (dim == 0) {
            table.additem(ident.getName(),1,1,level);
        } else if (dim == 1) {
            table.additem(ident.getName(),3,1,level);
        } else if (dim == 2) {
            table.additem(ident.getName(),4,1,level);
        }
    }

    private void ejudge_b_v(ArrayList<N_Treenode> elements) {
        N_SymItem ident = (N_SymItem) elements.get(0);
        int dim = cal_dim(elements);
        if (table.mulvar(ident.getName(), level) != -1) {
            E_Error.adderror("b", ident.getLine());
        }
        if (dim == 0) {
            table.additem(ident.getName(),1,2,level);
        } else if (dim == 1) {
            table.additem(ident.getName(),3,2,level);
        } else if (dim == 2) {
            table.additem(ident.getName(),4,2,level);
        }
    }

    private void ejudge_b_f(N_Treenode ident) {
        N_SymItem name = (N_SymItem) ident;
        if (table.mulvar(name.getName(),level) != -1) {
            E_Error.adderror("b", name.getLine());
        }
        if (this.functypenow.equals("int")) {
            table.additem(name.getName(),1,4,level);
        } else {
            table.additem(name.getName(),2,4,level);
        }
    }

    private void ejudge_b_fp(ArrayList<N_Treenode> elements) {
        N_SymItem ident = (N_SymItem) elements.get(1);
        int dim = cal_dim(elements);
        if (table.mulvar(ident.getName(), level + 1) != -1) {
            E_Error.adderror("b", ident.getLine());
        }
        if (dim == 0) {
            table.additem(ident.getName(),1,3,level+1);
        } else if (dim == 1) {
            table.additem(ident.getName(),3,3,level+1);
        } else if (dim == 2) {
            table.additem(ident.getName(),4,3,level+1);
        }
    }

    private void ejudge_cde_u(ArrayList<N_Treenode> elements) {
        N_SymItem funcnow = (N_SymItem) elements.get(0);
        if (table.findfunc(funcnow.getName())==-1) {
            E_Error.adderror("c",funcnow.getLine());
            this.funcfind = false;
        }
    }

    private void ejudge_c_l(ArrayList<N_Treenode> elements) {
        N_SymItem ident = (N_SymItem) elements.get(0);
        if (table.findvar(ident.getName(),level)==-1) {
            E_Error.adderror("c",ident.getLine());
        }
    }

    private void ejudge_h(N_Treenode ident) {
        N_MidItem lval = (N_MidItem) ident;
        N_SymItem tmpsym = (N_SymItem) lval.getfirst();
        if (table.isconst(tmpsym.getName(),level)){
            E_Error.adderror("h",tmpsym.getLine());
        }
    }

    private void ejudge_de(ArrayList<N_Treenode> elements) {
        N_SymItem funcnow = (N_SymItem) elements.get(0);
        ArrayList<Integer> typern = new ArrayList<>();
        ArrayList<Integer> typerr = table.funcrtype(funcnow.getName());
        if (elements.size() >= 3 && elements.get(2) instanceof N_MidItem) {
            N_MidItem funcrps = (N_MidItem) elements.get(2);
            //System.out.println(funcrps.getType());
            calrp(funcrps,typern);
        }
        if (typern.size() != typerr.size()) {
            E_Error.adderror("d",funcnow.getLine());
        } else {
            for(int i=0;i<typerr.size();i++) {
                if(!typern.get(i).equals(typerr.get(i))) {
                    E_Error.adderror("e",funcnow.getLine());
                    break;
                }
            }
        }
    }

    private void calrp(N_MidItem funcrps, ArrayList<Integer> typern) {
        ArrayList<N_Treenode> exps = funcrps.getNodes();
        for (N_Treenode exp: exps) {
            if (exp instanceof N_MidItem) {
                int type = caltp_exp((N_MidItem) exp);
                typern.add(type);
            }
        }
    }

    private int caltp_exp(N_MidItem exp) {
        N_MidItem addexp = (N_MidItem) exp.getfirst();
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

    private int caltp_addexp(N_MidItem addexp) {
        int ans = 2;
        ArrayList<N_Treenode> mulexps = addexp.getNodes();
        for (N_Treenode mulexp : mulexps) {
            if (mulexp instanceof N_MidItem) {
                N_MidItem mulexpn = (N_MidItem) mulexp;
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

    private int caltp_mulexp(N_MidItem mulexp) {
        int ans = 2;
        ArrayList<N_Treenode> uaexps = mulexp.getNodes();
        for (N_Treenode uaexp : uaexps) {
            if (uaexp instanceof N_MidItem) {
                N_MidItem uaexpn = (N_MidItem) uaexp;
                int tmptype = caltp_uaexp(uaexpn);
                ans = judgemul(tmptype,ans);
            }
        }
        return ans;
    }

    private int caltp_uaexp(N_MidItem uaexp) {
        int ans;
        ArrayList<N_Treenode> nodes = uaexp.getNodes();
        if (nodes.get(0) instanceof N_SymItem) {
            N_SymItem sym = (N_SymItem) nodes.get(0);
            ans = table.findfunctype(sym.getName());
        } else {
            N_MidItem mid = (N_MidItem) nodes.get(0);
            if (nodes.size()==1) {
                ans = caltp_priexp(mid);
            } else {
                ans = caltp_uaexp((N_MidItem) nodes.get(1));
            }
        }
        return ans;
    }

    private int caltp_priexp(N_MidItem priexp) {
        int ans = 2;
        ArrayList<N_Treenode> nodes = priexp.getNodes();
        if (nodes.size()==1) {
            N_MidItem tmp = (N_MidItem) nodes.get(0);
            if (tmp.getType().equals("Number")) {
                ans = 1;
            } else if (tmp.getType().equals("LVal")) {
                ans = caltp_lval(tmp);
            }
        } else {
            N_MidItem exp = (N_MidItem) nodes.get(1);
            ans = caltp_exp(exp);
        }
        return ans;
    }

    private int caltp_lval(N_MidItem lval) {
        int ans = 2;
        ArrayList<N_Treenode> nodes = lval.getNodes();
        N_SymItem ident = (N_SymItem) nodes.get(0);
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
