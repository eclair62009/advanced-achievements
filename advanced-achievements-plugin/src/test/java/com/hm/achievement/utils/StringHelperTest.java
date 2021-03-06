package com.hm.achievement.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Class for testing the text helpers.
 *
 * @author Pyves
 */
class StringHelperTest {

	@Test
	void shouldRemoveFormattingCodes() {
		String result = StringHelper.removeFormattingCodes("&0&1§2&3&4&5&6&7&8&9This &a&b§c&d&eis&f &ksome&l&m&n&o te&rxt!");

		assertEquals("This is some text!", result);
	}

	@Test
	void shouldNotRemoveInvalidFormattingCodes() {
		String result = StringHelper.removeFormattingCodes("Incorrect formatting codes: &h& z");

		assertEquals("Incorrect formatting codes: &h& z", result);
	}

	@Test
	void shouldReturnClosestMatchingString() {
		List<String> possibleMatches = Arrays.asList("nothing", "something", "random text", "amasing");
		String result = StringHelper.getClosestMatch("somaeThing", possibleMatches);

		assertEquals("something", result);
	}

}
