/**
 * 
 */
package exercise1;

import javacard.framework.*;
import sim.toolkit.*;

/**
 * @author azhukov
 *
 */
public class Exercise1 extends Applet implements ToolkitInterface, ToolkitConstants {
    static final short MAX_SMS_SUBMIT_PDU_LEN = 164; // SMS-SUBMIT
						     // {23.040_9.2.2.2}
    static final short MAX_TP_DA_LEN = 12; // TP-DA {23.040_9.2.2.2}
    static final short MAX_TP_UD_LEN = 140;

    static final byte[] menuTitle = { 'T', 'e', 's', 't' };
    static final byte[] menuItem1 = { 'R', 'u', 'n' };

    static final byte[] aiSMSSending = { 'S', 'e', 'n', 'd', 'i', 'n', 'g', ' ', 'S', 'M', 'S' };
    static final byte[] aiUSSDSending = { 'S', 'e', 'n', 'd', 'i', 'n', 'g', ' ', 'U', 'S', 'S', 'D' };

    static final byte[] enter1_Text = { 'E', 'n', 't', 'e', 'r', ' ', 'n', 'u', 'm', 'b', 'e', 'r' };
    static final byte enter1_CommandQualifier = 0; // digits only {102_223_8.6}
    static final short enter1_Min = 1;
    static final short enter1_Max = 21; // max number is '+'-sign + 10 bytes (20
					// BCD-digits) {23.040_9.2.2.2}

    static final byte[] enter2_Text = { 'E', 'n', 't', 'e', 'r', ' ', 't', 'e', 'x', 't' };
    static final byte enter2_CommandQualifier = 1; // SMS default alphabet
						   // {102_223_8.6}
    static final short enter2_Min = 0;
    static final short enter2_Max = 160; // max characters in sms with DCS=0

    static byte[] enteredNum, enteredText, PDU, TP_DA, TP_UD;

    static final byte[] interceptAddress = { (byte) 0x81, (byte) 0x21, (byte) 0x43 }; // 1234
    // private static final byte[] interceptText = { 's', 'u', 'b', 's' };
    static final byte[] ussdSub = { (byte) 0xF0, (byte) 0xAA, (byte) 0x98, (byte) 0x6C, (byte) 0x46, (byte) 0x53,
	    (byte) 0xD1, (byte) 0x66, (byte) 0xB2, (byte) 0xD8, (byte) 0x08 }; // ussd:
									       // *1234*4321#
									       // DCS=F0

    public static byte pack7Bit(byte[] dst, byte[] src, short noChars) {
	short charCnt, srcIdx, bitShift = 0;
	for (charCnt = 0, srcIdx = charCnt; charCnt < noChars; charCnt++, srcIdx++) {
	    short charCntMul7 = (short) (charCnt * 7);
	    short dstIdx = (short) (charCntMul7 / 8); // 0,0,1,2,3,4,5,6,7,7,8,9,...
	    bitShift = (short) (charCntMul7 % 8); // 0,7,6,5,4,3,2,1,0,7,...

	    short septet = (short) (src[srcIdx] & 0x7F);

	    dst[dstIdx] |= (septet << bitShift); // low part
	    if (bitShift > 1) // high part crosses a byte boundary
		dst[++dstIdx] = (byte) (septet >> (8 - bitShift)); // high part
	}
	return (byte) ((short)((short)(noChars + 1) * 7) / 8);
    }

    public static short makeTP_DA(byte[] dst, short dstOff, byte[] src, short srcOff, short srcLen) {
	short dstIdx = (short) (dstOff + 1), numIdx;
	if (src[srcOff] == '+') {
	    dst[dstIdx] = (byte) 0x91; // NPI is E.164, TON is International
				       // {102_223_8.1}
	    srcOff++;
	} else {
	    dst[dstIdx] = (byte) 0x81; // NPI is E.164, TON is Unknown
				       // {102_223_8.1}
	}
	dstIdx++;
	for (numIdx = 0; srcOff < srcLen; dstIdx += numIdx % 2, srcOff++, numIdx++) {
	    byte n = src[srcOff];
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
		return (short) -srcOff;
	    }

	    if (numIdx % 2 == 1)
		dst[dstIdx] |= (n << 4); // upper half-byte come second
	    else
		dst[dstIdx] = n; // lower half-byte come first
	}
	if (numIdx % 2 == 1)
	    dst[dstIdx++] |= 0xF0;

	return dst[dstOff] = (byte) (dstOff + dstIdx - 1);
    }

    // TP-VPF == 0: TP-VP field not present {23.040_9.2.3.3}
    static short makePDU_SMS_SUBMIT(byte[] PDU, byte TP_RD, byte TP_RP, byte TP_UDHI, byte TP_SRR, byte TP_MR,
	    byte[] TP_DA, byte TP_PID, byte TP_DCS, byte TP_UDL, byte[] TP_UD, byte TP_UD_length) {
	short PDU0 = 1 << 6; // TP-MTI = 1: SMS-SUBMIT {23.040_9.2.3.1}
	PDU0 |= TP_RD << 5;
	PDU0 |= TP_SRR << 2;
	PDU0 |= TP_UDHI << 1;
	PDU0 |= TP_RP;

	PDU[0] = (byte) PDU0;
	PDU[1] = TP_MR;

	short pi = (short) (TP_DA[0] + 1);
	Util.arrayCopyNonAtomic(TP_DA, (short) 0, PDU, (short) 2, pi);

	pi += 2;
	PDU[pi++] = TP_PID;
	PDU[pi++] = TP_DCS;
	PDU[pi++] = TP_UDL;
	Util.arrayCopyNonAtomic(TP_UD, (short) 0, PDU, (short) pi, TP_UD_length);
	return (short) (pi + TP_UD_length);
    }

    private Exercise1() {
	ToolkitRegistry reg = ToolkitRegistry.getEntry();
	reg.initMenuEntry(menuTitle, (short) 0, (short) menuTitle.length, PRO_CMD_DISPLAY_TEXT, false, (byte) 0,
		(short) 0);
	reg.setEvent(EVENT_MO_SHORT_MESSAGE_CONTROL_BY_SIM);
	new Debug();
    }

    public static void install(byte bArray[], short bOffset, byte bLength) {
	enteredNum = JCSystem.makeTransientByteArray(enter1_Max, JCSystem.CLEAR_ON_RESET);
	enteredText = JCSystem.makeTransientByteArray(enter2_Max, JCSystem.CLEAR_ON_RESET);
	// SMS-SUBMIT PDU with non-packed UD
	PDU = JCSystem.makeTransientByteArray((short) 184, JCSystem.CLEAR_ON_RESET);
	TP_DA = JCSystem.makeTransientByteArray(MAX_TP_DA_LEN, JCSystem.CLEAR_ON_RESET);
	TP_UD = JCSystem.makeTransientByteArray(MAX_TP_UD_LEN, JCSystem.CLEAR_ON_RESET);

	new Exercise1().register();
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

    static void processEventMenuSelection() {
	byte r;
	ProactiveHandler ph = ProactiveHandler.getTheHandler();

	ph.init(PRO_CMD_SELECT_ITEM, (byte) 0, DEV_ID_ME);
	ph.appendTLV(TAG_ALPHA_IDENTIFIER, menuTitle, (short) 0, (short) menuTitle.length);
	ph.appendTLV(TAG_ITEM, (byte) 1, menuItem1, (short) 0, (short) menuItem1.length);
	r = ph.send();
	switch (r) {
	case RES_CMD_PERF:
	    switch (ProactiveResponseHandler.getTheHandler().getItemIdentifier()) {
	    case 1:
		processItem1();
	    default:
	    }
	}
    }

    static void processItem1() {
	byte r;
	short enteredNum_length = 0, enteredText_length = 0;
	ProactiveResponseHandler prh;
	ProactiveHandler ph = ProactiveHandler.getTheHandler();

	ph.initGetInput(enter1_CommandQualifier, DCS_8_BIT_DATA, enter1_Text, (byte) 0, (short) enter1_Text.length,
		enter1_Min, enter1_Max);
	r = ph.send();
	switch (r) {
	case RES_CMD_PERF:
	    prh = ProactiveResponseHandler.getTheHandler();
	    enteredNum_length = prh.getTextStringLength();
	    prh.copyTextString(enteredNum, (short) 0);
	    break;
	default:
	    return;
	}

	ph = ProactiveHandler.getTheHandler();
	ph.initGetInput(enter2_CommandQualifier, DCS_8_BIT_DATA, enter2_Text, (byte) 0, (short) enter2_Text.length,
		enter2_Min, enter2_Max);
	r = ph.send();
	switch (r) {
	case RES_CMD_PERF:
	    prh = ProactiveResponseHandler.getTheHandler();
	    enteredText_length = prh.getTextStringLength();
	    prh.copyTextString(enteredText, (short) 0);
	    break;
	default:
	    return;
	}

	r = (byte) makeTP_DA(TP_DA, (short) 0, enteredNum, (short) 0, enteredNum_length);
	if (r < 0)
	    return;

	r = pack7Bit(TP_UD, enteredText, enteredText_length);
	// RD=RP=UDHI=SRR=MR=PID = 0
	short PDU_length = makePDU_SMS_SUBMIT(PDU, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, TP_DA, (byte) 0,
		DCS_DEFAULT_ALPHABET, (byte) enteredText_length, TP_UD, r);

	ph = ProactiveHandler.getTheHandler();
	// =1 - packing is required {102_223_8.6}
	ph.init(PRO_CMD_SEND_SHORT_MESSAGE, (byte) 0, DEV_ID_NETWORK);
	ph.appendTLV(TAG_ALPHA_IDENTIFIER, aiSMSSending, (short) 0, (short) aiSMSSending.length);
	// SMSC address
	// ph.appendTLV(TAG_ADDRESS, TP_SCA, (short) 0, (short) TP_SCA.length);

	ph.appendTLV(TAG_SMS_TPDU, PDU, (short) 0, PDU_length);
	r = ph.send();
	Debug.displayByte(r);
    }

    static void processMOShortMessageControlBySIM() {
	byte r;
	boolean intercepted;
	ProactiveHandler ph;
	EnvelopeHandler eh = EnvelopeHandler.getTheHandler();
	// looking to 2nd Address data object - TP_Destination_Address
	intercepted = (eh.findAndCompareValue(TAG_ADDRESS, (byte) 2, (short) 0, interceptAddress, (short) 0,
		(short) interceptAddress.length) == 0);
	if (intercepted) {
	    // Not allowed {31.111_7.3.2.2}
	    EnvelopeResponseHandler.getTheHandler().postAsBERTLV(SW1_RP_ACK, (byte) 1);

	    ph = ProactiveHandler.getTheHandler();
	    ph.init(PRO_CMD_SEND_USSD, (byte) 0, DEV_ID_NETWORK);
	    ph.appendTLV(TAG_ALPHA_IDENTIFIER, aiUSSDSending, (short) 0, (short) aiUSSDSending.length);
	    ph.appendTLV(TAG_USSD_STRING, ussdSub, (short) 0, (short) ussdSub.length);
	    r = ph.send();
	} else {
	    // Allowed {31.111_7.3.2.2}
	    EnvelopeResponseHandler.getTheHandler().postAsBERTLV(SW1_RP_ACK, (byte) 0);
	    Debug.displayByte((byte) 0x81);
	}
    }
}
