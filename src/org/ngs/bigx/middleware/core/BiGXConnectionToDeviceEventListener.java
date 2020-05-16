package org.ngs.bigx.middleware.core;

import java.awt.Event;
import java.util.EventListener;

public interface BiGXConnectionToDeviceEventListener extends EventListener {
    public void onMessageReceiveFromDevice(Event event, String message);
}