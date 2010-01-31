package rendezvous;


public class RedezvousStart {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		RendezvousPeer rdv = new RendezvousPeer();
		rdv.start("rendezvous1");
	}

}
