import java.io.*;
import java.util.ArrayList;

public class Lexical {

    private String fname;
    private File fin;
    private InputStreamReader istream;
    private BufferedReader bfread;
    private int linenum = 0;
    private int lineptr = 0;
    private int linelen = 0;
    private ArrayList<SymItem> items = new ArrayList<>();
    private ArrayList<String> linestr = new ArrayList<>();
    private char tmpchar = '\0';
    private boolean isend = false;
    private boolean changeline = false;

    public Lexical() throws IOException {
        this.fname = "testfile.txt";
        this.fin = new File(fname);
        this.istream = new InputStreamReader(new FileInputStream(fin));
        this.bfread = new BufferedReader(istream);
        String ltmp = null;
        while((ltmp = bfread.readLine()) != null){
            linestr.add(ltmp);
        }
        rep_anno();
        if(linestr.size() != 0) {
            //tmpchar = linestr.get(0).charAt(0);
            linelen = linestr.get(linenum).length();
            while(linelen==0 && linenum<linestr.size()){
                linenum++;
                linelen = linestr.get(linenum).length();
            }
            if(linelen!=0){
                tmpchar = linestr.get(linenum).charAt(0);
            } else {
                isend = true;
            }
        }
        gene_words();
    }

    public ArrayList<SymItem> getItems() {
        return this.items;
    }

    private void rep_anno(){
        boolean in_anno = false; //one-line anno
        boolean cro_anno = false; //one-or-multi line anno
        boolean in_str = false;
        for (int i=0;i<linestr.size();i++){
            StringBuilder tmp = new StringBuilder(linestr.get(i));
            for (int j=0;j<tmp.length();j++){
                char c_tmp = tmp.charAt(j);
                if (in_str && c_tmp != '"') {
                    continue;
                } else if(in_str && c_tmp == '"'){
                    in_str = false;
                } else if(!in_str && c_tmp == '"' && !in_anno && !cro_anno){
                    in_str = true;
                } else if (in_anno || (cro_anno && ((c_tmp != '*') ||
                        (c_tmp=='*' &&j<tmp.length()-1 && tmp.charAt(j+1)!='/')
                        ||(c_tmp=='*'&&j==tmp.length()-1)))) {
                    //anno -> space
                    tmp.setCharAt(j,' ');
                } else if (cro_anno && c_tmp=='*' &&
                        j<tmp.length()-1 && tmp.charAt(j+1)=='/'){
                    //cro_anno end
                    cro_anno = false;
                    tmp.setCharAt(j,' ');
                    tmp.setCharAt(j+1,' ');
                } else if (j<tmp.length()-1 && c_tmp=='/' && tmp.charAt(j+1)=='/') {
                    //in_anno begin
                    in_anno = true;
                    tmp.setCharAt(j,' ');
                } else if (j<tmp.length()-1 && c_tmp=='/' && tmp.charAt(j+1)=='*') {
                    //cro_anno begin
                    //System.out.println(c_tmp);
                    cro_anno = true;
                    tmp.setCharAt(j,' ');
                    tmp.setCharAt(j+1,' ');
                }
            }
            in_anno = false;
            //System.out.println(tmp.toString());
            linestr.set(i,tmp.toString());
        }
    }

    private void nextch(){
        if (changeline = true) {
            changeline = false;
        }
        while (lineptr == linelen) {
            if (linenum == linestr.size()-1 || linestr.size() == 0){
                isend = true;
                return;
            }
            linenum++;
            changeline = true;
            lineptr=0;
            linelen=linestr.get(linenum).length();
        }
        tmpchar = linestr.get(linenum).charAt(lineptr);
        lineptr++;
    }

    private int get_nowline() {
        if (changeline == true) {
            return linenum;
        } else {
            return linenum+1;
        }
    }

    private SymItem get_tpw(){
        Typename tmptype = null;
        while((tmpchar==' '||tmpchar=='\t') && !isend) nextch();
        //System.out.println(tmpchar);
        if (isend) {
            return new SymItem(Typename.EOF,"eof",linenum+1);
        } else if (Compiler.isidf(tmpchar)) {
            StringBuilder tmpname = new StringBuilder();
            tmpname.append(tmpchar);
            int ori_line = linenum+1;
            if (lineptr < linelen) {
            nextch();
            ori_line = linenum+1;
            while ((Compiler.isidf(tmpchar)||Compiler.isnum(tmpchar))){
                tmpname.append(tmpchar);
                //System.out.println(tmpchar);
                ori_line = linenum+1;
                nextch();
            }} else {
                nextch();
            }
            int i;
            //System.out.println("ok");
            for(i=0;i<Keyword.keys.size();i++){
                String keyname = Keyword.keys.get(i).getName();
                if(keyname.equals(tmpname.toString())){
                    break;
                }
            }
            if (i<Keyword.keys.size()){
                tmptype = Keyword.keys.get(i).getType();
            } else {
                tmptype = Typename.IDENFR;
            }
            //System.out.println("okk");
            return new SymItem(tmptype,tmpname.toString(),ori_line);
        } else if (Compiler.isnum(tmpchar)){
            StringBuilder tmpnum = new StringBuilder();
            int ori_line = linenum+1;
            while (Compiler.isnum(tmpchar)){
                tmpnum.append(tmpchar);
                ori_line = linenum+1;
                nextch();
            }
            tmptype = Typename.INTCON;
            return new SymItem(tmptype,tmpnum.toString(),ori_line);
        } else if (tmpchar == '<'){
            int ori_line = linenum+1;
            nextch();
            if (tmpchar == '='){
                tmptype = Typename.LEQ;
                ori_line = linenum+1;
                nextch();
                return new SymItem(tmptype,"<=",ori_line);
            } else {
                tmptype = Typename.LSS;
                return new SymItem(tmptype,"<",ori_line);
            }
        } else if (tmpchar == '>'){
            int ori_line = linenum+1;
            nextch();
            if (tmpchar == '='){
                tmptype = Typename.GEQ;
                ori_line = linenum+1;
                nextch();
                return new SymItem(tmptype,">=",ori_line);
            } else {
                tmptype = Typename.GRE;
                return new SymItem(tmptype,">",ori_line);
            }
        } else if (tmpchar == '!'){
            int ori_line = linenum+1;
            nextch();
            if (tmpchar == '='){
                tmptype = Typename.NEQ;
                ori_line = linenum+1;
                nextch();
                return new SymItem(tmptype,"!=",ori_line);
            } else {
                tmptype = Typename.NOT;
                return new SymItem(tmptype,"!",ori_line);
            }
        } else if (tmpchar == '='){
            int ori_line = linenum+1;
            nextch();
            if (tmpchar == '='){
                tmptype = Typename.EQL;
                ori_line = linenum+1;
                nextch();
                return new SymItem(tmptype,"==",ori_line);
            } else {
                tmptype = Typename.ASSIGN;
                return new SymItem(tmptype,"=",ori_line);
            }
        } else if (tmpchar == '"'){
            StringBuilder constr = new StringBuilder();
            constr.append(tmpchar);
            int ori_line = linenum+1;
            nextch();
            boolean wf = false;
            while (tmpchar != '"'){
                ori_line = linenum+1;
                if (tmpchar=='\\'){
                    if (lineptr == linelen ||
                            (lineptr < linelen && linestr.get(linenum).charAt(lineptr)!='n')) {
                        wf = true;
                    } else {
                        constr.append(tmpchar);
                    }
                } else if (tmpchar=='%'){
                    if (lineptr == linelen ||
                            (lineptr < linelen && linestr.get(linenum).charAt(lineptr)!='d')) {
                        wf = true;
                    } else {
                        constr.append(tmpchar);
                    }
                }else if (tmpchar == 32 || tmpchar == 33 ||
                        (tmpchar >= 40 && tmpchar <= 126)) {
                    constr.append(tmpchar);
                } else {
                    Error.adderror("a",ori_line);
                    wf = true;
                }
                nextch();
            }
            if (wf) {
                Error.adderror("a",ori_line);
            }
            constr.append(tmpchar);
            tmptype = Typename.STRCON;
            ori_line = linenum+1;
            nextch();
            return new SymItem(tmptype,constr.toString(),ori_line);
        } else if (tmpchar == '&'){
            int ori_line = linenum+1;
            nextch();
            if (tmpchar == '&') {
                tmptype = Typename.AND;
                ori_line = linenum+1;
                nextch();
                return new SymItem(tmptype,"&&",ori_line);
            }
        } else if (tmpchar == '|'){
            int ori_line = linenum+1;
            nextch();
            if(tmpchar == '|'){
                tmptype = Typename.OR;
                ori_line = linenum+1;
                nextch();
                return new SymItem(tmptype,"||",ori_line);
            }
        } else if (Keyword.syms.containsKey(tmpchar)){
            tmptype = Keyword.syms.get(tmpchar);
            StringBuilder tmpname = new StringBuilder();
            tmpname.append(tmpchar);
            int ori_line = linenum+1;
            nextch();
            //System.out.println(tmpname+" num:"+linenum+" ptr:"+lineptr);
            return new SymItem(tmptype,tmpname.toString(),ori_line);
        }
        int nline = linenum+1;
        nextch();
        return new SymItem(Typename.NULL,"null",nline);
    }

    public void gene_words() {
        nextch();
        SymItem tmp = get_tpw();
        while (!tmp.getType().equals(Typename.EOF) && !tmp.getType().equals(Typename.NULL)){
            items.add(tmp);
            tmp = get_tpw();
        }
    }

    public void print_table() throws IOException {
        BufferedWriter bout = new BufferedWriter(new FileWriter("output.txt"));
        for(int i=0;i<items.size();i++){
            StringBuilder sout = new StringBuilder();
            sout.append(items.get(i).getType().toString());
            sout.append(" ");
            sout.append(items.get(i).getName());
            //sout.append(" ");
            //sout.append(items.get(i).getLine());
            bout.write(sout.toString());
            bout.newLine();
        }
        bout.close();
    }
}
