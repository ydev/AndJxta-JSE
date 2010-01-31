package service;

import net.jxta.protocol.PipeAdvertisement;

public class Peer {
	private String name = null;
	private PipeAdvertisement pipeAdvertisement = null;
	private long lastUpdate = 0;

	public Peer(PipeAdvertisement pipeAdvertisement) {
		super();
		this.name = pipeAdvertisement.getName();
		this.pipeAdvertisement = pipeAdvertisement;
		this.lastUpdate = System.currentTimeMillis();
	}

	public boolean equals(Object obj) {
		Peer objPeer = (Peer) obj;

		if (objPeer.name.equals(this.name))
			return true;

		return false;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public PipeAdvertisement getPipeAdvertisement() {
		return pipeAdvertisement;
	}

	public void setPipeAdvertisement(PipeAdvertisement pipeAdvertisement) {
		this.pipeAdvertisement = pipeAdvertisement;
	}

	public long getLastUpdate() {
		return lastUpdate;
	}

	public void setLastUpdate(long lastUpdate) {
		this.lastUpdate = lastUpdate;
	}

	public String toString() {
		return name + " (" + pipeAdvertisement.getID().toString() + ")";
	}
}
