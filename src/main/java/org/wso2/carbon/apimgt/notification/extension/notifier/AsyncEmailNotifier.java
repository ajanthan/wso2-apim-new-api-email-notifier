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
package org.wso2.carbon.apimgt.notification.extension.notifier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.impl.notification.NotifierConstants;
import org.wso2.carbon.apimgt.impl.token.ClaimsRetriever;
import org.wso2.carbon.apimgt.impl.utils.APIUtil;
import org.wso2.carbon.apimgt.notification.internal.ServiceReferenceHolder;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;

import javax.mail.*;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;


/**
 * This class is used to send email notifications
 */
public class AsyncEmailNotifier implements Runnable {
    private static final Log log = LogFactory.getLog(AsyncEmailNotifier.class);
    private static final String SUBSCRIBER_ROLE = "Internal/subscriber";
    private static final String claimsRetrieverImplClass = "org.wso2.carbon.apimgt.impl.token.DefaultClaimsRetriever";
    private String adminEmail;
    private String emailPassword;
    private String apiName;
    private String apiVersion;
    private String apiProvider;
    private String tenantDomain;
    private Set<String> emailSet = new HashSet<>();

    public AsyncEmailNotifier() {
        log.info("Email notifier is initiated..");
    }

    public String getTenantDomain() {
        return tenantDomain;
    }

    public void setTenantDomain(String tenantDomain) {
        this.tenantDomain = tenantDomain;
    }

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

    public String getApiName() {
        return apiName;
    }

    public void setApiName(String apiName) {
        this.apiName = apiName;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public String getApiProvider() {
        return apiProvider;
    }

    public void setApiProvider(String apiProvider) {
        this.apiProvider = apiProvider;
    }

    @Override
    public void run() {
        setThreadLocalContext(getTenantDomain());
        try {
            getAllSubscribers();
        } catch (UserStoreException e) {
            log.error("Unable to get all the subscribers", e);
        }
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        Session session = Session.getInstance(props, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(adminEmail, emailPassword);
            }
        });
        for (String subscriber : emailSet) {
            try {
                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(adminEmail));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(subscriber));
                message.setSubject("A New API is Available");
                message.setText("An API: " + apiName + "(" + apiVersion + ") from " + apiProvider + " is available now!");
                Transport.send(message);
                log.info("Sent email to notify API creation");
            } catch (AddressException e) {
                log.error("Wrong email address", e);
            } catch (MessagingException e) {
                log.error("Unable to send email", e);
            }
        }
    }

    private void getAllSubscribers() throws UserStoreException {
        UserRealm realm = ServiceReferenceHolder.getInstance().getRealmService().getBootstrapRealm();
        UserStoreManager userStoreManager = realm.getUserStoreManager();
        String[] subscribers = userStoreManager.getUserListOfRole(SUBSCRIBER_ROLE);
        ClaimsRetriever claimsRetriever = null;
        for (String subscriber : subscribers) {
            try {
                claimsRetriever = getClaimsRetriever(claimsRetrieverImplClass);
            } catch (IllegalAccessException e) {
                log.error("Unable to get Claim Retriever", e);
            } catch (InstantiationException e) {
                log.error("Unable to get Claim Retriever", e);
            } catch (ClassNotFoundException e) {
                log.error("Unable to get Claim Retriever", e);
            }
            try {
                claimsRetriever.init();
            } catch (APIManagementException e) {
                log.error("Unable to get Claim Retriever", e);
            }
            String email = null;
            try {
                email = claimsRetriever.getClaims(subscriber).get(NotifierConstants.EMAIL_CLAIM);
            } catch (APIManagementException e) {
                log.error("Unable to get email claim ", e);
            }

            if (email != null && !email.isEmpty()) {
                emailSet.add(email);
            }
        }
    }

    protected ClaimsRetriever getClaimsRetriever(String claimsRetrieverImplClass)
        throws IllegalAccessException, InstantiationException, ClassNotFoundException {
        return (ClaimsRetriever) APIUtil.getClassForName(claimsRetrieverImplClass).newInstance();
    }

    private void setThreadLocalContext(String tenantDomain) {
        PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenantDomain);
        PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenantDomain, true);

    }
}