package me.suhsaechan.suhprojectutility.util;

import java.util.Random;

public class CommonUtil {
	private final Random random = new Random();

	private int getRandomNumber(int min, int max) {
		if (min == max) {
			return min;
		}
		return random.nextInt(max - min + 1) + min;
	}
}
