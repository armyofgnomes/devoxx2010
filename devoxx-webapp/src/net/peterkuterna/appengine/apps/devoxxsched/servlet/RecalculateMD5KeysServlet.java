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
import java.util.List;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.peterkuterna.appengine.apps.devoxxsched.jdo.PMF;
import net.peterkuterna.appengine.apps.devoxxsched.model.RequestHash;
import net.peterkuterna.appengine.apps.devoxxsched.util.Md5Calculator;


@SuppressWarnings("serial")
public class RecalculateMD5KeysServlet extends HttpServlet {
	
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		final PersistenceManager pm = PMF.get().getPersistenceManager();
		
		final Query query = pm.newQuery(RequestHash.class);
		try {
			List<RequestHash> results = (List<RequestHash>) query.execute();
			for (RequestHash requestHash : results) {
				final Md5Calculator md5Calculator = new Md5Calculator(requestHash.getRequestUri());
				final String newMd5 = md5Calculator.calculateMd5();
				if (newMd5 != null && !newMd5.equals(requestHash.getMd5Hash())) {
					requestHash.setMd5Hash(newMd5);
					requestHash.setDate(new Date());
					pm.makePersistent(requestHash);
				}
			}
		} finally {
			query.closeAll();
			pm.close();
		}
	}
	
}
