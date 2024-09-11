public class Optim_Midvar {
    private String name;
    private boolean used;
    private int deadtime;
    private int clas;
    private int level;
    private int type;
    private int localoff;

    public Optim_Midvar(String name,boolean used) {
        this.name = name;
        this.used = used;
        this.deadtime = 0;
        this.clas = 0;
        this.level = 0;
        this.type = 0;
        this.localoff = 0;
    }

    public Optim_Midvar(String name,int deadtime,int clas,int type,int level,int localoff) {
        this.name = name;
        this.used = false;
        this.deadtime = deadtime;
        this.clas = clas;
        this.type = type;
        this.level = level;
        this.localoff = localoff;
    }

    public int getLocaloff() {
        return this.localoff;
    }

    public int getClas() {
        return this.clas;
    }

    public int getLevel() {
        return this.level;
    }

    public int getType() {
        return this.type;
    }

    public String getName() {
        return this.name;
    }

    public boolean getUsed() {
        return this.used;
    }

    public int getDeadtime() {
        return this.deadtime;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }
}
