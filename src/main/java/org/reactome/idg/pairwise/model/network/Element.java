package org.reactome.idg.pairwise.model.network;

public class Element {
	public enum Group {
		NODES("nodes"),
		EDGES("edges");
		
		public final String label;
		private Group(String label) {
			this.label = label;
		}
		@Override
		public String toString() {
			return this.label;
		}
	}
	
	private Group group;
	private Data data;
	
	public Element(Group group, Data data) {
		this.group = group;
		this.data = data;
	}

	public Group getGroup() {
		return group;
	}

	public void setGroup(Group group) {
		this.group = group;
	}

	public Data getData() {
		return data;
	}

	public void setData(Data data) {
		this.data = data;
	}
}
