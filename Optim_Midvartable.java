import java.util.HashMap;

public class Optim_Midvartable {
    private HashMap<Integer,Optim_Midvar> regs = new HashMap<>();

    public Optim_Midvartable() {
        for(int i=0;i<=9;i++){
            Optim_Midvar init = new Optim_Midvar("$null",false);
            regs.put(i,init);
        }
    }

    public int AddMidvar(String name) {
        int ans=-1;
        for(int i=0;i<=9;i++){
            Optim_Midvar var = regs.get(i);
            if (var.getName().equals("$null")) {
                ans = i;
                regs.remove(i);
                Optim_Midvar now = new Optim_Midvar(name,false);
                regs.put(i,now);
                break;
            }
        }
        return ans;
    }

    public int FindMidvar(String name) {
        int ans = -1;
        for(int i=0;i<=9;i++){
            Optim_Midvar var = regs.get(i);
            if (var.getName().equals(name)) {
                ans = i;
                break;
            }
        }
        return ans;
    }

    public void DelMidvar(String name) {
        for(int i=0;i<=9;i++){
            Optim_Midvar var = regs.get(i);
            if (var.getName().equals(name)) {
                regs.remove(i);
                Optim_Midvar empty = new Optim_Midvar("$null",false);
                regs.put(i,empty);
                break;
            }
        }
    }

}
