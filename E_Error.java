import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.TreeMap;

public class E_Error {
    private static boolean haserror = false;
    private static HashMap<String,String> errormap = new HashMap<>();
    private static TreeMap<Integer,String> errors = new TreeMap<>();

    public E_Error() {
        init();
    }

    private void init() {
        errormap.put("a","Illegal Character");
        errormap.put("b","Multiple Ident");
        errormap.put("c","Undefined Ident");
        errormap.put("d","FuncPara Num Wrong");
        errormap.put("e","FuncPara Type Wrong");
        errormap.put("f","Void with Wrong Ret");
        errormap.put("g","Int with No Ret");
        errormap.put("h","Change Const");
        errormap.put("i","No ;");
        errormap.put("j","No )");
        errormap.put("k","No ]");
        errormap.put("l","Printf Format Error");
        errormap.put("m","Using bc without Cir");
    }

    public boolean gethaserror() {
        return E_Error.haserror;
    }

    public static void adderror(String etype,int linenum) {
        if (!E_Error.haserror) {
            E_Error.haserror = true;
        }
        E_Error.errors.put(linenum,etype);
    }

    public static void print_error() throws IOException {
        BufferedWriter bout = new BufferedWriter(new FileWriter("error.txt"));
        for (int i : errors.keySet()) {
            String s = i + " " + errors.get(i);
            /*if(errors.get(i).equals("d")) {
                continue;
            }*/
            bout.write(s);
            bout.newLine();
        }
        bout.close();
    }

}
