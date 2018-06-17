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
package org.spongepowered.common.event.listener;

import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.filter.cause.ContextValue;
import org.spongepowered.common.event.EventFilterTest;

public class ContextValueListener {

    public boolean contextListenerCalled;
    public boolean contextListenerCalledInc;
    public boolean contextListenerCalledEx;

    @Listener
    public void contextListener(EventFilterTest.SubEvent event,
            @ContextValue("OWNER") User user) {

        this.contextListenerCalled = true;
    }

    @Listener
    public void contextListenerInclude(EventFilterTest.SubEvent event,
            @ContextValue(value = "OWNER", typeFilter = User.class) Object user) {

        this.contextListenerCalledInc = true;
    }

    @Listener
    public void contextListenerExclude(EventFilterTest.SubEvent event,
            @ContextValue(value = "OWNER", typeFilter = User.class, inverse = true) Object user) {

        this.contextListenerCalledEx = true;
    }

}