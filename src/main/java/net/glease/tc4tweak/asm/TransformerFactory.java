package net.glease.tc4tweak.asm;

import cpw.mods.fml.relauncher.FMLLaunchHandler;
import cpw.mods.fml.relauncher.Side;
import org.objectweb.asm.ClassVisitor;

class TransformerFactory {
    private final TransformerProducer factory;
    private final Side activeSide;
    private final boolean expandFrames;
    private volatile ClassVisitor generated;

    public TransformerFactory(TransformerProducer factory) {
        this(factory, null, false);
    }

    public TransformerFactory(TransformerProducer factory, boolean expandFrames) {
        this(factory, null, expandFrames);
    }

    public TransformerFactory(TransformerProducer factory, Side activeSide) {
        this(factory, activeSide, false);
    }

    /**
     * @param factory      the constructor of actual ClassVisitor. First argument is api level. Second argument is downstream ClassVisitor
     * @param activeSide   the side this transformer will be active on. null for both side.
     * @param expandFrames whether the frames need to be recalculated
     */
    public TransformerFactory(TransformerProducer factory, Side activeSide, boolean expandFrames) {
        this.factory = factory;
        this.activeSide = activeSide;
        this.expandFrames = expandFrames;
    }

    public TransformerFactory(TransformerProducerSimple factory) {
        this(factory, null, false);
    }

    public TransformerFactory(TransformerProducerSimple factory, boolean expandFrames) {
        this(factory, null, expandFrames);
    }

    public TransformerFactory(TransformerProducerSimple factory, Side activeSide) {
        this(factory, activeSide, false);
    }

    /**
     * @param factory      the constructor of actual ClassVisitor. First argument is api level. Second argument is downstream ClassVisitor
     * @param activeSide   the side this transformer will be active on. null for both side.
     * @param expandFrames whether the frames need to be recalculated
     */
    public TransformerFactory(TransformerProducerSimple factory, Side activeSide, boolean expandFrames) {
        this.factory = factory;
        this.activeSide = activeSide;
        this.expandFrames = expandFrames;
    }

    public boolean isInactive() {
        return activeSide != null && activeSide != FMLLaunchHandler.side();
    }

    public final ClassVisitor apply(int api, String transformedName, ClassVisitor downstream) {
        if (generated == null) {
            synchronized (this) {
                if (generated == null)
                    generated = factory.apply(api, transformedName, downstream);
            }
        }
        return generated;
    }

    public boolean isExpandFrames() {
        return expandFrames;
    }

    interface TransformerProducer {
        ClassVisitor apply(int api, String transformedName, ClassVisitor downstream);
    }

    interface TransformerProducerSimple extends TransformerProducer {
        @Override
        default ClassVisitor apply(int api, String transformedName, ClassVisitor downstream) {
            return apply(api, downstream);
        }

        ClassVisitor apply(int api, ClassVisitor downstream);
    }
}
