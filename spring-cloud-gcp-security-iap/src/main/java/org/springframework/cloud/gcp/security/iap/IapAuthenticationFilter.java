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

package org.springframework.cloud.gcp.security.iap;

import javax.servlet.http.HttpServletRequest;

import org.springframework.cloud.gcp.security.iap.jwt.JwtTokenVerifier;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;

public class IapAuthenticationFilter extends AbstractPreAuthenticatedProcessingFilter {

	public static final String HEADER_NAME = "x-goog-iap-jwt-assertion";

	private JwtTokenVerifier jwtTokenVerifier;

	public IapAuthenticationFilter(JwtTokenVerifier jwtTokenVerifier,
									AuthenticationManager authenticationManager,
									AuthenticationDetailsSource<HttpServletRequest, ?> detailsSource) {
		this.jwtTokenVerifier = jwtTokenVerifier;
		setAuthenticationManager(authenticationManager);
		setAuthenticationDetailsSource(detailsSource);
	}

	@Override
	protected IapAuthentication getPreAuthenticatedPrincipal(HttpServletRequest request) {
		String assertion = request.getHeader(HEADER_NAME);
		IapAuthentication authentication = null;

		if (assertion != null) {
			authentication = this.jwtTokenVerifier.verifyAndExtractPrincipal(assertion);
		}

		return authentication;
	}

	@Override
	protected Object getPreAuthenticatedCredentials(HttpServletRequest request) {
		return request.getHeader(HEADER_NAME);
	}
}
