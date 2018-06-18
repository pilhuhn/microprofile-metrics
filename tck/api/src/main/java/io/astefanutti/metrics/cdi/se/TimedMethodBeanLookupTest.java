/**
 * Copyright © 2013 Antonin Stefanutti (antonin.stefanutti@gmail.com)
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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.astefanutti.metrics.cdi.se;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.atomic.AtomicLong;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.Timer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class TimedMethodBeanLookupTest {

    private final static String TIMER_NAME = MetricRegistry.name(TimedMethodBean1.class, "timedMethod");

    private final static AtomicLong TIMER_COUNT = new AtomicLong();

    @Deployment
    static Archive<?> createTestArchive() {
        return ShrinkWrap.create(JavaArchive.class)
            // Test bean
            .addClass(TimedMethodBean1.class)
            // Bean archive deployment descriptor
            .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Inject
    private MetricRegistry registry;

    @Inject
    private Instance<TimedMethodBean1> instance;

    @Test
    @InSequence(1)
    public void timedMethodNotCalledYet() {
        // Get a contextual instance of the bean
        TimedMethodBean1 bean = instance.get();

        assertThat("Timer is not registered correctly", registry.getTimers(), hasKey(TIMER_NAME));
        Timer timer = registry.getTimers().get(TIMER_NAME);

        // Make sure that the timer hasn't been called yet
        assertThat("Timer count is incorrect", timer.getCount(), is(equalTo(TIMER_COUNT.get())));
    }

    @Test
    @InSequence(2)
    public void callTimedMethodOnce() {
        // Get a contextual instance of the bean
        TimedMethodBean1 bean = instance.get();

        assertThat("Timer is not registered correctly", registry.getTimers(), hasKey(TIMER_NAME));
        Timer timer = registry.getTimers().get(TIMER_NAME);

        // Call the timed method and assert it's been timed
        bean.timedMethod();

        // Make sure that the timer has been called
        assertThat("Timer count is incorrect", timer.getCount(), is(equalTo(TIMER_COUNT.incrementAndGet())));
    }

    @Test
    @InSequence(3)
    public void removeTimerFromRegistry() {
        // Get a contextual instance of the bean
        TimedMethodBean1 bean = instance.get();

        assertThat("Timer is not registered correctly", registry.getTimers(), hasKey(TIMER_NAME));
        Timer timer = registry.getTimers().get(TIMER_NAME);

        // Remove the timer from metrics registry
        assertTrue(registry.unregister(TIMER_NAME, MetricType.TIMER));

        try {
            // Call the timed method and assert an exception is thrown
            bean.timedMethod();
        }
        catch (RuntimeException cause) {
            assertThat(cause, is(instanceOf(IllegalStateException.class)));
            assertThat(cause.getMessage(), is(equalTo("No timer with name [" + TIMER_NAME + "] found in registry [" + registry + "]")));
            // Make sure that the timer hasn't been called
            assertThat("Timer count is incorrect", timer.getCount(), is(equalTo(TIMER_COUNT.get())));
            return;
        }

        fail("No exception has been re-thrown!");
    }
}
