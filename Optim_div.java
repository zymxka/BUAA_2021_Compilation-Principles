import java.util.ArrayList;

public class Optim_div {
    //$a0,$a2,可用
    // targer $v1,可用
    // beichushu $a1
    private String target;
    private String beichushu;
    private int chushu;
    private String op;
    private ArrayList<String> optim;
    private boolean neg;
    private int idx;
    private int lableidx;

    //var1是一个reg,var2是一个数,var3也是一个reg!!!
    public Optim_div(String target,String var1,String var2,String op,int lableidx) {
        this.target = target;
        this.beichushu = var1;
        this.chushu = StrToint(var2);
        this.op = op;
        this.optim = new ArrayList<>();
        this.neg = false;
        this.idx = -1;
        this.lableidx = lableidx;
        gen_Optim_Div();
    }

    public ArrayList<String> getOptim() {
        return this.optim;
    }

    private int StrToint(String s) {
        return new Integer(s);
    }

    private void gen_Optim_Div() {
        //处理除数的符号
        if (chushu < 0) {
            chushu = -chushu;
            neg = true;
        }

        //计算k
        int copy = chushu;
        while(copy!=0) {
            idx++;
            copy/=2;
        }

        //进行特判
        int hasone = chushu&(chushu-1);
        if (hasone==0) {
            //二的幂次
            gen_2pow();
        } else {
            //不是二的幂次
            gen_Not2pow();
        }
    }

    private void gen_2pow() {
        String judgeasign = "slt $a0,"+beichushu+",$0";//q = (ll)(a)-(a<0);
        optim.add(judgeasign);
        String subbeichushu = "sub "+target+","+beichushu+",$a0";
        String youyi = "sra "+target+","+target+","+idx;
        String addret = "add "+target+","+target+",$a0";
        optim.add(subbeichushu);
        optim.add(youyi);
        optim.add(addret);
        Tail();
    }

    private void gen_Not2pow() {
        double g = 1;
        for(int i=1;i<=idx+32;i++) {
            g = g*2;
        }
        double f = g/chushu;
        long m = (long) Math.ceil(f);
        String judgeasign = "slt $a0,"+beichushu+",$0";
        String genabsa = "abs "+beichushu+","+beichushu;
        String setm = "li $a2,"+m;
        String multq = "multu $a2,"+beichushu;
        String moveq = "mfhi "+target;
        String youyi = "srl "+target+","+target+","+idx;
        String neglable = "bne $a0,$0,$NegLable"+lableidx;
        String jendlable = "j $NegEndlable"+lableidx;
        String negl = "$NegLable"+lableidx+":";
        String genneg = "neg "+target+","+target;
        String gennegbei = "neg "+beichushu+","+beichushu;
        String end = "$NegEndlable"+lableidx+":";
        optim.add(judgeasign);
        optim.add(genabsa);
        optim.add(setm);
        optim.add(multq);
        optim.add(moveq);
        optim.add(youyi);
        optim.add(neglable);
        optim.add(jendlable);
        optim.add(negl);
        optim.add(genneg);
        optim.add(gennegbei);
        optim.add(end);
        Tail();
    }

    private void Tail() {
        if (neg) {
            String negret = "sub "+target+",$0,"+target;
            optim.add(negret);
            chushu = -chushu;
        }
        if (!op.equals("DIV")) {
            String setchushu = "li $a2,"+chushu;
            String multicq = "mul "+target+",$a2,"+target;
            String subacq = "sub "+target+","+beichushu+","+target;
            optim.add(setchushu);
            optim.add(multicq);
            optim.add(subacq);
        }
    }
}
