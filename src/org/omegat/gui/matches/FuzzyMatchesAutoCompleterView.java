package org.omegat.gui.matches;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.omegat.core.Core;
import org.omegat.core.matching.NearString;
import org.omegat.gui.editor.autocompleter.AutoCompleterItem;
import org.omegat.gui.editor.autocompleter.AutoCompleterListView;
import org.omegat.util.OStrings;
import org.omegat.util.Preferences;

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
        for (NearString ns : matches) {
            String trans = ns.translation;
            if (contextualOnly) {
                if (!token.isEmpty() && trans.startsWith(token) && !trans.equals(token)) {
                    result.add(new AutoCompleterItem(trans, null, token.length()));
                }
            } else {
                result.add(new AutoCompleterItem(trans, null, 0));
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
