package dimaskama.boatracemod.race.track;


import java.util.ArrayList;

public class Track {
    public final ArrayList<Point> points = new ArrayList<>();

    public boolean readyForRace() {
        return (points.size() > 2 && points.get(0).isStart);
    }
}
