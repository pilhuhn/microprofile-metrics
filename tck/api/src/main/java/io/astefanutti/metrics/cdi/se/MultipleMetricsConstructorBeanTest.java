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
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.eclipse.microprofile.metrics.MetricRegistry;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.astefanutti.metrics.cdi.se.util.MetricsUtil;

@RunWith(Arquillian.class)
public class MultipleMetricsConstructorBeanTest {

    private String absoluteMetricName(String name) {
        return MetricsUtil.absoluteMetricName(MultipleMetricsConstructorBean.class, name);
    }

    @Deployment
    static Archive<?> createTestArchive() {
        return ShrinkWrap.create(JavaArchive.class)
                // Test bean
                .addClasses(MultipleMetricsConstructorBean.class, MetricsUtil.class)
                // Bean archive deployment descriptor
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Inject
    private MetricRegistry registry;

    @Inject
    private Instance<MultipleMetricsConstructorBean> instance;

    // @Test
    // @InSequence(1)
    // public void metricsConstructorNotCalledYet() {
    // assertThat("Metrics are not registered correctly",
    // registry.getMetrics().keySet(), is(empty()));
    // }

    @Test
    @InSequence(1)
    public void metricsConstructorCalled() {
        long count = 1L + Math.round(Math.random() * 10);
        for (int i = 0; i < count; i++) {
            instance.get();
        }

        // Make sure that the metrics have been called
        assertThat("HitCounter count is incorrect", registry.getHitCounters().get(absoluteMetricName("counter")).getCount(), is(equalTo(count)));
        assertThat("ParCounter count is incorrect", registry.getParallelCounters().get(absoluteMetricName("counter")).getCount(), is(0L));
        assertThat("Meter count is incorrect", registry.getMeters().get(absoluteMetricName("meter")).getCount(), is(equalTo(count)));
        assertThat("Timer count is incorrect", registry.getTimers().get(absoluteMetricName("timer")).getCount(), is(equalTo(count)));
    }
}
