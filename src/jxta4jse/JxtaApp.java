package jxta4jse;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import service.Jxta;

public class JxtaApp {
	public static String TAG = "JXTA4JSE";

	public static void main(String[] args) {
		BufferedReader stdin = new BufferedReader(new InputStreamReader(
				System.in));
		String name;

		System.out.print("Peername: ");
		try {
			name = stdin.readLine();
		} catch (IOException e) {
			System.exit(0);
			return;
		}

		Jxta peer = new Jxta();
		//peer.setActAsRendezvous(true);
		peer.start(name, new File("cache"), name, "");
	}

}
