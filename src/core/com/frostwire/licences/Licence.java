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

package com.frostwire.licences;

import java.util.Arrays;
import java.util.List;

/**
 * 
 * @author gubatron
 * @author aldenml
 *
 */
public abstract class Licence {

    public static final Licence UNKNOWN = new UnknownLicence();

    public static final Licence CC_BY = new CCLicence("CC BY", "Creative Commons Attribution", "http://creativecommons.org/licenses/by/3.0");
    public static final Licence CC_BY_SA = new CCLicence("CC BY-SA", "Creative Commons Attribution-ShareAlike", "http://creativecommons.org/licenses/by-sa/3.0");
    public static final Licence CC_BY_ND = new CCLicence("CC BY-ND", "Creative Commons Attribution-NoDerivs", "http://creativecommons.org/licenses/by-nd/3.0");
    public static final Licence CC_BY_NC = new CCLicence("CC BY-NC", "Creative Commons Attribution-NonCommercial", "http://creativecommons.org/licenses/by-nc/3.0");
    public static final Licence CC_BY_NC_SA = new CCLicence("CC BY", "Creative Commons Attribution-NonCommercial-ShareAlike", "http://creativecommons.org/licenses/by-nc-sa/3.0");
    public static final Licence CC_BY_NC_ND = new CCLicence("CC BY", "Creative Commons Attribution-NonCommercial-NoDerivs", "http://creativecommons.org/licenses/by-nc-nd/3.0");
    public static final Licence CC_CC0 = new CCLicence("CC0", "Creative Commons Public Domain Dedication", "http://creativecommons.org/publicdomain/zero/1.0");

    public static final Licence CC_PUBLIC_DOMAIN = new PublicDomainLicence();

    public static final List<Licence> CREATIVE_COMMONS = Arrays.asList(CC_BY, CC_BY_SA, CC_BY_ND, CC_BY_NC, CC_BY_NC_SA, CC_BY_NC_ND, CC_CC0, CC_PUBLIC_DOMAIN);

    private final String name;
    private final String url;

    Licence(String name, String url) {
        this.name = name;
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Licence)) {
            return false;
        }

        return name.equals(((Licence) o).name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }

    public static Licence creativeCommonsByUrl(String url) {
        Licence lic = UNKNOWN;

        if (url != null) {
            for (Licence cc : CREATIVE_COMMONS) {
                if (url.contains(cc.getUrl())) {
                    lic = cc;
                }
            }
        }

        return lic;
    }
}
