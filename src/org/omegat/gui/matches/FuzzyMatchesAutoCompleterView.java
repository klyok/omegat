package org.omegat.gui.matches;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.omegat.core.Core;
import org.omegat.core.matching.NearString;
import org.omegat.gui.editor.autocompleter.AutoCompleterItem;
import org.omegat.gui.editor.autocompleter.AutoCompleterListView;
import org.omegat.util.OStrings;
import org.omegat.util.Preferences;
import org.omegat.tokenizer.ITokenizer.StemmingMode;

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
        Set<String> seen = new HashSet<>();
        for (NearString ns : matches) {
            String[] words = getTokenizer().tokenizeWordsToStrings(ns.translation, StemmingMode.NONE);
            for (String w : words) {
                if (w.trim().isEmpty() || !seen.add(w)) {
                    continue;
                }
                if (contextualOnly) {
                    if (!token.isEmpty() && w.startsWith(token) && !w.equals(token)) {
                        result.add(new AutoCompleterItem(w, null, token.length()));
                    }
                } else {
                    result.add(new AutoCompleterItem(w, null, 0));
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
