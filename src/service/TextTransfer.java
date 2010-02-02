package service;

import java.io.IOException;
import java.util.Date;

import jxta4jse.JxtaApp;
import jxta4jse.Log;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.StringMessageElement;
import net.jxta.pipe.OutputPipe;

public class TextTransfer {
	private String peerId;
	private String instanceName;

	public TextTransfer(String peerId, String instanceName) {
		this.peerId = peerId;
		this.instanceName = instanceName;
	}

	public void sendText(OutputPipe pipe, String data) {
		Log.d(JxtaApp.TAG, "Try to send message now...");
		try {
			Message msg = new Message();
			MessageElement typeElem = new StringMessageElement("Type",
					Jxta.MessageType.TEXT.toString(), null);
			MessageElement fromElem = new StringMessageElement("From", peerId,
					null);
			MessageElement fromNameElem = new StringMessageElement("FromName",
					instanceName, null);
			MessageElement contentElem = new StringMessageElement("Content",
					data, null);

			msg.addMessageElement(typeElem);
			msg.addMessageElement(fromElem);
			msg.addMessageElement(fromNameElem);
			msg.addMessageElement(contentElem);
			pipe.send(msg);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		Log.d(JxtaApp.TAG, "Message was send");
	}

	public void receiveText(Message msg) {
		String from = msg.getMessageElement("From").toString();
		String fromName = msg.getMessageElement("FromName").toString();
		String content = msg.getMessageElement("Content").toString();

		Log.d(JxtaApp.TAG, "MESSAGE FROM " + new String(fromName) + " ("
				+ new Date() + "): " + new String(content) + " (PeerID: "
				+ new String(from) + ")");
	}
}
