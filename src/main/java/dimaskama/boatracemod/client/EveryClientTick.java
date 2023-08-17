package dimaskama.boatracemod.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.World;

public class EveryClientTick implements ClientTickEvents.EndWorldTick {
    @Override
    public void onEndTick(ClientWorld world) {
        if (!world.getRegistryKey().equals(World.OVERWORLD)) return;
        if (BoatRaceModClient.getRace() != null && BoatRaceModClient.getRace().timerActive)
            BoatRaceModClient.getRace().timer++;
    }
}
