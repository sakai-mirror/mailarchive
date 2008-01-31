/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2003, 2004, 2005, 2006 The Sakai Foundation.
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

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.db.api.SqlReader;
import org.sakaiproject.db.api.SqlService;
import org.sakaiproject.entity.api.Entity;
import org.sakaiproject.message.api.Message;
import org.sakaiproject.message.api.MessageChannel;
import org.sakaiproject.message.api.MessageChannelEdit;
import org.sakaiproject.message.api.MessageEdit;
import org.sakaiproject.time.api.Time;
// HACK PACKAGE
// import org.sakaiproject.util.BaseDbDoubleStorage;
import org.sakaiproject.util.StorageUser;
import org.sakaiproject.util.Xml;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

// TODO: Remove this:
import org.sakaiproject.exception.PermissionException;

import org.sakaiproject.javax.PagingPosition;
import org.sakaiproject.javax.Filter;
import org.sakaiproject.util.FormattedText;
import org.sakaiproject.util.StringUtil;
import org.sakaiproject.mailarchive.api.MailArchiveMessage;

/**
 * <p>
 * DbMailArchiveService fills out the BaseMailArchiveService with a database implementation.
 * </p>
 * <p>
 * The sql scripts in src/sql/chef_mailarchive.sql must be run on the database.
 * </p>
 */
public class DbMailArchiveService extends BaseMailArchiveService
{
	/** Our logger. */
	private static Log M_log = LogFactory.getLog(DbMailArchiveService.class);

	/** The name of the db table holding mail archive channels. */
	protected String m_cTableName = "MAILARCHIVE_CHANNEL";

	/** The name of the db table holding mail archive messages. */
	protected String m_rTableName = "MAILARCHIVE_MESSAGE";

	/** If true, we do our locks in the remote database, otherwise we do them here. */
	protected boolean m_locksInDb = true;

	protected static final String[] FIELDS = { "MESSAGE_DATE", "OWNER", "DRAFT", "PUBVIEW" };

	/**********************************************************************************************************************************************************************************************************************************************************
	 * Constructors, Dependencies and their setter methods
	 *********************************************************************************************************************************************************************************************************************************************************/

	/** Dependency: SqlService */
	protected SqlService m_sqlService = null;

	/**
	 * Dependency: SqlService.
	 * 
	 * @param service
	 *        The SqlService.
	 */
	public void setSqlService(SqlService service)
	{
		m_sqlService = service;
	}

	/**
	 * Configuration: set the table name for the container.
	 * 
	 * @param path
	 *        The table name for the container.
	 */
	public void setContainerTableName(String name)
	{
		m_cTableName = name;
	}

	/**
	 * Configuration: set the table name for the resource.
	 * 
	 * @param path
	 *        The table name for the resource.
	 */
	public void setResourceTableName(String name)
	{
		m_rTableName = name;
	}

	/**
	 * Configuration: set the locks-in-db
	 * 
	 * @param path
	 *        The storage path.
	 */
	public void setLocksInDb(String value)
	{
		m_locksInDb = new Boolean(value).booleanValue();
	}

	/** Set if we are to run the to-draft/owner conversion. */
	protected boolean m_convertToDraft = false;

	/**
	 * Configuration: run the to-draft/owner conversion
	 * 
	 * @param value
	 *        The conversion desired value.
	 */
	public void setConvertDraft(String value)
	{
		m_convertToDraft = new Boolean(value).booleanValue();
	}

	/** Configuration: to run the ddl on init or not. */
	protected boolean m_autoDdl = false;

	/**
	 * Configuration: to run the ddl on init or not.
	 * 
	 * @param value
	 *        the auto ddl value.
	 */
	public void setAutoDdl(String value)
	{
		m_autoDdl = new Boolean(value).booleanValue();
	}

	/**********************************************************************************************************************************************************************************************************************************************************
	 * Init and Destroy
	 *********************************************************************************************************************************************************************************************************************************************************/

	/**
	 * Final initialization, once all dependencies are set.
	 */
	public void init()
	{
		try
		{
			// if we are auto-creating our schema, check and create
			if (m_autoDdl)
			{
				m_sqlService.ddl(this.getClass().getClassLoader(), "sakai_mailarchive");
			}

			super.init();

			M_log.info("init(): tables: " + m_cTableName + " " + m_rTableName + " locks-in-db: " + m_locksInDb);

			// convert?
			if (m_convertToDraft)
			{
				m_convertToDraft = false;
				convertToDraft();
			}
		}
		catch (Throwable t)
		{
			M_log.warn("init(): ", t);
		}
	}

	/**********************************************************************************************************************************************************************************************************************************************************
	 * BaseMessageService extensions
	 *********************************************************************************************************************************************************************************************************************************************************/
     
	/**
	 * Construct a Storage object.
	 * 
	 * @return The new storage object.
	 */
	protected Storage newStorage()
	{
		return new DbStorage(this);

	} // newStorage
  
    // TODO: Remove these horrible hacks once we change the Storage API  
    // These do nothing unless they are overridden in the derived class
    // In this case it is DbMailArchiveService.java
    @Override
    public int countMessagesService(BaseMessageChannelEdit chan) throws PermissionException
	{
			System.out.println("DB countMessagesService");
			return ((DbStorage) m_storage).countMessages(chan);
	}

		/**
		 * @inheritDoc
		 */
    @Override
	public int countMessagesSearchService(BaseMessageChannelEdit chan, String search) throws PermissionException
	{
			System.out.println("DB countMessagesServiceSearch search="+search);
			return ((DbStorage) m_storage).countMessagesSearch(chan, search);
	}
    
    @Override
    public List getMessagesSearchService(BaseMessageChannelEdit chan, String search, boolean asc, PagingPosition pager)
        throws PermissionException
	{
			System.out.println("DB getMessagesSearchService search="+search);
			return ((DbStorage) m_storage).getMessages(chan, search, asc, pager);
	}
    // TODO: End temporary workwround to make this backwards compatible
    
	/**********************************************************************************************************************************************************************************************************************************************************
	 * Storage implementation
	 *********************************************************************************************************************************************************************************************************************************************************/

	protected class DbStorage extends BaseDbDoubleStorage implements Storage
	{
		/**
		 * Construct.
		 * 
		 * @param user
		 *        The StorageUser class to call back for creation of Resource and Edit objects.
		 */
		public DbStorage(StorageUser user)
		{
			super(m_cTableName, "CHANNEL_ID", m_rTableName, "MESSAGE_ID", "CHANNEL_ID", "MESSAGE_DATE", "OWNER", "DRAFT",
					"PUBVIEW", FIELDS, m_locksInDb, "channel", "message", user, m_sqlService);

		} // DbStorage
        
        // A call back to match before the XML is parsed and turned into a Resource
        // If we can decide here - it is more efficient that sending the XML through SAX
        // But for now - we wil do SAX - at least SAX does not waste too much memory
        // App server CPU is not the most critical resource.  So we return "I don't know"
        @Override
        public int matchXml(String theXml, String search)
        {
            return 0;  // Neither yes (1) or no(-1)
        }
        
        // A call back to allow us to do our own search decisions after
        // an mesage has been parsed and converted to a resource
        @Override
        public boolean matchEntity(Entity entry, String search)
        {
            // System.out.println("DB MAIL ENTITY MATCHING!!!! "+search+" ent="+entry);
            MailArchiveMessage msg = (MailArchiveMessage) entry;
			if (StringUtil.containsIgnoreCase(msg.getMailArchiveHeader().getSubject(), search)
					|| StringUtil.containsIgnoreCase(msg.getMailArchiveHeader().getFromAddress(), search)
					|| StringUtil.containsIgnoreCase(FormattedText.convertFormattedTextToPlaintext(msg.getBody()), search))
			{
                // System.out.println("YAYAYAYAYAY");
				return true;
			}
            return false;
        }
        
		/** Channels * */

		public boolean checkChannel(String ref)
		{
			return super.getContainer(ref) != null;
		}

		public MessageChannel getChannel(String ref)
		{
			return (MessageChannel) super.getContainer(ref);
		}

		public List getChannels()
		{
			return super.getAllContainers();
		}

		public MessageChannelEdit putChannel(String ref)
		{
			return (MessageChannelEdit) super.putContainer(ref);
		}

		public MessageChannelEdit editChannel(String ref)
		{
			return (MessageChannelEdit) super.editContainer(ref);
		}

		public void commitChannel(MessageChannelEdit edit)
		{
			super.commitContainer(edit);
		}

		public void cancelChannel(MessageChannelEdit edit)
		{
			super.cancelContainer(edit);
		}

		public void removeChannel(MessageChannelEdit edit)
		{
			super.removeContainer(edit);
		}

		public List getChannelIdsMatching(String root)
		{
			return super.getContainerIdsMatching(root);
		}

		/** messages * */

		public boolean checkMessage(MessageChannel channel, String id)
		{
			return super.checkResource(channel, id);
		}

		public Message getMessage(MessageChannel channel, String id)
		{
			return (Message) super.getResource(channel, id);
		}

		public List getMessages(MessageChannel channel)
		{
			return super.getAllResources(channel);
		}

		public List getMessages(MessageChannel channel,String search, boolean asc, PagingPosition pager)
		{
			return super.getAllResources(channel, null, search, asc, pager);
		}
        
		public int countMessages(MessageChannel channel)
		{
			return super.countResources(channel);
		}
        
        // TODO: remove countMessagesSearch after we have search filters
		public int countMessagesSearch(MessageChannel channel, String search)
		{
			return super.countResources(channel, null, search);
		}

        public int countMessages(MessageChannel channel, Filter filter)
        {
			return super.countResources(channel, filter, null);
		}
        
		public MessageEdit putMessage(MessageChannel channel, String id)
		{
			return (MessageEdit) super.putResource(channel, id, null);
		}

		public MessageEdit editMessage(MessageChannel channel, String id)
		{
			return (MessageEdit) super.editResource(channel, id);
		}

		public void commitMessage(MessageChannel channel, MessageEdit edit)
		{
			super.commitResource(channel, edit);
		}

		public void cancelMessage(MessageChannel channel, MessageEdit edit)
		{
			super.cancelResource(channel, edit);
		}

		public void removeMessage(MessageChannel channel, MessageEdit edit)
		{
			super.removeResource(channel, edit);
		}

		public List getMessages(MessageChannel channel, Time afterDate, int limitedToLatest, String draftsForId, boolean pubViewOnly)
		{
			return super.getResources(channel, afterDate, limitedToLatest, draftsForId, pubViewOnly);
		}
 
        public List getMessages(MessageChannel channel, Time afterDate, int limitedToLatest, String draftsForId, boolean pubViewOnly, PagingPosition pager)
		{
			return super.getResources(channel, afterDate, limitedToLatest, draftsForId, pubViewOnly);
		}

	} // DbStorage

	/**
	 * fill in the draft and owner db fields
	 */
	protected void convertToDraft()
	{
		M_log.info("convertToDraft");

		try
		{
			// get a connection
			final Connection connection = m_sqlService.borrowConnection();
			boolean wasCommit = connection.getAutoCommit();
			connection.setAutoCommit(false);

			// read all message records that need conversion
			String sql = "select CHANNEL_ID, MESSAGE_ID, XML from " + m_rTableName /* + " where OWNER is null" */;
			m_sqlService.dbRead(connection, sql, null, new SqlReader()
			{
				private int count = 0;

				public Object readSqlResultRecord(ResultSet result)
				{
					try
					{
						// create the Resource from the db xml
						String channelId = result.getString(1);
						String messageId = result.getString(2);
						String xml = result.getString(3);

						// read the xml
						Document doc = Xml.readDocumentFromString(xml);

						// verify the root element
						Element root = doc.getDocumentElement();
						if (!root.getTagName().equals("message"))
						{
							M_log.warn("convertToDraft(): XML root element not message: " + root.getTagName());
							return null;
						}
						Message m = new BaseMessageEdit(null, root);

						// pick up the fields
						String owner = m.getHeader().getFrom().getId();
						boolean draft = m.getHeader().getDraft();

						// update
						String update = "update " + m_rTableName
								+ " set OWNER = ?, DRAFT = ? where CHANNEL_ID = ? and MESSAGE_ID = ?";
						Object fields[] = new Object[4];
						fields[0] = owner;
						fields[1] = (draft ? "1" : "0");
						fields[2] = channelId;
						fields[3] = messageId;
						boolean ok = m_sqlService.dbWrite(connection, update, fields);

						if (!ok)
							M_log.info("convertToDraft: channel: " + channelId + " message: " + messageId + " owner: "
									+ owner + " draft: " + draft + " ok: " + ok);

						count++;
						if (count % 100 == 0)
						{
							M_log.info("convertToDraft: " + count);
						}
						return null;
					}
					catch (Throwable ignore)
					{
						return null;
					}
				}
			});

			connection.commit();
			connection.setAutoCommit(wasCommit);
			m_sqlService.returnConnection(connection);
		}
		catch (Throwable t)
		{
			M_log.warn("convertToDraft: failed: " + t);
		}

		M_log.info("convertToDraft: done");
	}

} // DbCachedMailArchiveService

