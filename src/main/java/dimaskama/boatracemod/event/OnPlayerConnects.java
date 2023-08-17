package dimaskama.boatracemod.event;

import dimaskama.boatracemod.BoatRaceMod;
import dimaskama.boatracemod.race.Racer;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.text.Text;

import java.util.Optional;

public class OnPlayerConnects implements ServerPlayConnectionEvents.Join {
    @Override
    public void onPlayReady(ServerPlayNetworkHandler handler, PacketSender sender, MinecraftServer server) {
        BoatRaceMod.LOGGER.info("Sending check-packet to " + handler.player.getEntityName());
        ServerPlayNetworking.send(handler.getPlayer(), BoatRaceMod.CHECK_CLIENT_RESPONSE, PacketByteBufs.empty());
        if (BoatRaceMod.getRace() == null) return;
        Optional<Racer> racer1 = BoatRaceMod.getRace().disconnectedRacers.stream().filter(racer -> racer.uuid.equals(handler.player.getUuid())).findAny();
        if (racer1.isPresent()) {
            Racer racer = racer1.get();
            BoatRaceMod.getRace().racers.add(racer);
            BoatRaceMod.getRace().disconnectedRacers.remove(racer);
            BoatRaceMod.sendRaceMessage(Text.translatable("global.racer_reconnected", racer.name));
        }
    }
}
