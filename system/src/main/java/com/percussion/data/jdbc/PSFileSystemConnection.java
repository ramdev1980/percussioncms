/*
 * Copyright 1999-2023 Percussion Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.percussion.data.jdbc;

import com.percussion.server.PSUserSession;
import com.percussion.server.PSUserSessionManager;

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

/**
 * The PSFileSystemConnection class implements connection handling for
 * the File System driver.
 * <p>
 * To handle commit and rollback for files, we must choose a strategy
 * for maintaining file consistency. Currently, there is no
 * transaction support.
 * <p>
 * When we are appending to a file, creating a new file, or completely
 * overwriting an existing file, the technique used in Notrix is quite
 * efficient. Notrix saves file positions (save points) each time a
 * transaction is started. If commit is called, the file position is
 * popped off the stack. If rollback is called, the file pointer is
 * set to the save point and the file is truncated.
 * <p>
 * The Notrix approach has two drawbacks.
 * <ol>
 * <li>it cannot be used when updating an existing file</li>
 * <li>the java.io classes do not support file truncation</li>
 * </ol>
 * To handle all scenarios, an alternative method can be followed. We will
 * make a copy of the existing file to a temporary file name. All changes
 * will be made against this file. As transactions are saved, it will be
 * used to overwrite the existing file. This is a less efficient model, but
 * covers all the bases and does not require native code.
 */
public class PSFileSystemConnection implements Connection
{
   /** 
    * @author chadloder
    * 
    * Creates a connection to the given catalog. This method has package
    * access and is meant to be called by the PSFileSystemDriver object
    * when a connection is requested from it.
    * 
    * @param   catalog   The directory to use as the base for this
    * file system connection.
    * 
    * @param   url   The URL associated with this connection. The URL
    * is of the "jdbc:psfilesystem" type. For informational puposes
    * only.
    * 
    * @param   driver   The driver instance that created this connection.
    *
    * @param   sessionId   The session id for the current PSUserSession object.
    * This must be specified if you are accessing file resources through
    * this connection.
    * 
    * @return A connection to the given catalog.
    * 
    */
   PSFileSystemConnection(
      String catalog,
      String url,
      String sessionId,
      PSFileSystemDriver driver)
      throws SQLException
   {
      // initialize all fields
      m_url = url;
      if (catalog == null)
         throw new SQLException("catalog is null");
      else
         m_catalog = catalog;
      m_driver = driver;
      m_userName = ""; // currently not used

      if (sessionId != null)
         m_session = PSUserSessionManager.getUserSession(sessionId);
   }

   /** 
    * @author chadloder
    * 
    * Gets the user name associated with this connection. Currently
    * returns the empty string.
    * 
    * @return   String   The username.
    * 
    */
   String getUserName()
   {
      return m_userName;
   }

   /** 
    * @author chadloder
    * 
    * Gets the driver that created this connection.
    * 
    * @return   PSJdbcDriver   The driver that created this connection.
    * 
    */
   PSJdbcDriver getDriver()
   {
      return m_driver;
   }

   /** 
    * @author chadloder
    * 
    * Gets the URL that this connection was created with.
    * 
    * @return   String   The URL that this connection was created
    * with.
    * 
    */
   String getURL()
   {
      return m_url;
   }

   /**
    * Create a File System statement execution object. This is the only
    * supported method at this time. Parameters are not supported.
    *
    * @return      the new Statement object (PSFileSystemStatement)
    *
    * @exception   SQLException   if an error occurs
    */
   public Statement createStatement() throws SQLException
   {
      checkClosed();
      return new PSFileSystemStatement(this);
   }

   /**
    * This is not currently supported.
    *
    * @param   sql   the SQL statement, optionally containing
    * placeholders ('?')
    *
    * @exception   SQLException   always thrown as this is not supported
    *
    * @deprecated Not supported
    */
   public PreparedStatement prepareStatement(String sql) throws SQLException
   {
      if (true)
         throw new SQLException("prepareStatement() not supported");
      checkClosed();
      return null;
   }

   /**
    * This is not currently supported.
    *
    * @param      sql            the SQL statement, optionally containing
    *                            placeholders ('?')
    *
    * @exception   SQLException   always thrown as this is not supported
    *
    * @deprecated Not supported
    */
   public CallableStatement prepareCall(String sql) throws SQLException
   {
      if (true)
         throw new SQLException("prepareCall() not supported");
      checkClosed();
      return null;
   }

   /**
    * This is not currently supported.
    *
    * @param      sql            the SQL statement, optionally containing
    *                            placeholders ('?')
    *
    * @exception   SQLException   always thrown as this is not supported
    *
    * @deprecated Not supported
    */
   public String nativeSQL(String sql) throws SQLException
   {
      if (true)
         throw new SQLException("prepareCall() not supported");
      checkClosed();
      return null;
   }

   /**
    * When this is enabled, all changes made to the file are written
    * to disk immediately. If this is disabled, changes will be cached
    * until rollback or commit is called.
    *
    * @param      autoCommit      <code>true</code> to enable auto-commit,
    *                            <code>false</code> to disable it
    *
    * @exception   SQLException   if an error occurs
    *
    * @deprecated Not supported
    */
   public void setAutoCommit(boolean autoCommit) throws SQLException
   {
      // do nothing, don't report errors
      //       if (true)
      //          throw new SQLException("not supported");
      checkClosed();
   }

   /**
    * Get the current auto-commit mode. When this is enabled, all changes
    * made to the file are written to disk immediately. If this is
    * disabled, changes will be cached until rollback or commit is called.
    *
    * @return                     <code>true</code> if auto-commit is enabled,
    *                            <code>false</code> otherwise
    *
    * @exception   SQLException   if an error occurs
    */
   public boolean getAutoCommit() throws SQLException
   {
      checkClosed();
      return false;
   }

   /**
    * Commits all changes made since the previous commit/rollback call.
    * This forces all stored changes made to the file to be written to disk.
    *
    * @exception   SQLException   if an error occurs
    *
    * @deprecated Not supported
    */
   public void commit() throws SQLException
   {
      if (true)
         throw new SQLException("not supported");
      checkClosed();
   }

   /**
    * Rolls back all changes made since the previous commit/rollback call.
    * This discards all stored changes made to the file, reverting it to its
    * previous state.
    *
    * @exception   SQLException   if an error occurs
    *
    * @deprecated Not supported
    */
   public void rollback() throws SQLException
   {
      if (true)
         throw new SQLException("not supported");
      checkClosed();
   }

   /**
    * Closes the file associated with this connection. Any subsequent
    * attempts to use this connection will cause an exception. Using this
    * method is more efficient than waiting for the garbage collector to
    * close the connection.
    *
    * @exception   SQLException   if an error occurs
    */
   public void close() throws SQLException
   {
      m_isClosed = true;
      m_metaData = null;
   }

   /**
    * Has the file associated with this connection already been closed?
    *
    * @return                     <code>true</code> if the file is closed,
    *                            <code>false</code> otherwise
    *
    * @exception   SQLException   if an error occurs
    */
   public boolean isClosed() throws SQLException
   {
      return m_isClosed;
   }

   /**
    * Accesses information about the files, directories, columns, etc.
    * associated with this connection.
    *
    * @return                     the DatabaseMetaData object
    *
    * @exception   SQLException   if an error occurs
    */
   public DatabaseMetaData getMetaData() throws SQLException
   {
      checkClosed();
      // only construct this when meta data is needed
      if (null == m_metaData)
         m_metaData = new PSFileSystemDatabaseMetaData(this);
      return m_metaData;
   }

   /**
    * Enables or disables read-only access to the file.
    *
    * @param      autoCommit      <code>true</code> to enable read-only
    *                            access, <code>false</code> to disable it
    *
    * @exception   SQLException   if an error occurs
    *
    * @deprecated Not supported
    */
   public void setReadOnly(boolean readOnly) throws SQLException
   {
      if (true)
         throw new SQLException("not supported");
      checkClosed();
   }

   /**
    * Is the file open in read-only mode?
    *
    * @return                     <code>true</code> if read-only is set,
    *                            <code>false</code> otherwise
    *
    * @exception   SQLException   if an error occurs
    */
   public boolean isReadOnly() throws SQLException
   {
      checkClosed();
      return false;
   }

   /**
    * Sets the directory to use as the base for this connection to the
    * file system.
    *
    * @param      catalog         the directory to use as the base
    *
    * @exception   SQLException   if an error occurs
    */
   public void setCatalog(String catalog) throws SQLException
   {
      checkClosed();
      m_catalog = catalog;
   }

   /**
    * Gets the directory being used as the base for this connection to the
    * file system.
    *
    * @return                     the directory being used as the base
    *
    * @exception   SQLException   if an error occurs
    */
   public String getCatalog() throws SQLException
   {
      checkClosed();
      return m_catalog;
   }

   /**
    * This is not currently supported.
    *
    * @param      level          the TRANSACTION_xxx isolation level
    *
    * @exception   SQLException   always thrown as this is not supported
    *
    * @deprecated Not supported
    */
   public void setTransactionIsolation(int level) throws SQLException
   {
      if (true)
         throw new SQLException("setTransactionIsolation() not supported");
      checkClosed();
   }

   /**
    * Gets the current transaction isolation level.
    *
    * @return      Connection.TRANSACTION_NONE is always returned as this
    *             driver does not support transaction isolation
    *
    * @exception   SQLException   if an error occurs
    */
   public int getTransactionIsolation() throws SQLException
   {
      checkClosed();
      return Connection.TRANSACTION_NONE;
   }

   /**
    * Gets the first warning reported on this connection. Warnings are
    * chained, so use the returned warning to iterate the warnings.
    *
    * @return      the first warning, or <code>null</code> of none exist
    *
    * @exception   SQLException   if an error occurs
    */
   public SQLWarning getWarnings() throws SQLException
   {
      checkClosed();
      return null;
   }

   /**
    * Removes all warnings associated with this connection.
    * @exception   SQLException   if an error occurs
    */
   public void clearWarnings() throws SQLException
   {
      checkClosed();
   }

   /**
    * JDBC 2.0 Creates a Statement object that will generate ResultSet
    * objects with the given type and concurrency. This method is the
    * same as the createStatement method above, but it allows the default
    * result set type and result set concurrency type to be overridden.
    *
    * @param   resultSetType            a result set type; see
    *                                  ResultSet.TYPE_XXX
    *
    * @param   resultSetConcurrency    a concurrency type; see
    *                                  ResultSet.CONCUR_XXX
    *
    * @return   a new Statement object
    *
    * @exception   SQLException   if a database access error occurs
    *
    * @deprecated Not supported
    */
   public Statement createStatement(
      int resultSetType,
      int resultSetConcurrency)
      throws SQLException
   {
      if (true)
         throw new SQLException("not supported");
      checkClosed();
      return null;
   }

   /**
    * JDBC 2.0 Creates a PreparedStatement object that will generate
    * ResultSet objects with the given type and concurrency. This method
    * is the same as the prepareStatement method above, but it allows the
    * default result set type and result set concurrency type to be
    * overridden.
    *
    * @param   resultSetType            a result set type; see
    *                                  ResultSet.TYPE_XXX
    *
    * @param   resultSetConcurrency    a concurrency type; see
    *                                  ResultSet.CONCUR_XXX
    *
    * @return   a new PreparedStatement object containing the pre-compiled
    *          SQL statement
    *
    * @exception   SQLException   if a database access error occurs
    *
    * @deprecated Not supported
    */
   public PreparedStatement prepareStatement(
      String sql,
      int resultSetType,
      int resultSetConcurrency)
      throws SQLException
   {
      if (true)
         throw new SQLException("not supported");
      checkClosed();
      return null;
   }

   /**
    * JDBC 2.0 Creates a CallableStatement object that will generate
    * ResultSet objects with the given type and concurrency. This method
    * is the same as the prepareCall method above, but it allows the
    * default result set type and result set concurrency type to be
    * overridden.
    *
    * @param   resultSetType            a result set type; see
    *                                  ResultSet.TYPE_XXX
    *
    * @param   resultSetConcurrency    a concurrency type; see
    *                                  ResultSet.CONCUR_XXX
    *
    * @return   a new CallableStatement object containing the pre-compiled
    *          SQL statement
    *
    * @exception   SQLException   if a database access error occurs
    *
    * @deprecated Not supported
    */
   public CallableStatement prepareCall(
      String sql,
      int resultSetType,
      int resultSetConcurrency)
      throws SQLException
   {
      if (true)
         throw new SQLException("not supported");
      checkClosed();
      return null;
   }

   /**
    * JDBC 2.0 Gets the type map object associated with this connection.
    * Unless the application has added an entry to the type map, the map
    * returned will be empty.
    *
    * @return   the java.util.Map object associated with this Connection
    *          object
    *
    * @deprecated Not supported
    */
   public Map getTypeMap() throws SQLException
   {
      checkClosed();
      return new HashMap();
   }

   /**
    * JDBC 2.0 Installs the given type map as the type map for this
    * connection. The type map will be used for the custom mapping of
    * SQL structured types and distinct types.
    *
    * @param   map      the java.util.Map object to install as the
    *                   replacement for this Connection object's default
    *                   type map
    *
    * @deprecated Not supported
    */
   public void setTypeMap(Map<String, Class<?>> map) throws SQLException
   {
      if (true)
         throw new SQLException("not supported");
      checkClosed();
   }

   protected void finalize() throws Throwable
   {
      close();
      super.finalize();
   }

   /**
    * Gets the effective user session for use with this driver.
    *
    * @author   chadloder
    * 
    * @version 1.8 1999/07/13
    * 
    * @return   PSUserSession
    */
   PSUserSession getUserSession()
   {
      return m_session;
   }

   protected void checkClosed() throws SQLException
   {
      if (isClosed())
         throw new SQLException("Connection has been closed");
   }

   /**
    * the effective user session for this connection. Can be <CODE>null</CODE>.
    */
   protected PSUserSession m_session;

   /**
    * the starting directory for the connection
    */
   protected String m_catalog;

   /**
    * the metadata for the start directory
    */
   protected DatabaseMetaData m_metaData;

   /**
    * the driver that created this connection
    */
   protected PSJdbcDriver m_driver;

   /**
    * the user name for this connection. currently empty
    */
   protected String m_userName;

   /**
    * the URL for this connection
    */
   protected String m_url;

   /**
    * true if this connection is closed
    */
   private boolean m_isClosed;

   /* (non-Javadoc)
    * @see java.sql.Connection#createStatement(int, int, int)
    */
   public Statement createStatement(
      int resultSetType,
      int resultSetConcurrency,
      int resultSetHoldability)
      throws SQLException
   {
      throw new UnsupportedOperationException("This method is not yet implemented");
   }

   /* (non-Javadoc)
    * @see java.sql.Connection#getHoldability()
    */
   public int getHoldability() throws SQLException
   {
      throw new UnsupportedOperationException("This method is not yet implemented");
   }

   /* (non-Javadoc)
    * @see java.sql.Connection#prepareCall(java.lang.String, int, int, int)
    */
   public CallableStatement prepareCall(
      String sql,
      int resultSetType,
      int resultSetConcurrency,
      int resultSetHoldability)
      throws SQLException
   {
      throw new UnsupportedOperationException("This method is not yet implemented");
   }

   /* (non-Javadoc)
    * @see java.sql.Connection#prepareStatement(java.lang.String, int, int, int)
    */
   public PreparedStatement prepareStatement(
      String sql,
      int resultSetType,
      int resultSetConcurrency,
      int resultSetHoldability)
      throws SQLException
   {
      throw new UnsupportedOperationException("This method is not yet implemented");
   }

   /* (non-Javadoc)
    * @see java.sql.Connection#prepareStatement(java.lang.String, int)
    */
   public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys)
      throws SQLException
   {
      throw new UnsupportedOperationException("This method is not yet implemented");
   }

   /* (non-Javadoc)
    * @see java.sql.Connection#prepareStatement(java.lang.String, int[])
    */
   public PreparedStatement prepareStatement(String sql, int[] columnIndexes)
      throws SQLException
   {
      throw new UnsupportedOperationException("This method is not yet implemented");
   }

   /* (non-Javadoc)
    * @see java.sql.Connection#prepareStatement(java.lang.String, java.lang.String[])
    */
   public PreparedStatement prepareStatement(String sql, String[] columnNames)
      throws SQLException
   {
      throw new UnsupportedOperationException("This method is not yet implemented");
   }

   /* (non-Javadoc)
    * @see java.sql.Connection#setHoldability(int)
    */
   public void setHoldability(int holdability) throws SQLException
   {
      throw new UnsupportedOperationException("This method is not yet implemented");
   }

   /* (non-Javadoc)
    * @see java.sql.Connection#setSavepoint()
    */
   public Savepoint setSavepoint() throws SQLException
   {
      throw new UnsupportedOperationException("This method is not yet implemented");
   }

   /* (non-Javadoc)
    * @see java.sql.Connection#releaseSavepoint(java.sql.Savepoint)
    */
   public void releaseSavepoint(Savepoint savepoint) throws SQLException
   {
      throw new UnsupportedOperationException("This method is not yet implemented");
   }

   /* (non-Javadoc)
    * @see java.sql.Connection#rollback(java.sql.Savepoint)
    */
   public void rollback(Savepoint savepoint) throws SQLException
   {
      throw new UnsupportedOperationException("This method is not yet implemented");
   }

   /* (non-Javadoc)
    * @see java.sql.Connection#setSavepoint(java.lang.String)
    */
   public Savepoint setSavepoint(String name) throws SQLException
   {
      throw new UnsupportedOperationException("This method is not yet implemented");
   }

   public Array createArrayOf(String typeName, Object[] elements)
         throws SQLException
   {
      return null;
   }

   public Blob createBlob() throws SQLException
   {
      throw new UnsupportedOperationException("This method is not yet implemented");
   }

   public Clob createClob() throws SQLException
   {
      throw new UnsupportedOperationException("This method is not yet implemented");
   }

   public NClob createNClob() throws SQLException
   {
      throw new UnsupportedOperationException("This method is not yet implemented");
   }

   public SQLXML createSQLXML() throws SQLException
   {
      throw new UnsupportedOperationException("This method is not yet implemented");
   }

   public Struct createStruct(String typeName, Object[] attributes)
         throws SQLException
   {
      throw new UnsupportedOperationException("This method is not yet implemented");
   }

   public Properties getClientInfo() throws SQLException
   {
      throw new UnsupportedOperationException("This method is not yet implemented");
   }

   public String getClientInfo(String name) throws SQLException
   {
      throw new UnsupportedOperationException("This method is not yet implemented");
   }

   public boolean isValid(int timeout) throws SQLException
   {
      throw new UnsupportedOperationException("This method is not yet implemented");
   }

   public void setClientInfo(Properties properties)
         throws SQLClientInfoException
   {
      throw new UnsupportedOperationException("This method is not yet implemented");
   }

   public void setClientInfo(String name, String value)
         throws SQLClientInfoException
   {
      throw new UnsupportedOperationException("This method is not yet implemented");
   }

   public boolean isWrapperFor(Class<?> iface) throws SQLException
   {
      throw new UnsupportedOperationException("This method is not yet implemented");
   }

   public <T> T unwrap(Class<T> iface) throws SQLException
   {
      throw new UnsupportedOperationException("This method is not yet implemented");
   }

   @Override
   public void setSchema(String schema) throws SQLException
   {
      throw new UnsupportedOperationException("This method is not yet implemented");
   }

   @Override
   public String getSchema() throws SQLException
   {
      throw new UnsupportedOperationException("This method is not yet implemented");
   }

   @Override
   public void abort(Executor executor) throws SQLException
   {
      throw new UnsupportedOperationException("This method is not yet implemented");
   }

   @Override
   public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException
   {
      throw new UnsupportedOperationException("This method is not yet implemented");
   }

   @Override
   public int getNetworkTimeout() throws SQLException
   {
      throw new UnsupportedOperationException("This method is not yet implemented");
   }
}
