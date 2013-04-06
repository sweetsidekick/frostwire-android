/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011, 2012, FrostWire(R). All rights reserved.
 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.frostwire.android.tests.misc;

import java.util.Formatter;
import java.util.Locale;

import android.test.ApplicationTestCase;
import android.test.mock.MockApplication;
import android.test.suitebuilder.annotation.LargeTest;
import android.widget.TextView;

/**
 * 
 * @author gubatron
 * @author aldenml
 *
 */
public class MemLeakTest extends ApplicationTestCase<MockApplication> {

    private StringBuilder formatBuilder;
    private Formatter formatter;
    private Runtime runtime;
    private final int MB = 1024 * 1024;
    private TextView textView;

    public MemLeakTest() {
        this(MockApplication.class);
    }

    public MemLeakTest(Class<MockApplication> applicationClass) {
        super(applicationClass);
        try {
            textView = new TextView(getContext());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        formatBuilder = new StringBuilder();
        formatter = new Formatter(formatBuilder, Locale.getDefault());
        runtime = Runtime.getRuntime();
    }

    @LargeTest
    public void testStringForTimeLeak() {
        int jumps = 100000;
        for (int i = Integer.MIN_VALUE; i < Integer.MAX_VALUE; i += jumps) {
            String s = stringForTime(i);
            if (textView != null) {
                textView.setText(s);
            }
            if (i % 20000000 == 0) {
                System.out.println(i);
                System.out.println(s);
                System.out.println(textView == null);
                System.out.println("Used memory: " + (runtime.totalMemory() - runtime.freeMemory()) / MB);
                System.out.println("Free memory: " + runtime.freeMemory() / MB);
                System.out.println("String Builder length: " + formatBuilder.length());
                System.out.println("================================================================");
            }
        }
    }

    private String stringForTime(int timeMs) {
        int totalSeconds = timeMs / 1000;

        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;

        formatBuilder.setLength(0);
        //formatBuilder.trimToSize();

        if (hours > 0) {
            return formatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return formatter.format("%02d:%02d", minutes, seconds).toString();
        }
    }
}