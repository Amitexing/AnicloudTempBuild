package pl.flezy.tempbuild.config;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import org.bukkit.Material;

import java.util.List;

public class Config extends OkaeriConfig {
    @Comment("Time in seconds before placed blocks decay and disappear")
    public int blockDecayTime = 30;
    @Comment("Whether decaying blocks should drop items when they disappear")
    public boolean dropBlocks = true;
    @Comment("Whether blocks in the region should be protected from explosions")
    public boolean protectFromExplosions = true;
    @Comment("Whether fire should be able to burn blocks in the region")
    public boolean allowFireBurn = false;
    @Comment("Whether fire should be able to ignite blocks in the region")
    public boolean allowFireIgnite = false;
    @Comment("Whether fire and mushroom should be able to spread in the region")
    public boolean allowBlockSpread = false;
    @Comment("Whether players should be able to replace non-collidable blocks in the region")
    public boolean allowReplaceNonCollidableBlocks = false;
    @Comment("Whether liquids should be able to flow in the region")
    public boolean allowLiquidFlow = false;
    @Comment("Whether players should be able to replace liquids in the region")
    @Comment("WARNING: This also prevents placing blocks on water in the region by players")
    public boolean allowReplaceLiquids = true;
    @Comment("List of blocks that cannot be placed in the region")
    public List<Material> blockedBlocks = List.of(
            Material.LAVA
    );
    @Comment("Allow breaking UltimateBlockRegen replacement blocks (bedrock) in temp-build regions")
    public boolean allowBreakUltimateBlockRegenBlocks = true;
}
