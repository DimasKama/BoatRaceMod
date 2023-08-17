package dimaskama.boatracemod.client;

import dimaskama.boatracemod.race.Race;
import dimaskama.boatracemod.race.Racer;
import dimaskama.boatracemod.race.track.Segment;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

public class Overlay implements HudRenderCallback {
    @Override
    public void onHudRender(DrawContext ctx, float tickDelta) {
        if (BoatRaceModClient.getRace() != null)
            drawOverlay(ctx, MinecraftClient.getInstance().textRenderer, BoatRaceModClient.getRace());
    }

    public static void drawOverlay(DrawContext ctx, TextRenderer textRenderer, Race race) {
        if (!BoatRaceModClient.OVERLAY.showDisplay) return;

        // 0 - last common lap
        // 1 - lap pb
        // 2 - sectors pbs
        final int s0 = BoatRaceModClient.OVERLAY.miscInterval0;
        final int s1 = BoatRaceModClient.OVERLAY.miscInterval1;
        final int s2 = race.sectors.size() > 1 ? BoatRaceModClient.OVERLAY.miscInterval2 : 0;
        final int sMod = BoatRaceModClient.OVERLAY.miscDisplay ? (int) (System.currentTimeMillis() % (s0 + s1 + s2)) : 0;
        int displayMode = 0;
        if (sMod > (s0 + s1)) displayMode = 2;
        else if (sMod > s0) displayMode = 1;

        Integer[] timerDisplay = BoatRaceModClient.OVERLAY.timerDisplay;
        float timerScale = BoatRaceModClient.OVERLAY.timerScale;
        Integer[] listDisplay = BoatRaceModClient.OVERLAY.listDisplay;
        float listScale = BoatRaceModClient.OVERLAY.listScale;

        MatrixStack matrixStack = ctx.getMatrices();
        matrixStack.scale(timerScale, timerScale, timerScale);
        int timerColor = 0x47a76a;
        if (!race.timerActive) timerColor = 0xffcf48;
        ctx.drawTextWithShadow(textRenderer, Race.getTimerString(race.timer, true), timerDisplay[0], timerDisplay[1], timerColor);
        float invTimerScale = 1f / timerScale;
        matrixStack.scale(invTimerScale, invTimerScale, invTimerScale);

        int x = listDisplay[0];
        int y = listDisplay[1];

        int commonEndedLap = -2;
        for (Racer racer : race.racers)
            if (commonEndedLap == -2 || racer.currentLap < commonEndedLap) commonEndedLap = racer.currentLap - 1;
        if (commonEndedLap == -2) commonEndedLap = -1;

        // Отрисовка топа
        matrixStack.scale(listScale, listScale, listScale);

        Style bold = Style.EMPTY.withBold(true);
        Style italic = Style.EMPTY.withItalic(true);

        String modeTitle = "Unexpected display mode";
        switch (displayMode) {
            case 0 -> modeTitle = "overlay.race";
            case 1 -> modeTitle = "overlay.lap_pb";
            case 2 -> modeTitle = "overlay.best_on_sector";
        }
        ctx.drawTextWithShadow(textRenderer, Text.translatable(modeTitle).setStyle(italic), x, y, 0xffffff);
        y += 12;

        switch (displayMode) {
            case 0 -> {
                ctx.drawTextWithShadow(textRenderer, Text.literal("Racer").setStyle(bold), x, y, 0xffffff);
                ctx.drawTextWithShadow(textRenderer, Text.literal("Lap").setStyle(bold), x + 40, y, 0xffffff);
                String intervalTitleString = "Interval";
                if (!race.raceEnded) intervalTitleString += "(" + (commonEndedLap + 1) + " lap)";
                else commonEndedLap++;
                if (commonEndedLap == -1) intervalTitleString = "";

                ctx.drawTextWithShadow(textRenderer, Text.literal(intervalTitleString).setStyle(bold), x + 71, y, 0xffffff);
                y += 10;

                for (int i = 0; i < race.racers.size(); i++) {
                    Racer racer = race.racers.get(i);
                    String lapDisplay = (racer.currentLap + 1) + "/" + race.totalLaps;
                    int color = 0xffffff;
                    if (racer.finished) {
                        if (i == 0) color = 0xffd700;
                        else if (i == 1) color = 0xc0c0c0;
                        else if (i == 2) color = 0xcd7f32;
                        lapDisplay = "Fin";
                    }
                    drawRacerInList(ctx, textRenderer, i, racer, x, y, color);

                    ctx.drawTextWithShadow(textRenderer, lapDisplay, x + 40, y, color);
                    String interval = "";
                    if (commonEndedLap != -1) {
                        if (i == 0)
                            interval = Race.getTimerString(race.racers.get(0).lapTime[commonEndedLap], false);
                        else {
                            int intervalInt = racer.lapTime[commonEndedLap] - race.racers.get(i - 1).lapTime[commonEndedLap];
                            interval = Race.getTimerString(intervalInt, false);
                            ctx.drawTextWithShadow(textRenderer, (intervalInt >= 0 ? "+" : "-"), x + (!race.raceEnded ? 85 : 85 - 20), y, color);
                        }
                    }
                    ctx.drawTextWithShadow(textRenderer, interval, x + (!race.raceEnded ? 93 : 93 - 21), y, color);
                    y += 10;
                }
            }
            case 1 -> {
                ctx.drawTextWithShadow(textRenderer, Text.literal("Racer").setStyle(bold), x, y, 0xffffff);
                ctx.drawTextWithShadow(textRenderer, Text.literal("Time").setStyle(bold), x + 40, y, 0xffffff);
                y += 10;

                race.sortByLapPb();
                int intervalReference = race.racers.get(0).lapPb;
                for (int i = 0; i < race.racers.size(); i++) {
                    Racer racer = race.racers.get(i);
                    drawRacerInList(ctx, textRenderer, i, racer, x, y, 0xffffff);
                    if (i == 0)
                        ctx.drawTextWithShadow(textRenderer, Race.getTimerString(intervalReference, true), x + 41, y, 0xffffff);
                    else
                        ctx.drawTextWithShadow(textRenderer, '+' + Race.getTimerString(racer.lapPb - intervalReference, true), x + 35, y, 0xffffff);
                    y += 10;
                }
                race.sort();
            }
            case 2 -> {
                ctx.drawTextWithShadow(textRenderer, Text.literal("Sector").setStyle(bold), x, y, 0xffffff);
                ctx.drawTextWithShadow(textRenderer, Text.literal("Racer").setStyle(bold), x + 44, y, 0xffffff);
                ctx.drawTextWithShadow(textRenderer, Text.literal("Time").setStyle(bold), x + 84, y, 0xffffff);
                y += 10;

                for (int i = 0; i < race.sectors.size(); i++) {
                    Segment sector = race.sectors.get(i);
                    ctx.drawTextWithShadow(textRenderer, String.valueOf(i + 1), x + 14, y, 0xffffff);
                    String bestRacerName = sector.bestRacer == null ? "" : sector.bestRacer.shortname;
                    ctx.drawTextWithShadow(textRenderer, bestRacerName, x + 50, y, 0xffffff);
                    String bestRacerTime = sector.bestRacer == null ? "" : Race.getTimerString(sector.bestRacer.sectorPb[i], true);
                    ctx.drawTextWithShadow(textRenderer, bestRacerTime, x + 84, y, 0xffffff);
                    y += 10;
                }
            }
        }
        float invListScale = 1f / listScale;
        matrixStack.scale(invListScale, invListScale, invListScale);
    }

    private static void drawRacerInList(DrawContext ctx, TextRenderer textRenderer, int i, Racer racer, int x, int y, int color) {
        ctx.drawTextWithShadow(textRenderer, (i + 1) + " " + racer.shortname, x, y, color);
    }
}
