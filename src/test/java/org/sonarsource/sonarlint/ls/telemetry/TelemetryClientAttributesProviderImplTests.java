/*
 * SonarLint Language Server
 * Copyright (C) 2009-2022 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarsource.sonarlint.ls.telemetry;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonarsource.sonarlint.core.client.api.common.RuleKey;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneRuleDetails;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneSonarLintEngine;
import org.sonarsource.sonarlint.ls.NodeJsRuntime;
import org.sonarsource.sonarlint.ls.connected.ProjectBindingManager;
import org.sonarsource.sonarlint.ls.settings.SettingsManager;
import org.sonarsource.sonarlint.ls.settings.WorkspaceSettings;
import org.sonarsource.sonarlint.ls.standalone.StandaloneEngineManager;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TelemetryClientAttributesProviderImplTests {

  private TelemetryClientAttributesProviderImpl underTest;
  private WorkspaceSettings workspaceSettings;
  private StandaloneSonarLintEngine standaloneSonarLintEngine;
  private Map<String, Object> additionalAttributes;

  @BeforeEach
  void init() {
    var nodeJsRuntime = mock(NodeJsRuntime.class);
    when(nodeJsRuntime.nodeVersion()).thenReturn("nodeVersion");
    var settingsManager = mock(SettingsManager.class);
    workspaceSettings = mock(WorkspaceSettings.class);
    var standaloneEngineManager = mock(StandaloneEngineManager.class);
    standaloneSonarLintEngine = mock(StandaloneSonarLintEngine.class);
    when(standaloneEngineManager.getOrCreateStandaloneEngine()).thenReturn(standaloneSonarLintEngine);
    when(settingsManager.getCurrentSettings()).thenReturn(workspaceSettings);
    when(workspaceSettings.getExcludedRules()).thenReturn(Collections.emptyList());
    when(workspaceSettings.getIncludedRules()).thenReturn(Collections.emptyList());
    additionalAttributes = new HashMap<>();
    underTest = new TelemetryClientAttributesProviderImpl(settingsManager, mock(ProjectBindingManager.class), nodeJsRuntime, standaloneEngineManager, additionalAttributes);
  }

  @Test
  void testNodeVersion() {
    assertThat(underTest.nodeVersion()).isPresent();
    assertThat(underTest.nodeVersion()).contains("nodeVersion");
  }

  @Test
  void testServerConnection() {
    assertThat(underTest.useSonarCloud()).isFalse();
    assertThat(underTest.usesConnectedMode()).isFalse();
  }

  @Test
  void testDevNotifications() {
    assertThat(underTest.devNotificationsDisabled()).isFalse();
  }

  @Test
  void testTelemetry() {
    assertThat(underTest.getDefaultDisabledRules()).isEmpty();
    assertThat(underTest.getNonDefaultEnabledRules()).isEmpty();
    assertThat(underTest.additionalAttributes()).isEmpty();
  }

  @Test
  void testAdditionalAttributes() {
    additionalAttributes.put("key", "value");
    assertThat(underTest.additionalAttributes()).containsEntry("key", "value");
  }

  @Test
  void testGetNonDefaultEnabledRules() {
    var ruleKey1 = mock(RuleKey.class);
    var ruleKey2 = mock(RuleKey.class);
    when(ruleKey1.toString()).thenReturn("ruleKey1");
    when(ruleKey2.toString()).thenReturn("ruleKey2");
    when(workspaceSettings.getIncludedRules()).thenReturn(List.of(ruleKey1, ruleKey2));
    when(standaloneSonarLintEngine.getAllRuleDetails()).thenReturn(Collections.emptyList());

    assertThat(underTest.getNonDefaultEnabledRules()).containsExactly("ruleKey2", "ruleKey1");
  }

  @Test
  void testGetNonDefaultEnabledFilteringRules() {
    var ruleKey1 = mock(RuleKey.class);
    var ruleKey2 = mock(RuleKey.class);
    when(ruleKey1.toString()).thenReturn("ruleKey1");
    when(ruleKey2.toString()).thenReturn("ruleKey2");
    var standaloneRule1 = mock(StandaloneRuleDetails.class);
    when(standaloneRule1.getKey()).thenReturn("ruleKey2");
    when(standaloneRule1.isActiveByDefault()).thenReturn(true);
    when(workspaceSettings.getIncludedRules()).thenReturn(List.of(ruleKey1, ruleKey2));
    when(standaloneSonarLintEngine.getAllRuleDetails()).thenReturn(List.of(standaloneRule1));

    assertThat(underTest.getNonDefaultEnabledRules()).containsExactly("ruleKey1");
  }

  @Test
  void testGetDefaultDisabledRules() {
    var ruleKey1 = mock(RuleKey.class);
    var ruleKey2 = mock(RuleKey.class);
    when(ruleKey1.toString()).thenReturn("ruleKey1");
    when(ruleKey2.toString()).thenReturn("ruleKey2");
    when(workspaceSettings.getExcludedRules()).thenReturn(List.of(ruleKey1, ruleKey2));
    when(standaloneSonarLintEngine.getAllRuleDetails()).thenReturn(Collections.emptyList());

    assertThat(underTest.getDefaultDisabledRules()).isEmpty();
  }

  @Test
  void testGetDefaultDisabledFilteringRules() {
    var ruleKey1 = mock(RuleKey.class);
    var ruleKey2 = mock(RuleKey.class);
    when(ruleKey1.toString()).thenReturn("ruleKey1");
    when(ruleKey2.toString()).thenReturn("ruleKey2");
    StandaloneRuleDetails standaloneRule1 = mock(StandaloneRuleDetails.class);
    when(standaloneRule1.getKey()).thenReturn("ruleKey1");
    when(standaloneRule1.isActiveByDefault()).thenReturn(true);
    when(workspaceSettings.getExcludedRules()).thenReturn(List.of(ruleKey1, ruleKey2));
    when(standaloneSonarLintEngine.getAllRuleDetails()).thenReturn(List.of(standaloneRule1));

    assertThat(underTest.getDefaultDisabledRules()).containsExactly("ruleKey1");
  }

}
