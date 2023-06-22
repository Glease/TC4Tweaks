package net.glease.tc4tweak.asm;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import static net.glease.tc4tweak.asm.ASMConstants.ASMCALLHOOK_INTERNAL_NAME;
import static net.glease.tc4tweak.asm.LoadingPlugin.dev;
import static net.glease.tc4tweak.asm.TC4Transformer.log;
import static net.glease.tc4tweak.modules.researchBrowser.DrawResearchBrowserBorders.BORDER_HEIGHT;
import static net.glease.tc4tweak.modules.researchBrowser.DrawResearchBrowserBorders.BORDER_WIDTH;
import static org.objectweb.asm.Opcodes.*;

class GuiResearchBrowserVisitor extends ClassVisitor {
    private static final String TARGET_INTERNAL_NAME = "thaumcraft/client/gui/GuiResearchBrowser";

    public GuiResearchBrowserVisitor(int api, ClassVisitor cv) {
        super(api, cv);
    }

    private static boolean isMouseClickedMethod(String name) {
        return dev ? "mouseClicked".equals(name) : "func_73864_a".equals(name);
    }

    private static boolean isDrawScreenMethod(String name) {
        return dev ? "drawScreen".equals(name) : "func_73863_a".equals(name);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = new FieldAccessDeflector(api, super.visitMethod(access, name, desc, signature, exceptions));
        if ("genResearchBackground".equals(name) && "(IIF)V".equals(desc)) {
            log.debug("Visiting genResearchBackground(IIF)V");
            return new GenResearchBackgroundVisitor(api, new ConstantToDynamicReplacer(api, new LimitResearchCategoryToPageVisitor(api, mv, 1), 9, "getTabPerSide", 1));
        } else if ("updateResearch".equals(name) && "()V".equals(desc)) {
            log.debug("Visiting updateResearch()V");
            return new UpdateResearchVisitor(api, new LimitResearchCategoryToPageVisitor(api, mv, 1));
        } else if (isMouseClickedMethod(name) && "(III)V".equals(desc) ||
                isDrawScreenMethod(name) && "(IIF)V".equals(desc)) {
            log.debug("Visiting {}{}", name, desc);
            return new ConstantToDynamicReplacer(api, new ConstantToDynamicReplacer(api, new LimitResearchCategoryToPageVisitor(api, mv, 1), 9, "getTabPerSide"),
                    280, "getTabIconDistance");
        }
        return mv;
    }

    private static class UpdateResearchVisitor extends MethodVisitor {
        public UpdateResearchVisitor(int api, MethodVisitor mv) {
            super(api, mv);
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String desc) {
            if (opcode == PUTSTATIC && TARGET_INTERNAL_NAME.equals(owner) && name.startsWith("guiMap") && "I".equals(desc)) {
                String side = name.substring(6);
                log.trace("Injecting modify callhook getNewGuiMap{}() before PUTSTATIC {}", side, name);
                super.visitMethodInsn(INVOKESTATIC, ASMCALLHOOK_INTERNAL_NAME, "getNewGuiMap" + side, "(I)I", false);
            }
            super.visitFieldInsn(opcode, owner, name, desc);
        }
    }

    private static class GenResearchBackgroundVisitor extends MethodVisitor {
        private int counterPaneWidth = 0;
        private int counterParticleDraw = 0;
        private boolean borderDrawChanged = false, found112 = false, backgroundDrawChanged = false;

        public GenResearchBackgroundVisitor(int api, MethodVisitor mv) {
            super(api, mv);
        }

        private static boolean isDrawTexturedModalRect(String methodName) {
            return dev ? "drawTexturedModalRect".equals(methodName) : "func_73729_b".equals(methodName);
        }

        @Override
        public void visitIntInsn(int opcode, int operand) {
            if (!found112 && operand == 112) {
                log.trace("Found 112");
                found112 = true;
            } else if (operand == 264) {
                log.trace("Replacing 264 with getTabDistance()");
                super.visitMethodInsn(INVOKESTATIC, ASMCALLHOOK_INTERNAL_NAME, "getTabDistance", "()I", false);
                return;
            }
            super.visitIntInsn(opcode, operand);
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String desc) {
            super.visitFieldInsn(opcode, owner, name, desc);
            if (opcode == GETFIELD && "thaumcraft/client/gui/GuiResearchBrowser".equals(owner) && "paneWidth".equals(name) && "I".equals(desc)) {
                counterPaneWidth++;
                if (counterPaneWidth > 2)
                    log.error("GuiResearchBrowser has been changed by other people! Things are not going to work right!");
            }
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            if (found112 && !backgroundDrawChanged && opcode == INVOKEVIRTUAL && owner.equals(TARGET_INTERNAL_NAME) && isDrawTexturedModalRect(name) && desc.equals("(IIIIII)V")) {
                backgroundDrawChanged = true;
                log.trace("Deflecting drawTexturedModalRect to drawResearchBrowserBackground");
                super.visitMethodInsn(INVOKESTATIC, ASMCALLHOOK_INTERNAL_NAME, "drawResearchBrowserBackground", "(L" + TARGET_INTERNAL_NAME + ";IIIIII)V", false);
            } else if (!borderDrawChanged && counterPaneWidth == 2 && opcode == INVOKEVIRTUAL && owner.equals(TARGET_INTERNAL_NAME) && isDrawTexturedModalRect(name) && desc.equals("(IIIIII)V")) {
                borderDrawChanged = true;
                log.trace("Deflecting drawTexturedModalRect to drawResearchBrowserBorders");
                super.visitMethodInsn(INVOKESTATIC, ASMCALLHOOK_INTERNAL_NAME, "drawResearchBrowserBorders", "(L" + TARGET_INTERNAL_NAME + ";IIIIII)V", false);
            } else if (opcode == INVOKESTATIC && owner.equals("thaumcraft/client/lib/UtilsFX") && name.equals("drawTexturedQuad") && desc.equals("(IIIIIID)V") && counterParticleDraw++ == 1) {
                log.trace("Deflecting drawTexturedQuad to drawResearchCategoryHintParticles");
                super.visitVarInsn(ALOAD, 0);
                super.visitMethodInsn(INVOKESTATIC, ASMCALLHOOK_INTERNAL_NAME, "drawResearchCategoryHintParticles", "(IIIIIIDL" + TARGET_INTERNAL_NAME + ";)V", false);
            } else {
                super.visitMethodInsn(opcode, owner, name, desc, itf);
            }
        }

        @Override
        public void visitEnd() {
            if (counterParticleDraw < 2)
                log.error("MISSED HINT PARTICLE REDIRECT. This fix will not work!");
            else if (counterParticleDraw > 2)
                log.error("WEIRD HINT PARTICLE REDIRECT. This fix will most likely not work!");
            if (!borderDrawChanged)
                log.error("MISSED BORDER DRAW INJECT. Research browser resizing will not work!");
            if (!backgroundDrawChanged)
                log.error("MISSED BACKGROUND DRAW INJECT. Research browser resizing will not work!");
            super.visitEnd();
        }
    }

    private static class FieldAccessDeflector extends MethodVisitor {
        private static final Map<String, String> TARGET_FIELDS = ImmutableMap.of("paneWidth", "getResearchBrowserWidth", "paneHeight", "getResearchBrowserHeight");
        private int widthReplaced = 0;

        public FieldAccessDeflector(int api, MethodVisitor mv) {
            super(api, mv);
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String desc) {
            String target;
            if (opcode == GETFIELD && TARGET_INTERNAL_NAME.equals(owner) && (target = TARGET_FIELDS.get(name)) != null && "I".equals(desc)) {
                log.trace("Replaced GETFIELD {} to INVOKESTATIC {}", name, target);
                super.visitInsn(POP);
                super.visitMethodInsn(INVOKESTATIC, ASMCALLHOOK_INTERNAL_NAME, target, "()I", false);
            } else {
                super.visitFieldInsn(opcode, owner, name, desc);
            }
        }

        @Override
        public void visitIntInsn(int opcode, int operand) {
            if (operand == 224 && widthReplaced < 2) {
                // third 224 in genResearchBackground is u coord
                log.trace("Replacing inner width 224 with getResearchBrowserWidth() - 2 * BORDER_WIDTH");
                super.visitMethodInsn(INVOKESTATIC, ASMCALLHOOK_INTERNAL_NAME, "getResearchBrowserWidth", "()I", false);
                super.visitIntInsn(BIPUSH, BORDER_WIDTH * 2);
                super.visitInsn(ISUB);
                widthReplaced++;
            } else if (operand == 196) {
                log.trace("Replacing inner height 196 with getResearchBrowserHeight() - 2 * BORDER_HEIGHT");
                super.visitMethodInsn(INVOKESTATIC, ASMCALLHOOK_INTERNAL_NAME, "getResearchBrowserHeight", "()I", false);
                super.visitIntInsn(BIPUSH, BORDER_HEIGHT * 2);
                super.visitInsn(ISUB);
            } else {
                super.visitIntInsn(opcode, operand);
            }
        }
    }

    private static class LimitResearchCategoryToPageVisitor extends MethodVisitor {
        private final int maxReplacement;
        private int replaced = 0;

        public LimitResearchCategoryToPageVisitor(int api, MethodVisitor mv, int maxReplacement) {
            super(api, mv);
            this.maxReplacement = maxReplacement;
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String desc) {
            if (replaced < maxReplacement && opcode == GETSTATIC && "thaumcraft/api/research/ResearchCategories".equals(owner) && "researchCategories".equals(name) && "Ljava/util/LinkedHashMap;".equals(desc)) {
                log.trace("Replacing ResearchCategories.researchCategories with getTabsOnCurrentPage()");
                super.visitVarInsn(ALOAD, 0);
                super.visitFieldInsn(GETFIELD, TARGET_INTERNAL_NAME, "player", "Ljava/lang/String;");
                super.visitMethodInsn(INVOKESTATIC, ASMCALLHOOK_INTERNAL_NAME, "getTabsOnCurrentPage", "(Ljava/lang/String;)Ljava/util/LinkedHashMap;", false);
                replaced++;
            } else
                super.visitFieldInsn(opcode, owner, name, desc);
        }
    }

    private static class ConstantToDynamicReplacer extends MethodVisitor {
        private final int maxReplacement;
        private final int toReplace;
        private final String target;
        private int replaced = 0;

        public ConstantToDynamicReplacer(int api, MethodVisitor mv, int toReplace, String target) {
            this(api, mv, toReplace, target, 1);
        }

        public ConstantToDynamicReplacer(int api, MethodVisitor mv, int toReplace, String target, int maxReplacement) {
            super(api, mv);
            this.maxReplacement = maxReplacement;
            this.toReplace = toReplace;
            this.target = target;
        }

        @Override
        public void visitIntInsn(int opcode, int operand) {
            if (replaced < maxReplacement && operand == toReplace) {
                log.trace("Replacing constant {} with {}()I", toReplace, target);
                super.visitMethodInsn(INVOKESTATIC, ASMCALLHOOK_INTERNAL_NAME, target, "()I", false);
                replaced++;
            } else {
                super.visitIntInsn(opcode, operand);
            }
        }
    }
}
