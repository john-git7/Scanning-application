public class Scan {
    private final int id;
    private final String name;
    private final int duration;
    private final boolean pause;

    private ScanState state;

    public Scan(int id, String name, int duration, boolean pause) {
        this.id = id;
        this.name = name;
        this.duration = duration;
        this.pause = pause;
        this.state = ScanState.IDLE;
    }

    public int getId() {
        return id;
    }
    public String getName() {
        return name;
    }
    public int getDuration() {
        return duration;
    }
    public boolean isPause() {
        return pause;
    }
    public synchronized ScanState getState() {
        return state;
    }
    public synchronized void setState(ScanState state) {
        this.state = state;
    }

    @Override
    public String toString() {
        return "Scan:" + id + ", " + name + ", " + duration + ", " + (pause ? "Yes" : "No") + ", " + state;
    }
}