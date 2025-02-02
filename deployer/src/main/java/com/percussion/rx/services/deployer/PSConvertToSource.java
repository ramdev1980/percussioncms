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
package com.percussion.rx.services.deployer;

import com.percussion.deployer.client.IPSDeployConstants;
import com.percussion.deployer.server.PSDeploymentHandler;
import com.percussion.deployer.server.uninstall.PSPackageUninstaller;
import com.percussion.server.PSServer;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.pkginfo.IPSPkgInfoService;
import com.percussion.services.pkginfo.PSPkgInfoServiceLocator;
import com.percussion.services.pkginfo.data.PSPkgInfo;
import com.percussion.services.pkginfo.data.PSPkgInfo.PackageType;
import com.percussion.utils.types.PSPair;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * This class handles converting installed packages to source.
 * 
 * @author lteixeira
 * 
 */
public class PSConvertToSource
{
   /**
    * Converts installed package to a source package.
    * 
    * @param packageName The package name.
    * 
    * @return PsPair of boolean <code>true</code> is successful 
    * and String of error message never <code>null</code> 
    * or empty.
    */
   public PSPair<Boolean, String> convert(String packageName) throws PSNotFoundException {
      PSPair<Boolean,String> message = new PSPair<>();
      
      IPSPkgInfoService pkgSvc = PSPkgInfoServiceLocator.getPkgInfoService();
      PSPkgInfo pkgInfo = pkgSvc.findPkgInfo(packageName);
      
      //Verify Package exists
      if (pkgInfo == null ||
            pkgInfo.getLastAction() == PSPkgInfo.PackageAction.UNINSTALL ||
            pkgInfo.getType() == PSPkgInfo.PackageType.DESCRIPTOR)
      {
         message.setFirst(false);
         message.setSecond("Package " + packageName + " is not a valid/successfully installed Package");
         
         return message;
      }

      //Check to see if package is locked
      if (!pkgInfo.isEditable())
      {
         message.setFirst(false);
         message.setSecond("Package " + packageName + " is locked and cannot be converted to source package");
         
         return message;
      }
      
      //Find Package
      File packageFile = getPkgFile(packageName);
      
      //Fail if missing Package
      if(packageFile == null)
      {
         message.setFirst(false);
         message.setSecond("Cannot find package file: " +  packageFile.getPath());
         
         return message;
      }
      
      PSPair<Boolean,String> msg = moveDescriptor(packageName);
      if (msg != null)
         return msg;
      
      //Delete config files
      PSPackageUninstaller uninstaller = new PSPackageUninstaller();
      uninstaller.deleteConfigFiles(pkgInfo);

      packageFile.delete();
      
      //Flip DB
      convertDB(packageName);
      
      //Successful
      message.setFirst(true);
      message.setSecond("Package " + packageName + " successfully convert to source");
      
      return message;
   }

   /**
    * Move the descriptor from {@link PSDeploymentHandler#IMPORT_ARCHIVE_DIR} to
    * {@link PSDeploymentHandler#EXPORT_DESC_DIR}. 
    * 
    * @param packageName the package name, assumed not blank.
    * 
    * @return an error message if there is any error. It may be
    * <code>null</code> if there is no error.
    */
   private PSPair<Boolean, String> moveDescriptor(String packageName)
   {
      PSPair<Boolean,String> message = new PSPair<>();
      
      // get the converted descriptor
      File descFile = new File(PSDeploymentHandler.IMPORT_ARCHIVE_DIR,
            packageName + ".xml");
      if (!descFile.exists())
      {
         message.setFirst(false);
         message.setSecond("Cannot find descriptor file: " + descFile.getPath());
         
         return message;
      }
      
      // move the descriptor file to the target (source) location
      File tgtFile = new File(PSDeploymentHandler.EXPORT_DESC_DIR, packageName
            + ".xml");
      try
      {
         FileUtils.copyFile(descFile, tgtFile);
      }
      catch (IOException e1)
      {
         message.setFirst(false);
         message.setSecond("Error saving Descriptor: "
               + e1.getLocalizedMessage());
         
         return message;
      }
      // remove the original descriptor file
      descFile.delete();
      
      return null;
   }
   
   /**
    * Loads the persisted directory path if it exists.
    * 
    * @param pkgname - name of package
    * 
    * @return the directory path or <code>null</code> if
    * not found.
    */
   private File getPkgFile(String pkgname)
   {
      String pkgFile = PSServer.getRxDir() + File.separator + "Packages" + 
              File.separator + "Percussion" + File.separator + pkgname + 
              IPSDeployConstants.ARCHIVE_EXTENSION;
      File file = new File(pkgFile);
      if(!file.exists()){
    	  //check Packages
    	  pkgFile = PSServer.getRxDir() + File.separator + "Packages" + File.separator + pkgname + 
                  IPSDeployConstants.ARCHIVE_EXTENSION;
          file = new File(pkgFile);
          if(!file.exists()){
        	  pkgFile = PSServer.getRxDir() + File.separator + "rx_resources" + File.separator + "widgets_generated" + File.separator + pkgname + 
                      IPSDeployConstants.ARCHIVE_EXTENSION;
              file = new File(pkgFile);       	  
          }else{
        	  return null;
          }
      }
         
      return file;
   }
   
   /**
    * Flip the DB pkgInfo type from Package to Descriptor
    * 
    * @param pkgName - package name
    */
   protected void convertDB(String pkgName) throws PSNotFoundException {
      IPSPkgInfoService pkgService = PSPkgInfoServiceLocator
            .getPkgInfoService();

      PSPkgInfo pkgInfo = pkgService.findPkgInfo(pkgName);
      PSPkgInfo pkgInfoMod = pkgService.loadPkgInfoModifiable(pkgInfo.getGuid());
      pkgInfoMod.setType(PackageType.DESCRIPTOR);
      pkgService.savePkgInfo(pkgInfoMod);
   }

}
