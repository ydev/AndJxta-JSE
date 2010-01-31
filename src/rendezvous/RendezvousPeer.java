package rendezvous;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import net.jxta.discovery.DiscoveryService;
import net.jxta.exception.PeerGroupException;
import net.jxta.peergroup.NetPeerGroupFactory;
import net.jxta.peergroup.PeerGroup;
import net.jxta.platform.NetworkConfigurator;
import net.jxta.protocol.ConfigParams;
import net.jxta.rendezvous.RendezVousService;
import net.jxta.rendezvous.RendezvousEvent;
import net.jxta.rendezvous.RendezvousListener;

public class RendezvousPeer implements RendezvousListener {
	private NetworkConfigurator configurator;
	// private String jxtaHome;
	private final static File jxtaHome = new File(System.getProperty(
			"JXTA_HOME_DEACT", "cache"));
	private String instanceName;
	private String peerID;
	private PeerGroup netPeerGroup;
	private RendezVousService netPeerGroupRendezvous;
	private DiscoveryService netPeerGroupDiscovery;

	public void start(String name) {
		instanceName = name;
		
		configureJXTA();
		startJXTA();
		// createPeerGroup();
		waitForQuit();

	}

	private void configureJXTA() {
		System.out.println("Configuring platform");
		System.out.println("RDV_HOME = " + jxtaHome);

		configurator = new NetworkConfigurator();
		configurator.setHome(new File(jxtaHome, instanceName));
		//configurator.setPeerId(peerID);
		configurator.setName("My Peer Name");
		configurator.setPrincipal("ofno");
		configurator.setPassword("consequence");
		configurator.setDescription("Private Rendezvous");
		configurator.setUseMulticast(false);

		URI seedingURI;
		seedingURI = new File("seeds.txt").toURI();
		configurator.addRdvSeedingURI(seedingURI);
		//configurator.addRelaySeedingURI(seedingURI);
		configurator.setMode(NetworkConfigurator.RDV_SERVER);//	+ NetworkConfigurator.RELAY_SERVER);

		//configurator.setUseOnlyRelaySeeds(true);
		configurator.setUseOnlyRendezvousSeeds(true);

		configurator.setTcpEnabled(true);
		configurator.setTcpIncoming(true);
		configurator.setTcpOutgoing(true);

		try {
			configurator.save();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

		System.out.println("Platform configured and saved");
	}

	private void startJXTA() {
		System.out.println("Starting JXTA platform");

		NetPeerGroupFactory factory;
		try {
			factory = new NetPeerGroupFactory((ConfigParams) configurator
					.getPlatformConfig(), new File(jxtaHome, instanceName).toURI());
			/*
			 * , IDFactory.fromURI(new URI(NetPeerGroupID)), NetPeerGroupName,
			 * (XMLElement)
			 * StructuredDocumentFactory.newStructuredDocument(MimeMediaType
			 * .XMLUTF8, "desc", NetPeerGroupName)
			 */
		} catch (PeerGroupException e) {
			// throw new PeerGroupException(e.getMessage());
			System.out.println("PeerGroupException " + e.getMessage());
			System.exit(1);
			return;
		}

		netPeerGroup = factory.getInterface();

		// The rendezvous service for NetPeerGroup
		netPeerGroupRendezvous = netPeerGroup.getRendezVousService();
		netPeerGroupRendezvous.addListener(this);
		netPeerGroupRendezvous.startRendezVous();

		// The NetPeerGroup discovery service
		netPeerGroupDiscovery = netPeerGroup.getDiscoveryService();

		System.out.println("Platform started");
	}

	/*
	 * private void createPeerGroup() throws Exception, PeerGroupException {
	 * 
	 * // The new-application subgroup parameters. String name = "My App Group";
	 * String desc = "My App Group Description"; String gid =
	 * "urn:jxta:uuid-79B6A084D3264DF8B641867D926C48D902"; String specID =
	 * "urn:jxta:uuid-309B33F10EDF48738183E3777A7C3DE9C5BFE5794E974DD99AC7D409F5686F3306"
	 * ;
	 * 
	 * StringBuilder sb = new StringBuilder("=Creating group:  ");
	 * sb.append(name).append(", "); sb.append(desc).append(", ");
	 * sb.append(gid).append(", "); sb.append(specID);
	 * System.out.println(sb.toString());
	 * 
	 * ModuleImplAdvertisement implAdv = netPeerGroup
	 * .getAllPurposePeerGroupImplAdvertisement(); ModuleSpecID modSpecID =
	 * (ModuleSpecID) IDFactory.fromURI(new URI( specID));
	 * implAdv.setModuleSpecID(modSpecID);
	 * 
	 * // Publish the Peer Group implementation advertisement.
	 * discovery.publish(implAdv); discovery.remotePublish(null, implAdv);
	 * 
	 * // Create the new group using the group ID, advertisement, name, and //
	 * description PeerGroupID groupID = (PeerGroupID) IDFactory.fromURI(new
	 * URI(gid)); newGroup = netPeerGroup.newGroup(groupID, implAdv, name,
	 * desc);
	 * 
	 * // Start the rendezvous for our applcation subgroup. apppgRendezvous =
	 * newGroup.getRendezVousService(); apppgRendezvous.addListener(this);
	 * apppgRendezvous.startRendezVous();
	 * 
	 * // Publish the group remotely. newGroup() handles the local publishing.
	 * PeerGroupAdvertisement groupAdv = newGroup.getPeerGroupAdvertisement();
	 * discovery.remotePublish(null, groupAdv);
	 * 
	 * System.out.println("Private Application newGroup = " + name +
	 * " created and published"); }
	 */

	synchronized public void waitForQuit() {
		try {
			wait();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void rendezvousEvent(RendezvousEvent event) {
		// TODO Auto-generated method stub
		String eventDescription;
		int eventType;

		eventType = event.getType();

		switch (eventType) {
		case RendezvousEvent.RDVCONNECT:
			eventDescription = "RDVCONNECT";
			break;
		case RendezvousEvent.RDVRECONNECT:
			eventDescription = "RDVRECONNECT";
			break;
		case RendezvousEvent.RDVDISCONNECT:
			eventDescription = "RDVDISCONNECT";
			break;
		case RendezvousEvent.RDVFAILED:
			eventDescription = "RDVFAILED";
			break;
		case RendezvousEvent.CLIENTCONNECT:
			eventDescription = "CLIENTCONNECT";
			break;
		case RendezvousEvent.CLIENTRECONNECT:
			eventDescription = "CLIENTRECONNECT";
			break;
		case RendezvousEvent.CLIENTDISCONNECT:
			eventDescription = "CLIENTDISCONNECT";
			break;
		case RendezvousEvent.CLIENTFAILED:
			eventDescription = "CLIENTFAILED";
			break;
		case RendezvousEvent.BECAMERDV:
			eventDescription = "BECAMERDV";
			break;
		case RendezvousEvent.BECAMEEDGE:
			eventDescription = "BECAMEEDGE";
			break;
		default:
			eventDescription = "UNKNOWN RENDEZVOUS EVENT";
		}

		System.out.println("RendezvousEvent:  event =  " + eventDescription
				+ " from peer = " + event.getPeer());
	}

}
