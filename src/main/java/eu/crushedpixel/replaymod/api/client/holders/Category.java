package eu.crushedpixel.replaymod.api.client.holders;

public enum Category {

	SURVIVAL(0), MINIGAME(1), BUILD(2);
	
	private int id;
	
	Category(int id) {
		this.id = id;
	}
	
	public int getId() {
		return this.id;
	}
	
	public Category fromId(int id) {
		for(Category c : values()) {
			if(c.id == id) return c;
		}
		return null;
	}
}
