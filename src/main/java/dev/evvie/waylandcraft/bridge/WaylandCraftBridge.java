package dev.evvie.waylandcraft.bridge;

import java.util.ArrayList;

import org.apache.commons.lang3.ArrayUtils;

public class WaylandCraftBridge {
	
	private long instance;
	private ArrayList<WLCToplevel> toplevels = new ArrayList<WLCToplevel>();
	
	static {
		System.loadLibrary("waylandcraft");
	}
	
	private WaylandCraftBridge(long handle) {
		this.instance = handle;
	}
	
	public static WaylandCraftBridge start() {
		long handle = init();
		return new WaylandCraftBridge(handle);
	}
	
	private WLCToplevel getOrCreate(long handle) {
		for(WLCToplevel toplevel : toplevels) {
			if(toplevel.getHandle() == handle) return toplevel;
		}
		WLCToplevel toplevel = new WLCToplevel(handle);
		WLCSurface surface = toplevelSurface(this.instance, handle);
		toplevel.setSurface(surface);
		
		toplevels.add(toplevel);
		return toplevel;
	}
	
	private void deleteNonExisting(long[] remainingHandles) {
		ArrayList<WLCToplevel> toplevels_new = new ArrayList<WLCToplevel>();
		for(WLCToplevel toplevel : this.toplevels) {
			if(ArrayUtils.contains(remainingHandles, toplevel.getHandle())) {
				toplevels_new.add(toplevel);
			}
			else {
				toplevel.takeHandle();
			}
		}
		this.toplevels = toplevels_new;
	}
	
	public void update() {
		update(this.instance);
		
		long[] toplevel_handles = toplevels(instance);
		deleteNonExisting(toplevel_handles);
		
		for(long handle : toplevel_handles) {
			WLCToplevel toplevel = getOrCreate(handle);
			WLCSurface root = toplevel.getSurfaceTree();
			updateSurface(root);
		}
	}
	
	public WLCToplevel[] getToplevels() {
		return toplevels.toArray(new WLCToplevel[toplevels.size()]);
	}
	
	public String getSocket() {
		return socket(this.instance);
	}
	
	private static native long init();
	private static native void update(long instance);
	private static native String socket(long instance);
	
	private static native long[] toplevels(long instance);
	private static native WLCSurface toplevelSurface(long instance, long handle);
	private static native void updateSurface(WLCSurface surface);
	
}
