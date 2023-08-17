package dimaskama.boatracemod.client;

import dimaskama.boatracemod.BoatRaceMod;
import dimaskama.boatracemod.config.OverlayConfig;
import dimaskama.boatracemod.race.Race;
import dimaskama.boatracemod.race.Racer;
import dimaskama.boatracemod.race.RawRacer;
import dimaskama.boatracemod.race.track.Point;
import dimaskama.boatracemod.race.track.Segment;
import dimaskama.boatracemod.race.track.Track;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec2f;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.UUID;

public class BoatRaceModClient implements ClientModInitializer {
    public static final OverlayConfig OVERLAY = new OverlayConfig("config/BoatRaceMod_Overlay.json");
    public static final Race EXAMPLE_RACE;
    private static Race RACE = null;

    static {
        ArrayList<RawRacer> racers = new ArrayList<>();
        racers.add(new RawRacer("ver", UUID.randomUUID(), "VER"));
        racers.add(new RawRacer("lec", UUID.randomUUID(), "LEC"));
        racers.add(new RawRacer("ham", UUID.randomUUID(), "HAM"));
        racers.add(new RawRacer("alo", UUID.randomUUID(), "ALO"));
        racers.add(new RawRacer("per", UUID.randomUUID(), "PER"));
        Track track = new Track();
        track.points.add(new Point(new Vec2f(0, 0), true, true));
        track.points.add(new Point(new Vec2f(0, 0), false, true));
        track.points.add(new Point(new Vec2f(0, 0), false, true));
        EXAMPLE_RACE = new Race(track, 25, racers);
        for (int i = 0; i < EXAMPLE_RACE.racers.size(); i++) {
            Racer racer = EXAMPLE_RACE.racers.get(i);
            racer.currentLap = 17 - i;
        }
        EXAMPLE_RACE.sectors.get(0).bestRacer = EXAMPLE_RACE.racers.get(3);
        EXAMPLE_RACE.racers.get(3).sectorPb[0] = 4926;
        EXAMPLE_RACE.sectors.get(1).bestRacer = EXAMPLE_RACE.racers.get(2);
        EXAMPLE_RACE.racers.get(2).sectorPb[1] = 3954;
        EXAMPLE_RACE.sectors.get(2).bestRacer = EXAMPLE_RACE.racers.get(1);
        EXAMPLE_RACE.racers.get(1).sectorPb[2] = 5123;
        EXAMPLE_RACE.timer = 9193;
        EXAMPLE_RACE.timerActive = true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onInitializeClient() {
        OVERLAY.loadOrCreate();
        String username = MinecraftClient.getInstance().getSession().getUsername();
        if (!BoatRaceMod.CONFIG.hasAccess.contains(username)) BoatRaceMod.CONFIG.hasAccess.add(username);
        BoatRaceMod.CONFIG.saveJson();
        ClientTickEvents.END_WORLD_TICK.register(new EveryClientTick());
        HudRenderCallback.EVENT.register(new Overlay());
        ClientPlayNetworking.registerGlobalReceiver(BoatRaceMod.CHECK_CLIENT_RESPONSE, (client, handler, buf, responseSender) -> {
            BoatRaceMod.LOGGER.info("Received check-packet");
            responseSender.sendPacket(BoatRaceMod.CHECK_CLIENT_RESPONSE, PacketByteBufs.empty());
        });
        ClientPlayNetworking.registerGlobalReceiver(BoatRaceMod.RACE_UPDATE, (client, handler, buf, responseSender) -> {
            BoatRaceMod.LOGGER.info("Received race update");
            byte[] bytes = buf.readByteArray();
            if (bytes.length <= 1) {
                RACE = null;
                BoatRaceMod.LOGGER.info("Race = null");
                return;
            }
            try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes); ObjectInputStream ois = new ObjectInputStream(bis)) {
                RACE = (Race) ois.readObject();
            } catch (ClassNotFoundException | IOException e) {
                BoatRaceMod.LOGGER.error("Exception occurred while reading race packet " + e);
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(BoatRaceMod.RACERS_UPDATE, (client, handler, buf, responseSender) -> {
            try (ByteArrayInputStream bis = new ByteArrayInputStream(buf.readByteArray()); ObjectInputStream ois = new ObjectInputStream(bis);
                 ByteArrayInputStream bis2 = new ByteArrayInputStream(buf.readByteArray()); ObjectInputStream ois2 = new ObjectInputStream(bis2)) {
                Object o = ois.readObject();
                if (o instanceof ArrayList) RACE.racers = (ArrayList<Racer>) o;
                Object o2 = ois2.readObject();
                if (o2 instanceof ArrayList) RACE.sectors = (ArrayList<Segment>) o2;
            } catch (ClassNotFoundException | IOException e) {
                BoatRaceMod.LOGGER.error("Exception occurred while reading racers packet " + e);
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(BoatRaceMod.TIMER_UPDATE, (client, handler, buf, responseSender) -> {
            RACE.timerActive = buf.readBoolean();
            RACE.timer = buf.readInt();
        });
    }

    public static Race getRace() {
        return RACE;
    }
}
