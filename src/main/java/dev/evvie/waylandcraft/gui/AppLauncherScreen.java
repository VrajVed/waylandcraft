package dev.evvie.waylandcraft.gui;

import java.util.ArrayList;
import java.util.List;

import dev.evvie.waylandcraft.WaylandCraft;
import dev.evvie.waylandcraft.desktop.DesktopEntry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class AppLauncherScreen extends Screen {
	
	private WaylandCraft wlc;
	private AppListWidget list;
	private EditBox searchBox;
	
	public AppLauncherScreen(WaylandCraft wlc) {
		super(Component.literal("App Launcher"));
		
		this.wlc = wlc;
	}
	
	@Override
	protected void init() {
		int listWidth = AppListWidget.ELEMENT_WIDTH;
		int listHeight = 170;
		list = new AppListWidget(this::launch, Component.literal("App List"));
		list.setRectangle(listWidth, listHeight, width / 2 - listWidth / 2, height / 2 - listHeight / 2);
		this.addRenderableWidget(this.list);
		
		// Search box is not added to widgets for custom focus / key enter rules
		searchBox = new EditBox(font, width / 2 - listWidth / 2, list.getY() - 25, listWidth, 20, Component.literal("Search"));
		searchBox.setResponder(this::filterSetEntries);
		searchBox.setFocused(true); // Eternally focused
		
		List<DesktopEntry> entries = wlc.xdgManager.entries().stream().filter((e) -> e.visible).toList();
		list.setEntries(entries);
	}
	
	private int similarityScore(String hay, String needle) {
		if(hay == null || needle == null) return 0;
		hay = hay.toLowerCase();
		needle = needle.toLowerCase();
		
		if(hay.equals(needle)) return 3;
		if(hay.startsWith(needle)) return 2;
		if(hay.contains(needle)) return 1;
		return 0;
	}
	
	private int sumSimilarityScore(String[] hays, String needle) {
		int score = 0;
		for(String hay : hays) {
			score += similarityScore(hay, needle);
		}
		return score;
	}
	
	private int entryMatchesStrScore(DesktopEntry entry, String str) {
		if(!entry.visible) return -1;
		int score = 0;
		score += 3 * similarityScore(entry.name, str);
		score += sumSimilarityScore(entry.keywords, str);
		score += similarityScore(entry.comment, str);
		score += similarityScore(entry.genericName, str);
		score += similarityScore(entry.exec, str);
		return score;
	}
	
	private void filterSetEntries(String filter) {
		List<DesktopEntry> entries = wlc.xdgManager.entries().stream()
				.map((entry) -> new RankedDesktopEntry(entry, entryMatchesStrScore(entry, filter)))
				.filter((r) -> r.score > 0)
				.sorted((r1, r2) -> r2.score - r1.score)
				.map((r) -> r.entry)
				.toList();
		list.setEntries(entries);
	}
	
	@Override
	public boolean keyPressed(int key, int scancode, int modifiers) {
		if(searchBox.keyPressed(key, scancode, modifiers)) return true;
		return super.keyPressed(key, scancode, modifiers);
	}
	
	@Override
	public boolean charTyped(char c, int i) {
		if(searchBox.charTyped(c, i)) return true;
		return super.charTyped(c, i);
	}
	
	@Override
	public boolean isPauseScreen() {
		return false;
	}
	
	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
		searchBox.render(guiGraphics, mouseX, mouseY, partialTicks);
	}
	
	public void launch(DesktopEntry entry) {
		wlc.bridge.execApp(entry.appId);
		this.onClose();
	}
	
	private static record RankedDesktopEntry(DesktopEntry entry, int score) {}
	
}
