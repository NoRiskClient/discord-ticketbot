package gg.norisk.ticketbot.util;

import java.util.Locale;
import java.util.ResourceBundle;

public class TranslationUtils {
  private static final Locale DEFAULT_LOCALE = Locale.ENGLISH;

  public static String translate(String key, Locale locale) {
    try {
      return ResourceBundle.getBundle("translations", locale).getString(key);
    } catch (Exception e) {
      try {
        return ResourceBundle.getBundle("translations", DEFAULT_LOCALE).getString(key);
      } catch (Exception ex) {
        return key;
      }
    }
  }
}
