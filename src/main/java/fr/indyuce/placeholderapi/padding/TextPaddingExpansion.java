package fr.indyuce.placeholderapi.padding;

import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.Configurable;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.clip.placeholderapi.expansion.Taskable;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TextPaddingExpansion extends PlaceholderExpansion implements Configurable, Taskable {

    @NotNull
    public String getAuthor() {
        return "Indyuce";
    }

    @Override
    @NotNull
    public String getIdentifier() {
        return "pad";
    }

    @Override
    @NotNull
    public String getVersion() {
        return "1.0";
    }

    //region Read from config

    private int defaultCharSize;
    private int negativeSpaceBaseChar;
    private Map<Character, Integer> charSizeExceptions;

    @Override
    public void start() {
        negativeSpaceBaseChar = getInt("NegativeSpaceBaseChar", 0xD0000);
        defaultCharSize = getInt("DefaultCharSize", 5);
        charSizeExceptions = getStringList("CharSizeExceptions").stream().map(String::toCharArray)
                .collect(Collectors.toMap(arr -> arr[0], arr -> Integer.parseInt(String.valueOf(arr[1]))));
    }

    @Override
    public void stop() {
        // Nothing to do
    }

    @Override
    public Map<String, Object> getDefaults() {
        return Map.of("DefaultCharSize", 5,
                "NegativeSpaceBaseChar", 0xD0000,
                "CharSizeExceptions", List.of("f4", "i1", "k4", "l2", "t3", "I3", "'1", " 3", ",1", "(3", ")3"));
    }

    //endregion

    private String setPlaceholders(OfflinePlayer player, String text) {
        return PlaceholderAPI.setBracketPlaceholders(player, text).replace("[prc]", "%");
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {

        // Place text in the middle
        if (params.startsWith("center_")) {
            final var split = params.substring(7).split("_", 2);
            final var targetPadding = Integer.parseInt(split[0]); // Target size of padded area
            final var text = setPlaceholders(player, split[1]); // Text to pad

            final var textLength = approximateLength(text);
            final var effectivePadding = Math.max(0, targetPadding - textLength);

            final var leftPadding = effectivePadding / 2;
            final var rightPadding = effectivePadding - leftPadding;
            return getSpaceFont(leftPadding) + text + getSpaceFont(rightPadding);
        }

        // Push text to the left
        if (params.startsWith("left_")) {
            final var split = params.substring(5).split("_", 2);
            final var targetPadding = Integer.parseInt(split[0]); // Target size of padded area
            final var text = setPlaceholders(player, split[1]); // Text to pad

            final var textLength = approximateLength(text);
            final var effectivePadding = Math.max(0, targetPadding - textLength);
            return text + getSpaceFont(effectivePadding);
        }

        // Push text to the right
        // pad_right_70_......
        if (params.startsWith("right_")) {
            final var split = params.substring(6).split("_", 2);
            final var targetPadding = Integer.parseInt(split[0]); // Target size of padded area
            final var text = setPlaceholders(player, split[1]); // Text to pad

            final var textLength = approximateLength(text);
            final var effectivePadding = Math.max(0, targetPadding - textLength);
            return getSpaceFont(effectivePadding) + text;
        }

        // Place text in the middle
        // pad_centerl_70_10_......
        if (params.startsWith("centerl_")) {
            final var split = params.substring(8).split("_", 3);
            final var targetPadding = Integer.parseInt(split[0]); // Target size of padded area
            final var textLength = Integer.parseInt(split[1]); // User provided text length !!!!!!
            final var text = setPlaceholders(player, split[2]); // Text to pad

            final var effectivePadding = Math.max(0, targetPadding - textLength);
            final var leftPadding = effectivePadding / 2;
            final var rightPadding = effectivePadding - leftPadding;
            return getSpaceFont(leftPadding) + text + getSpaceFont(rightPadding);
        }

        if (params.startsWith("space_")) {
            return getSpaceFont(Integer.parseInt(params.substring(6)));
        }

        if (params.startsWith("len_")) {
            final var text = setPlaceholders(player, params.substring(9));
            return String.valueOf(approximateLength(text));
        }

        return "PlaceholderNotFound";
    }

    /**
     * Size in between two characters, it seems to me like it's
     * the same whatever the resource pack. Not the size of a space
     */
    private static final int SEPARATOR_SIZE = 1;

    /**
     * Linear state machine that tries to approximate the length of a string
     * inside a tooltip. This is needed to then compute how many spaces are
     * required to center the item name at the middle of the lore tooltip.
     *
     * @param input String input
     * @return Approximate length of string input
     */
    private int approximateLength(@NotNull String input) {

        var length = 0;
        var _notEmpty = false;
        for (char next : input.toCharArray()) {
            if (_notEmpty) length += SEPARATOR_SIZE;
            _notEmpty = true;

            // TODO ignore color codes

            length += getCharSize(next);
        }
        return length;
    }

    private int getCharSize(char character) {
        @Nullable final var found = this.charSizeExceptions.get(character);
        return found != null ? found : this.defaultCharSize;
    }

    /**
     * Uses character convention from <a href="https://github.com/AmberWat/NegativeSpaceFont">this Github</a>
     *
     * @param width Target width in pixels of positive/negative space
     * @return String containing negative font with given size
     */
    @NotNull
    private String getSpaceFont(int width) {
        if (width < -8192 || width > 8192) throw new IllegalArgumentException("Size must be between -8192 and 8192");
        if (width == 0) return ""; // Easyyy
        final int codePoint = this.negativeSpaceBaseChar + width;
        return new String(Character.toChars(codePoint));
    }

    //endregion
}
