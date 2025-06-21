package org.omegat.core.statistics;

import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.omegat.core.data.IProject;
import org.omegat.core.data.ProjectProperties;
import org.omegat.core.statistics.FindMatches;
import org.omegat.core.statistics.FindMatchesTest.TestProject;
import org.omegat.core.matching.NearString;
import org.omegat.core.segmentation.Segmenter;
import org.omegat.tokenizer.DefaultTokenizer;
import org.omegat.tokenizer.LuceneEnglishTokenizer;
import org.omegat.util.Preferences;
import org.omegat.util.TestPreferencesInitializer;

public class FindMatchesLargeTMXTest {
    private Path tempDir;
    private Path tmx;

    @Before
    public void setup() throws Exception {
        tempDir = Files.createTempDirectory("largeTM");
        tmx = Files.createTempFile("large", ".tmx");
        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(tmx, StandardCharsets.UTF_8))) {
            pw.println("<?xml version='1.0' encoding='UTF-8'?>");
            pw.println("<!DOCTYPE tmx SYSTEM 'tmx14.dtd'>");
            pw.println("<tmx version='1.4'>");
            pw.println("<header creationtool='OmegaT' creationtoolversion='test' segtype='sentence' adminlang='en' srclang='en' datatype='PlainText'/>");
            pw.println("<body>");
            for (int i = 0; i < 10000; i++) {
                pw.println("<tu><tuv xml:lang='en'><seg>src" + i + "</seg></tuv><tuv xml:lang='en'><seg>trg" + i + "</seg></tuv></tu>");
            }
            pw.println("</body></tmx>");
        }
    }

    @After
    public void tearDown() throws Exception {
        Files.deleteIfExists(tmx);
        Files.deleteIfExists(tempDir);
    }

    @Test
    public void testLargeTmxSearch() throws Exception {
        TestPreferencesInitializer.init();
        Preferences.setPreference(Preferences.TM_SEARCH_PARALLEL, true);
        Segmenter seg = new Segmenter(Preferences.getSRX());
        ProjectProperties prop = new ProjectProperties(tempDir.toFile());
        prop.setSourceLanguage("en");
        prop.setTargetLanguage("en");
        prop.setSupportDefaultTranslations(false);
        IProject project = new FindMatchesTest.TestProject(prop, null, tmx.toFile(), new LuceneEnglishTokenizer(), new DefaultTokenizer(), seg);
        FindMatches finder = new FindMatches(project, seg, 5, false, 50);
        List<NearString> res = finder.search("src1", false, () -> false);
        assertFalse(res.isEmpty());
    }
}
