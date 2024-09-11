public class M_Quaternary {
    private String op;
    private String operant1;
    private String operant2;
    private String operant3;

    public M_Quaternary(String op, String operant1, String operant2, String operant3){
        this.op = op;
        this.operant1 = operant1;
        this.operant2 = operant2;
        this.operant3 = operant3;
    }

    public String getOp() {
        return this.op;
    }

    public String getOperant1() {
        return this.operant1;
    }

    public String getOperant2() {
        return this.operant2;
    }

    public String getOperant3() {
        return this.operant3;
    }

    public void setOperant3(String printflable) {
        this.operant3 = printflable;
    }

    public void setOperant1(String newop1) {
        this.operant1 = newop1;
    }

    public void setOperant2(String newop2) {
        this.operant2 = newop2;
    }


    @Override
    public String toString() {
        String ans = op + " " + operant1 + " " + operant2 + " " + operant3;
        return ans;
    }
}
