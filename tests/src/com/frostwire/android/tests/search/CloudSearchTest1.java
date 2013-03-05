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

package com.frostwire.android.tests.search;

import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;
import android.test.suitebuilder.annotation.MediumTest;

import com.frostwire.search.SearchManagerImpl;
import com.frostwire.search.WebSearchPerformer;
import com.frostwire.search.isohunt.ISOHuntSearchPerformer;
import com.frostwire.search.soundcloud.SoundcloudSearchPerformer;
import com.frostwire.search.vertor.VertorSearchPerformer;
import com.frostwire.search.youtube.YouTubeSearchPerformer;

/**
 * 
 * @author gubatron
 * @author aldenml
 *
 */
public class CloudSearchTest1 extends TestCase {

    @MediumTest
    public void testSoundcloud() {
        testPerformer(new SoundcloudSearchPerformer("frostclick", 5000));
    }

    @MediumTest
    public void testYoutube() {
        testPerformer(new YouTubeSearchPerformer("frostclick", 5000));
    }

    @MediumTest
    public void testISOHunt() {
        testPerformer(new ISOHuntSearchPerformer("frostclick", 5000));
    }
    
    @MediumTest
    public void testVertor() {
        testPerformer(new VertorSearchPerformer("frostclick", 5000));
    }

    private void testPerformer(WebSearchPerformer performer) {
        MockSearchResultListener l = new MockSearchResultListener();

        SearchManagerImpl manager = new SearchManagerImpl();
        manager.registerListener(l);
        manager.perform(performer);

        assertTrue("Waiting too much time", manager.awaitIdle(30));

        assertTrue("Did not finish or took too much time", manager.shutdown(5, TimeUnit.SECONDS));

        assertTrue("More than one result", l.getNumResults() > 1);

        l.logResults();
    }
}
