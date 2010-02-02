package rendezvous;

import java.io.File;

public class RedezvousStart {

	public static void main(String[] args) {
		RendezvousPeer rdv = new RendezvousPeer();
		rdv.start("rendezvous1", new File("cache"));
	}

}
