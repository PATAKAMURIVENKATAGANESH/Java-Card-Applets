/*
 * Copyright (c) 1998, 2025, Oracle and/or its affiliates. All rights reserved.
 */

package com.oracle.javacard.sample;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.stream.IntStream;

import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import javax.smartcardio.TerminalFactory;

import com.oracle.javacard.ams.AMService;
import com.oracle.javacard.ams.AMServiceFactory;
import com.oracle.javacard.ams.AMSession;
import com.oracle.javacard.ams.config.AID;
import com.oracle.javacard.ams.config.CAPFile;
import com.oracle.javacard.ams.script.APDUScript;
import com.oracle.javacard.ams.script.ScriptFailedException;
import com.oracle.javacard.ams.script.Scriptable;

public class AMSWalletClient {

	static final String isdAID = "aid:A000000151000000";
	static final String sAID_CAP = "aid:A00000006203010C06";
	static final String sAID_AppletClass = "aid:A00000006203010C0601";
	static final String sAID_AppletInstance = "aid:A00000006203010C0601";
	static final CommandAPDU selectApplet = new CommandAPDU(0x00, 0xA4, 0x04, 0x00, AID.from(sAID_AppletInstance).toBytes(), 256);

	/**
	 * Launch the sample
	 *
	 * @param args command arguments. Use {@code -cap=<capfile> -props=<propsfile>}
	 */
	public static void main(String[] args) {

		int iResult = 0;

		try {
			CAPFile appFile = CAPFile.from(getArg(args, "cap"));
			Properties props = new Properties();
			props.load(new FileInputStream(getArg(args, "props")));

			// Create and configure Application Management Service
			AMService ams = AMServiceFactory.getInstance("GP2.2");
			ams.setProperties(props);
			for (String key : ams.getPropertiesKeys()) {
				System.out.println(key + " = " + ams.getProperty(key));
			}

			// Application Management session used to deploy CAPFile
			AMSession deploy = ams.openSession(isdAID)   // select SD & open secure channel
					.load(sAID_CAP, appFile.getBytes())  // load an application file
					.install(sAID_CAP,                   // install application
							sAID_AppletClass, sAID_AppletInstance, new byte[] {0x05, 0x01, 0x02, 0x03, 0x04, 0x05})
					.close();

			// Application Management session used to undeploy CAPFile
			AMSession undeploy = ams.openSession(isdAID) // select SD & open secure channel
					.uninstall(sAID_AppletInstance)      // uninstall the application
					.unload(sAID_CAP)                    // unload the application code
					.close();

			// sample script: deploy an Applet, use it and undeploy it.
			TestScript testScript = new TestScript()

					.append( deploy )
					.append( selectApplet )

					//Verify user pin
					.append(new CommandAPDU(0x80, 0x20, 0x00, 0x00, new byte[] { 0x05, 0x01, 0x02, 0x03, 0x04, 0x05}),
							new ResponseAPDU(new byte[] { (byte)0x90, (byte)0x00}))

					//Get wallet balance
					.append(new CommandAPDU(0x80, 0x50, 0x00, 0x00, new byte[] {}, 256),
							new ResponseAPDU(new byte[] { 0x00, 0x00, (byte)0x90, (byte)0x00}))

					//Attempt to debit from an empty account
					.append(new CommandAPDU(0x80, 0x40, 0x00, 0x00, new byte[] {0x64}, 256),
							new ResponseAPDU(new byte[] { (byte)0x6A, (byte)0x85}))

					//Credit $100 to the empty account
					.append(new CommandAPDU(0x80, 0x30, 0x00, 0x00, new byte[] {0x64}),
							new ResponseAPDU(new byte[] { (byte)0x90, (byte)0x00}))

					//Get Balance
					.append(new CommandAPDU(0x80, 0x50, 0x00, 0x00, new byte[] {}, 256),
							new ResponseAPDU(new byte[] { 0x00, 0x64, (byte)0x90, (byte)0x00}))

					//Debit $50 from the account
					.append(new CommandAPDU(0x80, 0x40, 0x00, 0x00, new byte[] {0x32}),
							new ResponseAPDU(new byte[] { (byte)0x90, (byte)0x00}))

					//Get Balance
					.append(new CommandAPDU(0x80, 0x50, 0x00, 0x00, new byte[] {}, 256),
							new ResponseAPDU(new byte[] { 0x00, 0x32, (byte)0x90, (byte)0x00}))

					//Credit $128 to the account
					.append(new CommandAPDU(0x80, 0x30, 0x00, 0x00, new byte[] {(byte) 0x80}),
							new ResponseAPDU(new byte[] { (byte)0x6A, (byte)0x83}))

					//Get Balance
					.append(new CommandAPDU(0x80, 0x50, 0x00, 0x00, new byte[] {}, 256),
							new ResponseAPDU(new byte[] { 0x00, 0x32, (byte)0x90, (byte)0x00}))

					//Debit $51 from the account
					.append(new CommandAPDU(0x80, 0x40, 0x00, 0x00, new byte[] {0x33}),
							new ResponseAPDU(new byte[] { (byte)0x6A, (byte)0x85}))

					//Get Balance
					.append(new CommandAPDU(0x80, 0x50, 0x00, 0x00, new byte[] {}, 256),
							new ResponseAPDU(new byte[] { 0x00, 0x32, (byte)0x90, (byte)0x00}))

					//Debit $128 from the account
					.append(new CommandAPDU(0x80, 0x40, 0x00, 0x00, new byte[] {(byte) 0x80}),
							new ResponseAPDU(new byte[] { (byte)0x6A, (byte)0x83}))

					//Get Balance
					.append(new CommandAPDU(0x80, 0x50, 0x00, 0x00, new byte[] {}, 256),
							new ResponseAPDU(new byte[] { 0x00, 0x32, (byte)0x90, (byte)0x00}))

					//Reselect Wallet applet so that userpin is reset
					.append( selectApplet,
							new ResponseAPDU(new byte[] { (byte)0x90, (byte)0x00}))

					//Credit $127 to the account before pin verification
					.append(new CommandAPDU(0x80, 0x30, 0x00, 0x00, new byte[] {(byte) 0x7F}),
							new ResponseAPDU(new byte[] { (byte)0x63, (byte)0x01}))

					//Verify User pin with wrong pin value
					.append(new CommandAPDU(0x80, 0x20, 0x00, 0x00, new byte[] { 0x04, 0x01, 0x02, 0x03, 0x04}),
							new ResponseAPDU(new byte[] { (byte)0x63, (byte)0x00}))

					//Verify user pin again with correct pin value
					.append(new CommandAPDU(0x80, 0x20, 0x00, 0x00, new byte[] { 0x05, 0x01, 0x02, 0x03, 0x04, 0x05}),
							new ResponseAPDU(new byte[] { (byte)0x90, (byte)0x00}))

					//Get balance
					.append(new CommandAPDU(0x80, 0x50, 0x00, 0x00, new byte[] {}, 256),
							new ResponseAPDU(new byte[] { 0x00, 0x32, (byte)0x90, (byte)0x00}))

					.append(undeploy);

			// Terminal to simulator
			CardTerminal t = getTerminal("socket", "127.0.0.1", "9025"); // or getTerminal("pcsc");

			// Wait some seconds to allow connections
			if (t.waitForCardPresent(10000)) {
				System.out.println("Connection to simulator established: "+ t.getName());
				Card c = t.connect("*");
				System.out.println(getFormattedATR(c.getATR().getBytes()));

				List<ResponseAPDU> responses = testScript.run(c.getBasicChannel());
				c.disconnect(true);

				System.out.println("Responses count: " + responses.size());
			}
			else {
				System.out.println("Connection to simulator failed");
				iResult = -1;
			}

		} catch (NoSuchAlgorithmException | NoSuchProviderException | CardException | ScriptFailedException | IOException e) {
			e.printStackTrace();
			iResult = -1;
		}
		System.exit (iResult);
	}

	private static String getArg(String[] args, String argName) throws IllegalArgumentException {
		String value = null;

		for (String param : args) {
			if (param.startsWith("-" + argName + "=")) {
				value = param.substring(param.indexOf('=') + 1);
			}
		}

		if(value == null || value.length() == 0) {
			throw new IllegalArgumentException("Argument " + argName + " is missing");
		}
		return value;
	}

	/**
	 * Puts all ATR bytes into a single string using hexadecimal format
	 * @param ATR ATR bytes
	 * @return Formatted ATR
	 */
	private static String getFormattedATR(byte[] ATR) {
		StringBuilder sb = new StringBuilder();
		for (byte b : ATR) {
			sb.append(String.format("%02X ", b));
		}
		return String.format("ATR: [%s]", sb.toString().trim());
	}

	private static CardTerminal getTerminal(String... connectionParams) throws NoSuchAlgorithmException, NoSuchProviderException, CardException {
		TerminalFactory tf;
		String connectivityType = connectionParams[0];
		if (connectivityType.equals("socket")) {
			String ipaddr = connectionParams[1];
			String port = connectionParams[2];
			tf = TerminalFactory.getInstance("SocketCardTerminalFactoryType",
					List.of(new InetSocketAddress(ipaddr, Integer.parseInt(port))),
					"SocketCardTerminalProvider");
		} else {
			tf = TerminalFactory.getDefault();
		}
		return tf.terminals().list().get(0);
	}

	private static class TestScript extends APDUScript {
		private List<CommandAPDU>  commands = new LinkedList<>();
		private List<ResponseAPDU> responses = new LinkedList<>();
		private int index = 0;

		public List<ResponseAPDU> run(CardChannel channel) throws ScriptFailedException {
			return super.run(channel, c -> lookupIndex(c), r -> !isExpected(r));
		}

		@Override
		public TestScript append(Scriptable<CardChannel, CommandAPDU, ResponseAPDU> other) {
			super.append(other);
			return this;
		}

		public TestScript append(CommandAPDU apdu, ResponseAPDU expected) {
			super.append(apdu);
			this.commands.add(apdu);
			this.responses.add(expected);
			return this;
		}

		public TestScript append(CommandAPDU apdu) {
			super.append(apdu);
			return this;
		}

		private CommandAPDU lookupIndex(CommandAPDU apdu) {
			print(apdu);
			this.index = IntStream.range(0, this.commands.size())
			        .filter(i -> apdu == this.commands.get(i))
					.findFirst()
					.orElse(-1);
			return apdu;
		}

		private boolean isExpected(ResponseAPDU response) {

			ResponseAPDU expected = (index < 0)? response : this.responses.get(index);
			if (!response.equals(expected)) {
				System.out.println("Received: ");
				print(response);
				System.out.println("Expected: ");
				print(expected);
				return false;
			}
			print(response);
			return true;
		}

		private static void print(CommandAPDU apdu) {
			StringBuilder sb = new StringBuilder();
			sb.append(String.format("%02X%02X%02X%02X %02X[", apdu.getCLA(), apdu.getINS(), apdu.getP1(), apdu.getP2(), apdu.getNc()));
			for (byte b : apdu.getData()) {
				sb.append(String.format("%02X", b));
			}
			sb.append("]");
			System.out.format("[%1$tF %1$tT %1$tL %1$tZ] [APDU-C] %2$s %n", System.currentTimeMillis(), sb.toString());
		}

		private static void print(ResponseAPDU apdu) {
			byte[] bytes = apdu.getData();
			StringBuilder sb = new StringBuilder();
			for (byte b : bytes) {
				sb.append(String.format("%02X", b));
			}
			System.out.format("[%1$tF %1$tT %1$tL %1$tZ] [APDU-R] [%2$s] SW:%3$04X %n", System.currentTimeMillis(), sb.toString(), apdu.getSW());
		}
	}
}