import java.util.ArrayList;

public class IT_IdentTable {
    private ArrayList<IT_IdentItem> tableitems = new ArrayList<>();
    private ArrayList<IT_IdentItem> serachitems = new ArrayList<>();
    private ArrayList<IT_FuncItem> funcs = new ArrayList<>();
    private int index;
    private int addr;

    public IT_IdentTable() {
        this.index = 0;
        this.addr = 0;
    }

    public int getAddr() {
        return this.addr;
    }

    private int judge_item(IT_IdentItem item,int addr) {
        if (item.getType() == 1 || item.getType() == 2) {
            if (item.getClas() == 3) {
                item.setAddr(addr);
                return addr + item.getDim1len();
            } else if (item.getClas() == 4) {
                item.setAddr(addr);
                return addr + item.getDim1len()*item.getDim2len();
            } else {
                item.setAddr(addr);
                return addr+1;
            }
        } else if (item.getType() == 3) {
            item.setAddr(addr);
            return addr+1;
        } else if (item.getType() == 4) {
            item.setAddr(0);
            return addr;
        }
        return addr;
    }

    public void re_call_addr() {
        addr = 0;
        for(IT_IdentItem item:tableitems) {
            addr = judge_item(item,addr);
        }
        addr = 0;
        for(IT_IdentItem item:serachitems) {
            addr = judge_item(item,addr);
        }
    }

    public int getGloOffset(String name) {
        int index=0;
        for (IT_IdentItem item:tableitems) {
            if (item.getName().equals(name)) {
                index = item.getAddr();
                break;
            }
        }
        return index;
    }

    //判断是否是常数
    public boolean isconst(String name,int level){
        for(int i=tableitems.size()-1;i>=0;i--){
            IT_IdentItem tmp = tableitems.get(i);
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

    //函数参数类型
    public ArrayList<Integer> funcrtype(String name) {
        int index = 0;
        for(int i=0;i<funcs.size();i++) {
            if(funcs.get(i).getName().equals(name)) {
                index = funcs.get(i).getIndex();
            }
        }
        ArrayList<Integer> ans = new ArrayList<>();
        for(int i=index+1;i<serachitems.size();i++){
            IT_IdentItem tmp = serachitems.get(i);
            if (tmp.getType() != 3) {
                break;
            }
            ans.add(tmp.getClas());
        }
        return ans;
    }

    //函数参数个数
    public int funcparanum(String name) {
        int index = 0;
        for(int i=0;i<funcs.size();i++) {
            if(funcs.get(i).getName().equals(name)) {
                index = funcs.get(i).getIndex();
            }
        }
        int ans = 0;
        for(int i=index+1;i<serachitems.size();i++){
            IT_IdentItem tmp = serachitems.get(i);
            if (tmp.getType() != 3) {
                break;
            }
            ans++;
        }
        return ans;
    }

    //函数的变量总长度
    public int funclocallen(String name) {
        int index = 0;
        for(int i=0;i<funcs.size();i++) {
            if(funcs.get(i).getName().equals(name)) {
                index = funcs.get(i).getIndex();
            }
        }
        int len = 0;
        for(int i=index+1;i<serachitems.size();i++){
            IT_IdentItem tmp = serachitems.get(i);
            if (tmp.getType() == 4) {
                break;
            } else if (tmp.getType() == 3) {
                len += 4;
            } else if (tmp.getType() == 2) {
                if (tmp.getClas() == 1) {
                    len += 4;
                } else if (tmp.getClas() == 3) {
                    len += 4*tmp.getDim1len();
                } else if (tmp.getClas() == 4) {
                    len += 4*tmp.getDim1len()*tmp.getDim2len();
                }
            }
        }
        return len+4;
    }

    //添加元素
    public void additem(String name,int clas,int type,int level) {
        IT_IdentItem tmp = new IT_IdentItem(name,clas,type,level,index);
        tableitems.add(tmp);
        serachitems.add(tmp);
        if (type == 4) {
            IT_FuncItem funcitem = new IT_FuncItem(name,index);
            funcs.add(funcitem);
        }
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

    //退出函数时将所有不是const的局部变量pop了
    public void popnoconst(int level) {
        for(int i=tableitems.size()-1;i>=0;i--){
            IT_IdentItem tmp = tableitems.get(i);
            if (tmp.getLevel()>=level && tmp.getType() != 1) {
                tableitems.remove(i);
            }
        }
    }

    //找到函数
    public int findfunc(String name) {
        for(int i=0;i<funcs.size();i++) {
            if(funcs.get(i).getName().equals(name)) {
                return funcs.get(i).getIndex();
            }
        }
        return -1;
    }

    //找到变量的clas
    public int findvartype(String name) {
        int ans = 0;
        for(int i=tableitems.size()-1;i>=0;i--) {
            IT_IdentItem tmp = tableitems.get(i);
            if (tmp.getName().equals(name) && tmp.getType() != 4) {
                ans = tmp.getClas();
                break;
            }
        }
        return ans;
    }

    //找到变量的type
    public int findfunctype(String name) {
        int ans = 0;
        for(int i=tableitems.size()-1;i>=0;i--) {
            IT_IdentItem tmp = tableitems.get(i);
            if (tmp.getName().equals(name) && tmp.getType()==4) {
                ans = tmp.getClas();
                break;
            }
        }
        return ans;
    }

    //找到变量的位置
    public int findvar(String name,int level) {
        for(int i=tableitems.size()-1;i>=0;i--) {
            IT_IdentItem tmp = tableitems.get(i);
            if(tmp.getName().equals(name) && tmp.getLevel() <= level
                && tmp.getType() != 4) {
                return i;
            }
        }
        return -1;
    }

    //获取变量
    public IT_IdentItem getvar(String name, int level) {
        for(int i=tableitems.size()-1;i>=0;i--) {
            IT_IdentItem tmp = tableitems.get(i);
            if(tmp.getName().equals(name) && tmp.getLevel() <= level
                    && tmp.getType() != 4) {
                return tmp;
            }
        }
        return null;
    }

    //在searchtable里面找
    public IT_IdentItem getvar_search(String name, int level) {
        for(int i=this.serachitems.size()-1;i>=0;i--) {
            IT_IdentItem tmp = serachitems.get(i);
            if(tmp.getName().equals(name) && tmp.getLevel() <= level
                    && tmp.getType() != 4) {
                return tmp;
            }
        }
        return null;
    }

    //变量名是否重复
    public int mulvar(String name,int level) {
        for (int i=0;i<tableitems.size();i++) {
            IT_IdentItem tmp = tableitems.get(i);
            if (tmp.getName().equals(name) && tmp.getLevel()==level) {
                return i;
            }
        }
        return -1;
    }


}
