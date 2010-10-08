/*
 * Copyright 2010 Peter Kuterna
 *
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

package net.peterkuterna.appengine.apps.devoxxsched.servlet;

import java.io.IOException;
import java.util.Date;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.peterkuterna.appengine.apps.devoxxsched.jdo.PMF;
import net.peterkuterna.appengine.apps.devoxxsched.model.RequestHash;
import net.peterkuterna.appengine.apps.devoxxsched.util.Md5Calculator;


@SuppressWarnings("serial")
public class RequestMD5KeyServlet extends HttpServlet {
	
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		final String requestUri = req.getParameter("requestUri");
		
		if (requestUri != null && requestUri.startsWith("http://cfp.devoxx.com")) {
			final PersistenceManager pm = PMF.get().getPersistenceManager();
	
			RequestHash requestHash = getRequestHash(requestUri);
			if (requestHash == null) {
				final Md5Calculator md5Calculator = new Md5Calculator(requestUri);
				final String md5 = md5Calculator.calculateMd5();
				if (md5 != null) {
					requestHash = new RequestHash(requestUri, md5, new Date());
					if (requestHash != null 
							&& requestHash.getMd5Hash() != null) {
						try {
							pm.makePersistent(requestHash);
						} finally {
							pm.close();
						}
					}
				}
			}
			
			if (requestHash != null) {
				resp.getWriter().println(requestHash.getMd5Hash());
			} else {
				resp.getWriter().println("NOK");
			}
		}
	}
	
	private RequestHash getRequestHash(String requestUri) {
		final PersistenceManager pm = PMF.get().getPersistenceManager();
		final Query query = pm.newQuery(RequestHash.class);
		query.setFilter("requestUri == requestUriParam");
		query.declareParameters("String requestUriParam");
		query.setUnique(true);
		
		RequestHash requestHash = null;
		try {
			requestHash = (RequestHash) query.execute(requestUri);
		} finally {
			query.closeAll();
		}
		
		return requestHash;
	}

}
