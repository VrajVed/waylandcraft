package dev.evvie.waylandcraft.grabs;

import java.util.ArrayList;

import org.jetbrains.annotations.Nullable;

import dev.evvie.waylandcraft.WaylandCraft;
import dev.evvie.waylandcraft.WindowDisplay;
import dev.evvie.waylandcraft.WindowDisplay.DisplayHitResult;
import dev.evvie.waylandcraft.bridge.WLCAbstractWindow;
import dev.evvie.waylandcraft.bridge.WLCSurface;
import net.minecraft.world.phys.Vec3;

public class PointerGrabMap {
	
	private WaylandCraft wlc;
	private PointerGrab exclusiveGrab = null;
	private ImplicitGrabs implicitGrabs = null;
	
	public PointerGrabMap(WaylandCraft wlc) {
		this.wlc = wlc;
	}
	
	public boolean isGrabActive() {
		return implicitGrabs != null || exclusiveGrab != null;
	}
	
	public boolean isExclusiveGrabActive() {
		return exclusiveGrab != null;
	}
	
	public boolean isGrabActive(int button) {
		return (exclusiveGrab != null && exclusiveGrab.button == button) || (implicitGrabs != null && implicitGrabs.contains(button));
	}
	
	// Start implicit pointer grab on a surface. Surface MUST have active pointer focus!
	public void startImplicit(WindowDisplay window, WLCSurface surface, int button) {
		if(isExclusiveGrabActive()) return;
		
		if(implicitGrabs == null) implicitGrabs = new ImplicitGrabs(window, surface);
		if(implicitGrabs.contains(button)) return;
		
		int serial = wlc.bridge.sendButton(0x110 + button, 1);
		implicitGrabs.add(window, button, serial);
	}
	
	public void startExclusive(PointerGrab grab) {
		if(isExclusiveGrabActive()) return;
		
		this.releaseImplicit();
		
		try {
			grab.init();
		} catch (GrabDroppedException e) {
			return;
		}
		
		exclusiveGrab = grab;
	}
	
	public void moveWorld(Vec3 pos, Vec3 view, Vec3 up) {
		if(exclusiveGrab != null) {
			try {
				exclusiveGrab.moveWorld(pos, view, up);
			} catch(GrabDroppedException e) {
				exclusiveGrab = null;
			}
			
			return;
		}
		
		if(implicitGrabs == null) return;
		
		DisplayHitResult hitResult = implicitGrabs.window.intersect(pos, view);
		if(hitResult == null) return;
		
		Vec3 relativeCoords = hitResult.surfaceLocalOrigin.subtract(implicitGrabs.surface.xSubpos, implicitGrabs.surface.ySubpos, 0);
		wlc.bridge.sendMotion(relativeCoords.x, relativeCoords.y);
	}
	
	public void hover(WLCAbstractWindow window, WLCSurface surface, double x, double y) {
		if(exclusiveGrab != null) {
			try {
				exclusiveGrab.hover(window, surface, x, y);
			} catch(GrabDroppedException e) {
				exclusiveGrab = null;
			}
		}
	}
	
	public void release(int button) {
		if(exclusiveGrab != null && exclusiveGrab.button == button) {
			try {
				exclusiveGrab.release();
			} catch (GrabDroppedException e) {
				// No handling necessary, grab always removed
			}
			exclusiveGrab = null;
			return;
		}
		
		if(implicitGrabs == null) return;
		
		if(implicitGrabs.contains(button)) {
			wlc.bridge.sendButton(0x110 + button, 0);
			implicitGrabs.remove(button);
		}
		
		if(implicitGrabs.isEmpty()) implicitGrabs = null;
	}
	
	private void releaseImplicit() {
		if(implicitGrabs == null) return;
		
		for(ButtonPress press : implicitGrabs.getPressed()) {
			wlc.bridge.sendButton(0x110 + press.button, 0);
		}
		implicitGrabs = null;
	}
	
	public void releaseAll() {
		this.releaseImplicit();
		
		if(exclusiveGrab == null) return;
		
		try {
			exclusiveGrab.release();
		} catch (GrabDroppedException e) {
			// No handling necessary, grab always removed
		}
		exclusiveGrab = null;
	}
	
	public @Nullable ButtonPress releaseImplicitMatching(int serial) {
		if(isExclusiveGrabActive()) return null;
		if(implicitGrabs == null) return null;
		
		for(ButtonPress press : implicitGrabs.getPressed()) {
			if(press.serial == serial) {
				release(press.button);
				return press;
			}
		}
		return null;
	}
	
	// Not a real pointer grab, just a way to represent active button presses on a WindowDisplay
	private static class ImplicitGrabs {
		
		public final WindowDisplay window;
		public final WLCSurface surface;
		private ArrayList<ButtonPress> buttonsPressed = new ArrayList<ButtonPress>();
		
		public ImplicitGrabs(WindowDisplay window, WLCSurface surface) {
			this.window = window;
			this.surface = surface;
		}
		
		public boolean contains(int button) {
			return buttonsPressed.stream().anyMatch((press) -> press.button == button);
		}
		
		public boolean isEmpty() {
			return buttonsPressed.isEmpty();
		}
		
		public void add(WindowDisplay display, int button, int serial) {
			assert !contains(button);
			buttonsPressed.add(new ButtonPress(display, button, serial));
		}
		
		public void remove(int button) {
			assert contains(button);
			buttonsPressed.removeIf((press) -> press.button == button);
		}
		
		public ButtonPress[] getPressed() {
			return buttonsPressed.toArray(ButtonPress[]::new);
		}
		
	}
	
	public static record ButtonPress(WindowDisplay window, int button, int serial) {}
	
}
