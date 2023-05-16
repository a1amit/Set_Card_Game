package bguspl.set.ex;

public class FrozenPlayer {
    private int id;
    private long freezeTime;

    public FrozenPlayer(int id, long freezeTime) {
        this.id = id;
        this.freezeTime = freezeTime;
    }

    public int getId() {
        return id;
    }

    public long getFreezeTime() {
        return freezeTime;
    }
}
