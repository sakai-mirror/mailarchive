/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2004, 2005, 2006 The Sakai Foundation.
 *
 * Licensed under the Educational Community License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.opensource.org/licenses/ecl1.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 **********************************************************************************/
package org.sakaiproject.mailarchive.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.sakaiproject.mailarchive.api.MailArchiveServiceSql;



/**
 * methods for accessing mail archive data in a database.
 */
public class MailArchiveServiceSqlDefault implements MailArchiveServiceSql {

   // logger
   protected final transient Log logger = LogFactory.getLog(getClass());



   /**
    * return the sql statement which retrieves some fields from the specified table (mailarchive_message).
    */
   public String getFieldsSql(String table) {
      return "select CHANNEL_ID, MESSAGE_ID, XML from " + table /* + " where OWNER is null" */;
   }

   /**
    * return the sql statement which updates some fields in the specified table (mailarchive_message).
    */
   public String getUpdateFieldsSql(String table) {
      return "update " + table + " set OWNER = ?, DRAFT = ? where CHANNEL_ID = ? and MESSAGE_ID = ?";
   }
}
