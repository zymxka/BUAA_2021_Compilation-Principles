import java.util.ArrayList;

public class IT_Uninametable {
    private ArrayList<IT_IdentItem> tableitems = new ArrayList<>();
    private int index;

    public IT_Uninametable() {
        this.index = 0;
    }

    //添加有单独名字的元素
    public void adduniquename(String name,int clas,int type,int level,String uniname) {
        IT_IdentItem tmp = new IT_IdentItem(name,clas,type,level,index,uniname);
        tableitems.add(tmp);
        index++;
    }

    //添加元素
    public void additem(String name,int clas,int type,int level) {
        IT_IdentItem tmp = new IT_IdentItem(name,clas,type,level,index);
        tableitems.add(tmp);
        index++;
    }

    //退出函数时将所有局部变量pop了
    public void poplevel(int level) {
        for(int i=tableitems.size()-1;i>=0;i--){
            if (tableitems.get(i).getLevel()>=level) {
                tableitems.remove(i);
            }
        }
    }

    //获取变量
    private IT_IdentItem getvar(String name, int level) {
        for(int i=tableitems.size()-1;i>=0;i--) {
            IT_IdentItem tmp = tableitems.get(i);
            if(tmp.getName().equals(name) && tmp.getLevel() <= level
                    && tmp.getType() != 4) {
                return tmp;
            }
        }
        return null;
    }

    public String getuniname(String name, int level) {
        IT_IdentItem item = getvar(name,level);
        if (item!=null) {
            return item.getuniname();
        } else {
            return null;
        }
    }
}
