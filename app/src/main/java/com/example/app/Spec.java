package com.example.app;

import java.util.List;

public class Spec {
    public String appName;
    public String appDescription;
    public List<Screen> screens;

    public static class Screen {
        public String name;
        public String description;
        public List<Component> components;
        public List<Action> actions;
    }

    public static class Component {
        public String type;
        public String description;
        public List<Field> fields;
    }

    public static class Field {
        public String name;
        public String type;
    }

    public static class Action {
        public String type;
        public String description;
    }
}
