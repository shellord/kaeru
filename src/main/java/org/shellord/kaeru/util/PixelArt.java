package org.shellord.kaeru.util;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.Random;

/**
 * Centralized pixel-art drawing for the scene canvases.
 * Used by Setup, Focus, and Summary screens.
 */
public final class PixelArt {

    public enum Mode { DAY, NIGHT, DUSK }

    // ── Day palette ──
    private static final Color SKY_DAY     = Color.web("#c4846a");
    private static final Color SKY_HORIZON = Color.web("#b87858");
    private static final Color OCEAN_DAY   = Color.web("#7a6070");
    private static final Color OCEAN_LINE  = Color.web("#a08090");
    private static final Color GRASS_DAY   = Color.web("#5a6a48");
    private static final Color GRASS_DAY2  = Color.web("#6a7a58");
    private static final Color SUN_COLOR   = Color.web("#f0c060");

    // ── Night palette ──
    private static final Color SKY_NIGHT   = Color.web("#1a1830");
    private static final Color OCEAN_NIGHT = Color.web("#2a2850");
    private static final Color GRASS_NIGHT = Color.web("#202038");
    private static final Color GRASS_NIGHT2 = Color.web("#2a2848");
    private static final Color MOON_COLOR  = Color.web("#c8c8e8");

    // ── Dusk palette (summary screen) ──
    private static final Color SKY_DUSK    = Color.web("#9050a0");
    private static final Color SKY_DUSK2   = Color.web("#e89060");
    private static final Color OCEAN_DUSK  = Color.web("#4a3050");
    private static final Color GRASS_DUSK  = Color.web("#3a3848");
    private static final Color GRASS_DUSK2 = Color.web("#4a4858");

    private static final Color CLOUD_COLOR = Color.web("#e8c8a8");

    // ── Star data ──
    public static class Star {
        public double x, y, phase, speed, size;
    }

    public static Star[] generateStars(int count, double width, double height, long seed) {
        Random rnd = new Random(seed);
        Star[] arr = new Star[count];
        for (int i = 0; i < count; i++) {
            Star s = new Star();
            s.x = rnd.nextDouble() * width;
            s.y = rnd.nextDouble() * height * 0.65;
            s.phase = rnd.nextDouble() * Math.PI * 2;
            s.speed = 1.5 + rnd.nextDouble() * 2.5;
            s.size = (rnd.nextDouble() < 0.2) ? 2 : 1;
            arr[i] = s;
        }
        return arr;
    }

    // ── Cloud cache ──
    private static final double[] cloudSpeeds      = { 12.0, 18.0, 8.0,  22.0, 15.0 };
    private static final double[] cloudYPos        = { 8,    14,   18,   6,    12   };
    private static final double[] cloudOffsetStart = { 0,    100,  200,  300,  150  };
    private static final double[] cloudOpacity     = { 1.0,  0.7,  0.5,  0.85, 0.6  };

    /**
     * Draw the full scene. The mode determines the palette.
     * @param riderProgress 0..1 for rider position. Use -1 for parked at left, 2 for parked at right.
     */
    public static void drawScene(GraphicsContext gc, double W, double H, double t,
                                 Mode mode, double riderProgress, Star[] stars,
                                 boolean riderMoving, String topLeftLabel,
                                 String topRightLabel) {
        gc.clearRect(0, 0, W, H);

        switch (mode) {
            case NIGHT -> drawNightSky(gc, W, H, t, stars);
            case DUSK  -> drawDuskSky(gc, W, H, t, stars);
            case DAY   -> drawDaySky(gc, W, H, t);
        }

        // ocean
        double oceanY = H * 0.62;
        Color oceanColor = switch (mode) {
            case NIGHT -> OCEAN_NIGHT;
            case DUSK  -> OCEAN_DUSK;
            case DAY   -> OCEAN_DAY;
        };
        gc.setFill(oceanColor);
        gc.fillRect(0, oceanY, W, H - oceanY);

        Color oceanLine = switch (mode) {
            case NIGHT -> Color.web("#3a3870");
            case DUSK  -> Color.web("#7a5070");
            case DAY   -> OCEAN_LINE;
        };
        gc.setFill(oceanLine);
        gc.fillRect(0, oceanY - 2, W, 3);

        // ocean ripples
        gc.setFill(switch (mode) {
            case NIGHT -> Color.color(0.5, 0.5, 0.7, 0.3);
            case DUSK  -> Color.color(0.85, 0.7, 0.7, 0.3);
            case DAY   -> Color.color(0.85, 0.7, 0.7, 0.3);
        });
        for (int i = 0; i < 8; i++) {
            double rx = ((i * 60) + t * 8) % W;
            double ry = oceanY + 4 + (i % 2) * 3;
            gc.fillRect(rx, ry, 6, 1);
        }

        // grass
        double grassY = H * 0.75;
        Color grassColor = switch (mode) {
            case NIGHT -> GRASS_NIGHT;
            case DUSK  -> GRASS_DUSK;
            case DAY   -> GRASS_DAY;
        };
        Color grassTuft = switch (mode) {
            case NIGHT -> GRASS_NIGHT2;
            case DUSK  -> GRASS_DUSK2;
            case DAY   -> GRASS_DAY2;
        };
        gc.setFill(grassColor);
        gc.fillRect(0, grassY, W, H - grassY);
        gc.setFill(grassTuft);
        for (int x = 0; x < W; x += 8) {
            gc.fillRect(x, grassY - 3, 5, 4);
        }

        // labels
        if (topLeftLabel != null) {
            gc.setFont(Font.font("Courier New", FontWeight.BOLD, 11));
            gc.setFill(switch (mode) {
                case NIGHT -> Color.web("#a0c8e0cc");
                case DUSK  -> Color.web("#f8d8a0dd");
                case DAY   -> Color.web("#f0c890dd");
            });
            gc.fillText(topLeftLabel, 10, 16);
        }
        if (topRightLabel != null) {
            gc.setFont(Font.font("Courier New", FontWeight.BOLD, 10));
            gc.setFill(switch (mode) {
                case NIGHT -> Color.web("#5a5890");
                case DUSK  -> Color.web("#8a5050");
                case DAY   -> Color.web("#7a5030");
            });
            double tw = topRightLabel.length() * 7;
            gc.fillText(topRightLabel, W - tw - 8, 16);
        }

        // rider
        double riderX;
        if (riderProgress < 0) riderX = 16;                      // parked left
        else if (riderProgress > 1) riderX = W - 48;             // parked right
        else riderX = 16 + (W - 80) * riderProgress;
        double riderY = grassY - 26;
        drawRider(gc, riderX, riderY, riderMoving, t);
    }

    // ── Sky variants ──

    private static void drawDaySky(GraphicsContext gc, double W, double H, double t) {
        gc.setFill(SKY_DAY);
        gc.fillRect(0, 0, W, H * 0.55);
        gc.setFill(SKY_HORIZON);
        gc.fillRect(0, H * 0.45, W, H * 0.2);

        // sun
        double sunX = W - 56, sunY = 8;
        gc.setFill(Color.color(1.0, 0.85, 0.55, 0.18));
        gc.fillOval(sunX - 8, sunY - 8, 38, 38);
        gc.setFill(SUN_COLOR);
        gc.fillOval(sunX, sunY, 22, 22);

        // drifting clouds
        for (int i = 0; i < cloudSpeeds.length; i++) {
            double x = (cloudOffsetStart[i] + t * cloudSpeeds[i]) % (W + 80) - 40;
            drawCloud(gc, x, cloudYPos[i], cloudOpacity[i]);
        }

        // bird
        double birdX = 80 + (t * 25) % (W + 60) - 30;
        double flap = Math.sin(t * 8) * 2;
        gc.setStroke(Color.web("#5a3020"));
        gc.setLineWidth(1.5);
        gc.strokeLine(birdX, 22 + flap, birdX + 4, 19 + flap);
        gc.strokeLine(birdX + 4, 19 + flap, birdX + 8, 22 + flap);
    }

    private static void drawDuskSky(GraphicsContext gc, double W, double H, double t, Star[] stars) {
        // gradient: purple top, orange horizon
        gc.setFill(SKY_DUSK);
        gc.fillRect(0, 0, W, H * 0.35);
        gc.setFill(SKY_DUSK2);
        gc.fillRect(0, H * 0.35, W, H * 0.30);

        // setting sun (half visible at horizon)
        double sunX = W - 70, sunY = H * 0.45;
        gc.setFill(Color.color(1.0, 0.7, 0.4, 0.3));
        gc.fillOval(sunX - 14, sunY - 14, 50, 50);
        gc.setFill(Color.web("#f0a060"));
        gc.fillOval(sunX, sunY, 24, 24);

        // first stars appearing
        if (stars != null) {
            for (Star s : stars) {
                if (s.y > H * 0.3) continue;
                double tw = 0.3 + 0.4 * Math.sin(t * s.speed + s.phase);
                gc.setFill(Color.color(1.0, 1.0, 1.0, Math.max(0.1, tw)));
                gc.fillRect(s.x, s.y, 1, 1);
            }
        }
    }

    private static void drawNightSky(GraphicsContext gc, double W, double H, double t, Star[] stars) {
        gc.setFill(SKY_NIGHT);
        gc.fillRect(0, 0, W, H);

        if (stars != null) {
            for (Star s : stars) {
                if (s.y > H * 0.65) continue;
                double tw = 0.6 + 0.4 * Math.sin(t * s.speed + s.phase);
                gc.setFill(Color.color(1.0, 1.0, 1.0, Math.max(0.1, tw)));
                if (s.size >= 2) {
                    gc.fillRect(s.x - 1, s.y, 3, 1);
                    gc.fillRect(s.x, s.y - 1, 1, 3);
                } else {
                    gc.fillRect(s.x, s.y, 1, 1);
                }
            }
        }

        // shooting star every ~8s
        double shootCycle = t % 8.0;
        if (shootCycle < 0.8) {
            double progress = shootCycle / 0.8;
            double sx = 60 + progress * 220;
            double sy = 8 + progress * 18;
            gc.setStroke(Color.color(1, 1, 1, 1 - progress));
            gc.setLineWidth(1.2);
            gc.strokeLine(sx, sy, sx - 14, sy - 6);
        }

        // moon
        double moonX = W - 60, moonY = 6;
        gc.setFill(Color.color(0.78, 0.78, 0.91, 0.15));
        gc.fillOval(moonX - 6, moonY - 6, 34, 34);
        gc.setFill(MOON_COLOR);
        gc.fillOval(moonX, moonY, 22, 22);
        gc.setFill(SKY_NIGHT);
        gc.fillOval(moonX + 6, moonY - 2, 22, 22);
    }

    private static void drawCloud(GraphicsContext gc, double x, double y, double opacity) {
        gc.setFill(Color.color(
                CLOUD_COLOR.getRed(),
                CLOUD_COLOR.getGreen(),
                CLOUD_COLOR.getBlue(),
                opacity * 0.55
        ));
        gc.fillRoundRect(x,      y + 4, 44, 8, 4, 4);
        gc.fillRoundRect(x + 8,  y,     28, 10, 4, 4);
        gc.fillRoundRect(x + 14, y - 4, 18, 8, 4, 4);
    }

    // ── Rider (proper pixel art) ──

    private static final Color WHEEL_RIM    = Color.web("#2a1a08");
    private static final Color WHEEL_SPOKE  = Color.web("#e8b850");
    private static final Color FRAME        = Color.web("#d83828");
    private static final Color HANDLEBAR    = Color.web("#888888");
    private static final Color SEAT         = Color.web("#1a1208");
    private static final Color SHIRT        = Color.web("#e84858");
    private static final Color SHIRT_SHADE  = Color.web("#a02830");
    private static final Color SKIN         = Color.web("#f0b890");
    private static final Color SKIN_SHADE   = Color.web("#b88068");
    private static final Color HELMET       = Color.web("#e84858");
    private static final Color HELMET_DARK  = Color.web("#a02830");
    private static final Color HELMET_STRIPE = Color.web("#f0c020");
    private static final Color BACKPACK     = Color.web("#e8b820");
    private static final Color BACKPACK_DARK = Color.web("#a07818");
    private static final Color PANTS        = Color.web("#3050b8");

    /**
     * Draw a pixel-art cyclist. Only animation: slow subtle up/down bob.
     */
    public static void drawRider(GraphicsContext gc, double x, double y, boolean moving, double t) {
        // slow subtle bob — period ~2.5s, amplitude 1.5px
        double by = y + (moving ? Math.sin(t * 2.5) * 1.5 : 0);

        // ── shadow ──
        gc.setFill(Color.color(0, 0, 0, 0.18));
        gc.fillOval(x + 2, y + 26, 28, 4);

        // ── wheels (static, no rotation) ──
        drawWheel(gc, x + 4,  by + 18);
        drawWheel(gc, x + 20, by + 18);

        // ── frame ──
        gc.setStroke(FRAME);
        gc.setLineWidth(2);
        gc.strokeLine(x + 10, by + 24, x + 14, by + 10); // seat tube
        gc.strokeLine(x + 14, by + 10, x + 26, by + 24); // down tube
        gc.strokeLine(x + 10, by + 24, x + 26, by + 24); // chain stay

        // handlebar
        gc.setStroke(HANDLEBAR);
        gc.setLineWidth(2);
        gc.strokeLine(x + 26, by + 24, x + 22, by + 12);
        gc.strokeLine(x + 22, by + 12, x + 26, by + 9);

        // ── seat ──
        gc.setFill(SEAT);
        gc.fillRect(x + 12, by + 9, 6, 2);

        // ── legs (static) ──
        gc.setStroke(PANTS);
        gc.setLineWidth(2.5);
        gc.strokeLine(x + 15, by + 14, x + 14, by + 22);
        gc.strokeLine(x + 15, by + 14, x + 18, by + 22);

        // ── body ──
        gc.setFill(SHIRT);
        gc.fillRect(x + 12, by + 4, 8, 11);
        gc.setFill(SHIRT_SHADE);
        gc.fillRect(x + 18, by + 5, 2, 9);

        // ── arms ──
        gc.setFill(SHIRT);
        gc.fillRect(x + 18, by + 6, 5, 2);
        gc.setFill(SKIN);
        gc.fillRect(x + 22, by + 6, 2, 2);

        // ── backpack ──
        gc.setFill(BACKPACK);
        gc.fillRect(x + 8, by + 5, 5, 9);
        gc.setFill(BACKPACK_DARK);
        gc.fillRect(x + 12, by + 5, 1, 9);

        // ── head ──
        gc.setFill(SKIN);
        gc.fillRect(x + 13, by, 7, 6);
        gc.setFill(SKIN_SHADE);
        gc.fillRect(x + 18, by + 1, 2, 4);
        gc.setFill(Color.web("#1a0808"));
        gc.fillRect(x + 17, by + 2, 1, 1);

        // ── helmet ──
        gc.setFill(HELMET);
        gc.fillRect(x + 12, by - 2, 9, 3);
        gc.fillRect(x + 13, by - 3, 7, 1);
        gc.setFill(HELMET_STRIPE);
        gc.fillRect(x + 13, by - 1, 7, 1);
        gc.setFill(HELMET_DARK);
        gc.fillRect(x + 19, by - 2, 2, 3);
        gc.setFill(Color.web("#1a1208"));
        gc.fillRect(x + 13, by + 1, 4, 1);
    }

    private static void drawWheel(GraphicsContext gc, double topLeftX, double topLeftY) {
        double cx = topLeftX + 6;
        double cy = topLeftY + 6;

        // rim
        gc.setStroke(WHEEL_RIM);
        gc.setLineWidth(2.5);
        gc.strokeOval(topLeftX, topLeftY, 12, 12);

        // fixed cross spokes
        gc.setStroke(WHEEL_SPOKE);
        gc.setLineWidth(1);
        gc.strokeLine(cx - 5, cy, cx + 5, cy);
        gc.strokeLine(cx, cy - 5, cx, cy + 5);

        // hub
        gc.setFill(WHEEL_RIM);
        gc.fillOval(cx - 1.5, cy - 1.5, 3, 3);
    }

    private PixelArt() {}
}