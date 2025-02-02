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

package com.percussion.utils.io;

import com.percussion.utils.tools.IPSUtilsConstants;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;


public class PathUtils {

    public static final String ROOT_FOLDER_CHECK_ITEM = "rxconfig";
    public static final String USER_FOLDER_CHECK_ITEM = ".perc_config";


    private static ThreadLocal<File> threadRxRoot = new ThreadLocal<>();


    /**
     * Logger
     */
    private static final Logger log = LogManager.getLogger(IPSUtilsConstants.SERVER_LOG);

    private static volatile File RX_DIR = null;

    /**
     * Name of system property storing Rhythmyx root directory name.
     * Used for testing.
     * @see #getRxDir(String)
     */
    public static final String DEPLOY_DIR_PROP = "rxdeploydir";

    public static File getRxDir(){
        return getRxDir(null);
    }

    /***
     * Provides a container neutral method for detecting the RX install directory.
     *
     * If the rxdeployerdir system property is defined that will be used.
     *
     * Auto detection may be overridden by calling the setRxDir method.
     *
     * If the directory is not detected then the user.home system folder is returned.
     *
     * @param startDirectory Defaults to current working directory.  may be null
     */
    public static File getRxDir(String startDirectory)
    {
        String baseDir=startDirectory;
        if(StringUtils.isEmpty(startDirectory)) {
            baseDir = ".";
        }
        File threadFile = threadRxRoot.get();
        if (RX_DIR==null && threadFile==null)
        {
            synchronized (PathUtils.class)
            {
                if (RX_DIR==null)
                {
                    final String dir = System.getProperty(DEPLOY_DIR_PROP);
                    if (!StringUtils.isBlank(dir))
                    {
                        File file = new File(dir).getAbsoluteFile();
                        if (!file.exists())
                            throw new IllegalArgumentException("Cannot set rxDir system property "+DEPLOY_DIR_PROP+" value "+file.getAbsolutePath()+" does not exist");
                        RX_DIR = file;
                        log.info("RxDir set through "+DEPLOY_DIR_PROP+" system property to "+file.getAbsolutePath());
                        return file;
                    }
                    else{
                        RX_DIR =  autoDetectRxInstallDir(new File(baseDir).getAbsolutePath());
                        if(RX_DIR.isDirectory() && (RX_DIR.getName().equals(ROOT_FOLDER_CHECK_ITEM)
                        ||
                                RX_DIR.getName().equals(USER_FOLDER_CHECK_ITEM))) {

                            if( RX_DIR.getName().equals(ROOT_FOLDER_CHECK_ITEM)){
                                RX_DIR = RX_DIR.getParentFile();
                            }else{
                                RX_DIR = new File(String.format("%s%s%s", System.getProperty("user.home"),
                                        File.separator, USER_FOLDER_CHECK_ITEM));
                            }
                        }
                        log.info("RxDir set through detection of current path to "+RX_DIR.getAbsolutePath());
                    }
                }
            }
        }
        return threadFile!=null?threadFile:RX_DIR;
    }

    public static Path getRxPath()
    {
        return getRxDir(null).toPath();
    }

    public static void clearRxDir(){
        RX_DIR = null;
        threadRxRoot.remove();
    }

    public static void setRxDir(File rxDir) {
        if (rxDir==null)
            throw new IllegalArgumentException("Cannot set RxDir to null value");
          synchronized (PathUtils.class)
            {
                if(!rxDir.exists())
                {
                    throw new IllegalArgumentException("rxDir "+rxDir+" does not exist");
                }
                if (RX_DIR==null)
                {
                    RX_DIR = rxDir;
                } else if (RX_DIR.getAbsolutePath().equals(rxDir.getAbsolutePath()))
                {
                    log.debug("Rx dir already set to "+rxDir+" ignoring");
                }
                else
                {
                    log.warn("Changing rxDir value from "+RX_DIR.getAbsolutePath()+" to "+rxDir.getAbsolutePath()+" This has a system wide effect");
                    RX_DIR=rxDir;
                }
            }

    }

    public static void setThreadOnlyRxDir(File rxDir) {
            threadRxRoot.set(rxDir);
    }

    public static void unsetThreadOnlyRxDir(File rxDir) {
        threadRxRoot.remove();
    }


    /***
     * Given the current directory attempts to locate the rx install dir by walking
     * the parent folder path.
     *
     * @return a valid installation dir.
     */
    private static File autoDetectRxInstallDir(String path) {

        if (path==null)
            throw new IllegalArgumentException("Cannot find ObjectStore directory or " + DEPLOY_DIR_PROP + " system property not set");

        File candidate = new File(path).getAbsoluteFile();

        File[] children = candidate.listFiles();
        for(int i = 0; i < children.length;i++){
            if(children[i].isDirectory() && (children[i].getName().equals(ROOT_FOLDER_CHECK_ITEM)
                    ||
                    children[i].getName().equals(USER_FOLDER_CHECK_ITEM))){
                    return children[i];
            }
        }

        File newCandidate=null;
        if(!StringUtils.isEmpty(candidate.getAbsoluteFile().getParent())) {
             newCandidate = autoDetectRxInstallDir(candidate.getAbsoluteFile().getParent());
        }

        if(newCandidate == null || !newCandidate.exists() || !newCandidate.isDirectory() || newCandidate.getPath().equals(File.separator)){
            log.warn("Unable to auto-detect the installation directory, defaulting to user.home");
            return new File(String.format("%s%s%s",System.getProperty("user.home"),File.separator,".perc_config"));
        }

        return newCandidate;
    }

    /**
     * Return the string path to a file or directory that is relative to the
     * rhythmyx root
     *
     * @param path Input path, never <code>null</code> or empty
     * @return a fully qualified path to the file or directory desired. If it
     * doesn't exist, an IllegalArgumentException is thrown.
     */
    public static String getRxFile(String path)
    {
        if (path == null)
        {
            throw new IllegalArgumentException("path may not be null");
        }
        File item = new File(getRxDir(null), path);
        if (item.exists() == false)
        {
            throw new IllegalArgumentException("file does not exist: " + item.getAbsolutePath());
        }
        return item.getAbsolutePath();
    }

    /**
     * Find the ant jar by path pattern to avoid hard coding / forcing version.
     *
     * @param execPath Folder containing the jar
     * @param fileNameWithPattern A File name with a glob pattern like perc-ant-*.jar
     * @return Path to the ant jar
     * @throws IOException
     */
    public static Path getVersionLessJarFilePath(Path execPath, String fileNameWithPattern) throws IOException {
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(execPath.toAbsolutePath(), fileNameWithPattern)) {
            List<Path> paths = new ArrayList<>();
            for (Path path : ds) {
                paths.add(path);
            }
            if (paths.isEmpty()) {
                throw new IOException(fileNameWithPattern + " not found.");
            } else if (paths.size() == 1) {
                return paths.get(0);
            } else {
                System.out.println("Warning: Multiple " + fileNameWithPattern + " jars found, selecting the first one: " + paths.get(0).toAbsolutePath().toString());
                return paths.get(0);
            }
        }
    }

}
