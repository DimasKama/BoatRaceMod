package dimaskama.boatracemod.race.track;

import dimaskama.boatracemod.race.Racer;

import java.awt.geom.Point2D;
import java.io.Serializable;

public class Segment implements Serializable {
    public final transient Point prev, next;
    public final transient float length;
    public Racer bestRacer = null;

    public Segment(Point prev, Point next) {
        this.prev = prev;
        this.next = next;
        this.length = (float) (new Point2D.Float(prev.x, prev.z)).distance(new Point2D.Float(next.x, next.z));
    }

    public float getPlayerLine(float x, float z) {
        float r = (float) (((prev.z - z) * (prev.z - next.z) + (prev.x - x) * (prev.x - next.x)) / Point2D.distanceSq(prev.x, prev.z, next.x, next.z));
        Point2D.Float d = new Point2D.Float(prev.x + r * (next.x - prev.x), prev.z + r * (next.z - prev.z));
        return (float) d.distance(new Point2D.Float(prev.x, prev.z));
    }
}
