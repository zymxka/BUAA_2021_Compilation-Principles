import java.util.ArrayList;
import java.util.HashMap;

public class Keyword {
    public static ArrayList<SymItem> keys = new ArrayList<SymItem>(){};
    public static HashMap<Character,Typename> syms = new HashMap<Character,Typename>(){};
    //pig

    private void init(){
        keys.add(new SymItem(Typename.MAINTK,"main",0));
        keys.add(new SymItem(Typename.CONSTTK,"const",0));
        keys.add(new SymItem(Typename.INTTK,"int",0));
        keys.add(new SymItem(Typename.BREAKTK,"break",0));
        keys.add(new SymItem(Typename.CONTINUETK,"continue",0));
        keys.add(new SymItem(Typename.IFTK,"if",0));
        keys.add(new SymItem(Typename.ELSETK,"else",0));
        keys.add(new SymItem(Typename.WHILETK,"while",0));
        keys.add(new SymItem(Typename.GETINTTK,"getint",0));
        keys.add(new SymItem(Typename.PRINTFTK,"printf",0));
        keys.add(new SymItem(Typename.RETURNTK,"return",0));
        keys.add(new SymItem(Typename.VOIDTK,"void",0));
        syms.put('+',Typename.PLUS);
        syms.put('-',Typename.MINU);
        syms.put('*',Typename.MULT);
        syms.put('/',Typename.DIV);
        syms.put('%',Typename.MOD);
        syms.put(';',Typename.SEMICN);
        syms.put(',',Typename.COMMA);
        syms.put('(',Typename.LPARENT);
        syms.put(')',Typename.RPARENT);
        syms.put('[',Typename.LBRACK);
        syms.put(']',Typename.RBRACK);
        syms.put('{',Typename.LBRACE);
        syms.put('}',Typename.RBRACE);
    }

    public Keyword(){
        init();
    }
}
