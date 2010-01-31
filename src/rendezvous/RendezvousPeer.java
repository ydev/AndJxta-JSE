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
	private File cacheHome = new File("cache");
	private String instanceName;
	private PeerGroup netPeerGroup;
	private RendezVousService netPeerGroupRendezvous;

	public void start(String name, File cache) {
		instanceName = name;
		cacheHome = cache;
		
		configureJXTA();
		startJXTA();
		waitForQuit();

	}

	private void configureJXTA() {
		System.out.println("Configuring platform");
		System.out.println("RDV_HOME = " + new File(cacheHome, instanceName));

		configurator = new NetworkConfigurator();
		configurator.setHome(new File(cacheHome, instanceName));
		configurator.setName("Rendezvous Peer");
		configurator.setPrincipal("instanceName");
		configurator.setPassword("");
		configurator.setDescription("");
		configurator.setUseMulticast(true);

		URI seedingURI;
		seedingURI = new File("seeds.txt").toURI();
		configurator.addRdvSeedingURI(seedingURI);
		configurator.setMode(NetworkConfigurator.RDV_SERVER);//	+ NetworkConfigurator.RELAY_SERVER);

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
					.getPlatformConfig(), new File(cacheHome, instanceName).toURI());
		} catch (PeerGroupException e) {
			System.out.println("PeerGroupException " + e.getMessage());
			System.exit(1);
			return;
		}

		netPeerGroup = factory.getInterface();

		// The rendezvous service for NetPeerGroup
		netPeerGroupRendezvous = netPeerGroup.getRendezVousService();
		netPeerGroupRendezvous.addListener(this);
		netPeerGroupRendezvous.startRendezVous();

		System.out.println("Platform started");
	}

	synchronized public void waitForQuit() {
		try {
			wait();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void rendezvousEvent(RendezvousEvent event) {
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
