package net.glease.tc4tweak.asm;

import cpw.mods.fml.relauncher.FMLLaunchHandler;
import cpw.mods.fml.relauncher.Side;
import org.objectweb.asm.ClassVisitor;

import java.util.function.BiFunction;

class TransformerFactory {
    private final BiFunction<Integer, ClassVisitor, ClassVisitor> factory;
    private final Side activeSide;
    private final boolean expandFrames;
    private volatile ClassVisitor generated;

    public TransformerFactory(BiFunction<Integer, ClassVisitor, ClassVisitor> factory) {
        this(factory, null, false);
    }

    public TransformerFactory(BiFunction<Integer, ClassVisitor, ClassVisitor> factory, boolean expandFrames) {
        this(factory, null, expandFrames);
    }

    public TransformerFactory(BiFunction<Integer, ClassVisitor, ClassVisitor> factory, Side activeSide) {
        this(factory, activeSide, false);
    }

    /**
     * @param factory      the constructor of actual ClassVisitor. First argument is api level. Second argument is downstream ClassVisitor
     * @param activeSide   the side this transformer will be active on. null for both side.
     * @param expandFrames whether the frames need to be recalculated
     */
    public TransformerFactory(BiFunction<Integer, ClassVisitor, ClassVisitor> factory, Side activeSide, boolean expandFrames) {
        this.factory = factory;
        this.activeSide = activeSide;
        this.expandFrames = expandFrames;
    }

    public boolean isInactive() {
        return activeSide != null && activeSide != FMLLaunchHandler.side();
    }

    public final ClassVisitor apply(int api, ClassVisitor downstream) {
        if (generated == null) {
            synchronized (this) {
                if (generated == null)
                    generated = factory.apply(api, downstream);
            }
        }
        return generated;
    }

    public boolean isExpandFrames() {
        return expandFrames;
    }
}
