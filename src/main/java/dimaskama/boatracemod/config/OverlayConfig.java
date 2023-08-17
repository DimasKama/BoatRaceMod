package dimaskama.boatracemod.config;

public class OverlayConfig extends Config {
    public boolean showDisplay = true;
    public boolean miscDisplay = true;
    public int miscInterval0 = 5000;
    public int miscInterval1 = 5000;
    public int miscInterval2 = 5000;
    public Integer[] timerDisplay = {10, 10};
    public float timerScale = 1f;
    public Integer[] listDisplay = {10, 25};
    public float listScale = 1f;

    public OverlayConfig(String path) {
        super(path);
    }
}
