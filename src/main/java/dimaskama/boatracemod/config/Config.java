package dimaskama.boatracemod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dimaskama.boatracemod.BoatRaceMod;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;

public abstract class Config {
    private final transient String CONFPATH;

    public Config(String path) {
        CONFPATH = path;
    }

    public void loadOrCreate() {
        if (!new File("config").exists()) new File("config").mkdir();
        File file = new File(CONFPATH);
        if (!file.exists()) {
            saveJson();
        } else {
            try (FileReader f = new FileReader(CONFPATH)) {
                Config c = new Gson().fromJson(f, getClass());
                for (Field field : getClass().getDeclaredFields()) field.set(this, field.get(c));
            } catch (IOException | IllegalAccessException e) {
                BoatRaceMod.LOGGER.error("Exception occurred while reading config" + e);
            }
        }
    }

    public void saveJson() {
        try (FileWriter w = new FileWriter(CONFPATH)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(this, w);
            BoatRaceMod.LOGGER.info("Config saved: " + CONFPATH);
        } catch (IOException e) {
            BoatRaceMod.LOGGER.error("Exception occurred while saving config" + e);
        }
    }

    public void reset() {
        try {
            Config n = getClass().getConstructor(String.class).newInstance(CONFPATH);
            for (Field field : getClass().getDeclaredFields()) field.set(this, field.get(n));
        } catch (Exception e) {
            BoatRaceMod.LOGGER.error("Exception occurred while resetting config" + e);
        }
    }
}
