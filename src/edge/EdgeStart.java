package edge;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;


public class EdgeStart {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
		String name;
		
		System.out.print("Peername: ");
		try {
			name = stdin.readLine();
		} catch (IOException e1) {
			System.exit(0);
			return;
		}
		
		EdgePeer peer = new EdgePeer();
	      peer.start(name);
	}

}
