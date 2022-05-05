package de.tuebingen.sfs.psl.gui;

import java.util.HashMap;
import java.util.Stack;

import javafx.scene.control.Hyperlink;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

public class AtomVerbalizationRenderer {
	public static void fillTextFlow(String verbalization, TextFlow tf, FactWindow fw) {
		HashMap<Mode, String> modeToStyle = new HashMap<>();

		Stack<State> stack = new Stack<>();
		Mode currentMode = Mode.DEFAULT;
		HashMap<String, Mode> labelToMode = new HashMap<>();
		labelToMode.put("textit", Mode.ITALIC);
		labelToMode.put("textbf", Mode.BOLD);
		labelToMode.put("textcol", Mode.COLOR);
		labelToMode.put("url", Mode.URL);
		labelToMode.put("hidden-url", Mode.HIDDEN_URL);

		modeToStyle.put(Mode.ITALIC, "-fx-font-style:italic;");
		modeToStyle.put(Mode.BOLD, "-fx-font-weight:bold;");
		modeToStyle.put(Mode.COLOR, "-fx-fill:black;");
		modeToStyle.put(Mode.NORMAL, "");

		int normIndex = 0;
		for (int i = 0; i < verbalization.length(); i++) {
			String currentChar = verbalization.charAt(i) + "";
			if (currentChar.equals("\\")) {
				int optStart = verbalization.indexOf("[", i);
				int contentStart = verbalization.indexOf("{", i);
				int cmdEnd = (optStart >= 0 && optStart < contentStart) ? optStart : contentStart;
				if (cmdEnd >= 0 && labelToMode.containsKey(verbalization.substring(i + 1, cmdEnd))) {
					String label = verbalization.substring(i + 1, cmdEnd);
					if ((stack.isEmpty() && i != 0)) {
						currentMode = Mode.NORMAL;
						String entry = verbalization.substring(normIndex, i);
						createLabel(entry, currentMode, stack, tf, modeToStyle, fw);
					} else if (!stack.isEmpty()) {
						State temp = stack.pop();
						currentMode = temp.mode;
						String entry = verbalization.substring(temp.begin, i);
						createLabel(entry, temp.mode, stack, tf, modeToStyle, fw);
						temp.begin = i + 1;
						stack.push(temp);
					}

					State state = new State();
					state.begin = i;
					state.mode = labelToMode.get(label);
					stack.push(state);
				}
			} else if (currentChar.equals("}")) {
				State temp = stack.pop();
				String entry = verbalization.substring(temp.begin, i);
				createLabel(entry, temp.mode, stack, tf, modeToStyle, fw);
				normIndex = i;
				if (!stack.isEmpty()) {
					stack.peek().begin = i + 1;
				}
			} else if (i == verbalization.length() - 1) {
				currentMode = Mode.NORMAL;
				String entry = verbalization.substring(normIndex, i + 1);
				createLabel(entry, currentMode, stack, tf, modeToStyle, fw);
			}

		}
	}

	;

	private static void createLabel(String entry, Mode mode, Stack<State> stack, TextFlow tf,
			HashMap<Mode, String> modeToStyle, FactWindow fw) {
		if (!entry.isEmpty()) {
			String optional = "";
			if (entry.startsWith("\\") && entry.contains("[") && entry.contains("]")) {
				int optStart = entry.indexOf("[") + 1;
				if (entry.charAt(optStart - 2) != '\\')
					optional = entry.substring(optStart, indexOfUnescaped(entry, ']', optStart));
			}

			if (entry.startsWith("\\") && entry.contains("{")) {
				entry = entry.substring(entry.indexOf('{') + 1);
			}

			if (entry.startsWith("}"))
				entry = entry.substring(1);

			optional = optional.replaceAll("\\\\", "");
			entry = entry.replaceAll("\\\\", "");

			if (mode.equals(Mode.COLOR)) {
				modeToStyle.put(mode, "-fx-fill:" + optional + ";");
			}

			if (!entry.isEmpty()) {
				if (mode.equals(Mode.URL) || mode.equals(Mode.HIDDEN_URL)) {
					String linkEntry = entry;
					String linkText = (optional.isEmpty()) ? entry : optional;
					Hyperlink link = new Hyperlink(linkText);
					tf.getChildren().add(link);
					link.setOnAction(event -> {
						fw.onFormSelection(linkEntry);
					});

				} else {
					Text text = new Text(entry);
					tf.getChildren().add(text);
					StringBuilder styles = new StringBuilder(modeToStyle.get(mode));
					if (!stack.isEmpty()) {
						for (State st : stack) {
							styles.append(modeToStyle.get(st.mode));
						}
					}
//					text.setFont(Font.font("Verdana", FontWeight.NORMAL, 14));
					text.setStyle(styles + "");
				}
			}
		}
	}

	private static int indexOfUnescaped(String s, char c, int start) {
		boolean escape = false;
		for (int i = start; i < s.length(); i++) {
			if (s.charAt(i) == '\\')
				escape = true;
			else {
				if (s.charAt(i) == c && !escape)
					return i;
				else
					escape = false;
			}
		}
		return -1;
	}

	public enum Mode {
		ITALIC, BOLD, COLOR, NORMAL, DEFAULT, URL, HIDDEN_URL
	}

	static class State {
		int begin;
		Mode mode;
	}
}
