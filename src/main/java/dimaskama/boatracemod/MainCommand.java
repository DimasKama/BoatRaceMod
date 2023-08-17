package dimaskama.boatracemod;


import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dimaskama.boatracemod.race.Race;
import dimaskama.boatracemod.race.RawRacer;
import dimaskama.boatracemod.race.track.Point;
import dimaskama.boatracemod.race.track.Segment;
import dimaskama.boatracemod.race.track.Track;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.Vec2ArgumentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec2f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.server.command.CommandManager.*;


public class MainCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, RegistrationEnvironment environment) {
        LiteralCommandNode<ServerCommandSource> literalCommandNode = dispatcher.register(literal(BoatRaceMod.MOD_ID)
                .then(literal("track")
                        .requires((source) -> source.hasPermissionLevel(2) || BoatRaceMod.CONFIG.hasAccess.contains(source.getName()))
                        .then(literal("add")
                                .then(argument("name", StringArgumentType.string())
                                        .executes(context -> {
                                            if (checkRaceStarted(context)) return -1;
                                            String name = StringArgumentType.getString(context, "name");
                                            if (BoatRaceMod.CONFIG.tracks.containsKey(name))
                                                throw new SimpleCommandExceptionType(Text.translatable("command.exception.track_already_exists", name)).create();
                                            BoatRaceMod.CONFIG.tracks.put(name, new Track());
                                            BoatRaceMod.CONFIG.saveJson();
                                            context.getSource().sendFeedback(() -> Text.translatable("command.feedback.created_track", name), true);
                                            return 1;
                                        })))
                        .then(literal("delete")
                                .then(argument("name", StringArgumentType.string())
                                        .suggests(new TrackSuggestionProvider())
                                        .executes(context -> {
                                            if (checkRaceStarted(context)) return -1;
                                            String name = StringArgumentType.getString(context, "name");
                                            if (!checkTrackNameCorrect(context, name)) return -1;
                                            BoatRaceMod.CONFIG.tracks.remove(name);
                                            BoatRaceMod.CONFIG.saveJson();
                                            context.getSource().sendFeedback(() -> Text.translatable("command.feedback.deleted_track", name), true);
                                            return 1;
                                        })))
                        .then(literal("edit")
                                .then(argument("name", StringArgumentType.string())
                                        .suggests(new TrackSuggestionProvider())
                                        .then(literal("newpoint")
                                                .then(argument("pointNum", IntegerArgumentType.integer())
                                                        .suggests((context, builder) -> {
                                                            builder.suggest(BoatRaceMod.CONFIG.tracks.get(StringArgumentType.getString(context, "name")).points.size());
                                                            return builder.buildFuture();
                                                        })
                                                        .then(argument("pos", Vec2ArgumentType.vec2())
                                                                .executes(context -> addPoint(context, false, false))
                                                                .then(literal("start")
                                                                        .executes(context -> addPoint(context, true, true)))
                                                                .then(literal("sector")
                                                                        .executes(context -> addPoint(context, false, true)))
                                                                .then(literal("default")
                                                                        .executes(context -> addPoint(context, false, false))))))
                                        .then(literal("removelastpoint")
                                                .executes(context -> {
                                                    if (checkRaceStarted(context)) return -1;
                                                    String name = StringArgumentType.getString(context, "name");
                                                    if (!checkTrackNameCorrect(context, name)) return -1;
                                                    Track track = BoatRaceMod.CONFIG.tracks.get(name);
                                                    if (track.points.size() == 0)
                                                        throw new SimpleCommandExceptionType(Text.translatable("command.exception.no_point_to_remove")).create();
                                                    track.points.remove(track.points.size() - 1);
                                                    BoatRaceMod.CONFIG.saveJson();
                                                    context.getSource().sendFeedback(() -> Text.translatable("command.feedback.removed_last_point", name), true);
                                                    return 1;
                                                })))))
                .then(literal("racers")
                        .requires((source) -> source.hasPermissionLevel(2) || BoatRaceMod.CONFIG.hasAccess.contains(source.getName()))
                        .then(literal("list")
                                .executes(context -> {
                                    if (BoatRaceMod.CONFIG.racers.size() == 0)
                                        throw new SimpleCommandExceptionType(Text.translatable("command.exception.no_racers_added")).create();
                                    StringBuilder s = new StringBuilder();
                                    int size = BoatRaceMod.CONFIG.racers.size();
                                    BoatRaceMod.CONFIG.racers.forEach(racer -> {
                                        s.append(racer.name).append('(').append(racer.shortname).append(')');
                                        if (size - 1 != BoatRaceMod.CONFIG.racers.indexOf(racer)) s.append(", ");
                                    });
                                    context.getSource().sendMessage(Text.of(s.toString()));
                                    return 0;
                                }))
                        .then(literal("add")
                                .then(argument("player", EntityArgumentType.player())
                                        .then(argument("shortname", StringArgumentType.string())
                                                .suggests((context, builder) -> {
                                                    builder.suggest(EntityArgumentType.getPlayer(context, "player").getEntityName().toUpperCase().substring(0, 3));
                                                    return builder.buildFuture();
                                                })
                                                .executes(context -> {
                                                    if (checkRaceStarted(context)) return -1;
                                                    PlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                                                    UUID uuid = player.getUuid();
                                                    String shortname = StringArgumentType.getString(context, "shortname").toUpperCase();
                                                    for (RawRacer racer : BoatRaceMod.CONFIG.racers) {
                                                        if (racer.uuid.equals(uuid))
                                                            throw new SimpleCommandExceptionType(Text.translatable("command.exception.player_actually_racer", player.getEntityName())).create();
                                                        else if (racer.shortname.equalsIgnoreCase(shortname))
                                                            throw new SimpleCommandExceptionType(Text.translatable("command.exception.shortname_used", shortname)).create();
                                                    }
                                                    if (shortname.length() != 3)
                                                        throw new SimpleCommandExceptionType(Text.translatable("command.exception.incorrect_shortname", shortname)).create();
                                                    BoatRaceMod.CONFIG.racers.add(new RawRacer(player, shortname));
                                                    BoatRaceMod.CONFIG.saveJson();
                                                    context.getSource().sendFeedback(() -> Text.translatable("command.feedback.added_racer", player.getEntityName()), true);
                                                    return 1;
                                                }))))
                        .then(literal("remove")
                                .then(argument("player", StringArgumentType.string())
                                        .suggests((context, builder) -> {
                                            BoatRaceMod.CONFIG.racers.forEach(racer -> builder.suggest(racer.name));
                                            return builder.buildFuture();
                                        })
                                        .executes(context -> {
                                            if (checkRaceStarted(context)) return -1;
                                            String player = StringArgumentType.getString(context, "player");
                                            BoatRaceMod.CONFIG.racers.removeIf(racer -> racer.name.equalsIgnoreCase(player));
                                            BoatRaceMod.CONFIG.saveJson();
                                            context.getSource().sendFeedback(() -> Text.translatable("command.feedback.removed_racer", player), true);
                                            return 1;
                                        }))))
                .then(literal("race")
                        .requires((source) -> source.hasPermissionLevel(2) || BoatRaceMod.CONFIG.hasAccess.contains(source.getName()))
                        .then(literal("startevent")
                                .then(argument("name", StringArgumentType.string())
                                        .suggests(new TrackSuggestionProvider())
                                        .then(argument("laps", IntegerArgumentType.integer(1, 99))
                                                .executes(context -> {
                                                    if (checkRaceStarted(context)) return 1;
                                                    String name = StringArgumentType.getString(context, "name");
                                                    if (!checkTrackNameCorrect(context, name)) return -1;
                                                    Track track = BoatRaceMod.CONFIG.tracks.get(name);
                                                    if (!track.readyForRace())
                                                        throw new SimpleCommandExceptionType(Text.translatable("command.exception.at_least_3_points")).create();
                                                    if (BoatRaceMod.CONFIG.racers.size() == 0)
                                                        throw new SimpleCommandExceptionType(Text.translatable("command.exception.no_racers_in_list")).create();
                                                    List<String> offlineRacers = new ArrayList<>();
                                                    for (RawRacer racer : BoatRaceMod.CONFIG.racers) {
                                                        if (context.getSource().getWorld().getPlayers().stream().noneMatch(player -> player.getEntityName().equalsIgnoreCase(racer.name)))
                                                            offlineRacers.add(racer.name);
                                                    }
                                                    if (offlineRacers.size() != 0)
                                                        throw new SimpleCommandExceptionType(Text.translatable("command.exception.not_all_racers_online", Arrays.toString(offlineRacers.toArray()))).create();
                                                    BoatRaceMod.startRace(track, IntegerArgumentType.getInteger(context, "laps"));
                                                    float length = 0f;
                                                    for (Segment segment : BoatRaceMod.getRace().segments)
                                                        length += segment.length;
                                                    StringBuilder racers = new StringBuilder();
                                                    for (RawRacer rawRacer : BoatRaceMod.CONFIG.racers)
                                                        racers.append("\n").append(rawRacer.name);
                                                    BoatRaceMod.sendRaceMessage(Text.translatable("global.race_event_started").setStyle(Style.EMPTY.withColor(0xbbff22).withBold(true)));
                                                    BoatRaceMod.sendRaceMessage(Text.translatable("global.race_event_started_info",
                                                            name,
                                                            BoatRaceMod.getRace().totalLaps,
                                                            BoatRaceMod.getRace().sectors.size(),
                                                            (int) length).append(Text.literal(racers.toString()).setStyle(Style.EMPTY.withColor(0x99ff00))));
                                                    context.getSource().sendMessage(Text.translatable("command.feedback.timer_will_start")
                                                            .setStyle(Style.EMPTY.withColor(0xf9e076).withBold(true)));
                                                    BoatRaceMod.getRace().sendRaceUpdate(BoatRaceMod.HANDLERS);
                                                    return 1;
                                                }))))
                        .then(literal("go")
                                .executes(context -> go(3))
                                .then(argument("seconds", IntegerArgumentType.integer(3, 60))
                                        .executes(context -> go(IntegerArgumentType.getInteger(context, "seconds")))))
                        .then(literal("stop")
                                .executes(context -> {
                                    if (!(BoatRaceMod.getRace() != null && BoatRaceMod.getRace().timerActive && BoatRaceMod.getRace().countDown < 0))
                                        throw new SimpleCommandExceptionType(Text.translatable("command.exception.cant_do_it_now")).create();
                                    BoatRaceMod.getRace().timerActive = false;
                                    BoatRaceMod.sendRaceMessage(Text.translatable("global.race_paused"));
                                    BoatRaceMod.getRace().sendTimerUpdate(BoatRaceMod.HANDLERS);
                                    BoatRaceMod.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1f, 0.1f);
                                    return 1;
                                }))
                        .then(literal("finishevent")
                                .executes(context -> {
                                    if (BoatRaceMod.getRace() == null)
                                        throw new SimpleCommandExceptionType(Text.translatable("command.exception.cant_do_it_now")).create();
                                    throw new SimpleCommandExceptionType(Text.translatable("command.exception.write_confirm")).create();
                                })
                                .then(argument("confirm", StringArgumentType.string())
                                        .executes(context -> {
                                            if (BoatRaceMod.getRace() == null)
                                                throw new SimpleCommandExceptionType(Text.translatable("command.exception.cant_do_it_now")).create();
                                            if (StringArgumentType.getString(context, "confirm").equalsIgnoreCase("confirm")) {
                                                BoatRaceMod.finishRace();
                                                BoatRaceMod.sendRaceMessage(Text.translatable("global.race_finished"));
                                                Race.sendNullRace(BoatRaceMod.HANDLERS);
                                                return 1;
                                            }
                                            throw new SimpleCommandExceptionType(Text.translatable("command.exception.write_confirm")).create();
                                        })))
                ));
        dispatcher.register(CommandManager.literal("brm").redirect(literalCommandNode));
    }

    private static class TrackSuggestionProvider implements SuggestionProvider<ServerCommandSource> {
        @Override
        public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
            for (String key : BoatRaceMod.CONFIG.tracks.keySet()) builder.suggest(key);
            return builder.buildFuture();
        }
    }

    private static boolean checkTrackNameCorrect(CommandContext<ServerCommandSource> context, String name) {
        if (BoatRaceMod.CONFIG.tracks.containsKey(name)) return true;
        context.getSource().sendError(Text.translatable("command.exception.track_not_exists", name));
        return false;
    }

    private static boolean checkRaceStarted(CommandContext<ServerCommandSource> context) {
        if (BoatRaceMod.getRace() != null) {
            context.getSource().sendError(Text.translatable("command.exception.finish_race_before"));
            return true;
        }
        return false;
    }

    private static int addPoint(CommandContext<ServerCommandSource> context, boolean isStart, boolean isSector) throws CommandSyntaxException {
        if (checkRaceStarted(context)) return -1;
        String name = StringArgumentType.getString(context, "name");
        if (!checkTrackNameCorrect(context, name)) return -1;
        Track track = BoatRaceMod.CONFIG.tracks.get(name);
        int pointNum = IntegerArgumentType.getInteger(context, "pointNum");
        if (track.points.size() != pointNum)
            throw new SimpleCommandExceptionType(Text.translatable("command.exception.points_must_ascending_order")).create();
        if (track.points.stream().noneMatch(p -> p.isStart)) {
            if (!isStart)
                throw new SimpleCommandExceptionType(Text.translatable("command.exception.start_point_first")).create();
        } else if (isStart)
            throw new SimpleCommandExceptionType(Text.translatable("command.exception.only_one_start_point")).create();
        Vec2f vec2f = Vec2ArgumentType.getVec2(context, "pos");
        track.points.add(new Point(vec2f, isStart, isSector));
        BoatRaceMod.CONFIG.saveJson();
        context.getSource().sendFeedback(() -> Text.translatable("command.feedback.added_point", name), true);
        return 1;
    }

    private static int go(int seconds) throws CommandSyntaxException {
        if (!(BoatRaceMod.getRace() != null && !BoatRaceMod.getRace().timerActive && !BoatRaceMod.getRace().raceEnded && BoatRaceMod.getRace().countDown < 0))
            throw new SimpleCommandExceptionType(Text.translatable("command.exception.cant_do_it_now")).create();
        BoatRaceMod.getRace().countDown = seconds * 20;
        BoatRaceMod.sendRaceMessage(Text.translatable("global.race_countdown_started"));
        return 1;
    }
}
