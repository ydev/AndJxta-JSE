package edge;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Random;

import net.jxta.discovery.DiscoveryService;
import net.jxta.document.AdvertisementFactory;
import net.jxta.endpoint.ByteArrayMessageElement;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.id.IDFactory;
import net.jxta.peergroup.NetPeerGroupFactory;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.pipe.InputPipe;
import net.jxta.pipe.OutputPipe;
import net.jxta.pipe.PipeID;
import net.jxta.pipe.PipeMsgEvent;
import net.jxta.pipe.PipeMsgListener;
import net.jxta.pipe.PipeService;
import net.jxta.platform.NetworkConfigurator;
import net.jxta.protocol.ConfigParams;
import net.jxta.protocol.PipeAdvertisement;
import net.jxta.rendezvous.RendezVousService;
import net.jxta.rendezvous.RendezvousEvent;
import net.jxta.rendezvous.RendezvousListener;

public class EdgePeer implements RendezvousListener, PipeMsgListener {
	private NetworkConfigurator configurator;
	private final static File jxtaHome = new File(System.getProperty(
			"JXTA_HOME_DEACT", "cache"));
	private String instanceName;
	private PeerGroup netPeerGroup;
	private DiscoveryService netPeerGroupDiscovery;
	private RendezVousService netPeerGroupRendezvous;
	private String myPeerID;
	private String rdvlock = new String("rdvlock");
	private InputPipe inputPipe;
	private OutputPipe outputPipe;
	private Random rand;
	private String exitlock = new String("exitlock");
	private boolean connected;

	public void start(String name) {
		instanceName = name;

		rand = new Random();

		configureJXTA();
		// createApplicationPeerGroup();
		try {
			startJXTA();
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		waitForRdv();
		//doSomething();
		waitForInput();
		waitForQuit();

	}

	private void stop() {
		netPeerGroup.stopApp();
	}

	private void configureJXTA() {
		configurator = new NetworkConfigurator();
		configurator.setHome(new File(jxtaHome, instanceName));
		configurator.setPeerID(IDFactory
				.newPeerID(PeerGroupID.defaultNetPeerGroupID));
		configurator.setName("My Peer Name 2");
		configurator.setPrincipal(instanceName); // ("ofno2");
		configurator.setPassword("consequence");
		configurator.setDescription("I am a P2P Peer.");
		configurator.setUseMulticast(false);

		// fetch seeds from file, or alternately from network
		URI seedingURI = new File("seeds.txt").toURI();
		configurator.addRdvSeedingURI(seedingURI);
		configurator.addRelaySeedingURI(seedingURI);
		configurator.setUseOnlyRelaySeeds(true);
		configurator.setUseOnlyRendezvousSeeds(true);

		configurator.setTcpIncoming(false);

		try {
			configurator.save();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		System.out.println("Platform configured and saved");
	}

	private void startJXTA() throws Throwable {

		// clearCache(new File(jxtaHome, "cm2"));

		NetPeerGroupFactory factory = null;
		try {
			factory = new NetPeerGroupFactory((ConfigParams) configurator
					.getPlatformConfig(), new File(jxtaHome, instanceName)
					.toURI());
			/*
			 * , IDFactory.fromURI(new URI(NetPeerGroupID)), NetPeerGroupName,
			 * (XMLElement)
			 * StructuredDocumentFactory.newStructuredDocument(MimeMediaType
			 * .XMLUTF8, "desc", NetPeerGroupName) );
			 */
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Exiting...");
			System.exit(1);
		}

		netPeerGroup = factory.getInterface();

		netPeerGroupDiscovery = netPeerGroup.getDiscoveryService();

		netPeerGroupRendezvous = netPeerGroup.getRendezVousService();
		myPeerID = netPeerGroup.getPeerID().toString();

		netPeerGroupRendezvous.addListener(this);
	}

	@Override
	public void rendezvousEvent(RendezvousEvent event) {
		String eventDescription;
		int eventType = event.getType();
		switch (eventType) {
		case RendezvousEvent.RDVCONNECT:
			eventDescription = "RDVCONNECT";
			connected = true;
			break;
		case RendezvousEvent.RDVRECONNECT:
			eventDescription = "RDVRECONNECT";
			connected = true;
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
			connected = true;
			break;
		case RendezvousEvent.BECAMEEDGE:
			eventDescription = "BECAMEEDGE";
			break;
		default:
			eventDescription = "UNKNOWN RENDEZVOUS EVENT";
		}
		System.out.println(new Date().toString() + "  Rdv: event="
				+ eventDescription + " from peer = " + event.getPeer());

		synchronized (rdvlock) {
			if (connected) {
				rdvlock.notify();
			}
		}
	}

	public void waitForRdv() {
		synchronized (rdvlock) {
			while (!netPeerGroupRendezvous.isConnectedToRendezVous()) {
				System.out.println("Awaiting rendezvous connection...");
				try {
					if (!netPeerGroupRendezvous.isConnectedToRendezVous()) {
						rdvlock.wait();
					}
				} catch (InterruptedException e) {
					;
				}
			}
		}
	}

	private void doSomething() {
		setupPipe();
		new Thread("Send Thread") {
			public void run() {
				int sleepy = 10000;
				while (true) {
					sendToPeers();
					try {
						sleep(sleepy);
					} catch (InterruptedException e) {
					}
				}
			}
		}.start();
	}

	private void waitForInput() {
		setupPipe();
		new Thread("Send Thread") {
			public void run() {

				BufferedReader stdin = new BufferedReader(
						new InputStreamReader(System.in));
				String data;

				System.out.print("send: ");
				try {
					data = stdin.readLine();

					Message msg = new Message();
					MessageElement fromElem = new ByteArrayMessageElement(
							"From", null, myPeerID.toString().getBytes(
									"ISO-8859-1"), null);
					MessageElement msgElem = new ByteArrayMessageElement("Msg",
							null, data.getBytes("ISO-8859-1"), null);

					msg.addMessageElement(fromElem);
					msg.addMessageElement(msgElem);
					outputPipe.send(msg);
				} catch (IOException e1) {
					return;
				}
			}
		}.start();
	}

	private void setupPipe() {
		PipeAdvertisement propagatePipeAdv = (PipeAdvertisement) AdvertisementFactory
				.newAdvertisement(PipeAdvertisement.getAdvertisementType());

		try {
			byte[] bid = MessageDigest.getInstance("MD5").digest(
					"abcd".getBytes("ISO-8859-1"));
			PipeID pipeID = IDFactory.newPipeID(netPeerGroup.getPeerGroupID(),
					bid);
			propagatePipeAdv.setPipeID(pipeID);
			propagatePipeAdv.setType(PipeService.PropagateType);
			propagatePipeAdv.setName("A chattering propagate pipe");
			propagatePipeAdv.setDescription("verbose description");

			PipeService pipeService = netPeerGroup.getPipeService();
			inputPipe = pipeService.createInputPipe(propagatePipeAdv, this);
			outputPipe = pipeService.createOutputPipe(propagatePipeAdv, 1000);
			System.out.println("Propagate pipes and listeners created");
			System.out.println("Propagate PipeID: " + pipeID.toString());
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private void sendToPeers() {
		try {
			String data = new Integer(rand.nextInt()).toString();
			Message msg = new Message();

			MessageElement fromElem = new ByteArrayMessageElement("From", null,
					myPeerID.toString().getBytes("ISO-8859-1"), null);
			MessageElement msgElem = new ByteArrayMessageElement("Msg", null,
					data.getBytes("ISO-8859-1"), null);

			msg.addMessageElement(fromElem);
			msg.addMessageElement(msgElem);
			outputPipe.send(msg);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void pipeMsgEvent(PipeMsgEvent event) {
		try {
			Message msg = event.getMessage();
			byte[] msgBytes = msg.getMessageElement("Msg").getBytes(true);
			byte[] fromBytes = msg.getMessageElement("From").getBytes(true);

			String fromPeerID = new String(fromBytes);
			if (fromPeerID.equals(myPeerID)) {
				System.out.print("(from self): ");
			} else {
				System.out.print("(from other): ");
			}
			System.out.print(new Date());
			System.out.println(" " + fromPeerID + " says "
					+ new String(msgBytes));
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

	}

	private void waitForQuit() {
		synchronized (exitlock) {
			try {
				System.out.println("waiting for quit");
				exitlock.wait();
				System.out.println("Goodbye");
			} catch (InterruptedException e) {
				;
			}
		}
	}

}
