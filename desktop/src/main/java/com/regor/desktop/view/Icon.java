package com.regor.desktop.view;

import javafx.scene.Group;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;

/** Ícones vetoriais com a mesma grade, espessura e cor herdada do componente. */
public final class Icon extends StackPane {
    private final SVGPath path = new SVGPath();
    private final Group drawing;
    private String name = "heart";
    private double size = 20;

    public Icon() {
        var bounds = new Rectangle(24, 24, Color.TRANSPARENT);
        bounds.setMouseTransparent(true);
        path.getStyleClass().add("icon-stroke");
        drawing = new Group(bounds, path);
        getChildren().add(drawing);
        getStyleClass().add("icon");
        setMouseTransparent(true);
        setName(name);
        setSize(size);
    }

    public Icon(String name, double size) { this(); setName(name); setSize(size); }
    public String getName() { return name; }
    public void setName(String value) {
        name = value;
        path.setContent(switch (value) {
            case "home" -> "M3 10L12 3L21 10V21H15V14H9V21H3Z";
            case "compass" -> "M12 2A10 10 0 1 1 12 22A10 10 0 1 1 12 2 M16 8L14 14L8 16L10 10Z";
            case "user" -> "M16 7A4 4 0 1 1 8 7A4 4 0 1 1 16 7 M4 21V19C4 12 20 12 20 19V21";
            case "chat" -> "M21 11C21 16 17 19 12 19H8L3 22V16C1 13 2 7 5 5C11 0 21 4 21 11Z";
            case "bell" -> "M5 9C5 0 19 0 19 9V15L21 18H3L5 15Z M9 21C10 23 14 23 15 21";
            case "settings" -> "M9 3L8 6L5 6L3 10L5 12L3 15L5 19L8 18L10 21H14L16 18L19 19L21 15L19 12L21 10L19 6H16L15 3Z M16 12A4 4 0 1 1 8 12A4 4 0 1 1 16 12";
            case "logout" -> "M10 3H4V21H10 M9 12H22L18 8 M22 12L18 16";
            case "close" -> "M6 6L18 18 M18 6L6 18";
            case "star" -> "M12 2L15 8L22 9L17 14L18 21L12 18L6 21L7 14L2 9L9 8Z";
            case "camera" -> "M3 6H7L9 3H15L17 6H21V21H3Z M16 13A4 4 0 1 1 8 13A4 4 0 1 1 16 13";
            case "plus" -> "M12 5V19 M5 12H19";
            case "trash" -> "M3 6H21 M9 6V3H15V6 M5 6L6 21H18L19 6 M10 10V17 M14 10V17";
            case "check" -> "M4 12L9 17L20 6";
            case "pin" -> "M19 9C19 15 12 22 12 22S5 15 5 9A7 7 0 0 1 19 9Z M15 9A3 3 0 1 1 9 9A3 3 0 1 1 15 9";
            case "shield" -> "M12 2L21 6V12C21 18 12 22 12 22S3 18 3 12V6Z M8 12L11 15L16 9";
            case "palette" -> "M12 3C0 3 0 21 12 21C18 21 12 16 17 15C24 15 23 3 12 3Z M7 8H7.1 M12 6H12.1 M17 8H17.1 M6 13H6.1";
            case "arrow" -> "M4 12H20 M14 6L20 12L14 18";
            case "refresh" -> "M20 9A8 8 0 0 0 6 5L3 8 M3 3V8H8 M4 15A8 8 0 0 0 18 19L21 16 M21 21V16H16";
            case "search" -> "M18 10A8 8 0 1 1 2 10A8 8 0 1 1 18 10 M16 16L22 22";
            case "heart" -> "M12 21L3 12C-3 4 7-1 12 6C17-1 27 4 21 12Z";
            default -> throw new IllegalArgumentException("Ícone desconhecido: " + value);
        });
    }
    public double getSize() { return size; }
    public void setSize(double value) {
        size = value;
        drawing.setScaleX(value / 24);
        drawing.setScaleY(value / 24);
        setMinSize(value, value);
        setPrefSize(value, value);
        setMaxSize(value, value);
    }
}
