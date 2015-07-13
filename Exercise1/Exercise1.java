/**
 * 
 */
package Exercise1;

import javacard.framework.*;
import sim.toolkit.*;

/**
 * @author azhukov
 *
 */
public class Exercise1 extends Applet implements ToolkitInterface, ToolkitConstants {
	private static final byte[] menuTitle = { 'T', 'e', 's', 't' };
	private static final byte[] menuItem1 = { 'R', 'u', 'n' };

	private static final byte[] enter1_Text = { 'E', 'n', 't', 'e', 'r', ' ', 'n', 'u', 'm', 'b', 'e', 'r' };
	private static final byte enter1_CmdQlfr = 0; // digits only {102_223_8.6}
	private static final short enter1_Min = 4;
	private static final short enter1_Max = 20;

	private static final byte[] enter2_Text = { 'E', 'n', 't', 'e', 'r', ' ', 't', 'e', 'x', 't' };
	private static final byte enter2_CmdQlfr = 1; // SMS default alphabet
													// {102_223_8.6}
	private static final short enter2_Min = 0;
	private static final short enter2_Max = 160;

	private final byte[] enteredNum = new byte[enter1_Max];
	private final byte[] convertedNum = new byte[enter1_Max];
	private int convertedLength;
	private final byte[] enteredText = new byte[enter2_Max];

	private static final byte[] interceptAddress = { (byte) 0x81, (byte) 0x21, (byte) 0x43 }; // 1234
	// private static final byte[] interceptText = { 's', 'u', 'b', 's' };
	private static final byte[] ussdSub = { (byte) 0x0F, (byte) 0xAA, (byte) 0x98, (byte) 0x6C, (byte) 0x46,
			(byte) 0x53, (byte) 0xD1, (byte) 0x66, (byte) 0xB2, (byte) 0xD8, (byte) 0x08 }; // ussd:
																							// *1234*4321#
																							// DCS=0F

	public static int AddressTLV(byte[] source, byte[] converted) {
		int ci, si, ni;
		if (source[0] == '+') {
			converted[0] = (byte) 0x91; // NPI is E.164, TON is International
										// {102_223_8.1}
			si = 1;
		} else {
			converted[0] = (byte) 0x81; // NPI is E.164, TON is Unknown
										// {102_223_8.1}
			si = 0;
		}
		for (ci = 1, ni = 0; si < source.length; ci += ni % 2, si++, ni++) {
			byte n = source[si];
			switch (n) {
			case '0':
			case '1':
			case '2':
			case '3':
			case '4':
			case '5':
			case '6':
			case '7':
			case '8':
			case '9':
				n -= '0';
				break;
			case '*':
				n = (byte) 0x0A;
				break;
			case '#':
				n = (byte) 0x0B;
				break;
			default:
				return -1;
			}

			if (ni % 2 == 1)
				converted[ci] |= (n << 4); // upper half-byte
			else
				converted[ci] = n; // lower half-byte
		}
		if (ni % 2 == 1)
			converted[ci++] |= 0xF0;

		return ci;
	}

	public Exercise1() {
		ToolkitRegistry reg = ToolkitRegistry.getEntry();
		reg.initMenuEntry(menuTitle, (short) 0, (short) menuTitle.length, PRO_CMD_DISPLAY_TEXT, false, (byte) 0,
				(short) 0);
		reg.setEvent(EVENT_MO_SHORT_MESSAGE_CONTROL_BY_SIM);
		register();
	}

	public static void install(byte bArray[], short bOffset, byte bLength) {
		new Exercise1();
	}

	public void process(APDU apdu) {
	}

	public void processToolkit(byte event) {
		byte result;
		ProactiveHandler ph;
		EnvelopeHandler eh;
		boolean formCompleted, intercepted;

		switch (event) {
		case EVENT_MENU_SELECTION:
			ph = ProactiveHandler.getTheHandler();
			ph.init(PRO_CMD_SELECT_ITEM, (byte) 0, DEV_ID_ME);
			ph.appendTLV((byte) (TAG_ALPHA_IDENTIFIER | TAG_SET_CR), menuTitle, (short) 0, (short) menuTitle.length);
			ph.appendTLV((byte) (TAG_ITEM | TAG_SET_CR), (byte) 1, menuItem1, (short) 0, (short) menuItem1.length);
			result = ph.send();
			switch (result) {
			case RES_CMD_PERF:
				switch (ProactiveResponseHandler.getTheHandler().getItemIdentifier()) {
				case 1:
					formCompleted = false;
					ph = ProactiveHandler.getTheHandler();
					ph.initGetInput(enter1_CmdQlfr, DCS_8_BIT_DATA, enter1_Text, (byte) 0, (short) enter1_Text.length,
							enter1_Min, enter1_Max);
					result = ph.send();
					switch (result) {
					case RES_CMD_PERF:
						ProactiveResponseHandler.getTheHandler().copyTextString(enteredNum, (short) 0);
						formCompleted &= true;
						break;
					}
					if (!formCompleted)
						break;

					ph = ProactiveHandler.getTheHandler();
					ph.initGetInput(enter2_CmdQlfr, DCS_8_BIT_DATA, enter2_Text, (byte) 0, (short) enter2_Text.length,
							enter2_Min, enter2_Max);
					result = ph.send();
					switch (result) {
					case RES_CMD_PERF:
						ProactiveResponseHandler.getTheHandler().copyTextString(enteredText, (short) 0);
						formCompleted &= true;
						break;
					}

					if ((convertedLength = AddressTLV(enteredNum, convertedNum)) > 0)
						formCompleted &= true;

					if (!formCompleted)
						break;

					ph = ProactiveHandler.getTheHandler();
					// packing not required {102_223_8.6}
					ph.init(PRO_CMD_SEND_SHORT_MESSAGE, (byte) 0, DEV_ID_NETWORK);
					ph.appendTLV((byte) (TAG_ADDRESS | TAG_SET_CR), convertedNum, (short) 0, (short) convertedLength);
					ph.appendTLV((byte) (TAG_SMS_TPDU | TAG_SET_CR), enteredText, (short) 0,
							(short) enteredText.length);
					result = ph.send();
					break;
				default:
				}
				break;
			case EVENT_MO_SHORT_MESSAGE_CONTROL_BY_SIM:
				eh = EnvelopeHandler.getTheHandler();
				intercepted = (eh.findAndCompareValue(TAG_ADDRESS, (byte) 2, (short) 0, interceptAddress, (short) 0,
						(short) interceptAddress.length) == 0);
				if (intercepted) {
					// Not allowed {31.111_7.3.2.2}
					EnvelopeResponseHandler.getTheHandler().postAsBERTLV(SW1_RP_ACK, (byte) 1);

					ph = ProactiveHandler.getTheHandler();
					ph.init(PRO_CMD_SEND_USSD, (byte) 0, DEV_ID_NETWORK);
					ph.appendTLV(TAG_USSD_STRING, ussdSub, (short) 0, (short) ussdSub.length);
					result = ph.send();
				} else {
					// Allowed, no modification {31.111_7.3.2.2}
					EnvelopeResponseHandler.getTheHandler().postAsBERTLV(SW1_RP_ACK, (byte) 0);
				}
				break;
			default:
			}
		}
	}
}
