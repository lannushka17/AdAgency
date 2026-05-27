package com.sab.adagency;

/**
 * Точка входу для Fat JAR ({@code java -jar}). Не успадковує Application —
 * інакше JavaFX не завантажиться коректно через classpath.
 */
public final class Launcher {
    public static void main(String[] args) {
        Main.main(args);
    }

    private Launcher() {}
}
