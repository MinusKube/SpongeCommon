/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.common.event.filter.delegate;

import static org.objectweb.asm.Opcodes.ACONST_NULL;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.IFEQ;
import static org.objectweb.asm.Opcodes.IFNE;
import static org.objectweb.asm.Opcodes.INSTANCEOF;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.PUTFIELD;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.spongepowered.api.GameRegistry;
import org.spongepowered.api.event.cause.EventContext;
import org.spongepowered.api.event.cause.EventContextKey;
import org.spongepowered.api.event.filter.cause.ContextValue;
import org.spongepowered.common.SpongeImpl;
import org.spongepowered.common.registry.SpongeGameRegistry;

import java.lang.reflect.Parameter;
import java.util.Optional;

public class ContextValueFilterSourceDelegate extends ContextFilterSourceDelegate {

    private final ContextValue anno;

    public ContextValueFilterSourceDelegate(ContextValue anno) {
        this.anno = anno;

        Optional<EventContextKey> opKey = SpongeImpl.getRegistry().getType(EventContextKey.class, this.anno.value());

        if(!opKey.isPresent()) {
            throw new IllegalArgumentException(String.format(
                    "The %s context key specified by the context value annotation was not found",
                    this.anno.value()
            ));
        }
    }

    @Override
    public void createFields(ClassWriter cw) {
        FieldVisitor fv = cw.visitField(0, "contextKey", Type.getDescriptor(EventContextKey.class),
                "Lorg/spongepowered/api/event/cause/EventContextKey<*>;", null);
        fv.visitEnd();
    }

    @Override
    public void writeCtor(String name, ClassWriter cw, MethodVisitor mv) {
        mv.visitVarInsn(ALOAD, 0);

        mv.visitMethodInsn(INVOKESTATIC, Type.getInternalName(SpongeImpl.class), "getRegistry",
                Type.getMethodDescriptor(Type.getType(SpongeGameRegistry.class)), false);
        mv.visitLdcInsn(Type.getType(EventContextKey.class));
        mv.visitLdcInsn(this.anno.value());

        mv.visitMethodInsn(INVOKEINTERFACE, Type.getInternalName(GameRegistry.class), "getType",
                Type.getMethodDescriptor(
                        Type.getType(Optional.class),
                        Type.getType(Class.class), Type.getType(String.class)
                ),
                true
        );

        mv.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(Optional.class), "get",
                "()Ljava/lang/Object;", false);
        mv.visitTypeInsn(CHECKCAST, Type.getInternalName(EventContextKey.class));

        mv.visitFieldInsn(PUTFIELD, name, "contextKey", Type.getDescriptor(EventContextKey.class));
    }

    @Override
    protected void insertContextCall(String name, MethodVisitor mv, Parameter param, Class<?> targetType) {
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, name, "contextKey", Type.getDescriptor(EventContextKey.class));

        mv.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(EventContext.class), "get",
                Type.getMethodDescriptor(
                        Type.getType(Optional.class),
                        Type.getType(EventContextKey.class)
                ),
                false
        );

        mv.visitInsn(ACONST_NULL);
        mv.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(Optional.class), "orElse",
                Type.getMethodDescriptor(
                        Type.getType(Object.class),
                        Type.getType(Object.class)
                ),
                false
        );
    }

    @Override
    protected void insertTransform(MethodVisitor mv, Parameter param, Class<?> targetType, int local) {
        Label failure = new Label();
        Label success = new Label();

        mv.visitVarInsn(ALOAD, local);
        mv.visitTypeInsn(INSTANCEOF, Type.getInternalName(targetType));

        if (this.anno.typeFilter().length != 0) {
            mv.visitJumpInsn(IFEQ, failure);
            mv.visitVarInsn(ALOAD, local);
            // For each type we do an instance check and jump to either failure or success if matched
            for (int i = 0; i < this.anno.typeFilter().length; i++) {
                Class<?> filter = this.anno.typeFilter()[i];
                if (i < this.anno.typeFilter().length - 1) {
                    mv.visitInsn(DUP);
                }
                mv.visitTypeInsn(INSTANCEOF, Type.getInternalName(filter));
                if (this.anno.inverse()) {
                    mv.visitJumpInsn(IFNE, failure);
                } else {
                    mv.visitJumpInsn(IFNE, success);
                }
            }
            if (this.anno.inverse()) {
                mv.visitJumpInsn(GOTO, success);
            }
            // If the annotation was not reversed then we fall into failure as no types were matched
        } else {
            mv.visitJumpInsn(IFNE, success);
        }
        mv.visitLabel(failure);
        mv.visitInsn(ACONST_NULL);
        mv.visitInsn(ARETURN);

        mv.visitLabel(success);
    }
}
