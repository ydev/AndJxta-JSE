package discovery;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;

import sun.rmi.runtime.Log;

import net.jxta.discovery.DiscoveryEvent;
import net.jxta.discovery.DiscoveryListener;
import net.jxta.discovery.DiscoveryService;
import net.jxta.document.Advertisement;
import net.jxta.peergroup.PeerGroup;
import net.jxta.platform.NetworkManager;
import net.jxta.protocol.DiscoveryResponseMsg;
import net.jxta.protocol.PeerAdvertisement;
import net.jxta.protocol.PipeAdvertisement;
import edge.Peer;

/**
 * Illustrates the use of Discovery Service
 */
public class DiscoveryClient implements Runnable, DiscoveryListener {
	private transient NetworkManager manager;
	private transient DiscoveryService discovery;
	private ArrayList<Peer> peerList = new ArrayList<Peer>();

	/**
	 * Constructor for the DiscoveryClient
	 */
	public DiscoveryClient(NetworkManager manager) {
		this.manager = manager;

		// Get the NetPeerGroup
		PeerGroup netPeerGroup = manager.getNetPeerGroup();
		// get the discovery service
		discovery = netPeerGroup.getDiscoveryService();
	}

	/**
	 * loop forever attempting to discover advertisements every minute
	 */
	public void run() {
		long waittime = 30 * 1000L;
		try {
			// Add ourselves as a DiscoveryListener for DiscoveryResponse events
			discovery.addDiscoveryListener(this);

			while (true) {
				System.out.println("Sending a Discovery Message");
				// look for any peer
				discovery.getRemoteAdvertisements(
				// no specific peer (propagate)
						null,
						// Adv type
						DiscoveryService.ADV,
						// Attribute = name
						null, // "Name",
						// Value = the tutorial
						null, // "Discovery tutorial",
						// one advertisement response is all we are looking for
						50,
						// no query specific listener. we are using a global
						// listener
						null);
				
				discovery.getLocalAdvertisements(DiscoveryService.ADV, null, null);
			
				// wait a bit before sending a discovery message
				try {
					System.out.println("Sleeping for :" + waittime);
					Thread.sleep(waittime);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * This method is called whenever a discovery response is received, which
	 * are either in response to a query we sent, or a remote publish by another
	 * node
	 * 
	 * @param ev
	 *            the discovery event
	 */
	public void discoveryEvent(DiscoveryEvent ev) {
		DiscoveryResponseMsg res = ev.getResponse();
		String name = "unknown";

		// Get the responding peer's advertisement
		PeerAdvertisement peerAdv = res.getPeerAdvertisement();

		// some peers may not respond with their peerAdv name
		if (peerAdv != null)
			peerAdv.getName();
		name = ev.getSource().toString();

		System.out
				.println("###############################################################################################");
		System.out.println("Got a Discovery Response ["
				+ res.getResponseCount() + " elements] from peer: " + name);
		
		Advertisement adv = null;
		Enumeration en = res.getAdvertisements();

		if (en != null) {
			while (en.hasMoreElements()) {
				adv = (Advertisement) en.nextElement();

				//System.out.println("   Type: " + adv.getClass().toString()); // + adv.toString());
				
				if (adv instanceof PipeAdvertisement) {
					PipeAdvertisement pipeAdv = (PipeAdvertisement) adv;

					try {
						// change the list only when the new peer is not the
						// current peer itself
						if (!pipeAdv.getName().equals(
								manager.getConfigurator().getPrincipal())) {
							Peer newPeer = new Peer(pipeAdv.getName(), pipeAdv,
									System.currentTimeMillis());
							
							addPeerListItem(newPeer);
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}

		for (int i = 0; i < peerList.size(); i++)
			System.out.println("Found Peer Advertisement, List now ----> "
					+ "PeerADV Name:" + peerList.get(i).getName()
					+ " ----> SocketADV ID: "
					+ peerList.get(i).getPipeAdvertisement().getID());

		System.out
				.println("###############################################################################################");

	}

	private synchronized void addPeerListItem(Peer peer) {
		if (peerList.contains(peer)) {
			peerList.remove(peer);
		}
		peerList.add(peer);
	}
	
	public synchronized ArrayList<Peer> getPeerList() {
		return peerList;
	}
}