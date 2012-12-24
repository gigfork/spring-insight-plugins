/**
 * Copyright (c) 2010-2012 Axon Framework All Rights Reserved.
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

package org.axonframework.insight.plugin.axon;

import java.util.Collections;

import org.axonframework.commandhandling.CommandMessage;
import org.axonframework.commandhandling.GenericCommandMessage;
import org.axonframework.commandhandling.annotation.CommandHandler;
import org.axonframework.unitofwork.DefaultUnitOfWork;
import org.axonframework.unitofwork.UnitOfWork;
import org.junit.Test;

import com.springsource.insight.collection.OperationCollectionAspectSupport;
import com.springsource.insight.collection.test.OperationCollectionAspectTestSupport;
import com.springsource.insight.intercept.operation.Operation;
import com.springsource.insight.intercept.operation.OperationMap;

public class CommandHandlerOperationCollectionAspectTest extends OperationCollectionAspectTestSupport {
    
    @Test
    public void annotatedCommandHandlerOperationCollected() {
        new TestCommandHandler().handleCommand(new TestCommand());
        
        Operation op = getLastEntered();

        assertEquals("org.axonframework.insight.plugin.axon.CommandHandlerOperationCollectionAspectTest$TestCommand", op.get("commandType"));
        assertEquals("handleCommand", op.getSourceCodeLocation().getMethodName());
    }
    
    @Test
    public void commandHandlerOperationCollected() throws Throwable {
        new TestCommandHandler().handle(
                new GenericCommandMessage<CommandHandlerOperationCollectionAspectTest.TestCommand>(
                        new TestCommand(), Collections.singletonMap("someKey", (Object) "someValue")),
                new DefaultUnitOfWork());
        
        Operation op = getLastEntered();

        assertEquals("org.axonframework.insight.plugin.axon.CommandHandlerOperationCollectionAspectTest$TestCommand", op.get("commandType"));
        assertEquals("handle", op.getSourceCodeLocation().getMethodName());
        OperationMap map = op.get("metaData", OperationMap.class);
        assertNotNull("CommandMessage metadata missing in operation", map);
        assertEquals(1, map.size());
        assertEquals("someValue", map.get("someKey"));
    }

    @Override
    public OperationCollectionAspectSupport getAspect() {
        return CommandHandlerOperationCollectionAspect.aspectOf();
    }
    
    static class TestCommand {}
    
    static class TestCommandHandler implements org.axonframework.commandhandling.CommandHandler<TestCommand> {
        @CommandHandler
        void handleCommand(TestCommand Command) {}

        public Object handle(CommandMessage<TestCommand> commandMessage, UnitOfWork unitOfWork) throws Throwable {
            return null;
        }
    }

}
