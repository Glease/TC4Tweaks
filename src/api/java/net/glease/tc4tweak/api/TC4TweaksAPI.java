/* SPDX-License-Identifier: MIT */
/**
 * TC4Tweaks API
 * Copyright (c) 2025 Glease
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package net.glease.tc4tweak.api;

import java.util.ServiceLoader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TC4TweaksAPI {
    /**
     * The API version. API version will not necessarily advance along with main mod, but rather the every time
     * API has changed.
     */
    public static final String VERSION = "1.5.32";
    private static final Logger log = LogManager.getLogger("TC4TweaksAPI");
    private static BrowserPagingAPI browserPagingAPI;

    /**
     * Get an {@link BrowserPagingAPI}. Will provide a fallback version in case of main mod absence.
     * The dummy will respond to API calls as if TC4Tweaks does not exist, e.g. every tab will be on the same page
     */
    public static BrowserPagingAPI getBrowserPagingAPI() {
        if (browserPagingAPI == null) {
            BrowserPagingAPI api = getService(BrowserPagingAPI.class);
            if (api == null) {
                log.warn("Browser Paging API not available! Using dummy Browser Paging API.");
                browserPagingAPI = new DummyBrowserPagingAPI();
            } else {
                browserPagingAPI = api;
            }
        }
        return browserPagingAPI;
    }

    private static <T> T getService(Class<T> serviceClass) {
        ServiceLoader<T> loader = ServiceLoader.load(serviceClass);
        T first = null;
        for (T t : loader) {
            if (first == null) {
                first = t;
            }
            if (t.getClass().getName().startsWith("net.gase.tc4tweak")) {
                return t;
            }
        }
        return first;
    }

}
