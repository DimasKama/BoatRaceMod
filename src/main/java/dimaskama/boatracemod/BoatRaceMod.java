package dimaskama.boatracemod;

import dimaskama.boatracemod.config.RaceConfig;
import dimaskama.boatracemod.event.OnPlayerConnects;
import dimaskama.boatracemod.event.OnPlayerDisconnects;
import dimaskama.boatracemod.race.EveryRaceTick;
import dimaskama.boatracemod.race.Race;
import dimaskama.boatracemod.race.track.Track;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;

public class BoatRaceMod implements ModInitializer {
    public static final String MOD_ID = "boatracemod";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    public static final RaceConfig CONFIG = new RaceConfig("config/BoatRaceMod.json");
    public static final Identifier CHECK_CLIENT_RESPONSE = new Identifier(MOD_ID, "checkclientresponse");
    public static final Identifier RACE_UPDATE = new Identifier(MOD_ID, "raceupdate");
    public static final Identifier RACERS_UPDATE = new Identifier(MOD_ID, "racersupdate");
    public static final Identifier TIMER_UPDATE = new Identifier(MOD_ID, "timerupdate");
    public static final ArrayList<ServerPlayerEntity> HANDLERS = new ArrayList<>();
    private static Race RACE = null;

    @Override
    public void onInitialize() {
        CONFIG.loadOrCreate();
        CommandRegistrationCallback.EVENT.register(MainCommand::register);
        ServerTickEvents.START_WORLD_TICK.register(new EveryRaceTick());
        ServerPlayConnectionEvents.JOIN.register(new OnPlayerConnects());
        ServerPlayConnectionEvents.DISCONNECT.register(new OnPlayerDisconnects());
        ServerPlayNetworking.registerGlobalReceiver(CHECK_CLIENT_RESPONSE, ((server, player, handler, buf, responseSender) -> {
            HANDLERS.add(player);
            if (RACE == null) Race.sendNullRace(player);
            else RACE.sendRaceUpdate(player);
            LOGGER.info("Received response on check-packet from " + player.getEntityName());
        }));
        LOGGER.info("Mod initialized");
    }

    public static void sendRaceMessage(Text text) {
        HANDLERS.forEach(player -> player.sendMessage(text));
    }

    public static void playSound(SoundEvent sound, float volume, float pitch) {
        HANDLERS.forEach(player -> player.playSound(sound, SoundCategory.MASTER, volume, pitch));
    }

    public static Race getRace() {
        return RACE;
    }

    public static void startRace(Track track, int totalLaps) {
        RACE = new Race(track, totalLaps, CONFIG.racers);
    }

    public static void finishRace() {
        RACE = null;
    }
}
