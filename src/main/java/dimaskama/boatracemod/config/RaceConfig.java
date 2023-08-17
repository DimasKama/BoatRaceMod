package dimaskama.boatracemod.config;

import dimaskama.boatracemod.race.RawRacer;
import dimaskama.boatracemod.race.track.Track;

import java.util.ArrayList;
import java.util.HashMap;

public class RaceConfig extends Config {
    public HashMap<String, Track> tracks = new HashMap<>();
    public ArrayList<RawRacer> racers = new ArrayList<>();
    public ArrayList<String> hasAccess = new ArrayList<>();

    public RaceConfig(String path) {
        super(path);
    }
}
