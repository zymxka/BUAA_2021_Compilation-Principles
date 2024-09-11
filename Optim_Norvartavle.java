import java.util.HashMap;

public class Optim_Norvartavle {
    private HashMap<Integer,Optim_Midvar> regs = new HashMap<>();

    public Optim_Norvartavle() {
        for(int i=0;i<=6;i++){
            Optim_Midvar init = new Optim_Midvar("$null",0,0,0,0,0);
            regs.put(i,init);
        }
    }

    public HashMap<Integer,Optim_Midvar> getRegs() {
        return this.regs;
    }

    public int JudgeNorvar(int index) {
        int ret = -1;
        for(int i=0;i<=6;i++) {
            Optim_Midvar var = regs.get(i);
            if (var.getName().equals("$null")) {
                ret = i;
                break;
            }
        }
        if (ret != -1) {
            return ret;
        }
        for(int i=0;i<=6;i++) {
            Optim_Midvar var = regs.get(i);
            if (var.getDeadtime()<index) {
                ret = i;
                break;
            }
        }
        return ret;
    }

    public void AddNorvar(String name,int index,int dtime,int clas,int type,int level,int localoff) {
        Optim_Midvar nvar = new Optim_Midvar(name,dtime,clas,type,level,localoff);
        regs.remove(index);
        regs.put(index,nvar);
    }

    public int FindNorvar(String name,int clas,int type,int level) {
        int ans = -1;
        for(int i=0;i<=6;i++) {
            Optim_Midvar var = regs.get(i);
            if (var.getName().equals(name) && var.getClas()==clas
                    && var.getType()==type && var.getLevel()==level) {
                ans = i;
                break;
            }
        }
        return ans;
    }

}
