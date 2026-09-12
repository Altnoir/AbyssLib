package com.altnoir.abysslib.reginth.util;

import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

@SuppressWarnings("null")
public class DebugMarkers {
    
    private static final String PREFIX = "Reginth.";
    
    private static Marker marker(String name) {
        return MarkerManager.getMarker(PREFIX + name);
    }
    
    public static final Marker REGISTER = marker("REGISTER");
    public static final Marker DATA = marker("DATA");
}