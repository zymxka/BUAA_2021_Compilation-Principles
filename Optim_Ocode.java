import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class Optim_Ocode {
    private ArrayList<String> oldcodes;
    private ArrayList<String> optim;

    public Optim_Ocode(ArrayList<String> oldcodes) {
        this.oldcodes = oldcodes;
        this.optim = new ArrayList<>();
        Optim();
    }

    public void print_target() throws IOException {
        BufferedWriter bout = new BufferedWriter(new FileWriter("mips.txt"));
        for (String i : oldcodes) {
            bout.write(i);
            bout.newLine();
        }
        bout.close();
    }

    private void Optim() {
        FixLi_Move();
    }

    private void FixLi_Move() {
        for(int i=0;i<oldcodes.size()-1;i++) {
            String listr = oldcodes.get(i);
            String movstr = oldcodes.get(i+1);
            if (listr.length() > 2 && movstr.length() > 4) {
                if((listr.substring(0,2)).equals("li")
                    && (movstr.substring(0,4)).equals("move")) {
                    String lireg = getlireg(listr);
                    String movreg = getfinal(movstr);
                    if (lireg.equals(movreg)) {
                        //System.out.println("ok");
                        String numstr = getfinal(listr);
                        String newstr = gen_Li_Move(movstr,numstr);
                        optim.add(newstr);
                        i++;
                    } else {
                        optim.add(listr);
                    }
                } else {
                    optim.add(listr);
                }
            } else {
                optim.add(listr);
            }
        }
        optim.add(oldcodes.get(oldcodes.size()-1));
        this.oldcodes = this.optim;
        this.optim = new ArrayList<>();
    }

    private String getlireg(String listr) {
        int idx = 0;
        StringBuilder ans = new StringBuilder();
        while (listr.charAt(idx)!='$') {
            idx++;
        }
        while (listr.charAt(idx)!=',') {
            ans.append(listr.charAt(idx));
            idx++;
        }
        //System.out.println(ans.toString());
        return ans.toString();
    }

    private String getfinal(String movstr) {
        int idx=0;
        while (movstr.charAt(idx)!=',') {
            idx++;
        }
        //System.out.println(movstr.substring(idx+1));
        return movstr.substring(idx+1);
    }

    private String gen_Li_Move(String movstr,String numstr) {
        int idx=0;
        String ans = "li";
        while (movstr.charAt(idx)!=' ') {
            idx++;
        }
        int start = idx;
        while (movstr.charAt(idx)!=',') {
            idx++;
        }
        ans = ans + movstr.substring(start,idx)+","+numstr;
        //System.out.println(ans);
        return ans;
    }
}
