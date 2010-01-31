package discovery;

import java.io.IOException;
import java.util.Date;

import net.jxta.discovery.DiscoveryService;
import net.jxta.document.AdvertisementFactory;
import net.jxta.endpoint.Message;
import net.jxta.id.IDFactory;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.pipe.PipeMsgEvent;
import net.jxta.pipe.PipeMsgListener;
import net.jxta.pipe.PipeService;
import net.jxta.platform.NetworkManager;
import net.jxta.protocol.PipeAdvertisement;

/**
 * Illustrates the use of Discovery Service Note this is for illustration
 * purposes and is not meant as a blue-print
 */
public class DiscoveryServer implements Runnable, PipeMsgListener {
	private transient NetworkManager manager;
	private transient DiscoveryService discovery;

	private PipeAdvertisement pipeAdv;

	/**
	 * Constructor for the DiscoveryServer
	 */
	public DiscoveryServer(NetworkManager manager) {
		this.manager = manager;

		PeerGroup netPeerGroup = manager.getNetPeerGroup();
		// get the discovery service
		discovery = netPeerGroup.getDiscoveryService();
	}

	/**
	 * create a new pipe adv, publish it for 2 minutes network time, sleep for 3
	 * minutes, then repeat
	 */
	public void run() {
		final long lifetime = 2 * 60 * 1000L;
		final long expiration = 2 * 60 * 1000L;
		final long waittime = 1 * 60 * 1000L;

		try {
			createPipeAdvertisement();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		try {
			manager.getNetPeerGroup().getPipeService().createInputPipe(getPipeAdvertisement(), this);
		} catch (IOException e1) {
			e1.printStackTrace();
		}

				while (true) {
					try {
						PipeAdvertisement pipeAdv = getPipeAdvertisement();
						// publish the advertisement with a lifetime of 2
						// minutes
						System.out
								.println("Publishing the following advertisement with lifetime :"
										+ lifetime
										+ " expiration :"
										+ expiration);
						//System.out.println(pipeAdv.toString());
						discovery.publish(pipeAdv, lifetime, expiration);
						discovery.remotePublish(pipeAdv, expiration);

					} catch (Exception e) {
						System.out
								.println("failed to bind to the JxtaServerPipe due to the following exception");
						e.printStackTrace();
					}

					try {
						System.out.println("Sleeping for: " + waittime);
						Thread.sleep(waittime);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

	}

	/**
	 * Creates a pipe advertisement
	 * 
	 * @throws IOException
	 */
	public void createPipeAdvertisement() throws IOException {
		PipeAdvertisement advertisement = (PipeAdvertisement) AdvertisementFactory
				.newAdvertisement(PipeAdvertisement.getAdvertisementType());
		advertisement.setPipeID(IDFactory
				.newPipeID(PeerGroupID.defaultNetPeerGroupID));
		advertisement.setType(PipeService.UnicastType);
		advertisement.setName(manager.getConfigurator().getPrincipal());

		pipeAdv = advertisement;
	}

	public PipeAdvertisement getPipeAdvertisement() throws IOException {
		return pipeAdv;
	}

	@Override
	public void pipeMsgEvent(PipeMsgEvent event) {
		try {
			Message msg = event.getMessage();
			byte[] msgBytes = msg.getMessageElement("Msg").getBytes(true);
			byte[] fromBytes = msg.getMessageElement("From").getBytes(true);

			String fromPeerID = new String(fromBytes);
			System.out.print("(from ???): ");

			System.out.print(new Date());
			System.out.println(" " + fromPeerID + " says "
					+ new String(msgBytes));
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

	}
}