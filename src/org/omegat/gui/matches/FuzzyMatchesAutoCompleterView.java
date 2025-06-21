package org.omegat.gui.matches;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Locale;

import org.omegat.core.Core;
import org.omegat.core.matching.NearString;
import org.omegat.gui.editor.autocompleter.AutoCompleterItem;
import org.omegat.gui.editor.autocompleter.AutoCompleterListView;
import org.omegat.gui.editor.autocompleter.AutoCompleter;
import org.omegat.util.OStrings;
import org.omegat.util.Preferences;
import org.omegat.util.StringUtil;
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
        List<AutoCompleterItem> contextual = new ArrayList<>();
        List<AutoCompleterItem> all = new ArrayList<>();
        Set<String> seenContext = new HashSet<>();
        Set<String> seenAll = new HashSet<>();
        Locale locale = getTargetLocale();
        for (NearString ns : matches) {
            String trans = ns.translation;
            String[] words = getTokenizer().tokenizeWordsToStrings(trans, ITokenizer.StemmingMode.NONE);
            for (String word : words) {
                if (seenAll.add(word.toLowerCase(locale))) {
                    all.add(new AutoCompleterItem(word, null, 0));
                }
                if (!token.isEmpty() && word.startsWith(token) && !word.equals(token)) {
                    if (seenContext.add(word.toLowerCase(locale))) {
                        String payload = StringUtil.matchCapitalization(word, token, locale);
                        contextual.add(new AutoCompleterItem(payload, null, token.length()));
                    }
                }
            }
        }

        if (!contextual.isEmpty() || contextualOnly) {
            return contextual;
        }
        return all;
    }

    @Override
    public String itemToString(AutoCompleterItem item) {
        return item.payload;
    }

    @Override
    protected boolean isEnabled() {
        return Preferences.isPreferenceDefault(Preferences.AC_FUZZY_MATCH_ENABLED, true);
    }

    @Override
    public boolean shouldPopUp() {
        String leadingText = getLeadingText();
        List<AutoCompleterItem> entries = computeListData(leadingText, true);
        return !entries.isEmpty()
                && (leadingText.codePointCount(0, leadingText.length()) > 1
                        || entries.size() <= AutoCompleter.PAGE_ROW_COUNT);
    }

    private Locale getTargetLocale() {
        return getTargetLanguage().getLocale();
    }
}
