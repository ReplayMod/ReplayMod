package eu.crushedpixel.replaymod.api.client.holders;

public class ApiError {

	public ApiError(int id, String desc) {
		this.id = id;
		this.desc = desc;
	}
	
	private int id;
	private String desc;
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getDesc() {
		return desc;
	}
	public void setDesc(String desc) {
		this.desc = desc;
	}
	
	
}
