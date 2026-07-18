package net.somesoap.rtngodot;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.events.Event;

public class SurfaceReadyEvent extends Event<SurfaceReadyEvent> {
	public static final String EVENT_NAME = "topSurfaceReady";

	public SurfaceReadyEvent(int surfaceId, int viewId) {
		super(surfaceId, viewId);
	}

	@Override
	public String getEventName() {
		return EVENT_NAME;
	}

	@Override
	public boolean canCoalesce() {
		return false;
	}

	@Override
	protected WritableMap getEventData() {
		WritableMap event = Arguments.createMap();
		event.putBoolean("ready", true);
		return event;
	}
}
