public class Compiler {
    public static void main(String[] args) throws Exception{
        boolean optim = true;
        T_Keyword keyword = new T_Keyword();
        E_Error error = new E_Error();
        G_Lexical words = new G_Lexical(false);
        G_Parser parser = new G_Parser(words.getItems());
        parser.CompUnit();
        E_MEJudge ejudge = new E_MEJudge(parser.getCompuint());
        if (error.gethaserror()) {
            E_Error.print_error();
        } else if (optim) {
            Optim_GenMcode mid = new Optim_GenMcode(parser.getCompuint());
            M_UniqueName uniconst = new M_UniqueName(mid.getmidcode());
            uniconst.printmid();
            Optim_Midcodes optimmid = new Optim_Midcodes(uniconst.getNew_quas());
            optimmid.printmid();
            Optim_Varlife varlife = new Optim_Varlife(optimmid.getOptim());
            Optim_MOcode out = new Optim_MOcode(optimmid.getOptim(),
                    mid.getTable(), varlife.getFuncvartable());
            out.print_target();
            Optim_Ocode optimout = new Optim_Ocode(out.getTargetcode());
            optimout.print_target();
        } else {
            M_GenMcode mid = new M_GenMcode(parser.getCompuint());
            M_UniqueName uniconst = new M_UniqueName(mid.getmidcode());
            uniconst.printmid();
            O_MOcode out = new O_MOcode(uniconst.getNew_quas(), mid.getTable());
            out.print_target();
        }
    }

    public static boolean isidf(char x){
        return (x>='a' && x<='z') || (x>='A' && x<='Z') || x=='_' ;
    }
    public static boolean isnum(char x){
        return (x>='0' && x<='9');
    }
}
