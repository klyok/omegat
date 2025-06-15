package org.omegat.gui.matches;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import org.omegat.core.Core;
import org.omegat.core.matching.NearString;
import org.omegat.gui.editor.autocompleter.AutoCompleterItem;
import org.omegat.gui.editor.autocompleter.AutoCompleterListView;
import org.omegat.util.OStrings;
import org.omegat.util.Preferences;
import org.omegat.tokenizer.ITokenizer;

/**
 * Auto-completion view offering suggestions from fuzzy match results.
 */
public class FuzzyMatchesAutoCompleterView extends AutoCompleterListView {

    public FuzzyMatchesAutoCompleterView() {
        super(OStrings.getString("AC_FUZZY_VIEW"));
    }

    @Override
    public List<AutoCompleterItem> computeListData(String prevText, boolean contextualOnly) {
        MatchesTextArea matcher = (MatchesTextArea) Core.getMatcher();
        List<NearString> matches = matcher.getDisplayedMatches();
        if (matches.isEmpty()) {
            return Collections.emptyList();
        }
        String token = getLastToken(prevText);
        List<AutoCompleterItem> result = new ArrayList<>();
        Set<String> added = new HashSet<>();
        for (NearString ns : matches) {
            String trans = ns.translation;
            String[] words = getTokenizer().tokenizeWordsToStrings(trans, ITokenizer.StemmingMode.NONE);
            for (String word : words) {
                if (added.contains(word)) {
                    continue;
                }
                if (contextualOnly) {
                    if (!token.isEmpty() && word.startsWith(token) && !word.equals(token)) {
                        result.add(new AutoCompleterItem(word, null, token.length()));
                        added.add(word);
                    }
                } else {
                    result.add(new AutoCompleterItem(word, null, 0));
                    added.add(word);
                }
            }
        }
        return result;
    }

    @Override
    public String itemToString(AutoCompleterItem item) {
        return item.payload;
    }

    @Override
    protected boolean isEnabled() {
        return Preferences.isPreferenceDefault(Preferences.AC_FUZZY_MATCH_ENABLED, true);
    }
}
