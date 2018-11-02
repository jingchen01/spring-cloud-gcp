/*
 *  Copyright 2018 original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.cloud.gcp.security.iap.jwk;

import java.net.URL;
import java.security.interfaces.ECPublicKey;
import java.text.ParseException;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.annotations.VisibleForTesting;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Cached registry implementation.
 * If key not found, attempts to re-download JWK registry, at most once per minute.
 *
 * Registry format: https://tools.ietf.org/html/rfc7517#section-8.1.2
 */
public class JwkRegistry {

	private static final Log LOGGER = LogFactory.getLog(JwkRegistry.class);

	// Wait at least 60 seconds before cache can be redownloaded.
	private static final int MIN_MS_BEFORE_RETRY = 60000;

	private Clock clock = Clock.systemUTC();

	private JwkLoader jwkLoader;

	private Map<String, JWK> keyCache = new HashMap<>();

	private long lastJwkStoreDownloadTimestamp;

	public JwkRegistry(URL verificationUrl) {
		this.jwkLoader = new JwkLoader(verificationUrl);
	}

	public ECPublicKey getPublicKey(String kid, String alg) {
		JWK jwk = this.keyCache.get(kid);
		if (jwk == null) {
			jwk = downloadJwkKeysIfCacheNotFresh(kid);
		}

		ECPublicKey ecPublicKey = null;

		if (jwk == null) {
			LOGGER.warn(String.format("JWK key [%s] not found.", kid));
		}
		else if (!jwk.getAlgorithm().getName().equals(alg)) {
			LOGGER.warn(String.format(
					"JWK key alorithm [%s] does not match expected algorithm [%s].", jwk.getAlgorithm(), alg));
		}
		else {
			try {
				ecPublicKey = ECKey.parse(jwk.toJSONString()).toECPublicKey();
			}
			catch (JOSEException | ParseException e) {
				LOGGER.warn("JWK Public key extraction failed.", e);
			}
		}

		return ecPublicKey;
	}

	private JWK downloadJwkKeysIfCacheNotFresh(String kid) {
		if (this.clock.millis() - this.lastJwkStoreDownloadTimestamp > MIN_MS_BEFORE_RETRY) {
			JWKSet jwkSet = this.jwkLoader.load();
			if (jwkSet != null) {
				this.lastJwkStoreDownloadTimestamp = this.clock.millis();
				this.keyCache = jwkSet.getKeys().stream().collect(Collectors.toMap(JWK::getKeyID, Function.identity()));
			}
		}

		return this.keyCache.get(kid);
	}

	@VisibleForTesting
	void setClock(Clock clock) {
		this.clock = clock;
	}
}
