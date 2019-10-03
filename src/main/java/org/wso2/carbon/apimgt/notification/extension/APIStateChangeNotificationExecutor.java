/*
 *  Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.apimgt.notification.extension;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.apimgt.api.WorkflowResponse;
import org.wso2.carbon.apimgt.impl.dto.WorkflowDTO;
import org.wso2.carbon.apimgt.impl.workflow.*;
import org.wso2.carbon.apimgt.notification.extension.notifier.AsyncEmailNotifier;
import org.wso2.carbon.context.CarbonContext;

import java.util.*;

/**
 * APIStateChangeNotificationExecutor sends emails when API LC change from given status to a status
 */
public class APIStateChangeNotificationExecutor extends WorkflowExecutor {


    private static final Log log = LogFactory.getLog(APIStateChangeNotificationExecutor.class);

    private String stateList;
    private String adminEmail;
    private String emailPassword;

    public String getAdminEmail() {
        return adminEmail;
    }

    public void setAdminEmail(String adminEmail) {
        this.adminEmail = adminEmail;
    }

    public String getEmailPassword() {
        return emailPassword;
    }

    public void setEmailPassword(String emailPassword) {
        this.emailPassword = emailPassword;
    }


    public String getStateList() {
        return stateList;
    }

    public void setStateList(String stateList) {
        this.stateList = stateList;
    }


    @Override
    public String getWorkflowType() {
        return WorkflowConstants.WF_TYPE_AM_API_STATE;
    }

    @Override
    public List<WorkflowDTO> getWorkflowDetails(String workflowStatus) throws WorkflowException {
        return Collections.emptyList();
    }

    @Override
    public WorkflowResponse execute(WorkflowDTO workflowDTO) throws WorkflowException {

        log.info("Executing API State change Workflow.");
        log.info("Execute workflowDTO " + workflowDTO.toString());


        if (stateList != null) {
            Map<String, List<String>> stateActionMap = getSelectedStatesToApprove();
            APIStateWorkflowDTO apiStateWorkFlowDTO = (APIStateWorkflowDTO) workflowDTO;

            if (stateActionMap.containsKey(apiStateWorkFlowDTO.getApiCurrentState().toUpperCase())
                && stateActionMap.get(apiStateWorkFlowDTO.getApiCurrentState().toUpperCase())
                .contains(apiStateWorkFlowDTO.getApiLCAction())) {
                AsyncEmailNotifier notfier = new AsyncEmailNotifier();
                notfier.setAdminEmail(getAdminEmail());
                notfier.setEmailPassword(getEmailPassword());
                notfier.setApiName(apiStateWorkFlowDTO.getApiName());
                notfier.setApiVersion(apiStateWorkFlowDTO.getApiVersion());
                notfier.setApiProvider(apiStateWorkFlowDTO.getApiProvider());
                notfier.setTenantDomain(CarbonContext.getThreadLocalCarbonContext().getTenantDomain());
                Thread notificationThread = new Thread(notfier);
                notificationThread.start();

            }
            super.execute(workflowDTO);
            // For any other states, act as simpleworkflow executor.
            workflowDTO.setStatus(WorkflowStatus.APPROVED);
            // calling super.complete() instead of complete() to act as the simpleworkflow executor
            complete(workflowDTO);

        } else {
            String msg = "State change list is not provided. Please check <stateList> element in ";
            log.error(msg);
            throw new WorkflowException(msg);
        }

        return new GeneralWorkflowResponse();
    }

    /**
     * Complete the API state change extension process.
     */
    @Override
    public WorkflowResponse complete(WorkflowDTO workflowDTO) throws WorkflowException {
        if (log.isDebugEnabled()) {
            log.debug("Completing API State change Workflow..");
            log.debug("response: " + workflowDTO.toString());
        }

        workflowDTO.setUpdatedTime(System.currentTimeMillis());
        super.complete(workflowDTO);
        return new GeneralWorkflowResponse();
    }

    /**
     * Read the user provided lifecycle states for the approval task. These are provided in the extension-extension.xml
     */
    private Map<String, List<String>> getSelectedStatesToApprove() {
        Map<String, List<String>> stateAction = new HashMap<String, List<String>>();
        // exract selected states from stateList and populate the map
        if (stateList != null) {
            // list will be something like ' Created:Publish,Created:Deploy as a Prototype,Published:Block ' String
            // It will have State:action pairs
            String[] statelistArray = stateList.split(",");
            for (int i = 0; i < statelistArray.length; i++) {
                String[] stateActionArray = statelistArray[i].split(":");
                if (stateAction.containsKey(stateActionArray[0].toUpperCase())) {
                    ArrayList<String> actionList = (ArrayList<String>) stateAction
                        .get(stateActionArray[0].toUpperCase());
                    actionList.add(stateActionArray[1]);
                } else {
                    ArrayList<String> actionList = new ArrayList<String>();
                    actionList.add(stateActionArray[1]);
                    stateAction.put(stateActionArray[0].toUpperCase(), actionList);
                }
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("selected states: " + stateAction.toString());
        }
        return stateAction;
    }

}
