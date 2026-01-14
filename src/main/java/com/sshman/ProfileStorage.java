package com.sshman;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProfileStorage {
    private static final String PROFILES_FILE = "profiles.json";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private final Path configDir;
    private final Path profilesFile;

    public ProfileStorage() {
        this.configDir = Path.of(System.getProperty("user.home"), ".sshman");
        this.profilesFile = configDir.resolve(PROFILES_FILE);
        ensureConfigDirExists();
    }

    private void ensureConfigDirExists() {
        try {
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create config directory: " + e.getMessage(), e);
        }
    }

    public List<Profile> loadProfiles() {
        if (!Files.exists(profilesFile)) {
            return new ArrayList<>();
        }

        try (Reader reader = Files.newBufferedReader(profilesFile)) {
            Type listType = new TypeToken<ArrayList<Profile>>(){}.getType();
            List<Profile> profiles = gson.fromJson(reader, listType);
            return profiles != null ? profiles : new ArrayList<>();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load profiles: " + e.getMessage(), e);
        }
    }

    public void saveProfiles(List<Profile> profiles) {
        try (Writer writer = Files.newBufferedWriter(profilesFile)) {
            gson.toJson(profiles, writer);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save profiles: " + e.getMessage(), e);
        }
    }

    public void addProfile(Profile profile) {
        List<Profile> profiles = loadProfiles();

        Optional<Profile> existing = profiles.stream()
            .filter(p -> p.getAlias().equals(profile.getAlias()))
            .findFirst();

        if (existing.isPresent()) {
            throw new IllegalArgumentException("Profile with alias '" + profile.getAlias() + "' already exists in " + profilesFile);
        }

        profiles.add(profile);
        saveProfiles(profiles);
    }

    public Optional<Profile> getProfile(String alias) {
        return loadProfiles().stream()
            .filter(p -> p.getAlias().equals(alias))
            .findFirst();
    }

    public boolean deleteProfile(String alias) {
        List<Profile> profiles = loadProfiles();
        boolean removed = profiles.removeIf(p -> p.getAlias().equals(alias));

        if (removed) {
            saveProfiles(profiles);
        }

        return removed;
    }
}
