package exercise1;

import javacard.framework.*;
import sim.toolkit.*;

public class Debug implements ToolkitConstants {

	private static final byte[] hexDigets = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E',
			'F' };
	private static final short manyBytes_max_length = (byte) 368;

	private static byte[] oneByte;
	private static byte[] manyBytes;
	private static short manyBytes_length;

	private static void convertBytes(byte b) {
		short bb = (short) (b & 0xFF);
		oneByte[0] = hexDigets[bb >> 4];
		oneByte[1] = hexDigets[bb & 0x0F];
	}

	private static void convertBytes(byte[] b, short b_length) {
		short mbi = 0;
		for (short bi = 0; (bi < b_length) && (mbi < manyBytes_max_length); bi++) {
			short bb = (short) (b[bi] & 0xFF);
			manyBytes[mbi++] = hexDigets[bb >> 4];
			manyBytes[mbi++] = hexDigets[bb & 0x0F];
		}
		manyBytes_length = mbi;
	}

	public Debug() {
		oneByte = JCSystem.makeTransientByteArray((short) 2, JCSystem.CLEAR_ON_RESET);
		manyBytes = JCSystem.makeTransientByteArray(manyBytes_max_length, JCSystem.CLEAR_ON_RESET);
	}

	static void displayByte(byte b) {
		convertBytes(b);
		ProactiveHandler ph = ProactiveHandler.getTheHandler();
		ph.initDisplayText((byte) 0x80, DCS_8_BIT_DATA, oneByte, (short) 0, (short) 2);
		ph.send();
	}

	static void displayByte(byte[] b, short b_length) {
		convertBytes(b, b_length);
		ProactiveHandler ph = ProactiveHandler.getTheHandler();
		ph.initDisplayText((byte) 0x80, DCS_8_BIT_DATA, manyBytes, (short) 0, manyBytes_length);
		ph.send();
	}
}
