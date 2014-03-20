package se.vidstige.jadb.server;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.Charset;

public class AdbProtocolHandler implements Runnable {
	private final Socket socket;
    private final AdbResponder responder;

    public AdbProtocolHandler(Socket socket, AdbResponder responder) {
		this.socket = socket;
        this.responder = responder;
    }

	@Override
	public void run()
	{
        System.out.println("Serving client");

		try {

            while (true)
            {
                DataInput input = new DataInputStream(socket.getInputStream());
                OutputStreamWriter output = new OutputStreamWriter(socket.getOutputStream());
                byte[] buffer = new byte[4];
                input.readFully(buffer);
                String encodedLength = new String(buffer, Charset.forName("utf-8"));
                int length = Integer.parseInt(encodedLength, 16);

                buffer = new byte[length];
                input.readFully(buffer);
                String command = new String(buffer, Charset.forName("utf-8"));

                responder.onCommand(command);

                if ("host:version".equals(command)) {
                    output.write("OKAY");
                    send(output, String.format("%04x", responder.getVersion()));
                }
                else if ("host:transport-any".equals(command))
                {
                    output.write("OKAY");
                }
                else if ("host:devices".equals(command)) {
                    output.write("OKAY");
                    for (AdbDeviceResponder d : responder.getDevices())
                    {
                        send(output, d.getSerial() + "\t" + d.getType() + "\n");
                    }
                }
                else
                {
                    output.write("FAIL");
                    send(output, "Unknown command: " + command);
                }
                output.flush();
            }
		} catch (IOException e) {
            System.out.println(e.getMessage());
		}		
	}
	
	private String getCommandLength(String command) {
		return String.format("%04x", command.length());
	}
	
	public void send(OutputStreamWriter writer, String response) throws IOException {
		writer.write(getCommandLength(response));
		writer.write(response);
	}
}