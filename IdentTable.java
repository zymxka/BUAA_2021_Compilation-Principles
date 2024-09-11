import java.util.ArrayList;
import java.util.HashMap;

public class IdentTable {
    private ArrayList<IdentItem> tableitems = new ArrayList<>();
    private ArrayList<IdentItem> serachitems = new ArrayList<>();
    private ArrayList<FuncItem> funcs = new ArrayList<>();
    private int index;

    public IdentTable() {
        this.index = 0;
    }

    public boolean isconst(String name,int level){
        for(int i=tableitems.size()-1;i>=0;i--){
            IdentItem tmp = tableitems.get(i);
            if(tmp.getName().equals(name) && tmp.getLevel()<=level){
                if (tmp.getType() == 1) {
                    return true;
                } else if (tmp.getType() == 2 || tmp.getType() == 3) {
                    return false;
                }
            }
        }
        return false;
    }

    public ArrayList<Integer> funcrtype(String name) {
        int index = 0;
        for(int i=0;i<funcs.size();i++) {
            if(funcs.get(i).getName().equals(name)) {
                index = funcs.get(i).getIndex();
            }
        }
        ArrayList<Integer> ans = new ArrayList<>();
        for(int i=index+1;i<serachitems.size();i++){
            IdentItem tmp = serachitems.get(i);
            if (tmp.getType() != 3) {
                break;
            }
            ans.add(tmp.getClas());
        }
        return ans;
    }

    public void additem(String name,int clas,int type,int level) {
        IdentItem tmp = new IdentItem(name,clas,type,level,index);
        tableitems.add(tmp);
        serachitems.add(tmp);
        if (type == 4) {
            FuncItem funcitem = new FuncItem(name,index);
            funcs.add(funcitem);
        }
        index++;
    }

    public void poplevel(int level) {
        for(int i=tableitems.size()-1;i>=0;i--){
            if (tableitems.get(i).getLevel()==level) {
                tableitems.remove(i);
            }
        }
    }

    public int findfunc(String name) {
        for(int i=0;i<funcs.size();i++) {
            if(funcs.get(i).getName().equals(name)) {
                return funcs.get(i).getIndex();
            }
        }
        return -1;
    }

    public int findvartype(String name) {
        int ans = 0;
        for(int i=tableitems.size()-1;i>=0;i--) {
            IdentItem tmp = tableitems.get(i);
            if (tmp.getName().equals(name) && tmp.getType() != 4) {
                ans = tmp.getClas();
                break;
            }
        }
        return ans;
    }

    public int findfunctype(String name) {
        int ans = 0;
        for(int i=tableitems.size()-1;i>=0;i--) {
            IdentItem tmp = tableitems.get(i);
            if (tmp.getName().equals(name) && tmp.getType()==4) {
                ans = tmp.getClas();
                break;
            }
        }
        return ans;
    }

    public int findvar(String name,int level) {
        for(int i=tableitems.size()-1;i>=0;i--) {
            IdentItem tmp = tableitems.get(i);
            if(tmp.getName().equals(name) && tmp.getLevel() <= level
                && tmp.getType() != 4) {
                return i;
            }
        }
        return -1;
    }

    public int mulvar(String name,int level) {
        for (int i=0;i<tableitems.size();i++) {
            IdentItem tmp = tableitems.get(i);
            if (tmp.getName().equals(name) && tmp.getLevel()==level) {
                return i;
            }
        }
        return -1;
    }


}
