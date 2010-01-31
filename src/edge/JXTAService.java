package edge;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.Date;
import java.util.HashMap;

import net.jxta.discovery.DiscoveryService;
import net.jxta.endpoint.ByteArrayMessageElement;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.id.IDFactory;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.pipe.OutputPipe;
import net.jxta.pipe.PipeMsgEvent;
import net.jxta.pipe.PipeMsgListener;
import net.jxta.pipe.PipeService;
import net.jxta.platform.NetworkConfigurator;
import net.jxta.platform.NetworkManager;
import net.jxta.protocol.PipeAdvertisement;
import net.jxta.rendezvous.RendezVousService;
import net.jxta.rendezvous.RendezvousEvent;
import net.jxta.rendezvous.RendezvousListener;
import discovery.DiscoveryClient;
import discovery.DiscoveryServer;

public class JXTAService implements RendezvousListener, PipeMsgListener {
	private NetworkConfigurator configurator;
	private String instanceName;
	private PeerGroup netPeerGroup;
	private DiscoveryService netPeerGroupDiscovery;
	private RendezVousService netPeerGroupRendezvous;
	private String myPeerID;
	private String rdvlock = new String("rdvlock");
	private String exitlock = new String("exitlock");
	private boolean connected;
	private NetworkManager networkManager;
	private static URI rdvlist = new File("seeds.txt").toURI();
	//private static String rdvlist = "http://192.168.178.74/seeds.txt";
	
	private DiscoveryClient discoveryClient;
	private DiscoveryServer discoveryServer;
	private HashMap<String, OutputPipe> establishedPipes;
	
	private File instanceHome;
	private String peerName;
	private String username;
	private String password;
	
	public void start(String name, File home, String peer, String user,
			String pass) {
		instanceName = name;
		instanceHome = home;
		peerName = peer;
		username = user;
		password = pass;

		configureJXTA();
		try {
			startJXTA();
		} catch (Throwable e) {
			e.printStackTrace();
		}
		waitForRdv();
		
		// discovery
		Thread discoveryServerThread = new Thread(discoveryServer = new DiscoveryServer(networkManager), "Discovery Server Thread");
		discoveryServerThread.start();
		
		Thread discoveryClientThread = new Thread(discoveryClient = new DiscoveryClient(networkManager), "Discovery Client Thread");
		discoveryClientThread.start();
		
		establishedPipes = new HashMap<String, OutputPipe>();
		
		waitForInput();
		waitForQuit();
	}

	public void stop() {
		netPeerGroup.stopApp();
	}

	private void configureJXTA() {
		try {
			networkManager = new NetworkManager(NetworkManager.ConfigMode.EDGE,
					"My Local Network", new File(instanceHome, instanceName).toURI());
			
			configurator = networkManager.getConfigurator();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	
		/*
		ConfigurationFactory.setName(instanceName);
		ConfigurationFactory.setTCPPortRange(9700, 9799);
		ConfigurationFactory.setPeerID(configurator.getPeerID());
		try {
			ConfigurationFactory.setRdvSeedingURI(new URI(rdvlist));
			ConfigurationFactory.setRelaySeedingURI(new URI(rdvlist));
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		ConfigurationFactory factory = ConfigurationFactory.newInstance();
		//factory.setUseOnlyRelaySeeds(true);
		factory.setUseOnlyRendezvousSeeds(true);
		*/

		//configurator = new NetworkConfigurator();
		configurator.setHome(new File(instanceHome, instanceName));
		configurator.setPeerID(IDFactory
				.newPeerID(PeerGroupID.defaultNetPeerGroupID));
		configurator.setName(peerName);
		configurator.setPrincipal(username); // ("ofno2");
		configurator.setPassword(password);
		configurator.setDescription("I am a P2P Peer.");
		configurator.setUseMulticast(true);

		// fetch seeds from file, or alternately from network
		configurator.addRdvSeedingURI(rdvlist);
		configurator.addRelaySeedingURI(rdvlist);
		//configurator.setUseOnlyRelaySeeds(true);
		configurator.setUseOnlyRendezvousSeeds(true);
		
		//configurator.setTcpIncoming(false);

		/*
		try {
			configurator.save();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		*/
		System.out.println("Platform configured and saved");
	}

	private void startJXTA() throws Throwable {
		netPeerGroup = networkManager.startNetwork();

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

	private void waitForInput() {
		final BufferedReader stdin = new BufferedReader(
				new InputStreamReader(System.in));
		
		new Thread("Send Thread") {
			public void run() {
				while (true) {
					String peername;
					String message;
					
					try {
						System.out.print("peer-name: ");
						peername = stdin.readLine();
						System.out.println(peername);

						System.out.print("send message: ");
						message = stdin.readLine();
						
						sendMsgToPeer(peername, message);
					} catch (IOException e2) {
						e2.printStackTrace();
					}
				}
			}
		}.start();
	}
	
	public void sendMsgToPeer(String peername, String message) {
		PipeAdvertisement peer = null;
		
		synchronized (discoveryClient.getPeerList()) {
			//System.out.println(discoveryClient.getGroupList().get(0).getName());
			System.out.println(discoveryClient.getPeerList().size());
			
			for (int i = 0; i < discoveryClient.getPeerList().size(); i++) {
				System.out.println(discoveryClient.getPeerList().get(i).getName() + " == " + peername);
				if (discoveryClient.getPeerList().get(i).getName() != null && discoveryClient.getPeerList().get(i).getName().equals(peername)) {
					peer = discoveryClient.getPeerList().get(i).getPipeAdvertisement();
				}
			}
		
		}
		
		if (peer == null) {
			System.out.println("Peer not found while discovery");
			return;
		}
		
		OutputPipe pipe = setupPipe(peer);
		
		if (pipe != null)
			sendMsgOnPipe(pipe, message);
		
		// hold all open pipes for later use
		//closePipe(pipe, peer);
	}

	private OutputPipe setupPipe(PipeAdvertisement peerAdv) {
		/*
		if (establishedPipes.containsKey(peerAdv.getName())) {
			JxtaBiDiPipe pipe = establishedPipes.get(peerAdv.getName());
			System.out.println("Pipe already exist, use established pipe");
			return pipe;
		}
		*/

		PipeService pipeService = netPeerGroup.getPipeService();
		OutputPipe outputPipe = null;
		try {
			outputPipe = pipeService.createOutputPipe(peerAdv, 1 * 60 * 1000);
		} catch (IOException e) {
			e.printStackTrace();
		}

		
		System.out.println("Message Pipe to peer established: " + peerAdv.getName());
		
		//establishedPipes.put(peerAdv.getName(), pipe);
		
		return outputPipe;
	}
	
	private void closePipe(OutputPipe pipe, PipeAdvertisement peerAdv) {
		pipe.close();
		
		//establishedPipes.remove(peerAdv.getName());
		
		System.out.println("Message Pipe to peer closed");
	}
	
	private void sendMsgOnPipe(OutputPipe pipe, String data) {
		System.out.println("Try to send message now");
		try {
			Message msg = new Message();
			MessageElement fromElem = new ByteArrayMessageElement(
					"From", null, myPeerID.toString().getBytes(
							"ISO-8859-1"), null);
			MessageElement fromNameElem = new ByteArrayMessageElement("FromName",
					null, instanceName.getBytes("ISO-8859-1"), null);
			MessageElement msgElem = new ByteArrayMessageElement("Msg",
					null, data.getBytes("ISO-8859-1"), null);

			msg.addMessageElement(fromElem);
			msg.addMessageElement(fromNameElem);
			msg.addMessageElement(msgElem);
			pipe.send(msg);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		System.out.println("Message send.");
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
