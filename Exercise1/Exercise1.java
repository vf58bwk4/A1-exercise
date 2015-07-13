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
	private static final short MAX_SMS_SUBMIT_PDU_LEN = 164; // SMS-SUBMIT
																// {23.040_9.2.2.2}
	private static final short MAX_TP_DA_LEN = 12; // TP-DA {23.040_9.2.2.2}

	private static final byte[] menuTitle = { 'T', 'e', 's', 't' };
	private static final byte[] menuItem1 = { 'R', 'u', 'n' };

	private static final byte[] enter1_Text = { 'E', 'n', 't', 'e', 'r', ' ', 'n', 'u', 'm', 'b', 'e', 'r' };
	private static final byte enter1_CommandQualifier = 0; // digits only
															// {102_223_8.6}
	private static final short enter1_Min = 1;
	private static final short enter1_Max = 21; // max number is '+'-sign + 10
												// bytes (20 BCD-digits)
												// {23.040_9.2.2.2}

	private static final byte[] enter2_Text = { 'E', 'n', 't', 'e', 'r', ' ', 't', 'e', 'x', 't' };
	private static final byte enter2_CommandQualifier = 1; // SMS default
															// alphabet
	// {102_223_8.6}
	private static final short enter2_Min = 0;
	private static final short enter2_Max = 160; // max characters in sms with
													// DCS=0

	private static byte[] enteredNum = new byte[enter1_Max];
	private static byte[] enteredText = new byte[enter2_Max];
	private static byte[] PDU = new byte[184]; // SMS-SUBMIT PDU with non-packed UD
	private static byte[] TP_DA = new byte[MAX_TP_DA_LEN];

	private static final byte[] interceptAddress = { (byte) 0x81, (byte) 0x21, (byte) 0x43 }; // 1234
	// private static final byte[] interceptText = { 's', 'u', 'b', 's' };
	private static final byte[] ussdSub = { (byte) 0xF0, (byte) 0xAA, (byte) 0x98, (byte) 0x6C, (byte) 0x46,
			(byte) 0x53, (byte) 0xD1, (byte) 0x66, (byte) 0xB2, (byte) 0xD8, (byte) 0x08 }; // ussd:
																							// *1234*4321#
																							// DCS=F0

	public static int Make_TP_DA(byte[] converted, byte[] source, short sourceLength) {
		int ci, si, ni;
		if (source[0] == '+') {
			converted[1] = (byte) 0x91; // NPI is E.164, TON is International
										// {102_223_8.1}
			si = 1;
		} else {
			converted[1] = (byte) 0x81; // NPI is E.164, TON is Unknown
										// {102_223_8.1}
			si = 0;
		}
		for (ci = 2, ni = 0; si < sourceLength; ci += ni % 2, si++, ni++) {
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
				return -si;
			}

			if (ni % 2 == 1)
				converted[ci] |= (n << 4); // upper half-byte
			else
				converted[ci] = n; // lower half-byte
		}
		if (ni % 2 == 1)
			converted[ci++] |= 0xF0;

		return converted[0] = (byte) (ci - 1);
	}

	// TP-VPF == 0: TP-VP field not present {23.040_9.2.3.3}
	public static short Make_SMS_Submit_PDU(byte[] PDU, byte TP_RD, byte TP_RP, byte TP_UDHI, byte TP_SRR, byte TP_MR,
			byte[] TP_DA, byte TP_PID, byte TP_DCS, byte TP_UDL, byte[] TP_UD, short TP_UDLength) {
		byte PDU0 = (byte) 0;
		PDU0 |= 1 << 6; // TP-MTI = 1: SMS-SUBMIT {23.040_9.2.3.1}
		PDU0 |= TP_RD << 5;
		PDU0 |= TP_SRR << 2;
		PDU0 |= TP_UDHI << 1;
		PDU0 |= TP_RP;

		PDU[0] = PDU0;
		PDU[1] = TP_MR;

		short pi = (short) (TP_DA[0] + 1);
		Util.arrayCopyNonAtomic(TP_DA, (short) 0, PDU, (short) 2, pi);

		pi += 2;
		PDU[pi++] = TP_PID;
		PDU[pi++] = TP_DCS;
		PDU[pi++] = TP_UDL;
		Util.arrayCopyNonAtomic(TP_UD, (short) 0, PDU, (short) pi, TP_UDLength);
		return (short) (pi + TP_UDLength);
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
		switch (event) {
		case EVENT_MENU_SELECTION:
			processEventMenuSelection();
			break;
		case EVENT_MO_SHORT_MESSAGE_CONTROL_BY_SIM:
			processMOShortMessageControlBySIM();
			break;
		default:
		}
	}

	private static void processEventMenuSelection() {
		byte result;
		ProactiveHandler ph;
		ProactiveResponseHandler prh;
		boolean formCompleted;
		short enteredNum_length = 0, enteredText_length = 0;

		ph = ProactiveHandler.getTheHandler();
		ph.init(PRO_CMD_SELECT_ITEM, (byte) 0, DEV_ID_ME);
		ph.appendTLV((byte) TAG_ALPHA_IDENTIFIER, menuTitle, (short) 0, (short) menuTitle.length);
		ph.appendTLV((byte) TAG_ITEM, (byte) 1, menuItem1, (short) 0, (short) menuItem1.length);
		result = ph.send();
		switch (result) {
		case RES_CMD_PERF:
			switch (ProactiveResponseHandler.getTheHandler().getItemIdentifier()) {
			case 1:
				formCompleted = false;
				ph = ProactiveHandler.getTheHandler();
				ph.initGetInput(enter1_CommandQualifier, DCS_8_BIT_DATA, enter1_Text, (byte) 0,
						(short) enter1_Text.length, enter1_Min, enter1_Max);
				result = ph.send();
				switch (result) {
				case RES_CMD_PERF:
					prh = ProactiveResponseHandler.getTheHandler();
					enteredNum_length = prh.getTextStringLength();
					prh.copyTextString(enteredNum, (short) 0);
					formCompleted &= true;
					break;
				}
				if (!formCompleted)
					break;

				ph = ProactiveHandler.getTheHandler();
				ph.initGetInput(enter2_CommandQualifier, DCS_8_BIT_DATA, enter2_Text, (byte) 0,
						(short) enter2_Text.length, enter2_Min, enter2_Max);
				result = ph.send();
				switch (result) {
				case RES_CMD_PERF:
					prh = ProactiveResponseHandler.getTheHandler();
					enteredText_length = prh.getTextStringLength();
					prh.copyTextString(enteredText, (short) 0);
					formCompleted &= true;
					break;
				}

				if (Make_TP_DA(TP_DA, enteredNum, enteredNum_length) > 0)
					formCompleted &= true;

				if (!formCompleted)
					break;

				ph = ProactiveHandler.getTheHandler();
				// packing is required {102_223_8.6}
				ph.init(PRO_CMD_SEND_SHORT_MESSAGE, (byte) 1, DEV_ID_NETWORK);
				ph.appendTLV((byte) TAG_ADDRESS, TP_DA, (short) 1, (short) TP_DA[0]);

				// TP-RD = TP-RP TP-UDHI = TP-SRR = TP-MR = TP-PID = TP-DCS
				// = 0
				short PDU_length = Make_SMS_Submit_PDU(PDU, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, TP_DA,
						(byte) 0, (byte) 0, (byte) enteredText_length, enteredText, enteredText_length);
				ph.appendTLV((byte) TAG_SMS_TPDU, PDU, (short) 0, PDU_length);
				result = ph.send();
				break;
			default:
			}
		}
	}

	private static void processMOShortMessageControlBySIM() {
		byte result;
		ProactiveHandler ph;
		EnvelopeHandler eh;
		boolean intercepted;

		eh = EnvelopeHandler.getTheHandler();
		// looking to 2nd Address data object - TP_Destination_Address
		intercepted = (eh.findAndCompareValue(TAG_ADDRESS, (byte) 2, (short) 0, interceptAddress, (short) 0,
				(short) interceptAddress.length) == 0);
		if (intercepted) {
			// Not allowed {31.111_7.3.2.2}
			EnvelopeResponseHandler.getTheHandler().postAsBERTLV(SW1_RP_ACK, (byte) 1);

			ph = ProactiveHandler.getTheHandler();
			ph.init(PRO_CMD_SEND_USSD, (byte) 0, DEV_ID_NETWORK);
			ph.appendTLV(TAG_USSD_STRING, ussdSub, (short) 0, (short) ussdSub.length);
			result = ph.send();
		}
	}

}
