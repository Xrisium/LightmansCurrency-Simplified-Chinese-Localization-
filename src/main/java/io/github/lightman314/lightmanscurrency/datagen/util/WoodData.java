package io.github.lightman314.lightmanscurrency.datagen.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class WoodData {

    public final Supplier<? extends ItemLike> logBlock;
    public final ResourceLocation logSideTexture;
    public final ResourceLocation logTopTexture;
    public final Supplier<? extends ItemLike> plankBlock;
    public final ResourceLocation plankTexture;
    public final Supplier<? extends ItemLike> slabBlock;

    private WoodData(Supplier<? extends ItemLike> logBlock, Supplier<? extends ItemLike> plankBlock, Supplier<? extends ItemLike> slabBlock, ResourceLocation logSideTexture, ResourceLocation logTopTexture, ResourceLocation plankTexture)
    {
        this.logBlock = logBlock;
        this.logSideTexture = logSideTexture;
        this.logTopTexture = logTopTexture;
        this.plankBlock = plankBlock;
        this.plankTexture = plankTexture;
        this.slabBlock = slabBlock;
    }

    public static WoodData of1(Supplier<RegistryObject<? extends ItemLike>> logBlock, Supplier<RegistryObject<? extends ItemLike>> plankBlock, Supplier<RegistryObject<? extends ItemLike>> slabBlock, ResourceLocation logSideTexture, ResourceLocation logTopTexture, ResourceLocation plankTexture) { return new WoodData(() -> logBlock.get().get(), () -> plankBlock.get().get(), () -> slabBlock.get().get(), logSideTexture, logTopTexture, plankTexture); }
    public static WoodData of1(Supplier<RegistryObject<? extends ItemLike>> logBlock, Supplier<RegistryObject<? extends ItemLike>> plankBlock, Supplier<RegistryObject<? extends ItemLike>> slabBlock, String logSideTexture, String logTopTexture, String plankTexture) { return new WoodData(() -> logBlock.get().get(), () -> plankBlock.get().get(), () -> slabBlock.get().get(), new ResourceLocation(logSideTexture), new ResourceLocation(logTopTexture), new ResourceLocation(plankTexture)); }
    public static WoodData of2(Supplier<? extends ItemLike> logBlock, Supplier<? extends ItemLike> plankBlock, Supplier<? extends ItemLike> slabBlock, ResourceLocation logSideTexture, ResourceLocation logTopTexture, ResourceLocation plankTexture) { return new WoodData(logBlock, plankBlock, slabBlock, logSideTexture, logTopTexture, plankTexture); }
    public static WoodData of2(Supplier<? extends ItemLike> logBlock, Supplier<? extends ItemLike> plankBlock, Supplier<? extends ItemLike> slabBlock, String logSideTexture, String logTopTexture, String plankTexture) { return new WoodData(logBlock, plankBlock, slabBlock, new ResourceLocation(logSideTexture), new ResourceLocation(logTopTexture), new ResourceLocation(plankTexture)); }
    public static WoodData of(ItemLike logBlock, ItemLike plankBlock, ItemLike slabBlock, ResourceLocation logSideTexture, ResourceLocation logTopTexture, ResourceLocation plankTexture) { return new WoodData(() -> logBlock, () -> plankBlock, () -> slabBlock, logSideTexture, logTopTexture, plankTexture); }
    public static WoodData of(ItemLike logBlock, ItemLike plankBlock, ItemLike slabBlock, String logSideTexture, String logTopTexture, String plankTexture) { return new WoodData(() -> logBlock, () -> plankBlock, () -> slabBlock, new ResourceLocation(logSideTexture), new ResourceLocation(logTopTexture), new ResourceLocation(plankTexture)); }

}
