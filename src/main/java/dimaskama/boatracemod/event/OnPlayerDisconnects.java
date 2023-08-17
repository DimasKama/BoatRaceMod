package dimaskama.boatracemod.event;

import dimaskama.boatracemod.BoatRaceMod;
import dimaskama.boatracemod.race.Racer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.text.Text;

import java.util.Optional;

public class OnPlayerDisconnects implements ServerPlayConnectionEvents.Disconnect {
    @Override
    public void onPlayDisconnect(ServerPlayNetworkHandler handler, MinecraftServer server) {
        BoatRaceMod.HANDLERS.remove(handler.getPlayer());
        if (BoatRaceMod.getRace() == null) return;
        Optional<Racer> racer1 = BoatRaceMod.getRace().racers.stream().filter(racer -> racer.uuid.equals(handler.player.getUuid())).findAny();
        if (racer1.isPresent()) {
            Racer racer = racer1.get();
            BoatRaceMod.getRace().disconnectedRacers.add(racer);
            BoatRaceMod.getRace().racers.remove(racer);
            BoatRaceMod.sendRaceMessage(Text.translatable("global.racer.disconnected", racer.name));
        }
    }
}
