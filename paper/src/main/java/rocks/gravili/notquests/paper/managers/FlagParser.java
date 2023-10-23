package rocks.gravili.notquests.paper.managers;

import java.util.Map;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class FlagParser {
    private static final String FLAG_PATTERN = "--[a-zA-Z]+ [a-zA-Z0-9%]+";

    public static Map<String, String> parseFlags(String stringToParse) {
        return Pattern.compile(FLAG_PATTERN)
                .matcher(stringToParse)
                .results()
                .map(MatchResult::group)
                .toList()
                .stream()
                .collect(
                        Collectors.toMap(flag -> flag.split(" ")[0].replace("-", ""),
                        flag -> flag.split(" ")[1].replace("-", ""))
                );
    }
}
