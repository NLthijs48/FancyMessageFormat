package me.wiefferink.interactivemessenger.testing.parsergenerator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import junit.framework.TestCase;
import me.wiefferink.interactivemessenger.generators.ConsoleGenerator;
import me.wiefferink.interactivemessenger.generators.TellrawGenerator;
import me.wiefferink.interactivemessenger.message.InteractiveMessage;
import me.wiefferink.interactivemessenger.parsers.YamlParser;
import me.wiefferink.interactivemessenger.testing.Log;
import me.wiefferink.interactivemessenger.testing.RunTests;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ParserGeneratorTestCase extends TestCase {

	private File file;
	private static final Gson gson = new GsonBuilder().setPrettyPrinting().setLenient().create();

	public ParserGeneratorTestCase(File file) {
		super();
		// Cleanup .txt from the name
		String name = file.getName();
		if(name.endsWith(".txt") && name.length() > 4) {
			name = name.substring(0, name.length()-4);
		}
		setName(name);
		this.file = file;
	}

	@Override
	protected void runTest() {
		Log.info("\n");
		Log.info("┌───────────────────────────────────────────────────────────────────────────────");
		Log.info("│ Test:", RunTests.getName(file));
		Log.info("└───────────────────────────────────────────────────────────────────────────────");

		// Read input and output from the file
		List<String> input = new ArrayList<>();
		StringBuilder expectedTellrawOutputBuilder = null;
		StringBuilder expectedConsoleOutputBuilder = null;
		boolean canIncrement = false;
		int state = 0; // 0: reading input,
		try(BufferedReader fileReader = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
			String line;
			while((line = fileReader.readLine()) != null) {
				// (part of) empty line barrier
				if(line.isEmpty()) {
					if(canIncrement) {
						state++;
						canIncrement = false;
					}
					continue;
				}

				canIncrement = true;
				// Input line
				if(state == 0) {
					input.add(line);
				}
				// Expected tellraw output line
				else if(state == 1) {
					if(expectedConsoleOutputBuilder == null) {
						expectedConsoleOutputBuilder = new StringBuilder();
					} else {
						expectedConsoleOutputBuilder.append("\n");
					}
					expectedConsoleOutputBuilder.append(line);
				}
				// Expected console output line
				else if(state == 2) {
					if(expectedTellrawOutputBuilder == null) {
						expectedTellrawOutputBuilder = new StringBuilder();
					} else {
						expectedTellrawOutputBuilder.append("\n");
					}
					expectedTellrawOutputBuilder.append(line);
				}
			}
		} catch(IOException e) {
			error("Failed to read file: "+ExceptionUtils.getStackTrace(e));
		}

		String expectedConsoleOutput = expectedConsoleOutputBuilder == null ? "" : expectedConsoleOutputBuilder.toString();
		String expectedTellrawOutput = expectedTellrawOutputBuilder == null ? "" : expectedTellrawOutputBuilder.toString();

		// Check if the input is defined (don't check output to make it easy to add tests, assertEquals will fail anyway)
		if(input.isEmpty()) {
			error("Input is not defined");
			return;
		}

		Log.info("  Input:");
		Log.printIndented(2, input);
		Log.info("  Expected ConsoleGenerator output:");
		Log.printIndented(2, expectedConsoleOutput);
		Log.info("  Expected TellrawGenerator output:");
		Log.printIndented(2, expectedTellrawOutput);

		// Parse into InteractiveMessage
		InteractiveMessage parsedMessage = YamlParser.parse(input);
		Log.info("  Parsed InteractiveMessage:", parsedMessage);

		// Generate result of ConsoleGenerator
		String actualConsoleOutput = ConsoleGenerator.generate(parsedMessage);
		Log.info("  Generated by ConsoleGenerator:");
		Log.printIndented(2, actualConsoleOutput);

		// Generate result of TellrawGenerator
		List<String> actualTellrawOutputList = TellrawGenerator.generate(parsedMessage);
		String actualTellrawOutputString = "["+StringUtils.join(actualTellrawOutputList, ",")+"]";

		// Parse actual output into Json
		JsonElement actualTellrawOutputJson;
		try {
			actualTellrawOutputJson = gson.fromJson(actualTellrawOutputString, JsonElement.class);
		} catch(JsonSyntaxException e) {
			error("Generated Json output is invalid:", actualTellrawOutputString, "\n\n"+ExceptionUtils.getStackTrace(e));
			return;
		}
		Log.info("  Generated by TellrawGenerator:");
		Log.printIndented(2, gson.toJson(actualTellrawOutputJson));

		// Parse expected output into Json
		JsonElement expectedTellrawOutputJson;
		try {
			expectedTellrawOutputJson = gson.fromJson(expectedTellrawOutput, JsonElement.class);
		} catch(JsonSyntaxException e) {
			error("Expected output is invalid Json:", ExceptionUtils.getStackTrace(e));
			return;
		}

		// Test if equal
		assertEquals(expectedConsoleOutput, actualConsoleOutput);
		assertEquals(gson.toJson(expectedTellrawOutputJson), gson.toJson(actualTellrawOutputJson));
	}

	/**
	 * Fail the test and print the error message
	 * @param parts The message parts
	 */
	private void error(Object... parts) {
		Log.error(parts);
		fail(StringUtils.join(parts, " "));
	}

}