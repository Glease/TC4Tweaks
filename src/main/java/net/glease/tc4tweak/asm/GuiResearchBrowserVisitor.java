package net.glease.tc4tweak.asm;

import com.google.common.collect.ImmutableMap;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import java.util.Map;

import static net.glease.tc4tweak.asm.ASMConstants.ASMCALLHOOK_INTERNAL_NAME;
import static net.glease.tc4tweak.asm.LoadingPlugin.dev;
import static net.glease.tc4tweak.modules.researchBrowser.DrawResearchBrowserBorders.BORDER_HEIGHT;
import static net.glease.tc4tweak.modules.researchBrowser.DrawResearchBrowserBorders.BORDER_WIDTH;
import static org.objectweb.asm.Opcodes.*;

public class GuiResearchBrowserVisitor extends ClassVisitor {
    private static final String TARGET_INTERNAL_NAME = "thaumcraft/client/gui/GuiResearchBrowser";

    private static class UpdateResearchVisitor extends MethodVisitor {
        public UpdateResearchVisitor(int api, MethodVisitor mv) {
            super(api, mv);
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String desc) {
            if (opcode == PUTSTATIC && TARGET_INTERNAL_NAME.equals(owner) && name.startsWith("guiMap") && "I".equals(desc)) {
                String side = name.substring(6);
                TC4Transformer.log.debug("Injecting modify callhook getNewGuiMap{}() before PUTSTATIC {}", side, name);
                super.visitMethodInsn(INVOKESTATIC, ASMCALLHOOK_INTERNAL_NAME, "getNewGuiMap" + side, "(I)I", false);
            }
            super.visitFieldInsn(opcode, owner, name, desc);
        }
    }

    private static class GenResearchBackgroundVisitor extends MethodVisitor {
        private int counter = 0;
        private boolean borderDrawChanged = false, found112 = false, backgroundDrawChanged = false;

        public GenResearchBackgroundVisitor(int api, MethodVisitor mv) {
            super(api, mv);
        }

        private static boolean isDrawResearchBrowserBackground(String methodName) {
            return dev ? "drawResearchBrowserBackground".equals(methodName) : "func_73729_b".equals(methodName);
        }

        @Override
        public void visitIntInsn(int opcode, int operand) {
            if (!found112 && operand == 112) {
                TC4Transformer.log.debug("Found 112");
                found112 = true;
            }
            super.visitIntInsn(opcode, operand);
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String desc) {
            super.visitFieldInsn(opcode, owner, name, desc);
            if (opcode == GETFIELD && "thaumcraft/client/gui/GuiResearchBrowser".equals(owner) && "paneWidth".equals(name) && "I".equals(desc)) {
                counter++;
                if (counter > 2)
                    TC4Transformer.log.warn("GuiResearchBrowser has been changed by other people! Things are not going to work right!");
            }
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            if (found112 && !backgroundDrawChanged && opcode == INVOKEVIRTUAL && owner.equals(TARGET_INTERNAL_NAME) && isDrawResearchBrowserBackground(name) && desc.equals("(IIIIII)V")) {
                backgroundDrawChanged = true;
                TC4Transformer.log.debug("Deflecting drawTexturedModalRect to drawResearchBrowserBackground");
                super.visitMethodInsn(INVOKESTATIC, ASMCALLHOOK_INTERNAL_NAME, "drawResearchBrowserBackground", "(L" + TARGET_INTERNAL_NAME + ";IIIIII)V", false);
            } else if (!borderDrawChanged && counter == 2 && opcode == INVOKEVIRTUAL && owner.equals(TARGET_INTERNAL_NAME) && isDrawResearchBrowserBackground(name) && desc.equals("(IIIIII)V")) {
                borderDrawChanged = true;
                TC4Transformer.log.debug("Deflecting drawTexturedModalRect to drawResearchBrowserBorders");
                super.visitMethodInsn(INVOKESTATIC, ASMCALLHOOK_INTERNAL_NAME, "drawResearchBrowserBorders", "(L" + TARGET_INTERNAL_NAME + ";IIIIII)V", false);
            } else {
                super.visitMethodInsn(opcode, owner, name, desc, itf);
            }
        }

        @Override
        public void visitEnd() {
            if (!borderDrawChanged)
                TC4Transformer.log.warn("MISSED BORDER DRAW INJECT. Research browser resizing will not work!");
            if (!backgroundDrawChanged)
                TC4Transformer.log.warn("MISSED BACKGROUND DRAW INJECT. Research browser resizing will not work!");
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
                TC4Transformer.log.debug("Replaced GETFIELD {} to INVOKESTATIC {}", name, target);
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
                TC4Transformer.log.debug("Replacing inner width 224 with getResearchBrowserWidth() - 2 * BORDER_WIDTH");
                super.visitMethodInsn(INVOKESTATIC, ASMCALLHOOK_INTERNAL_NAME, "getResearchBrowserWidth", "()I", false);
                super.visitIntInsn(BIPUSH, BORDER_WIDTH * 2);
                super.visitInsn(ISUB);
                widthReplaced++;
            } else if (operand == 196) {
                TC4Transformer.log.debug("Replacing inner height 196 with getResearchBrowserHeight() - 2 * BORDER_HEIGHT");
                super.visitMethodInsn(INVOKESTATIC, ASMCALLHOOK_INTERNAL_NAME, "getResearchBrowserHeight", "()I", false);
                super.visitIntInsn(BIPUSH, BORDER_HEIGHT * 2);
                super.visitInsn(ISUB);
            } else {
                super.visitIntInsn(opcode, operand);
            }
        }
    }

    public GuiResearchBrowserVisitor(int api, ClassVisitor cv) {
        super(api, cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = new FieldAccessDeflector(api, super.visitMethod(access, name, desc, signature, exceptions));
        if ("genResearchBackground".equals(name) && "(IIF)V".equals(desc)) {
            TC4Transformer.log.debug("Visiting genResearchBackground(IIF)V");
            return new GenResearchBackgroundVisitor(api, mv);
        } else if ("updateResearch".equals(name) && "()V".equals(desc)) {
            TC4Transformer.log.debug("Visiting updateResearch()V");
            return new UpdateResearchVisitor(api, mv);
        }
        return mv;
    }
}
