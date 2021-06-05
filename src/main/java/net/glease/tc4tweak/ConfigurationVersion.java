package net.glease.tc4tweak;

import net.minecraftforge.common.config.Configuration;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Objects;

enum ConfigurationVersion {
    LEGACY(null) {
        @Override
        protected void step(Configuration c) {
            throw new IllegalStateException();
        }
    },
    V1() {
        private final String[] propsToClient = {
                "inverted",
                "updateInterval",
                "addTooltip",
                "browserScale",
                "limitBookSearchToCategory",
                "limitOversizedNodeRender",
        };
        @Override
        protected void step(Configuration c) {
            for (String name : propsToClient) {
                c.moveProperty("general", name, "client");
            }
        }
    };

    private static final ConfigurationVersion[] VALUES = values();

    private final String versionMarker;

    ConfigurationVersion() {
        this.versionMarker = name();
    }

    ConfigurationVersion(String versionMarker) {
        this.versionMarker = versionMarker;
    }

    @Nullable
    public String getVersionMarker() {
        return versionMarker;
    }

    protected abstract void step(Configuration c);

    public static void migrateToLatest(Configuration c) {
        for (int i = identify(c).ordinal() + 1; i < VALUES.length; i++) {
            VALUES[i].step(c);
        }
    }

    public static ConfigurationVersion latest() {
        return VALUES[VALUES.length - 1];
    }

    public static ConfigurationVersion identify(Configuration c) {
        return Arrays.stream(VALUES).filter(v -> Objects.equals(c.getLoadedConfigVersion(), v.getVersionMarker())).findFirst().orElse(LEGACY);
    }
}
