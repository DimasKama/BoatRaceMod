package dimaskama.boatracemod.race;

import net.minecraft.entity.player.PlayerEntity;

import java.io.Serializable;
import java.util.UUID;

public class RawRacer implements Serializable {
    public final String name;
    public final UUID uuid;
    public final String shortname;

    public RawRacer(PlayerEntity player, String shortname) {
        this.name = player.getEntityName();
        this.uuid = player.getUuid();
        this.shortname = shortname;
    }

    public RawRacer(String name, UUID uuid, String shortname) {
        this.name = name;
        this.uuid = uuid;
        this.shortname = shortname;
    }
}
