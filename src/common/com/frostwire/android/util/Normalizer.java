/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011, 2012, FrostWire(TM). All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.frostwire.android.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.util.Log;

/**
 * Provides normalization functions according to
 * <a href="http://www.unicode.org/unicode/reports/tr15/tr15-23.html">Unicode Standard Annex #15:
 * Unicode Normalization Forms</a>. Normalization can decompose and compose
 * characters for equivalency checking.
 * 
 * This class internally use the java.text.Normalizer using reflection (it exists in Froyo as a
 * non public class).

 * @author gubatron
 * @author aldenml
 *
 */
public final class Normalizer {

    private static final String TAG = "FW.Normalizer";

    private static Method isNormalizedMethod;
    private static Method normalizeMethod;
    private static Class<?> formEnum;

    static {
        try {
            Class<?> c = Class.forName("java.text.Normalizer");
            for (Method m : c.getDeclaredMethods()) {
                if (m.getName().startsWith("isNormalized")) {
                    isNormalizedMethod = m;
                }
                if (m.getName().startsWith("normalize")) {
                    normalizeMethod = m;
                }
            }
            formEnum = Class.forName("java.text.Normalizer$Form");
        } catch (Throwable e) {
            Log.w(TAG, "Can't use internal normalizer: java.text.Normalizer", e);
        }
    }

    /**
     * The normalization forms supported by the Normalizer. These are specified in
     * <a href="http://www.unicode.org/unicode/reports/tr15/tr15-23.html">Unicode Standard
     * Annex #15</a>.
     */
    public static enum Form {
        /**
         * Normalization Form D - Canonical Decomposition.
         */
        NFD,

        /**
         * Normalization Form C - Canonical Decomposition, followed by Canonical Composition.
         */
        NFC,

        /**
         * Normalization Form KD - Compatibility Decomposition.
         */
        NFKD,

        /**
         * Normalization Form KC - Compatibility Decomposition, followed by Canonical Composition.
         */
        NFKC;
    }

    /**
     * Check whether the given character sequence <code>src</code> is normalized
     * according to the normalization method <code>form</code>.
     *
     * @param src character sequence to check
     * @param form normalization form to check against
     * @return true if normalized according to <code>form</code>
     */
    public static boolean isNormalized(CharSequence src, Form form) {
        try {
            return (Boolean) isNormalizedMethod.invoke(null, src, getForm(form));
        } catch (Throwable e) {
            Log.e(TAG, "Internal normalizer nor working: isNormalized returns fake true");
            return true;
        }
    }

    /**
     * Normalize the character sequence <code>src</code> according to the
     * normalization method <code>form</code>.
     *
     * @param src character sequence to read for normalization
     * @param form normalization form
     * @return string normalized according to <code>form</code>
     */
    public static String normalize(String src, Form form) {
        try {
            return (String) normalizeMethod.invoke(null, src, getForm(form));
        } catch (Throwable e) {
            Log.e(TAG, "Internal normalizer nor working: normalize returns the very same string");
            return src;
        }
    }

    private Normalizer() {
    }

    private static Object getForm(Form form) throws IllegalArgumentException, SecurityException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        return Enum.class.getDeclaredMethod("valueOf", Class.class, String.class).invoke(null, formEnum, ((Enum<?>) form).name());
    }
}
