package dimaskama.boatracemod.race.track;

import net.minecraft.util.math.Vec2f;

public class Point {
    public final float x, z;
    public final boolean isStart, isSector;

    public Point(Vec2f vec2f, boolean isStart, boolean isSector) {
        this.x = vec2f.x;
        this.z = vec2f.y;
        this.isStart = isStart;
        this.isSector = isSector;
    }
}
