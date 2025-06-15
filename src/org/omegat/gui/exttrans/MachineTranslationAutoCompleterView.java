package org.omegat.gui.exttrans;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import org.omegat.core.Core;
import org.omegat.gui.editor.autocompleter.AutoCompleterItem;
import org.omegat.gui.editor.autocompleter.AutoCompleterListView;
import org.omegat.util.OStrings;
import org.omegat.util.Preferences;
import org.omegat.tokenizer.ITokenizer;

/**
 * Auto-completion view providing suggestions from machine translation results.
 */
public class MachineTranslationAutoCompleterView extends AutoCompleterListView {

    public MachineTranslationAutoCompleterView() {
        super(OStrings.getString("AC_MT_VIEW"));
    }

    @Override
    public List<AutoCompleterItem> computeListData(String prevText, boolean contextualOnly) {
        List<MachineTranslationInfo> infos = Core.getMachineTranslatePane().getDisplayedTranslations();
        if (infos == null || infos.isEmpty()) {
            return Collections.emptyList();
        }
        String token = getLastToken(prevText);
        List<AutoCompleterItem> result = new ArrayList<>();
        Set<String> added = new HashSet<>();
        for (MachineTranslationInfo info : infos) {
            String tr = info.result;
            String[] words = getTokenizer().tokenizeWordsToStrings(tr, ITokenizer.StemmingMode.NONE);
            for (String word : words) {
                if (added.contains(word)) {
                    continue;
                }
                if (contextualOnly) {
                    if (!token.isEmpty() && word.startsWith(token) && !word.equals(token)) {
                        result.add(new AutoCompleterItem(word, new String[] { info.translatorName }, token.length()));
                        added.add(word);
                    }
                } else {
                    result.add(new AutoCompleterItem(word, new String[] { info.translatorName }, 0));
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
        return Preferences.isPreferenceDefault(Preferences.AC_MACHINETRANSLATION_ENABLED, true);
    }
}
