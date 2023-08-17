package dimaskama.boatracemod.race;

import dimaskama.boatracemod.BoatRaceMod;
import dimaskama.boatracemod.race.track.Segment;
import dimaskama.boatracemod.race.track.Track;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Race implements Serializable {
    public final int totalLaps;
    public final transient ArrayList<Segment> segments = new ArrayList<>();
    public transient int countDown = -1;
    public transient int timer = 0;
    public transient boolean timerActive = false;
    public boolean raceEnded = false;
    public transient ArrayList<Racer> disconnectedRacers = new ArrayList<>();
    public transient Racer bestLap;
    public ArrayList<Segment> sectors;
    public ArrayList<Racer> racers = new ArrayList<>();

    public Race(Track track, int totalLaps, ArrayList<RawRacer> rawRacers) {
        int size = track.points.size();
        for (int i = 0; i < size; i++) {
            if (i == size - 1) {
                segments.add(new Segment(track.points.get(i), track.points.get(0)));
                break;
            }
            segments.add(new Segment(track.points.get(i), track.points.get(i + 1)));
        }
        sectors = new ArrayList<>(segments.stream().filter(segment -> segment.next.isSector).toList());

        this.totalLaps = totalLaps;
        for (RawRacer rawRacer : rawRacers) racers.add(new Racer(rawRacer, track, totalLaps, sectors.size()));
        sort();
    }

    public boolean sort() {
        boolean changed = false;
        for (int i = 0; i < racers.size() - 1; i++) {
            if (racers.get(i).compareTo(racers.get(i + 1)) > 0) {
                Collections.swap(racers, i, i + 1);
                changed = true;
                if (i > 0) i -= 2;
            }
        }
        return changed;
    }

    public void sortByLapPb() {
        for (int i = 0; i < racers.size() - 1; i++) {
            if (racers.get(i).lapPb - racers.get(i + 1).lapPb > 0) {
                Collections.swap(racers, i, i + 1);
                if (i > 0) i -= 2;
            }
        }
    }

    public void sendRaceUpdate(ServerPlayerEntity player) {
        List<ServerPlayerEntity> players = new ArrayList<>();
        players.add(player);
        sendRaceUpdate(players);
    }

    public void sendRaceUpdate(List<ServerPlayerEntity> players) {
        PacketByteBuf packetByteBuf = PacketByteBufs.create();
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream ous = new ObjectOutputStream(bos)) {
            ous.writeObject(this);
            packetByteBuf.writeByteArray(bos.toByteArray());
        } catch (IOException e) {
            BoatRaceMod.LOGGER.error("Exception occurred while sending race packet" + e);
        }
        players.forEach(player -> ServerPlayNetworking.send(player, BoatRaceMod.RACE_UPDATE, packetByteBuf));
    }

    public void sendRacersUpdate(List<ServerPlayerEntity> players) {
        PacketByteBuf packetByteBuf = PacketByteBufs.create();
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream ous = new ObjectOutputStream(bos)) {
            ous.writeObject(racers);
            packetByteBuf.writeByteArray(bos.toByteArray());
        } catch (IOException e) {
            BoatRaceMod.LOGGER.error("Exception occurred while sending racers packet " + e);
        }
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream ous = new ObjectOutputStream(bos)) {
            ous.writeObject(sectors);
            packetByteBuf.writeByteArray(bos.toByteArray());
        } catch (IOException e) {
            BoatRaceMod.LOGGER.error("Exception occurred while sending racers packet " + e);
        }
        players.forEach(player -> ServerPlayNetworking.send(player, BoatRaceMod.RACERS_UPDATE, packetByteBuf));
    }

    public void sendTimerUpdate(List<ServerPlayerEntity> players) {
        PacketByteBuf packetByteBuf = PacketByteBufs.create();
        packetByteBuf.writeBoolean(timerActive);
        packetByteBuf.writeInt(timer);
        players.forEach(player -> ServerPlayNetworking.send(player, BoatRaceMod.TIMER_UPDATE, packetByteBuf));
    }

    public static void sendNullRace(ServerPlayerEntity player) {
        List<ServerPlayerEntity> list = new ArrayList<>();
        list.add(player);
        sendNullRace(list);
    }

    public static void sendNullRace(List<ServerPlayerEntity> players) {
        PacketByteBuf packetByteBuf = PacketByteBufs.create();
        packetByteBuf.writeByteArray(new byte[0]);
        players.forEach(player -> ServerPlayNetworking.send(player, BoatRaceMod.RACE_UPDATE, packetByteBuf));
    }

    public static String getTimerString(int timer, boolean showHours) {
        timer = Math.abs(timer);
        int h = timer / 20 / 60 / 60;
        int mins = timer / 20 / 60;
        String ssecs = String.format("%02d", timer / 20 % 60);
        String smilsecs = String.format("%03d", timer % 20 * 50);
        if (!showHours || h == 0) return String.format("%s:%s.%s", mins, ssecs, smilsecs);
        return String.format("%s:%02d:%s.%s", h, mins % 60, ssecs, smilsecs);
    }
}
