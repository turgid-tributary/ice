/**
 * Copyright (C) 2025 New York City Department of Health and Mental Hygiene, Bureau of Immunization
 * Contributions by HLN Consulting, LLC
 * <p>
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version. You should have received a copy of the GNU Lesser
 * General Public License along with this program. If not, see <http://www.gnu.org/licenses/> for more
 * details.
 * <p>
 * The above-named contributors (HLN Consulting, LLC) are also licensed by the New York City
 * Department of Health and Mental Hygiene, Bureau of Immunization to have (without restriction,
 * limitation, and warranty) complete irrevocable access and rights to this project.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; THE
 * <p>
 * SOFTWARE IS PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING,
 * BUT NOT LIMITED TO, WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE COPYRIGHT HOLDERS, IF ANY, OR DEVELOPERS BE LIABLE FOR
 * ANY CLAIM, DAMAGES, OR OTHER LIABILITY OF ANY KIND, ARISING FROM, OUT OF, OR IN CONNECTION WITH
 * THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * <p>
 * For more information about this software, see http://www.hln.com/ice or send
 * correspondence to ice@hln.com.
 */

package org.cdsframework.ice.service.configurations;

import java.io.InputStream;
import java.util.Date;
import java.util.function.Function;

import org.cdsframework.ice.config.IceProperties;
import org.cdsframework.ice.util.KnowledgeModuleUtils;
import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.builder.ReleaseId;
import org.kie.api.runtime.KieContainer;
import org.opencds.config.api.KnowledgeLoader;
import org.opencds.config.api.model.KMId;
import org.opencds.config.api.model.KnowledgeModule;
import org.opencds.config.api.model.impl.KMIdImpl;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class IceKnowledgeLoader implements KnowledgeLoader<InputStream, IceKnowledgePackage>
{
    @Setter
    private static IceProperties iceProperties;

    // droolsPath removed - no longer needed

    @Override
    public IceKnowledgePackage loadKnowledgePackage(final KnowledgeModule knowledgeModule,
            final Function<KnowledgeModule, InputStream> knowledgeModuleInputSreamFunction)
    {
        final String _METHODNAME = "loadKnowledgePackage(): ";
        if (knowledgeModule == null)
        {
            final String lErrStr = "KnowledgeModule not supplied";
            log.error(_METHODNAME + lErrStr);
            throw new IllegalArgumentException(lErrStr);
        }

        KMId lKMId = knowledgeModule.getKMId();
        if (lKMId == null)
        {
            final String lErrStr = "KMId not populated";
            log.error(_METHODNAME + lErrStr);
            throw new IllegalArgumentException(lErrStr);
        }

        if (lKMId.getScopingEntityId() == null || lKMId.getBusinessId() == null || lKMId.getVersion() == null)
        {
            final String errStr = "ScopingID and/or BusinessID and/or Version not specified";
            log.error(_METHODNAME + errStr);
            throw new IllegalArgumentException(errStr);
        }

        String lRequestedKmId = KnowledgeModuleUtils.returnStringRepresentationOfKnowledgeModuleName(lKMId.getScopingEntityId(),
                lKMId.getBusinessId(), lKMId.getVersion());
        if (lRequestedKmId.equals("org.nyc.cir^ICE^1.0.0"))
        {
            lRequestedKmId = "gov.nyc.cir^ICE^1.0.0";
            lKMId = KMIdImpl.create("gov.nyc.cir", "ICE", "1.0.0");
        }
        log.debug("Loading pre-compiled ICE Drools KJAR - Knowledge Module {}", lRequestedKmId);

        final String lBaseRulesScopingKmId =
                KnowledgeModuleUtils.returnStringRepresentationOfKnowledgeModuleName(
                        iceProperties.getIceBaseRulesScopingEntityId(),
                        lKMId.getBusinessId(),
                        iceProperties.getIceBaseRulesVersion());

        final KieServices kieServices = KieServices.Factory.get();

        // Load pre-compiled KJAR using its Maven coordinates
        final ReleaseId releaseId = kieServices.newReleaseId(
                "org.cdsframework",
                "ice-rules-kjar",
                "4.0.1-SNAPSHOT");

        final KieContainer kieContainer = kieServices.newKieContainer(releaseId);
        final KieBase kieBase = kieContainer.getKieBase();

        log.debug("Date/Time {}; Base Rules Scoping Km Id: {}; Loaded from KJAR: {}",
                lRequestedKmId, lBaseRulesScopingKmId, new Date());

        return new IceKnowledgePackage(lKMId, kieBase);
    }
}
