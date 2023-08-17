package dimaskama.boatracemod.race;

import dimaskama.boatracemod.race.track.Track;
import org.jetbrains.annotations.NotNull;

public class Racer extends RawRacer {
    public boolean finished = false;
    public transient int currentSegment;
    public int currentLap = -1;
    public int[] lapTime;
    public int lapPb = 0;
    public transient int currentSector = -1;
    public transient int sectorTime = 0;
    public int[] sectorPb;
    public transient float currentDist = 0f;

    public Racer(RawRacer rawRacer, Track track, int totalLaps, int sectorsNumber) {
        super(rawRacer.name, rawRacer.uuid, rawRacer.shortname);
        currentSegment = track.points.size() - 1;
        lapTime = new int[totalLaps];
        sectorPb = new int[sectorsNumber];
    }

    public int compareTo(@NotNull Racer o) {
        if (finished) return 0;
        int c = currentLap - o.currentLap;
        if (c == 0) c = currentSegment - o.currentSegment;
        if (c == 0) {
            float f = (currentDist - o.currentDist);
            if (f < 0) c = (int) Math.floor(f);
            else c = (int) Math.ceil(f);
        }
        if (c == 0) c = name.compareTo(o.name);
        return -c;
    }
}
