/*
 * Copyright 2017 Hewlett Packard Enterprise Development Company, L.P.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License.
 */

package com.hp.autonomy.frontend.find.idol.dashboards;

import com.hp.autonomy.frontend.find.core.test.AbstractFindIT;
import com.hp.autonomy.frontend.find.core.web.FindController;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.RequestBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DirtiesContext
public class IdolDashboardControllerIT extends AbstractFindIT {
    private static final String UUID = "b1c71fad-a52d-47bf-a121-f71500bd7ddb";
    private static final String DASHBOARD_CONFIG = TEST_DIR + "/customization/dashboards.json";
    private static final String DASHBOARD_CONFIG_BACKUP = TEST_DIR + "/customization/dashboards.json.bak";
    private static final String REPLACEMENT_CONFIG = "target/classes/IdolDashboardControllerIT-Config-1.json";
    private static final String RENAMED_DASHBOARD_CONFIG = "target/classes/IdolDashboardControllerIT-Config-2.json";
    private static final String DASHBOARD_URL = "http://abc.xyz/public/dashboards/Figs";
    private static final String ROOT_URL = "/";

    @Override
    @Before
    public void setUp() {
        super.setUp();
        // Back up current config file
        copyFileReplaceExisting(DASHBOARD_CONFIG, DASHBOARD_CONFIG_BACKUP);
    }

    @After
    public void tearDown() {
        // Restore original config file, delete backup
        moveFileReplaceExisting(DASHBOARD_CONFIG_BACKUP, DASHBOARD_CONFIG);
    }

    @Test
    public void testReloadConfig() throws Exception {
        final String replacementConfigContents = new String(Files.readAllBytes(Paths.get(REPLACEMENT_CONFIG)), "UTF-8");
        assertTrue("Replacement config contains UUID", replacementConfigContents.contains(UUID));

        currentConfigContainsUUID(false);
        // Replace current config file
        copyFileReplaceExisting(REPLACEMENT_CONFIG, DASHBOARD_CONFIG);

        triggerConfigReload(DASHBOARD_URL, DASHBOARD_URL);

        currentConfigContainsUUID(true);
    }

    @Test
    public void testReloadConfigDetectsDashboardWasRenamed() throws Exception {
        // Replace current config file
        copyFileReplaceExisting(REPLACEMENT_CONFIG, DASHBOARD_CONFIG);

        triggerConfigReload(DASHBOARD_URL, DASHBOARD_URL);

        // Load another config file, where the "Figs" dashboard had been deleted
        copyFileReplaceExisting(RENAMED_DASHBOARD_CONFIG, DASHBOARD_CONFIG);

        triggerConfigReload(DASHBOARD_URL, ROOT_URL);
    }

    private void currentConfigContainsUUID(final boolean expected) throws Exception {
        final RequestBuilder requestToAppPath = get(FindController.APP_PATH)
                .with(authentication(adminAuth()));

        mockMvc.perform(requestToAppPath)
                .andExpect(status().isOk())
                .andDo(mvcResult -> {
                    final String response = mvcResult.getResponse().getContentAsString();
                    assertEquals(expected, response.contains(UUID));
                });
    }

    private void triggerConfigReload(final String referer, final String redirect) throws Exception {
        mockMvc.perform(
                get(IdolDashboardController.DASHBOARD_CONFIG_RELOAD_PATH)
                        .with(authentication(adminAuth()))
                        .header("referer", referer))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl(redirect));
    }

    private void copyFileReplaceExisting(final String from, final String to) {
        final Path fromPath = Paths.get(from);
        final Path toPath = Paths.get(to);
        try {
            Files.copy(fromPath, toPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (final IOException e) {
            throw new IllegalStateException("Could not replace current dashboard config file", e);
        }
    }

    private void moveFileReplaceExisting(final String from, final String to) {
        final Path fromPath = Paths.get(from);
        final Path toPath = Paths.get(to);
        try {
            Files.move(fromPath, toPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (final IOException e) {
            throw new IllegalStateException("Could not replace current dashboard config file", e);
        }
    }
}
