import java.util.ArrayList;
import java.util.HashMap;

public class T_Keyword {
    public static ArrayList<N_SymItem> keys = new ArrayList<N_SymItem>(){};
    public static HashMap<Character, T_Typename> syms = new HashMap<Character, T_Typename>(){};
    //pig

    private void init(){
        keys.add(new N_SymItem(T_Typename.MAINTK,"main",0));
        keys.add(new N_SymItem(T_Typename.CONSTTK,"const",0));
        keys.add(new N_SymItem(T_Typename.INTTK,"int",0));
        keys.add(new N_SymItem(T_Typename.BREAKTK,"break",0));
        keys.add(new N_SymItem(T_Typename.CONTINUETK,"continue",0));
        keys.add(new N_SymItem(T_Typename.IFTK,"if",0));
        keys.add(new N_SymItem(T_Typename.ELSETK,"else",0));
        keys.add(new N_SymItem(T_Typename.WHILETK,"while",0));
        keys.add(new N_SymItem(T_Typename.GETINTTK,"getint",0));
        keys.add(new N_SymItem(T_Typename.PRINTFTK,"printf",0));
        keys.add(new N_SymItem(T_Typename.RETURNTK,"return",0));
        keys.add(new N_SymItem(T_Typename.VOIDTK,"void",0));
        syms.put('+', T_Typename.PLUS);
        syms.put('-', T_Typename.MINU);
        syms.put('*', T_Typename.MULT);
        syms.put('/', T_Typename.DIV);
        syms.put('%', T_Typename.MOD);
        syms.put(';', T_Typename.SEMICN);
        syms.put(',', T_Typename.COMMA);
        syms.put('(', T_Typename.LPARENT);
        syms.put(')', T_Typename.RPARENT);
        syms.put('[', T_Typename.LBRACK);
        syms.put(']', T_Typename.RBRACK);
        syms.put('{', T_Typename.LBRACE);
        syms.put('}', T_Typename.RBRACE);
    }

    public T_Keyword(){
        init();
    }
}
