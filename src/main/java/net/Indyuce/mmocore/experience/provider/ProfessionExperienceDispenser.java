package net.Indyuce.mmocore.experience.provider;

import net.Indyuce.mmocore.experience.EXPSource;
import net.Indyuce.mmocore.experience.Profession;
import net.Indyuce.mmocore.api.player.PlayerData;
import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;

public class ProfessionExperienceDispenser implements ExperienceDispenser {
    private final Profession profession;

    public ProfessionExperienceDispenser(Profession profession) {
        this.profession = profession;
    }

    @Override
    public void giveExperience(PlayerData playerData, double experience, @Nullable Location hologramLocation) {
        hologramLocation = !profession.getOption(Profession.ProfessionOption.EXP_HOLOGRAMS) ? null
                : hologramLocation == null ? getPlayerLocation(playerData) : hologramLocation;
        playerData.getCollectionSkills().giveExperience(profession, experience, EXPSource.SOURCE, hologramLocation);
    }

    @Override
    public boolean matches(PlayerData playerData) {
        return true;
    }
}
