package elkaproj.config.language;

import elkaproj.DebugWriter;

import java.io.File;
import java.io.FileFilter;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses language files.
 */
public class LanguageLoader {

    private static final Pattern LANGUAGE_REGEX = Pattern.compile("^strings\\.(?<code>[a-z]{2}-[A-Z]{2})\\.txt$");
    private static final String LANGUAGE_FILE_PATTERN = "strings.%s.txt";

    private final Set<String> availableLanguages;
    private final Class<?> resourceClass;

    /**
     * Creates a new language loader.
     *
     * @param resourceClass Class to use for loading resources.
     */
    public LanguageLoader(Class<?> resourceClass) {
        this.resourceClass = resourceClass;
        this.availableLanguages = this.getAvailableLanguages();
        for (String langCode : this.availableLanguages) {
            DebugWriter.INSTANCE.logMessage("LANG-PRSR", "Available language: %s", langCode);
        }
    }

    /**
     * Loads specified language.
     *
     * @param code Language code of the language to load.
     * @return Loaded language.
     */
    public Language loadLanguage(String code) {
        if (!this.availableLanguages.contains(code))
            throw new IllegalArgumentException("Specified language is not available.");

        String languageFileName = String.format(LANGUAGE_FILE_PATTERN, code);
        try {
            InputStream languageFile = this.resourceClass.getResourceAsStream("/" + languageFileName);
            try (LanguageParser parser = new LanguageParser(languageFile)) {
                return parser.parse();
            }
        } catch (IllegalStateException | IllegalArgumentException ex) {
            DebugWriter.INSTANCE.logError("LANG-LDR", ex, "Failed to load language %s.", code);
            throw ex;
        } catch (Exception ex) {
            DebugWriter.INSTANCE.logError("LANG-LDR", ex, "Failed to load language %s.", code);
            return null;
        }
    }

    /**
     * Lists all available languages.
     *
     * @return A list of available languages, as language codes.
     */
    public Set<String> getAvailableLanguages() {
        HashSet<String> languages = new HashSet<>();

        File jarFile = new File(this.resourceClass
                .getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .getPath());

        if (jarFile.isFile()) { // compiled jar
            DebugWriter.INSTANCE.logMessage("LANG", "Using JAR scanner");

            try (JarFile jar = new JarFile(jarFile)) {
                Enumeration<JarEntry> jarEntries = jar.entries();
                while (jarEntries.hasMoreElements()) {
                    JarEntry jarEntry = jarEntries.nextElement();
                    Matcher m = LANGUAGE_REGEX.matcher(jarEntry.getName());
                    if (!m.matches())
                        continue;

                    languages.add(m.group("code"));
                }
            } catch (Exception ex) {
                DebugWriter.INSTANCE.logError("LANG", ex, "Cannot process jar file");

                languages.clear();
                return languages;
            }
        } else { // classpath directory
            DebugWriter.INSTANCE.logMessage("LANG", "Using classpath scanner");

            URL url = this.resourceClass.getResource("/.res");
            if (url != null) {
                try {
                    final File root = new File(url.toURI()).getParentFile();
                    for (File langdef : Objects.requireNonNull(root.listFiles(new LanguageFileFilter()))) {
                        Matcher m = LANGUAGE_REGEX.matcher(langdef.getName());
                        //noinspection ResultOfMethodCallIgnored
                        m.matches();
                        languages.add(m.group("code"));
                    }
                } catch (Exception ex) {
                    DebugWriter.INSTANCE.logError("LANG", ex, "Cannot process classpath file");

                    languages.clear();
                    return languages;
                }
            }
        }

        return languages;
    }

    private static class LanguageFileFilter implements FileFilter {
        @Override
        public boolean accept(File file) {
            return file.isFile() && LANGUAGE_REGEX.matcher(file.getName()).matches();
        }
    }
}
