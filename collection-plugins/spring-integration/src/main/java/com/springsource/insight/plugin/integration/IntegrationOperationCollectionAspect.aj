/**
 * Copyright (c) 2009-2011 VMware, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.springsource.insight.plugin.integration;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.SuppressAjWarnings;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.context.IntegrationObjectSupport;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.handler.ServiceActivatingHandler;
import org.springframework.integration.transformer.Transformer;

import com.springsource.insight.intercept.operation.Operation;
import com.springsource.insight.intercept.operation.OperationType;
import com.springsource.insight.util.StringUtil;

/**
 * Aspect collecting operations for Spring Integration frames; channels, 
 * message handlers, and transformers are supported.
 *
 */
public aspect IntegrationOperationCollectionAspect extends AbstractIntegrationOperationCollectionAspect {
    public static final OperationType TYPE = OperationType.valueOf("integration_operation");

    public static final String CHANNEL = "Channel";
    public static final String MESSAGE_HANDLER = "MessageHandler";
    public static final String TRANSFORMER = "Transformer";

    private Map<String, Operation> opCache = new ConcurrentHashMap<String, Operation>();
    
    interface HasMethodAndObject {}
    declare parents: ServiceActivatingHandler implements HasMethodAndObject;
    
    private Object HasMethodAndObject.insightObject = null;
    private String HasMethodAndObject.insightMethodName = null;
    
    public void   HasMethodAndObject.__setInsightObject(Object obj) { this.insightObject = obj; }
    public Object HasMethodAndObject.__getInsightObject() { return this.insightObject; }
    
    public void   HasMethodAndObject.__setInsightMethodName(Method method) { this.insightMethodName = method != null ? method.getName() : null; }
    public void   HasMethodAndObject.__setInsightMethodName(String method) { this.insightMethodName = method; }
    public String HasMethodAndObject.__getInsightMethodName() { return insightMethodName; }
    
    @SuppressAjWarnings
    ServiceActivatingHandler around(Object object) : 
                   call(public ServiceActivatingHandler.new(Object)) && args(object) {
        
        ServiceActivatingHandler handler = proceed(object);
        
        if (handler instanceof HasMethodAndObject) {
            ((HasMethodAndObject) handler).__setInsightObject(object);
        }
        
        return handler;
    }
    
    @SuppressAjWarnings
    ServiceActivatingHandler around(Object object, Method method) : 
        call(public ServiceActivatingHandler.new(Object, Method)) && args(object, method) {
        
        ServiceActivatingHandler handler = proceed(object, method);
        
        if (handler instanceof HasMethodAndObject) {
            ((HasMethodAndObject) handler).__setInsightObject(object);
            ((HasMethodAndObject) handler).__setInsightMethodName(method);
        }
        
        return handler;
    }
    
    @SuppressAjWarnings
    ServiceActivatingHandler around(Object object, String method) : 
        call(public ServiceActivatingHandler.new(Object, String)) && args(object, method) {
        
        ServiceActivatingHandler handler = proceed(object, method);
        
        if (handler instanceof HasMethodAndObject) {
            ((HasMethodAndObject) handler).__setInsightObject(object);
            ((HasMethodAndObject) handler).__setInsightMethodName(method);
        }
        
        return handler;
    }
    
    public pointcut collectionPoint() : 
        // filter out anonymous channels
        execution (* org.springframework.integration.context.IntegrationObjectSupport+.*(..))
            &&
        (execution(boolean org.springframework.integration.MessageChannel+.send(org.springframework.integration.Message, long))
            || execution(void org.springframework.integration.core.MessageHandler+.handleMessage(org.springframework.integration.Message))
            || execution(* org.springframework.integration.transformer.Transformer+.transform(org.springframework.integration.Message)));

    private Operation createCachedOperation(Object target, String beanName) {
        String beanType = target.getClass().getSimpleName();
        String generalType = "unknown";
        String label = "Spring Integration";
        
        if (target instanceof HasMethodAndObject) {
            generalType = MESSAGE_HANDLER;
            label = beanName;
        } else if (target instanceof MessageChannel) {
            generalType = CHANNEL;
            label = beanType + "#" + beanName;
        } else if (target instanceof MessageHandler) {
            generalType = MESSAGE_HANDLER;
            label = beanType + "#" + beanName;
        } else if (target instanceof Transformer) {
            generalType = TRANSFORMER;
            label = beanType + "#" + beanName;
        }
        
        Operation cachedOp = new Operation()
                .type(TYPE)
                .label(label)
                .put("siComponentType", generalType)
                .put("siSpecificType", beanType)
                .put("beanName", beanName);
        opCache.put(beanName, cachedOp);
        return cachedOp;
    }

    @Override
    protected Operation createOperation(JoinPoint jp) {
        Object target = jp.getTarget();
        String beanName = null;
        
        if (target instanceof HasMethodAndObject) {
            HasMethodAndObject methodAndObject = (HasMethodAndObject) target;
            
            Object obj = methodAndObject.__getInsightObject();
            
            if (obj != null) {
                String method = methodAndObject.__getInsightMethodName();
                
                String simpleName = obj.getClass().getSimpleName();
                int length = simpleName.length();
                
                if (!StringUtil.isEmpty(method)) {
                    length += 1 + method.length();
                }
                
                StringBuilder builder = new StringBuilder(length);
                
                builder.append(simpleName);
                
                if (!StringUtil.isEmpty(method)) {
                    builder.append('#').append(method);
                }
                
                beanName = builder.toString();
            }
        } else {
            beanName = ((IntegrationObjectSupport) target).getComponentName();
        }
        
        Operation cachedOp = null;

        if (beanName == null) {
            beanName = "anonymous";
        } else {
            cachedOp = opCache.get(beanName);
        }

        if (cachedOp == null) {
            cachedOp = createCachedOperation(target, beanName);
        }

        // Payload type can be different on every message
        Message<?> message = (Message<?>) jp.getArgs()[0];
        Class<?> payloadClazz = message.getPayload().getClass();
        String payloadType;
        if (!payloadClazz.isArray()) {
            payloadType = payloadClazz.getName();
        } else {
            payloadType = payloadClazz.getComponentType().getSimpleName() + "[]";
        }

        // The id is different on every message
        String idHeader = message.getHeaders().getId().toString();

        Operation op = new Operation().copyPropertiesFrom(cachedOp);

        op.label(cachedOp.getLabel())
                .type(TYPE)
                .put("payloadType", payloadType)
                .put("idHeader", idHeader);
    	return op;
    }

    @Override
    public boolean isEndpoint() {
        return true;
    }

}
