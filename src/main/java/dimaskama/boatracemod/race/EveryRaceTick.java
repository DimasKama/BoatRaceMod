package dimaskama.boatracemod.race;

import dimaskama.boatracemod.BoatRaceMod;
import dimaskama.boatracemod.race.track.Segment;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.world.World;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public class EveryRaceTick implements ServerTickEvents.StartWorldTick {
    @Override
    public void onStartTick(ServerWorld world) {
        if (world.getRegistryKey() != World.OVERWORLD) return;
        Race race = BoatRaceMod.getRace();
        if (race == null) return;
        try {
            if (race.countDown >= 0) {
                if ((race.countDown <= 100 && race.countDown % 20 == 0) || race.countDown % 200 == 0) {
                    if (race.countDown == 0) {
                        race.timerActive = true;
                        BoatRaceMod.getRace().sendTimerUpdate(BoatRaceMod.HANDLERS);
                        BoatRaceMod.sendRaceMessage(Text.translatable("global.race_started").setStyle(Style.EMPTY.withColor(0x00ff00)));
                        BoatRaceMod.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING, 1f, 0.5f);
                    } else {
                        BoatRaceMod.sendRaceMessage(Text.translatable("global.race_starts_in", race.countDown / 20).setStyle(Style.EMPTY.withColor(0xffff00)));
                        BoatRaceMod.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
                    }
                }
                race.countDown--;
            }
            if (!race.timerActive || race.raceEnded) return;
            List<ServerPlayerEntity> players = world.getPlayers();
            AtomicBoolean racersChanged = new AtomicBoolean();
            race.racers.forEach(racer -> {
                Optional<ServerPlayerEntity> playerEntityOptional = players.stream().filter(player -> player.getUuid().equals(racer.uuid)).findAny();
                if (playerEntityOptional.isEmpty()) return;
                ServerPlayerEntity playerEntity = playerEntityOptional.get();
                Segment currentsegment = race.segments.get(racer.currentSegment);
                racer.currentDist = currentsegment.getPlayerLine((float) playerEntity.getX(), (float) playerEntity.getZ());
                if (racer.currentDist > currentsegment.length) {
                    if (currentsegment.next.isStart || currentsegment.next.isSector) {
                        if (racer.currentLap >= 0) {
                            int inter = race.timer - racer.sectorTime;
                            if (race.sectors.size() > 1 && (racer.sectorPb[racer.currentSector] == 0 || racer.sectorPb[racer.currentSector] > inter)) {
                                int otherRacerPb = race.sectors.get(racer.currentSector).bestRacer == null ? 0 : race.sectors.get(racer.currentSector).bestRacer.sectorPb[racer.currentSector];
                                if (otherRacerPb == 0 || otherRacerPb > inter) {
                                    if (otherRacerPb == 0 || !race.sectors.get(racer.currentSector).bestRacer.equals(racer))
                                        BoatRaceMod.sendRaceMessage(Text.translatable(
                                                "global.racer_is_best_on_sector", racer.name, racer.currentSector + 1,
                                                Race.getTimerString(inter, true)).setStyle(Style.EMPTY.withColor(0x999999)));
                                    race.sectors.get(racer.currentSector).bestRacer = racer;
                                } else playerEntity.sendMessage(Text.translatable("direct.new_sector_pb",
                                        racer.currentSector + 1,
                                        Race.getTimerString(inter, true),
                                        Race.getTimerString(racer.sectorPb[racer.currentSector], true)).setStyle(Style.EMPTY.withColor(0x777777)));
                                racer.sectorPb[racer.currentSector] = inter;
                            }
                            racer.sectorTime = race.timer;
                        }
                        if (currentsegment.next.isStart) {
                            if (racer.currentLap >= 0) {
                                int inter = racer.currentLap == 0 ? race.timer : race.timer - racer.lapTime[racer.currentLap - 1];
                                if (racer.lapPb == 0 || racer.lapPb > inter) {
                                    if (race.bestLap == null || (!race.bestLap.equals(racer) && race.bestLap.lapPb > inter)) {
                                        race.bestLap = racer;
                                        BoatRaceMod.sendRaceMessage(Text.translatable("global.racer_have_best_lap",
                                                racer.name, Race.getTimerString(inter, true)).setStyle(Style.EMPTY.withColor(0x999999)));
                                    } else playerEntity.sendMessage(Text.translatable("direct.new_lap_pb",
                                            Race.getTimerString(inter, true), Race.getTimerString(racer.lapPb, true)).setStyle(Style.EMPTY.withColor(0x777777)));
                                    racer.lapPb = inter;
                                }
                                racer.lapTime[racer.currentLap] = race.timer;
                            }
                            if (racer.currentLap == race.totalLaps - 1) racer.finished = true;
                            else racer.currentLap++;
                            racer.currentSector = 0;
                            racer.currentSegment = 0;
                            racersChanged.set(true);
                        } else {
                            racer.currentSector++;
                            racer.currentSegment++;
                            racersChanged.set(true);
                        }
                    } else {
                        racer.currentSegment++;
                    }
                }
            });
            if (!race.raceEnded && race.racers.stream().allMatch(racer -> racer.finished)) {
                race.raceEnded = true;
                race.timerActive = false;
                race.sendRaceUpdate(BoatRaceMod.HANDLERS);
                BoatRaceMod.sendRaceMessage(Text.translatable("global.race_ended").setStyle(Style.EMPTY.withColor(0xffff00)));
                BoatRaceMod.playSound(SoundEvents.ENTITY_ENDER_DRAGON_AMBIENT, 0.5f, 2f);
            }
            race.timer++;
            if (race.sort() || racersChanged.get()) race.sendRacersUpdate(BoatRaceMod.HANDLERS);
            if (race.timer % 128 == 15) race.sendTimerUpdate(BoatRaceMod.HANDLERS);
        } catch (Exception e) {
            BoatRaceMod.LOGGER.error("An exception occurred with race. Race finished", e);
            BoatRaceMod.finishRace();
            Race.sendNullRace(BoatRaceMod.HANDLERS);
            BoatRaceMod.sendRaceMessage(Text.literal("An exception occurred with race. Race finished").setStyle(Style.EMPTY.withColor(0xff0000)));
        }
    }
}
