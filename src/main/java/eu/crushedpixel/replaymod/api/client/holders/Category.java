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
	
	public String toNiceString() {
		return (""+this).charAt(0)+(""+this).substring(1).toLowerCase();
	}
	
	public Category next() {
		for(int i=0; i<values().length; i++) {
			if(values()[i] == this) {
				if(i == values().length-1) {
					i=-1;
				}
				return values()[i+1];
			}
		}
		return this;
	}
}
