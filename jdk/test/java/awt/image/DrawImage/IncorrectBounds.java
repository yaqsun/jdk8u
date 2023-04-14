/*
 * Copyright (c) 2013, 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */


import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;

/**
 * @test
 * @key headful
 * @bug 8000629
 * @summary Temporary backbuffer in the DrawImage should not fill background
 * outside of source image bounds.
 * @author Sergey Bylokhov
 */
public final class IncorrectBounds {

    private static final int width = 400;
    private static final int height = 400;

    public static void main(final String[] args) {
        GraphicsEnvironment ge =
                GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsConfiguration gc =
                ge.getDefaultScreenDevice().getDefaultConfiguration();
        VolatileImage vi = gc.createCompatibleVolatileImage(width / 4,
                                                            height / 4);
        final BufferedImage bi = new BufferedImage(width, height,
                                                   BufferedImage.TYPE_INT_ARGB);
        while (true) {
            vi.validate(gc);
            Graphics2D g2d = vi.createGraphics();
            g2d.setColor(Color.green);
            g2d.fillRect(0, 0, width / 4, height / 4);
            g2d.dispose();

            if (vi.validate(gc) != VolatileImage.IMAGE_OK) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {
                }
                continue;
            }

            Graphics2D g = bi.createGraphics();
            g.setComposite(AlphaComposite.Src);
            g.setColor(Color.red);
            g.fillRect(0, 0, width, height);
            // Use sx and sy outside of VI bounds.
            g.drawImage(vi, 0, 0, width / 2, height / 2, 0, 0, width * 2,
                        height * 2, null);
            g.dispose();

            if (vi.contentsLost()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {
                }
                continue;
            }

            for (int x = 0; x < width; ++x) {
                for (int y = 0; y < height; ++y) {
                    if (x < width / 16 && y < height / 16) {
                        if (bi.getRGB(x, y) != Color.green.getRGB()) {
                            throw new RuntimeException("Test failed.");
                        }
                    } else {
                        if (bi.getRGB(x, y) != Color.red.getRGB()) {
                            throw new RuntimeException("Test failed.");
                        }
                    }
                }
            }
            break;
        }
    }
}
