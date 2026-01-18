package com.plotsquared.fabric.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Decoder;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.PlotId;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Cod;

import java.util.List;

public class PlotSquaredDataAttachments {

    public static AttachmentType<String> PLOT_DATA;
    public static AttachmentType<GlobalPos> P2;
    public static AttachmentType<String> SHULKER_PLOT;
    public static AttachmentType<Boolean> PS_CUSTOM_SPAWNED;
    public static AttachmentType<Boolean> KEEP;
    public static AttachmentType<Boolean> PS_TMP_TELEPORT;
    public static AttachmentType<List<String>> PLOT;

    static {
        PLOT_DATA = AttachmentRegistry.createPersistent(ResourceLocation.fromNamespaceAndPath("plotsquared", "plot_data"), Codec.STRING);
        P2 = AttachmentRegistry.createPersistent(ResourceLocation.fromNamespaceAndPath("plotsquared", "p2"), GlobalPos.CODEC);
        SHULKER_PLOT = AttachmentRegistry.createPersistent(ResourceLocation.fromNamespaceAndPath("plotsquared", "shulkerplot"), Codec.STRING);
        PS_CUSTOM_SPAWNED = AttachmentRegistry.createPersistent(ResourceLocation.fromNamespaceAndPath("plotsquared", "ps_custom_spawned"),
                Codec.BOOL);
        KEEP = AttachmentRegistry.createPersistent(ResourceLocation.fromNamespaceAndPath("plotsquared", "keep"), Codec.BOOL);
        PS_TMP_TELEPORT = AttachmentRegistry.createPersistent(ResourceLocation.fromNamespaceAndPath("plotsquared", "ps_tmp_teleport"), Codec.BOOL);
        PLOT = AttachmentRegistry.createPersistent(ResourceLocation.fromNamespaceAndPath("plotsquared", "plot"), Codec.STRING.listOf());
    }

    public PlotSquaredDataAttachments() {}


}
