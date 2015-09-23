package exercise1Test;

import static org.junit.Assert.*;
import org.junit.Test;
import exercise1.Exercise1;

public class Exercise1Test {

	@Test
	public void tryMake_TP_DA() {
		byte[] enteredNumber1 = { '8', '9' };
		byte[] convertedNumber = new byte[8];
		int converted;

		converted = Exercise1.Make_TP_DA(convertedNumber, (short) 0, enteredNumber1, (short) 0,
				(short) enteredNumber1.length);
		System.out.println("Converted = " + converted);
		if (converted >= 0) {
			String enteredNumber1S = new String(enteredNumber1);
			System.out.println(enteredNumber1S);
			for (byte i = 0; i < converted + 1; ++i)
				System.out.format("%02X", convertedNumber[i]);
		}
	}

	@Test
	public void testMake_TP_DA() {
		short convertedLen;

		byte[] enteredNumber1 = { '8', '9' };
		byte[] expectedNumber1 = { (byte) 0x02, (byte) 0x81, (byte) 0x98 };
		byte[] actualNumber1 = new byte[expectedNumber1.length];

		byte[] enteredNumber2 = { '+', '8', '9' };
		byte[] expectedNumber2 = { (byte) 0x02, (byte) 0x91, (byte) 0x98 };
		byte[] actualNumber2 = new byte[expectedNumber2.length];

		byte[] enteredNumber3 = { '8', '9', '*', '2', '#' };
		byte[] expectedNumber3 = { (byte) 0x04, (byte) 0x81, (byte) 0x98, (byte) 0x2A, (byte) 0xFB };
		byte[] actualNumber3 = new byte[expectedNumber3.length];

		convertedLen = Exercise1.Make_TP_DA(actualNumber1, (short) 0, enteredNumber1, (short) 0,
				(short) enteredNumber1.length);
		assertEquals(convertedLen, expectedNumber1[0]);
		assertArrayEquals("Haha", expectedNumber1, actualNumber1);

		convertedLen = Exercise1.Make_TP_DA(actualNumber2, (short) 0, enteredNumber2, (short) 0,
				(short) enteredNumber2.length);
		assertEquals(convertedLen, expectedNumber2[0]);
		assertArrayEquals("Haha", expectedNumber2, actualNumber2);

		convertedLen = Exercise1.Make_TP_DA(actualNumber3, (short) 0, enteredNumber3, (short) 0,
				(short) enteredNumber3.length);
		assertEquals(convertedLen, expectedNumber3[0]);
		assertArrayEquals("Haha", expectedNumber3, actualNumber3);
	}

	@Test
	public void tryPack_7bit() {
		short l7;
		byte[] test1 = { 'A', 'B', 'C', 'D' };
		byte[] test2 = { 's', 'c', 'i', 'w', 'b', 'c' };
		byte[] test3 = { 'A' };
		byte[] pack = new byte[20];

		l7 = Exercise1.Pack_7bit(pack, test3, (short) test3.length);
		for (int i = 0; i < l7; i++) {
			System.out.format("%02X", pack[i]);
		}
		System.out.println();
	}

	@Test
	public void testPack_7bit() {
		short l7;
		byte[] test1 = { 'A', 'B', 'C', 'D' };
		byte[] test2 = { 's', 'c', 'i', 'w', 'b', 'c' };
		byte[] test3 = { 'A' };
		byte[] pack = new byte[20];
	}
	
	@Test
	public void tryBitArithmetic1() {
		byte b = (byte) 0x81;
		short ss = b; // -127
		short us = (short) (b & 0xFF); // 129
		System.out.format("%n%d -> %d %d %n", b, ss, us);
	}
}
