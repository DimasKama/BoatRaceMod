package dimaskama.boatracemod.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

public class ModMenuImpl implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return OptionsScreen::new;
    }

    public static class OptionsScreen extends Screen {
        private final Screen parent;
        private ButtonListWidget list;

        public OptionsScreen(Screen parent) {
            super(Text.translatable("configscreen.race_overlay"));
            this.parent = parent;
        }

        @Override
        protected void init() {
            int width = MinecraftClient.getInstance().getWindow().getScaledWidth();
            int height = MinecraftClient.getInstance().getWindow().getScaledHeight();
            this.list = new ButtonListWidget(client, width, height, 32, height - 32, 25);

            list.addOptionEntry(SimpleOption.ofBoolean(
                    "configscreen.show_display",
                    BoatRaceModClient.OVERLAY.showDisplay,
                    (value) -> BoatRaceModClient.OVERLAY.showDisplay = value
            ), SimpleOption.ofBoolean(
                    "configscreen.misc_list_display",
                    BoatRaceModClient.OVERLAY.miscDisplay,
                    (value) -> BoatRaceModClient.OVERLAY.miscDisplay = value
            ));
            list.addSingleOptionEntry(new SimpleOption<>(
                    "configscreen.misc_show_0",
                    SimpleOption.emptyTooltip(),
                    (optionText, value) -> Text.translatable("configscreen.misc_show_0", String.format("%s.%s", value / 1000, value / 10 % 100)),
                    new SimpleOption.ValidatingIntSliderCallbacks(1000, 10000),
                    BoatRaceModClient.OVERLAY.miscInterval0,
                    integer -> BoatRaceModClient.OVERLAY.miscInterval0 = integer
            ));
            list.addSingleOptionEntry(new SimpleOption<>(
                    "configscreen.misc_show_1",
                    SimpleOption.emptyTooltip(),
                    (optionText, value) -> Text.translatable("configscreen.misc_show_1", String.format("%s.%s", value / 1000, value / 10 % 100)),
                    new SimpleOption.ValidatingIntSliderCallbacks(1000, 10000),
                    BoatRaceModClient.OVERLAY.miscInterval1,
                    integer -> BoatRaceModClient.OVERLAY.miscInterval1 = integer
            ));
            list.addSingleOptionEntry(new SimpleOption<>(
                    "configscreen.misc_show_2",
                    SimpleOption.emptyTooltip(),
                    (optionText, value) -> Text.translatable("configscreen.misc_show_2", String.format("%s.%s", value / 1000, value / 10 % 100)),
                    new SimpleOption.ValidatingIntSliderCallbacks(1000, 10000),
                    BoatRaceModClient.OVERLAY.miscInterval2,
                    integer -> BoatRaceModClient.OVERLAY.miscInterval2 = integer
            ));
            list.addOptionEntry(new SimpleOption<>(
                    "configscreen.timer_x",
                    SimpleOption.emptyTooltip(),
                    (optionText, value) -> Text.translatable("configscreen.timer_x", String.format("%spx", value)),
                    new SimpleOption.ValidatingIntSliderCallbacks(0, width - 40),
                    BoatRaceModClient.OVERLAY.timerDisplay[0],
                    integer -> BoatRaceModClient.OVERLAY.timerDisplay[0] = integer
            ), new SimpleOption<>(
                    "configscreen.list_x",
                    SimpleOption.emptyTooltip(),
                    (optionText, value) -> Text.translatable("configscreen.list_x", String.format("%spx", value)),
                    new SimpleOption.ValidatingIntSliderCallbacks(0, width - 40),
                    BoatRaceModClient.OVERLAY.listDisplay[0],
                    integer -> BoatRaceModClient.OVERLAY.listDisplay[0] = integer
            ));
            list.addOptionEntry(new SimpleOption<>(
                    "configscreen.timer_y",
                    SimpleOption.emptyTooltip(),
                    (optionText, value) -> Text.translatable("configscreen.timer_y", String.format("%spx", value)),
                    new SimpleOption.ValidatingIntSliderCallbacks(0, height - 8),
                    BoatRaceModClient.OVERLAY.timerDisplay[1],
                    integer -> BoatRaceModClient.OVERLAY.timerDisplay[1] = integer
            ), new SimpleOption<>(
                    "configscreen.list_y",
                    SimpleOption.emptyTooltip(),
                    (optionText, value) -> Text.translatable("configscreen.list_y", String.format("%spx", value)),
                    new SimpleOption.ValidatingIntSliderCallbacks(0, height - 8),
                    BoatRaceModClient.OVERLAY.listDisplay[1],
                    integer -> BoatRaceModClient.OVERLAY.listDisplay[1] = integer
            ));
            list.addOptionEntry(
                    new SimpleOption<>(
                            "configscreen.timer_scale",
                            SimpleOption.emptyTooltip(),
                            (optionText, value) -> Text.translatable("configscreen.timer_scale", String.format("%s.%02d", value / 100, value % 100) + "x"),
                            new SimpleOption.ValidatingIntSliderCallbacks(100, 300),
                            (int) BoatRaceModClient.OVERLAY.timerScale * 100,
                            integer -> BoatRaceModClient.OVERLAY.timerScale = ((float) integer) / 100
                    ), new SimpleOption<>(
                            "configscreen.list_scale",
                            SimpleOption.emptyTooltip(),
                            (optionText, value) -> Text.translatable("configscreen.list_scale", String.format("%s.%02d", value / 100, value % 100) + "x"),
                            new SimpleOption.ValidatingIntSliderCallbacks(100, 300),
                            (int) BoatRaceModClient.OVERLAY.listScale * 100,
                            integer -> BoatRaceModClient.OVERLAY.listScale = ((float) integer) / 100
                    ));
            addSelectableChild(list);

            addDrawableChild(new ButtonWidget(width / 2 - 155, height - 27, 150, 20, Text.translatable("configscreen.reset"), (button) -> {
                BoatRaceModClient.OVERLAY.reset();
                close();
                MinecraftClient.getInstance().setScreen(this);
            }));
            addDrawableChild(new ButtonWidget(width / 2 + 5, height - 27, 150, 20, ScreenTexts.DONE, (button) -> close()));

        }

        @Override
        public void close() {
            BoatRaceModClient.OVERLAY.saveJson();

            if (parent != null && this.client != null) {
                client.setScreen(parent);
            } else {
                super.close();
            }
        }

        @Override
        public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            this.renderBackground(matrices);
            this.list.render(matrices, mouseX, mouseY, delta);
            drawCenteredText(matrices, textRenderer, title, width / 2, 15, 0xffffff);

            Overlay.drawOverlay(matrices, textRenderer, BoatRaceModClient.getRace() == null ? BoatRaceModClient.EXAMPLE_RACE : BoatRaceModClient.getRace());

            super.render(matrices, mouseX, mouseY, delta);
        }
    }
}
